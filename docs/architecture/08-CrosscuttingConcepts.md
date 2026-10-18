# 8. Querschnittliche Konzepte

### 8.1 Single Source of Truth (SSoT)
Die Architektur erzwingt, dass die Datenbank der SSoT ist. Ein typischer Ablauf für externe Daten sieht so aus:
1.  **Sync Trigger:** Ein Use Case ruft die REST-API auf.
2.  **Persistenz:** Die erhaltenen Daten werden in Room-Entities umgewandelt und gespeichert/aktualisiert.
3.  **Reaktivität:** Repositories bieten `Flow<T>`-Schnittstellen an, die direkt an die DAO-Abfragen gekoppelt sind.
4.  **UI-Update:** Sobald Room die Änderung in der DB registriert, emittiert der Flow neue Daten, die über das ViewModel an die UI fließen.

### 8.2 Dependency Injection
Das Projekt nutzt kein DI-Framework wie Hilt oder Dagger. Stattdessen wird ein manueller `AppContainer` verwendet, der in der `Application`-Klasse initialisiert wird. ViewModels werden über spezifische `ViewModelFactory`-Klassen erzeugt, die ihre Abhängigkeiten aus dem `AppContainer` beziehen.

### 8.3 Persistenz-Konzept
Die App nutzt zwei unterschiedliche Persistenz-Technologien je nach Anwendungsfall:
*   **Room (SQLite):** Wird für strukturierte, relationale Daten verwendet (Spieler, Turniere, Games, Rankings). Dies bildet die fachliche SSoT.
*   **Jetpack DataStore (Preferences):** Wird für einfache Schlüssel-Wert-Paare zur Steuerung des App-Zustands verwendet (z. B. `tournament_id` des aktiven Turniers). Dies ermöglicht die nahtlose Fortführung einer Sitzung nach App-Neustarts.

### 8.4 Eindeutigkeit von Spielernamen
Es darf keine zwei Spieler mit gleichem Namen (Vor- und Nachname) geben:
*   Die Eindeutigkeit wird über die Kombination aus Vor- und Nachname (`UNIQUE(vorname, name)`) definiert (case-insensitive und getrimmt).
*   Bei der Neuanlage wird geprüft, ob bereits ein Spieler mit identischem Vor- und Nachnamen existiert. Bei Duplikaten wird die Anlage mit einer Fehlermeldung abgewiesen.
*   In der lokalen Room-Datenbank wird die Eindeutigkeit über einen Unique-Index auf `(vorname, name)` erzwungen.
*   Dies entspricht auch dem Schema im WordPress-Backend (`UNIQUE KEY forename_surname (forename, surname)`).

### 8.5 User Interface & Design System
Die visuelle Gestaltung folgt dem **Material 3 (M3)** Standard. 
*   **Theming:** Zentral definiert in `ui/theme/Theme.kt`. Es unterstützt sowohl Light- als auch Dark-Mode sowie Dynamic Color (ab Android 12).
*   **Farben:** Die Farbpalette ist in `ui/theme/Color.kt` definiert und nutzt M3-spezifische Rollen (Primary, Secondary, Tertiary, Surface etc.).
*   **Typografie:** Die Textstile sind in `ui/theme/Type.kt` nach dem M3-Schema (Headline, Title, Body, Label) strukturiert.
*   **Komponenten:** Es werden ausschließlich M3-Komponenten (`androidx.compose.material3`) verwendet.

### 8.6 Synchronisations- und Live-Status-Konzept (Turnierverlauf & Auto-Sync)
Um eine optimale User Experience ohne störende Wartezeiten und gleichzeitig eine konsistente Datenhaltung im WordPress-Backend zu garantieren, setzt die App auf einen asynchronen, blockierungsfreien Hintergrund-Sync mit transparentem Live-Status:

1. **Automatischer Hintergrund-Sync (Auto-Sync):**
   * **Sofortige lokale Persistenz:** Jede Ergebniseingabe in der Kreuztabelle wird unmittelbar in SQLite (Room) gespeichert (`GameEntity.dirty = 1`). Die UI reagiert sofort und verzögerungsfrei (Optimistic UI / Offline-First).
   * **Asynchroner Trigger:** Direkt nach dem lokalen Schreibzugriff stößt das `CrosstableViewModel` im Hintergrund (`viewModelScope`) den `TournamentSyncManager.triggerSync(tournamentId)` an. Die UI wird zu keinem Zeitpunkt blockiert.

2. **Kaskadierendes Batching & Abhängigkeiten:**
   * Der `TournamentSyncManager` serialisiert parallele Sync-Anfragen über eine `Mutex`.
   * Bei jedem Durchlauf werden Abhängigkeiten in definierter Reihenfolge aufgelöst:
     1. `syncPlayersUseCase()`: Neu lokal angelegte Spieler werden übertragen, um deren WordPress-`remoteId` zu erhalten.
     2. `syncTournamentUseCase()`: Das Turnier wird remote angelegt (falls noch nicht geschehen), um die `tournamentRemoteId` zu sichern.
     3. `syncGameResultsUseCase()`: Alle ausstehenden Partien (`dirty = 1`) werden ermittelt und gebatcht per `POST /game` übertragen. Hierbei werden die WordPress-Player-IDs (gemappt via Join in `GameDao.getGamesForSync`) und eventuell bereits vorhandene `remoteGameId`s übermittelt.
     4. Nach erfolgreicher Bestätigung werden die Partien in Room auf `dirty = 0` und mit ihrer `remoteId` aktualisiert.
   * **Offline-Resilienz:** War das Gerät offline oder schlug ein Sync fehl, verbleiben die Änderungen auf `dirty = 1`. Bei der nächsten Ergebniseingabe (oder manuellem Sync) werden automatisch alle noch ungesynchronisierten Partien gebündelt nachgesendet.

3. **Serverseitiges Upsert (Korrekturfreundlichkeit):**
   * Solange ein Turnier den Status `open` hat, können Ergebnisse jederzeit geändert oder korrigiert werden.
   * Das WordPress-Plugin (`CreateGameHandler.php`) prüft vor dem Anlegen einer Partie, ob die Paarung `(tournament_id, player1_id, player2_id, leg_type)` bereits existiert. Wenn ja, wird nahtlos ein Update (`updateSingle`) ausgeführt. Dadurch werden MySQL Duplicate Key Errors vermieden.
   * Sobald das Turnier finalisiert ist (`finalized`), lehnt das Backend Schreibversuche strikt mit `HTTP 403 Forbidden` ab.

4. **Synchronisationsstatus (`SyncStatus`):**
   * `SYNCED`: Alle lokalen Partien und das Turnier sind vollständig mit dem Server synchronisiert.
   * `SYNCING`: Ein Synchronisationsvorgang wird gerade im Hintergrund ausgeführt.
   * `FAILED`: Ein Sync-Versuch ist fehlgeschlagen (z. B. keine Internetverbindung) und es existieren ungesyncte Daten.
   * `FINALIZED`: Das Turnier ist abgeschlossen (entweder regulär finalisiert oder via HTTP 403 vom Server gemeldet). Die Kreuztabelle wird in den Schreibschutzmodus geschaltet.

5. **UI- & UX-Visualisierung:**
   * **Globales TopAppBar Sync-Icon (`CrosstableScreen`):**
     * `SYNCED`: Wolke mit Haken (`Icons.Default.CloudDone`). Ein Klick zeigt die Bestätigung "Bereits synchronisiert" (verhindert unnötigen Netzwerk-Traffic).
     * `SYNCING`: Dezenter Dreh-Indikator (`CircularProgressIndicator`).
     * `FAILED`: Warnsymbol (`Icons.Default.SyncProblem`). **Ein Klick darauf löst gezielt einen manuellen Sync aus**.
     * `FINALIZED`: Schloss-Symbol (`Icons.Default.Lock`).
   * **Detaillierte Zellen-Indikatoren:**
     * An Zellen mit ungespeichertem Status (`dirty = true`) wird in der oberen Ecke ein kleiner Punkt angezeigt. Sobald der Server den Empfang bestätigt, verschwindet der Punkt.
     * **Konfigurierbar:** Über `AppSettingsStorage` (DataStore) und den Einstellungsbildschirm (`SettingsScreen`) kann dieser Detail-Indikator vom Benutzer ein- oder ausgeschaltet werden (Standard: aktiviert).

