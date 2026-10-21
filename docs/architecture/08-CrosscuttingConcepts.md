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
*   **Room (SQLite):** Wird für strukturierte, relationale Daten verwendet (Spieler, Turniere, Games, Rankings, Modus-Katalog). Dies bildet die fachliche SSoT. Partien (`games`) werden kanonisch gespeichert (1 Datensatz pro Paarung und `leg` mit `player1Id < player2Id`).
*   **Jetpack DataStore (Preferences):** Wird für einfache Schlüssel-Wert-Paare verwendet und ist physisch nach Lebenszyklus in zwei getrennte DataStore-Dateien unterteilt:
    *   `tournament_state` (`TournamentStorage`): Speichert den kurzlebigen Zustand der aktuellen Turniersitzung (`tournament_id`, `finalized`) für die nahtlose Fortführung nach App-Neustarts. Beim Zurücksetzen (`resetAll()`) werden gezielt nur diese beiden Schlüssel entfernt.
    *   `app_settings` (`AppSettingsStorage`): Speichert dauerhafte Benutzereinstellungen (z. B. `show_detailed_sync_status`) unabhängig vom Turnier-Lebenszyklus.

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
   * **Initialer Turnier-Sync beim Start:** Direkt beim Öffnen der Kreuztabelle (`checkInitialStatus`) prüft der `TournamentSyncManager`, ob das Turnier bereits auf dem Server existiert (`remoteId != null`). Ist es noch ungesynct (`remoteId == null`), wird sofort `triggerSync(tournamentId)` ausgeführt, sodass Turnier und zugeordnete Teilnehmer (`player_ids`) unmittelbar nach dem Start in der WordPress-Datenbank (`monatsblitz_tournaments` + `monatsblitz_tournament_players`) angelegt werden. Offene Initial-Partien (`GameResult.Open`) starten mit `dirty = false` und werden dabei noch nicht übertragen.
   * **Sofortige lokale Persistenz:** Jede Ergebniseingabe in der Kreuztabelle wird kanonisch (`player1Id < player2Id`) unmittelbar in SQLite (Room) gespeichert (`GameEntity.dirty = 1`, außer wenn eine noch nie synchronisierte Partie wieder auf `Open` gestellt wird). Die UI reagiert sofort und verzögerungsfrei (Optimistic UI / Offline-First).
   * **Asynchroner Trigger:** Direkt nach dem lokalen Schreibzugriff stößt das `CrosstableViewModel` im Hintergrund (`viewModelScope`) den `TournamentSyncManager.triggerSync(tournamentId)` an. Die UI wird zu keinem Zeitpunkt blockiert.

2. **Kaskadierendes Batching & Abhängigkeiten:**
   * Der `TournamentSyncManager` serialisiert parallele Sync-Anfragen über eine `Mutex`.
   * Bei jedem Durchlauf werden Abhängigkeiten in definierter Reihenfolge aufgelöst:
     1. `syncPlayersUseCase()`: Neu lokal angelegte Spieler werden übertragen, um deren WordPress-`remoteId` zu erhalten.
     2. `syncTournamentUseCase()`: Das Turnier wird samt den `player_ids` seiner Teilnehmer remote angelegt (falls noch nicht geschehen), um die `tournamentRemoteId` zu sichern. Existiert am selben Tag bereits ein offenes Turnier auf dem Server, wird dieses zurückgesetzt bzw. wiederverwendet.
     3. `syncGameResultsUseCase()`: Alle ausstehenden Partien (`dirty = 1` mit gesetztem Ergebnis oder bestehender `remoteId`) werden ermittelt und gebatcht per `POST /game` übertragen. Hierbei werden die WordPress-Player-IDs (gemappt via Join in `GameDao.getGamesForSync`) und eventuell bereits vorhandene `remoteGameId`s übermittelt.
     4. Nach erfolgreicher Bestätigung werden die Partien in Room auf `dirty = 0` und mit ihrer `remoteId` aktualisiert. Der Gesamtstatus wechselt nur dann auf `SyncStatus.SYNCED`, wenn sowohl `tournament.remoteId != null` als auch `remainingDirtyGames.isEmpty()` erfüllt sind.
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



### 8.7 Modus-Verwaltung, Snapshot-Prinzip & Vorabauswahl (Rotation)
Für die Turniermodi setzt die Architektur auf ein zweistufiges Konzept:
1. **Katalog (`modes`) vs. Snapshot im Turnier (`tournaments.mode`):**
   * Die Tabelle `modes` fungiert als dynamischer Vorrat für die Auswahlliste.
   * Das Turnier speichert den tatsächlichen Modusnamen als Snapshot-String (`VARCHAR(30)`). Dies schützt die unveränderliche Historie und verhindert, dass spätere Modus-Löschungen oder Namenskorrekturen historische Auswertungen beeinflussen.
2. **Dynamische Neuanlage & One-Shot-Turniere:**
   * Neue Modi können direkt aus der App im `TournamentSetupScreen` angelegt werden.
   * Bei gesetzter Option *"Dauerhaft speichern"* wird nach einer Bestätigungsabfrage der Modus in Room persistiert und an `POST /mode` übertragen.
   * Für Sonderturniere (z. B. *"Jubiläumsblitz"*) kann der Modus als One-Shot genutzt werden: Er wird nur für das aktuelle Turnier gesetzt, ohne den Katalog zu verunreinigen.
3. **Automatisierte Vorabauswahl & Rotation:**
   * Über `SettingsScreen` konfigurierbar: `Keine`, `Fester Modus` oder `Rotation`.
   * Bei der Rotation ermittelt die App den nächsten Zyklusschritt anhand der jüngsten Turniere auf dem WordPress-Server (`GET /tournaments`). Hierdurch ist die Rotation absolut multi-device-fest.
   * One-Shot-Turniere werden bei der Bestimmung des letzten Modus automatisch ignoriert.
