# 2. Technische Anforderungen - Monatsblitz Android App

Dieses Dokument beschreibt die technischen Details, Datenstrukturen und Algorithmen zur Umsetzung der funktionalen Anforderungen.

## 2.1 Domain-Modell: Player
Das Modell für Spieler unterstützt die Identifikation und Synchronisation mit dem WordPress-Backend.

| Feld | Typ | Beschreibung |
| :--- | :--- | :--- |
| `localId` | Long | Eindeutige Kennung in der lokalen Room-Datenbank (Primary Key). |
| `remoteKey` | Long? | ID des Spielers in der WordPress-Datenbank (`null` bei noch nicht synchronisierten Spielern). |
| `firstName` | String | Vorname des Spielers. |
| `lastName` | String | Nachname des Spielers. |

### Namenseindeutigkeit (Constraint & Validierung)
*   **Eindeutigkeit:** Es darf keine zwei Spieler mit gleichem Namen geben (Kombination aus `firstName` und `lastName`).
*   **Validierung:**
    *   Vor dem Anlegen oder Speichern eines Spielers werden Vor- und Nachname getrimmt (führende/nachfolgende Whitespaces entfernt).
    *   Die Eindeutigkeitsprüfung erfolgt case-insensitive über `firstName` und `lastName`.
    *   In der lokalen Room-Datenbank wird ein `Index(value = ["firstName", "lastName"], unique = true)` geführt.
    *   Sowohl im UI-Layer (`AddPlayerViewModel`) als auch im Use Case (`CreatePlayerUseCase`) wird vor dem Einfügen geprüft, ob bereits ein Spieler mit identischem Vor- und Nachnamen existiert. Bei Duplikaten wird die Operation abgewiesen.

## 2.2 Zustandsmodell der App
Der Turnier-Lifecycle wird über Zustände gesteuert, die im `TournamentStorage` (DataStore) und in der Datenbank persistiert werden.

| Zustand | Trigger | Auswirkung |
| :--- | :--- | :--- |
| **Setup** | App-Start ohne aktives Turnier | Anzeige `TournamentSetupScreen`, Spieler-Sync aktiv. |
| **Running** | Klick "Turnier starten" oder Fortsetzen | Schreibzugriff auf Kreuztabelle, manueller und automatischer Sync möglich. |
| **Conflict** | Datenkonflikt beim Read-before-Write Sync | Anzeige des Konflikt-Dialogs mit Optionen (Force Read, Force Write, Keine Aktion), Stopp des automatischen Syncs bis zur Entscheidung. |
| **Finalized** | Erfolgreicher Aufruf von `/finalize` | Schreibschutz für alle Geräte, Wechsel zur Ranking-Ansicht. |

## 2.3 Algorithmen

### 2.3.1 Paarungs-Generierung (Round Robin)
Bei Turnierstart werden alle Paarungen nach dem Prinzip "Jeder-gegen-Jeden" erzeugt.
*   **Einrundig:** `n * (n - 1) / 2` Spiele.
*   **Doppelrundig:** `n * (n - 1)` Spiele (Hin- und Rückspiel mit vertauschten Farben/Legs).
*   Die Generierung erfolgt initial im `CreateNewGamesUseCase`.

### 2.3.2 Synchronisations- und Merge-Algorithmus (Read-before-Write)
Um gleichzeitige Erfassungen auf mehreren Geräten ohne Datenverlust zu ermöglichen, folgt der Sync einem strikten **Read-before-Write**-Ablauf:

1.  **Read (Server-Abruf):**
    *   Abruf des aktuellen Turnierstands vom Backend (`GET /games/{tournament_id}`).
2.  **Klassifizierung & Merge:**
    *   Die lokalen Spiele (Room) und Remote-Spiele werden anhand der Paarung (z. B. Spieler-IDs und Runde) abgeglichen:
        *   **Remote-Aktualisierung (Merge):** Liegt remote ein Ergebnis vor, das lokal noch nicht erfasst oder identisch ist (lokal `dirty = 0`), wird der Remote-Stand lokal übernommen.
        *   **Lokale Neuerung:** Wurde das Spiel nur lokal geändert (`dirty = 1`) und ist remote noch unberührt bzw. ohne Ergebnis, wird das Spiel für den Upload vorgemerkt.
        *   **Konflikt:** Wurde das Spiel lokal geändert (`dirty = 1`) **und** auf dem Server liegt ein abweichendes Ergebnis vor, liegt ein Datenkonflikt vor.
3.  **Konfliktbehandlung:**
    *   Wird mindestens ein Konflikt festgestellt, wechselt der Zustand zu **Conflict**.
    *   Dem Benutzer wird ein Dialog mit den abweichenden Ergebnissen und drei Optionen präsentiert:
        *   **Force Read:** Remote-Ergebnisse überschreiben die lokalen Daten. Für die betroffenen Spiele wird `dirty = 0` gesetzt.
        *   **Force Write:** Lokale Ergebnisse überschreiben die Daten im Backend. Die Spiele bleiben für den Upload vorgemerkt (`dirty = 1`) und werden per Schreibaufruf an den Server gesendet.
        *   **Keine Aktion:** Der Synchronisationsvorgang wird abgebrochen. Lokale und Remote-Daten bleiben unverändert, die betroffenen Spiele behalten `dirty = 1`. Der Konflikt bleibt bestehen und wird beim nächsten Sync erneut erkannt.
4.  **Write (Upload unstrittiger / freigegebener Daten):**
    *   Alle für den Upload vorgemerkten Spiele mit `dirty = 1` werden an das Backend gesendet (`POST /game` bzw. Batch).
    *   Bei HTTP 200 OK wird lokal `dirty = 0` gesetzt.
    *   Rückkehr in den Zustand **Running**.

## 2.4 API-Schnittstellen (WordPress Plugin)
Anforderungen an die REST-API zur Unterstützung der Multi-Device-Synchronisation und Datenintegrität:

### 2.4.1 Geräteunabhängige Synchronisation
*   Alle Geräte sind gleichberechtigt; es gibt keine Bindung an eine `device_id` oder Master-Rolle.
*   **Lese-Endpunkte:**
    *   `GET /games/{tournament_id}`: Liefert alle Spiele und aktuellen Ergebnisse eines Turniers für den Read-before-Write-Schritt.
    *   `GET /players`: Liefert alle registrierten Spieler.
*   **Schreib-Endpunkte:**
    *   `POST /game`: Erstellt oder aktualisiert Spiele/Ergebnisse. Unterstützt das Überschreiben bestehender Ergebnisse (für reguläre Updates sowie Force-Write).
    *   `POST /player`: Erstellt neue Spieler. Lehnt Duplikate (gleicher Vor- und Nachname) mit HTTP 400/409 ab.
    *   `POST /finalize`: Setzt den Turnierstatus serverseitig auf `finalized`. Weitere Schreibzugriffe für dieses Turnier werden danach mit HTTP 403 / 409 abgewiesen.

## 2.5 Datenfluss, Dirty-Flags & Live-Synchronisation
Änderungen an Partien werden in Room sofort mit einem `dirty`-Flag (`dirty = 1`) versehen. Der Abgleich mit dem WordPress-Backend folgt einem nicht-blockierenden Hintergrund-Konzept:

1.  **Sync-Trigger:**
    *   **Automatisch (Auto-Sync):** Wird nach jeder lokalen Ergebniseingabe im Hintergrund angestoßen (`TournamentSyncManager.triggerSync`).
    *   **Manuell (On-Demand):** Tappen auf das Sync-Icon in der TopAppBar löst die Synchronisation aus, sofern ungesyncte Partien vorliegen oder der Status `FAILED` ist.
2.  **Abhängigkeiten-Auflösung & Serialisierung:**
    *   Alle Synchronisationsaufrufe werden über eine `Mutex` im `TournamentSyncManager` serialisiert.
    *   Vor dem Senden von Partien wird sichergestellt, dass beteiligte neue Spieler (`SyncPlayersUseCase`) und das Turnier (`SyncTournamentUseCase`) remote existieren, um gültige Remote-IDs bereitzustellen.
3.  **Batch-Upload & Upsert:**
    *   Alle noch offenen Partien (`dirty = 1`) werden ermittelt und gebündelt an `POST /game` gesendet (`SyncGameResultsUseCase`).
    *   Das WordPress-Plugin führt bei existierender Paarung `(tournament_id, player1_id, player2_id, leg_type)` ein nahtloses Update aus (`updateSingle`).
4.  **Erfolg & Bestätigung:**
    *   Nach erfolgreicher Rückmeldung vom Server wird `dirty = 0` gesetzt und die vom Server vergebene `remoteId` gespeichert.
    *   Der Gesamtstatus wechselt auf `SYNCED`.
5.  **Fehlerbehandlung & Offline-Betrieb:**
    *   Bei Netzwerkproblemen oder Serverausfall verbleiben die Spiele auf `dirty = 1` und der Status wechselt auf `FAILED`.
    *   Bei der nächsten Ergebniseingabe (oder manuellem Sync) werden die zuvor aufgelaufenen Änderungen automatisch mitübertragen.
    *   Erhält die App einen `HTTP 403 Forbidden` (z. B. Turnier serverseitig bereits finalisiert), wird das Turnier lokal finalisiert und die Bearbeitung gesperrt.
6.  **Zellen-Indikatoren & Präferenzen:**
    *   Ungesyncte Zellen können durch einen dezenten Punkt visualisiert werden.
    *   Diese Anzeige ist über `AppSettingsStorage` (DataStore) in den App-Einstellungen konfigurierbar (Standard: aktiviert).
