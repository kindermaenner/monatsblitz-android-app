# 2. Technische Anforderungen - Monatsblitz Android App

Dieses Dokument beschreibt die technischen Details, Datenstrukturen und Algorithmen zur Umsetzung der funktionalen Anforderungen.

## 2.1 Domain-Modell: Player
Das Modell für Spieler muss erweitert werden, um die Synchronisations- und Unterscheidungslogik zu unterstützen.

| Feld | Typ | Beschreibung |
| :--- | :--- | :--- |
| `localId` | Long | Eindeutige Kennung in der lokalen Room-Datenbank. |
| `remoteKey` | Long? | ID des Spielers in der WordPress-Datenbank (`null` bei fehlendem Sync). |
| `firstName` | String | Vorname des Spielers. |
| `lastName` | String | Nachname des Spielers. |
| `displaySuffix` | String? | Optionaler Zusatz zur Unterscheidung bei Namensgleichheit (z. B. "(1)"). |

## 2.2 Zustandsmodell der App
Der Turnier-Lifecycle wird über Zustände gesteuert, die im `TournamentStorage` (DataStore) und in der Datenbank persistiert werden.

| Zustand | Trigger | Auswirkung |
| :--- | :--- | :--- |
| **Setup** | App-Start ohne aktives Turnier | Anzeige `TournamentSetupScreen`, Spieler-Sync aktiv. |
| **Running (Master)** | Klick "Turnier starten" oder Übernahme | Schreibzugriff auf Kreuztabelle, automatischer Sync der Ergebnisse. |
| **Running (Read-Only)** | Übernahme durch anderes Gerät | Eingabefelder gesperrt, lesender Sync vom Server zur Aktualisierung der Anzeige. |
| **Conflict** | Sync-Fehler (409 Conflict) | Anzeige des Konflikt-Dialogs, Stopp des automatischen Syncs. |
| **Finalized** | Erfolgreicher Aufruf von `/finalize` | Schreibschutz für alle Geräte, Wechsel zur Ranking-Ansicht. |

## 2.3 Algorithmen

### 2.3.1 Paarungs-Generierung (Round Robin)
Bei Turnierstart werden alle Paarungen nach dem Prinzip "Jeder-gegen-Jeden" erzeugt.
*   **Einrundig:** `n * (n - 1) / 2` Spiele.
*   **Doppelrundig:** `n * (n - 1)` Spiele (Hin- und Rückspiel mit vertauschten Farben/Legs).
*   Die Generierung erfolgt initial im `CreateNewGamesUseCase`.

### 2.3.2 Grand Prix Berechnung (Jahreswertung)
Die Berechnung der Jahreswertung erfolgt lokal in der App basierend auf den vom Server geladenen historischen Daten.
1.  **Punkte-Zuweisung:** 
    *   Rang 1: 5 Pkt, Rang 2: 4 Pkt, Rang 3: 3 Pkt, Rang 4: 2 Pkt, Rang 5: 1 Pkt.
    *   Bei Dense Ranking (z.B. zwei Spieler auf Rang 2) erhalten beide die volle Punktzahl (4 Pkt). Der nächste Spieler (Rang 3) erhält 3 Pkt.
2.  **Aggregation:** 
    *   Abruf aller Platzierungspunkte des Spielers im aktuellen Kalenderjahr.
    *   Sortierung der Punkte absteigend.
    *   Summe der ersten 5 Einträge (Top 5).

## 2.4 API Gaps (Änderungsbedarf WordPress Plugin)
Basierend auf der existierenden `MonatsblitzApi` fehlen folgende Funktionen für die neuen Anforderungen:

### 2.4.1 Master-Management & Concurrency
Um den Multi-Device-Support sicherzustellen, muss die API den Master-Status verwalten.
*   **NEU: `POST /tournaments/{id}/claim-master`**
    *   Ermöglicht einem Gerät, die Schreibhoheit anzufordern.
    *   Speichert eine eindeutige `device_id` als aktuellen Master auf dem Server.
*   **ÄNDERUNG: `POST /game` (Update/Create)**
    *   Muss HTTP 409 (Conflict) zurückgeben, wenn die anfragende `device_id` nicht der aktuelle Master ist.
    *   Antwort-Body bei 409 muss den aktuellen Stand der Spiele auf dem Server enthalten.

### 2.4.2 Historische Grand Prix Daten
*   **NEU: `GET /results/yearly/{year}`**
    *   Liefert eine Liste aller bisherigen Turnierplatzierungen und vergebenen Grand-Prix-Punkte für alle Spieler im Jahr.
    *   Wird von der App nach der Finalisierung zur Berechnung der neuen Gesamtwertung benötigt.

## 2.5 Datenfluss & Dirty-Flags
Änderungen werden lokal in Room mit einem `dirty`-Flag markiert.
1.  **Sync-Trigger:** `SyncGameResultsUseCase` sucht alle Games mit `dirty = 1`.
2.  **Upload:** Senden der Daten an den Server inkl. `device_id`.
3.  **Success:** Bei Erfolg (HTTP 200) wird das `dirty`-Flag auf `0` gesetzt.
4.  **Conflict:** Bei HTTP 409 wird der lokale Zustand in den Modus **Conflict** versetzt (siehe 2.2).
5.  **Download Sync (Read-Only):** In diesem Modus darf die App regelmäßig den Server-Zustand abfragen. Sofern lokal keine ungesynchronisierten Daten (`dirty = 1`) vorliegen, werden lokale Datensätze durch die Server-Antwort aktualisiert.
