# App Gap-Analyse: Monatsblitz Android App

Diese Analyse gleicht den aktuellen Implementierungsstand der **Monatsblitz Android App** gegen die Spezifikationen in `docs/` ([`01-FunctionalRequirements.md`](./specs/01-FunctionalRequirements.md), [`02-TechnicalRequirements.md`](./specs/02-TechnicalRequirements.md), [`api-gap-analysis.md`](./api-gap-analysis.md)), die dokumentierten Architekturregeln ([`00-Architecture.md`](./architecture/00-Architecture.md) bis [`11-RisksAndTechnicalDebt.md`](./architecture/11-RisksAndTechnicalDebt.md), [`AGENT.md`](../AGENT.md)) sowie Best Practices für **Clean Architecture**, **UX (Material 3)** und **Android-Entwicklung** ab.

---

## 1. Management Summary & Kritikalitäts-Matrix

| ID | Bereich | Befund / Gap | Schweregrad |
| :--- | :--- | :--- | :---: |
| **GAP-F1** | Fachlogik & Sync | ~~Asymmetrische Paarungs-Erzeugung vs. symmetrische Speicherung führt zu offenen `dirty=true`-Leichen (`"offen"`) und doppelten API-Payloads~~ | ✅ Erledigt |
| **GAP-F2** | Spezifikation / Sync | Initialer Turnier-/Ergebnisabgleich beim App-Start (§1.3) und Read-before-Write (§2.3.2) fehlen (Dokumentations-Diskrepanz V1 vs. V2) | 🟠 Hoch |
| **GAP-F3** | Spezifikation / UI | Detaillierte Sync-Indikatoren für **neue Spieler** werden in den Einstellungen versprochen, aber im Setup-Screen nicht angezeigt | 🟡 Mittel |
| **GAP-F4** | Datenmodell / DB | ~~Namenseindeutigkeit in SQLite (`Index(vorname, name)`) ist ohne `COLLATE NOCASE` auf DB-Ebene case-sensitive~~ | ✅ Erledigt |
| **GAP-A1** | Clean Architecture | ~~**Schichtenverletzung im Domain-Layer:** 6 von 10 Use Cases importieren direkt Room-DAOs, Retrofit-APIs, DTOs oder DataStore aus `infrastructure`~~ | ✅ Erledigt |
| **GAP-A2** | Clean Architecture | ~~**Schichtenverletzung im UI-Layer:** `RootViewModel`, `RankingViewModel`, `CrosstableViewModel` und `SettingsScreen` greifen direkt auf `infrastructure` (DAOs, API, Storage) zu~~ | ✅ Erledigt |
| **GAP-A3** | Clean Architecture | ~~Doppelte Verantwortlichkeit beim Speichern des Turnierzustands (`CreateTournamentUseCase` UND `TournamentRepositoryImpl`)~~ | ✅ Erledigt |
| **GAP-A4** | Code-Hygiene | ~~Verwaiste Altlasten aus ADR-001 (`RootUiState`, `RootEffect`, `ErrorComponent`, `LoadingComponent`) noch im Codebestand~~ | ✅ Erledigt |
| **GAP-UX1** | UX / Material 3 | `RankingScreen` und `RankingTable` nutzen kein `Scaffold`, kein `LazyColumn` (nicht scrollbar!), keine M3-Typografie/Abstände und formatieren Punkte als `1.0` statt `1` / `½` | 🟠 Hoch |
| **GAP-UX2** | UX / Workflow | Kein manueller Spieler-Sync (z. B. Pull-to-Refresh / Sync-Button) und kein Lade-/Offline-Feedback im `TournamentSetupScreen` | 🟠 Hoch |
| **GAP-UX3** | UX / Workflow | Fehlende Warnung bei noch offenen Partien vor „Turnier abschließen“ sowie fehlender Read-Only-Zustand im `RankingScreen` | 🟡 Mittel |
| **GAP-UX4** | UX / Sicherheit | ~~Zurück-Navigation in `CrosstableScreen` löscht sofort den lokalen Turnierstatus (`resetAll()`), selbst wenn das Turnier bereits auf dem Server existiert oder `FINALIZED` ist~~ | ✅ Erledigt |
| **GAP-AND1** | Android Best Practice | ~~Hintergrund-Sync läuft im `viewModelScope` von `CrosstableViewModel` und wird bei Screen-Wechsel (z. B. zur Rangliste) abgebrochen~~ | ✅ Erledigt |
| **GAP-AND2** | Android Best Practice | ~~`fallbackToDestructiveMigration(dropAllTables = true)` in `AppDatabase` birgt Risiko von komplettem Datenverlust bei App-Updates während eines Turniers~~ | ✅ Erledigt |
| **GAP-AND3** | Android Best Practice | ~~Nutzung von `collectAsState()` statt lifecycle-sensitivem `collectAsStateWithLifecycle()` in allen Screens~~ | ✅ Erledigt |
| **GAP-AND4** | Android Best Practice | ~~`RankingViewModel` lädt bei jeder Änderung in der Kreuztabelle ungefiltert `playerDao.getAllPlayers()` auf dem Collector-Pfad~~ | ✅ Erledigt |
| **GAP-AND5** | Android Best Practice | ~~Fehlende Netzwerk-Konnektivitätsüberwachung (`ConnectivityManager`) für Auto-Sync bei Verbindungsrückkehr~~ | ✅ Erledigt |

---

## 2. Funktionale & Technische Anforderungs-Gaps (`docs/specs/`)

### GAP-F1: Inkonsistenz zwischen Paarungs-Generierung, Ergebnis-Speicherung und Sync (✅ Erledigt)
Das Datenmodell wurde durchgängig auf das **kanonische Modell** (`player1Id < player2Id`, 1 Datensatz pro Paarung und `leg`) vereinheitlicht:
* Initiale `GameResult.Open`-Partien werden mit `dirty = false` angelegt (`NewGame.toEntity()`) und in `GameDao.getGamesForSync()` ausgefiltert, solange noch keine `remoteId` existiert.
* `SetGameResultUseCase` und `TournamentRepositoryImpl.updateGameResult()` speichern Ergebnisse kanonisch normalisiert; `Tournament.findGame()` und `CalculatePlayerPointsUseCase` werten beide Spielrichtungen über `result.opposite()` aus.
* `TournamentSyncManager.checkInitialStatus()` startet bei neuen Turnieren ohne gespielte Partien in `SyncStatus.SYNCED`.

---

### GAP-F2: Diskrepanz in den Specs bzgl. App-Start-Sync & Read-before-Write (🟠 Hoch)
Zwischen den Anforderungsdokumenten in `docs/` und der Implementierung besteht eine Dokumentations- und Funktionslücke:
* **Konflikt- & Merge-Algorithmus:** [`02-TechnicalRequirements.md` §2.2 & §2.3.2](./specs/02-TechnicalRequirements.md) sowie [`01-FunctionalRequirements.md` §1.2](./specs/01-FunctionalRequirements.md) beschreiben den Zustand `Conflict`, den Konfliktdialog (`Force Read`, `Force Write`, `Keine Aktion`) und den `GET /games/{tournament_id}` Read-before-Write-Ablauf als aktive Anforderung. Demgegenüber stellen [`01-FunctionalRequirements.md` §1.4](./specs/01-FunctionalRequirements.md) und [`api-gap-analysis.md` §1](./api-gap-analysis.md) klar, dass Multi-Device & Merge auf **V2+** verschoben wurden. Im Code werden [`MonatsblitzApi.getGames()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/api/MonatsblitzApi.kt) und [`MonatsblitzApi.getTournaments()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/api/MonatsblitzApi.kt) nirgends aufgerufen.
* **Initialer Datenabgleich beim App-Start ([`01-FunctionalRequirements.md` §1.3](./specs/01-FunctionalRequirements.md)):**
  [`RootViewModel`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/root/RootViewModel.kt) prüft lediglich, ob ein lokal im DataStore gespeichertes Turnier auf dem Server bereits `finalized` ist (`isRemoteTournamentFinalized`). Falls die App neu installiert wurde oder der lokale Speicher leer ist, auf dem Server aber ein offenes Turnier existiert, wird dieses **nicht** vom Server geladen.

---

### GAP-F3: Fehlender Sync-Indikator für ungesyncte Spieler im Setup-Screen (✅ Erledigt)
* `Player`-Domainmodell um `dirty: Boolean = false` erweitert und in `PlayerMapper` gemappt.
* `TournamentSetupViewModel` triggert nach erfolgreichem `addNewPlayer()` direkt im Hintergrund `SyncPlayersUseCase` an.
* `TournamentSetupScreen` und `PlayerRow` werten `state.showDetailedSyncIndicators` sowie `player.dirty` aus und rendern den dezenten 6.dp Indikatorpunkt (`MaterialTheme.colorScheme.tertiary`), solange der Spieler lokal noch ungesynct ist.

---

### GAP-F4: Namenseindeutigkeit und Feldnamen im Domain-Modell `Player` (🟡 Mittel)
* **Datenbank-Index:** [`PlayerEntity`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/persistence/room/entity/PlayerEntity.kt) definiert `Index(value = ["vorname", "name"], unique = true)`. In SQLite ist ein Standard-Index **case-sensitive** (sofern die Spalten nicht mit `collate = ColumnInfo.NOCASE` annotiert sind). Die Case-Insensitivity wird aktuell nur in [`PlayerDao.findPlayersByName()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/persistence/room/dao/PlayerDao.kt) via `LOWER(...)` geprüft, nicht aber durch den DB-Constraint selbst geschützt.
* **Naming-Abweichung zur Spezifikation:** [`02-TechnicalRequirements.md` §2.1](./specs/02-TechnicalRequirements.md) spezifiziert englische Feldnamen (`localId`, `remoteKey`, `firstName`, `lastName`) sowie `CreatePlayerUseCase` / `AddPlayerViewModel`. Im Code werden gemischt deutsch/englische Bezeichner (`id`, `vorname`, `name` in [`Player`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/model/Player.kt) vs. `forename`, `surname` in [`PlayerDto`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/api/dto/PlayerDto.kt)) verwendet.

---

## 3. Clean Architecture & Projekt-Architektur-Analyse

Gemäß [`04-SolutionStrategy.md`](./architecture/04-SolutionStrategy.md), [`05-BuildingBlockView.md`](./architecture/05-BuildingBlockView.md) und dem Komponentendiagramm ([`building-blocks-l1.puml`](./architecture/src/building-blocks-l1.puml)) gilt eine strikte Schichtenarchitektur:
* `ui` $\rightarrow$ darf nur `domain` kennen.
* `domain` $\rightarrow$ enthält Geschäftslogik (Use Cases, Domain-Modelle, Repository-Interfaces) und muss **frei von Frameworks (Android, Room, Retrofit, DataStore) und `infrastructure`-Klassen** sein.
* `infrastructure` $\rightarrow$ implementiert die `domain`-Interfaces und kapselt Room, Retrofit und DataStore.

### Architektur-Abgleich: Soll vs. Ist

```mermaid
flowchart TD
    subgraph UI ["UI Layer (ui.*)"]
        Screens["Compose Screens"]
        VMs["ViewModels (Root, Setup, Crosstable, Ranking)"]
    end

    subgraph Domain ["Domain Layer (domain.*)"]
        UseCases["Use Cases"]
        RepoInterfaces["Repository Interfaces (PlayerRepository, TournamentRepository)"]
        Models["Domain Models"]
    end

    subgraph Infra ["Infrastructure Layer (infrastructure.*)"]
        RepoImpl["Repository Impl"]
        SyncMgr["TournamentSyncManager"]
        DAOs["Room DAOs & Entities"]
        API["Retrofit MonatsblitzApi & DTOs"]
        DataStore["TournamentStorage & AppSettingsStorage"]
    end

    Screens --> VMs
    VMs --> UseCases
    VMs --> RepoInterfaces
    UseCases --> RepoInterfaces
    RepoImpl -.->|implements| RepoInterfaces
    RepoImpl --> DAOs
    RepoImpl --> DataStore

    %% Architekturverletzungen (Ist)
    Screens ==>|Verletzung: SettingsScreen| DataStore
    VMs ==>|Verletzung: RootVM, RankingVM, CrosstableVM| DAOs
    VMs ==>|Verletzung: RootVM| API
    VMs ==>|Verletzung: RootVM, CrosstableVM| DataStore
    VMs ==>|Verletzung: CrosstableVM| SyncMgr
    UseCases ==>|Verletzung: 6 Use Cases| DAOs
    UseCases ==>|Verletzung: 4 Use Cases| API
    UseCases ==>|Verletzung: 2 Use Cases| DataStore

    linkStyle 8,9,10,11,12,13,14,15 stroke:#d32f2f,stroke-width:2px;
```

### GAP-A1: Massive Schichtenverletzung im Domain-Layer (✅ Erledigt)
Sämtliche Use Cases in `de.kindermaenner.monatsblitz.domain.usecase` (`CreateTournamentUseCase`, `CreateTournamentRankingsUseCase`, `FinalizeTournamentUseCase`, `SyncGameResultsUseCase`, `SyncPlayersUseCase`, `SyncTournamentUseCase`, `SetGameResultUseCase`) wurden vollständig von `infrastructure.*`- und `android.*`-Abhängigkeiten entkoppelt und arbeiten ausschließlich gegen Domain-Modelle und Domain-Repository-Interfaces (`PlayerRepository`, `TournamentRepository`).

### GAP-A2: Schichtenverletzung im UI-Layer (✅ Erledigt)
* `RootViewModel`, `RankingViewModel` und `CrosstableViewModel` nutzen ausschließlich Domain-Interfaces (`TournamentRepository`, `SettingsRepository`, `TournamentSyncService`) und Domain-Modelle (`SyncStatus`, `TournamentRankingItem`).
* `SettingsScreen` wurde gemäß [`ADR-003`](./architecture/adr/ADR-003-jetpack-navigation.md) in das Package `ui.settings` verschoben und um ein eigenes `SettingsViewModel` (`SettingsViewModelFactory`) ergänzt.

### GAP-A3: Doppelte Zustands-Persistenz bei Turnieranlage (✅ Erledigt)
Das Speichern des initialen Turnierzustands (`tournamentStorage.saveTournamentState(tournamentId, false)`) erfolgt nur noch zentral in `TournamentRepositoryImpl.createTournament()` und wurde aus `CreateTournamentUseCase` entfernt.

### GAP-A4: Tote Altlasten aus ADR-001 (✅ Erledigt)
Die nach [`ADR-001`](./architecture/adr/ADR-001-navigation-approach.md) bzw. [`ADR-003`](./architecture/adr/ADR-003-jetpack-navigation.md) verwaisten Dateien (`RootEffect.kt`, `RootUiState.kt`, `ErrorComponent.kt`, `LoadingComponent.kt`) wurden vollständig aus dem Codebestand entfernt.

---

## 4. UX & Material 3 Best Practices (`ADR-004`)

### GAP-UX1: `RankingScreen` und `RankingTable` sind gestalterisch unvollständig (✅ Erledigt)
* `RankingScreen` nutzt ein vollwertiges Material 3 `Scaffold` mit Inset-Unterstützung.
* `RankingTable` verwendet eine scrollbare `LazyColumn`, ansprechend gestalteten M3-Tabellenkopf (`surfaceContainerHigh`), `HorizontalDivider` und Treppchen-Hervorhebung (Top 3 Medaillenfarben).
* Saubere Punkteformatierung ohne unschönes `.0` (`2` statt `2.0`).

### GAP-UX2: Fehlendes Sync-Feedback und fehlender manueller Sync im `TournamentSetupScreen` (✅ Erledigt)
* `TournamentSetupTopBar` bietet eine manuelle Synchronisationsschaltfläche (`Icons.Default.Sync`), die `SyncPlayersUseCase` ausführt und währenddessen einen `CircularProgressIndicator` anzeigt.
* `TournamentPlayerList` wertet `state.isLoading && state.players.isEmpty()` aus und rendert einen zentrierten Ladeindikator beim initialen Laden der Spieler.

### GAP-UX3: Finalisierungs-Absicherung und Read-Only-Modus im `RankingScreen` (✅ Erledigt)
* `RankingUiState.Ready` hält `openGamesCount` und `isReadOnly`.
* Bei `openGamesCount > 0` warnt `FinalizeConfirmDialog` explizit: *„Achtung: Es sind noch X Partien offen!“*
* Ist das Turnier serverseitig abgeschlossen (`SyncStatus.FINALIZED`), wird der Finalisierungsbutton deaktiviert und gesperrt (`isReadOnly = true`).

### GAP-UX4: Gefährliches Verhalten beim „Turnier abbrechen“ / Zurück-Button in `CrosstableScreen` (✅ Erledigt)
* `TournamentStorage` (`"tournament_state"`) und `AppSettingsStorage` (`"app_settings"`) nutzen in `AppContainer` getrennte DataStore-Dateien; `TournamentStorage.resetAll()` entfernt gezielt nur `tournamentIdKey` und `finalizedKey`.
* In `CrosstableScreen` wird bei `SyncStatus.FINALIZED` ohne „Turnier abbrechen?“-Dialog direkt zum Setup zurückgekehrt.
* Beim Abbruch eines offenen Turniers wird das Turnier lokal aus Room gelöscht (`TournamentRepositoryImpl.resetTournament()`); existiert am selben Datum bereits ein offenes Turnier auf dem Server, wird dieses beim erneuten Start im WordPress-Plugin (`CreateTournamentHandler.php`) und in `SyncTournamentUseCase` (Fallback bei HTTP 409) wiederverwendet bzw. zurückgesetzt.

### GAP-UX5: Kreuztabellen-Orientierung und Ergebnisanzeige (✅ Erledigt)
* In `CrosstableHeader` werden unter den Spaltennummern `1..N` nun übersichtlich die Namens-Initialen des jeweiligen Spielers (z. B. `M.M.`) angezeigt, um auch bei großen Teilnehmerfeldern jederzeit die Orientierung zu behalten.

---

## 5. Android-Entwicklung Best Practices

### GAP-AND1: Hintergrund-Synchronisation an `viewModelScope` gekoppelt (✅ Erledigt)
* `TournamentSyncManager` verfügt nun über einen eigenen Application-weiten `CoroutineScope(SupervisorJob() + Dispatchers.IO)` und führt `checkInitialStatus()` sowie `triggerSync()` unabhängig vom aufrufenden `viewModelScope` aus.

### GAP-AND2: `fallbackToDestructiveMigration` und fehlender Schema-Export in Room (✅ Erledigt)
* `AppDatabase` exportiert das Room-Schema (`exportSchema = true`, `room.schemaLocation` nach `app/schemas`), nutzt Version 5 mit expliziter, datenerhaltender `MIGRATION_4_5` (`COLLATE NOCASE` auf `players.vorname` und `players.name`) und verzichtet auf `fallbackToDestructiveMigration(dropAllTables = true)`.

### GAP-AND3: Fehlende Lifecycle-Awareness bei Flow-Collection in Compose (✅ Erledigt)
* Sämtliche Screens (`MainActivity`, `TournamentSetupScreen`, `CrosstableScreen`, `RankingScreen`, `SettingsScreen`) nutzen `collectAsStateWithLifecycle()` aus `androidx.lifecycle:lifecycle-runtime-compose`.

### GAP-AND4: Ineffiziente Datenbankabfrage in `TournamentRepositoryImpl.observeTournamentRanking` & Re-Allokationen in `CrosstableScreen` (✅ Erledigt)
* `TournamentPlayerDao.observeRankingWithPlayers()` lädt die Rangliste samt Spielernamen direkt per SQL-`LEFT JOIN`.
* `CrosstableViewModel` berechnet `rows: List<CrosstableRowData>` einmalig im `combine`-Block vor; `CrosstableScreen` rendert direkt `items(state.rows, key = { it.rowIndex })`.

### GAP-AND5: Fehlende Netzwerk-Konnektivitätsüberwachung (ConnectivityManager) (✅ Erledigt)
* `NetworkSyncObserver` registriert einen `ConnectivityManager.NetworkCallback` und stößt bei `onAvailable` automatisch die Synchronisation eines offenen Turniers an.

---

## 6. Empfohlene Roadmap (Architekturkonformes Vorgehen)

Um die identifizierten Gaps im Sinne von [`AGENT.md`](../AGENT.md) kontrolliert und ohne Regressionsrisiko aufzulösen, empfiehlt sich eine Aufteilung in vier abgeschlossene Arbeitspakete:

1. ~~**Paket 1 – Fachliche Korrektheit & Datenintegrität (GAP-F1, GAP-UX4):**~~ ✅ **Erledigt**
   * Kanonisches Speichermodell (`player1Id < player2Id`) und `dirty = false` für initiale `Open`-Spiele umgesetzt.
   * Physische Trennung der DataStore-Instanzen (`tournament_state` und `app_settings`), gezieltes Key-Löschen in `TournamentStorage.resetAll()` und sauberes Abbruch-/Wiederverwendungsverhalten für Turniere am selben Tag implementiert.
2. ~~**Paket 2 – Clean Architecture Refactoring (GAP-A1 bis GAP-A4):**~~ ✅ **Erledigt**
   * Rückführung aller Infrastruktur-Abhängigkeiten aus `domain.usecase` und `ui.*` hinter die Domain-Interfaces (`PlayerRepository`, `TournamentRepository`, `SettingsRepository`, `TournamentSyncService`) sowie Verschiebung von `SettingsScreen` inklusive `SettingsViewModel` nach `ui.settings`.
   * Entfernung der doppelten Zustandspersistenz (`GAP-A3`) und der verwaisten Dateien aus ADR-001 (`GAP-A4`).
3. ~~**Paket 3 – Android Lifecycle & Resilienz (GAP-AND1 bis GAP-AND5, GAP-F4):**~~ ✅ **Erledigt**
   * Eigener Application-Scope für `TournamentSyncManager` + `NetworkSyncObserver` (`NetworkCallback`) für Auto-Sync bei Verbindungsrückkehr.
   * Umstellung auf `collectAsStateWithLifecycle()`, SQL-Join im Ranking-DAO, Vorberechnung der Kreuztabellen-Zeilen im ViewModel sowie Aktivierung von `exportSchema = true` und `MIGRATION_4_5` (`COLLATE NOCASE`) in Room.
4. ~~**Paket 4 – UX & Material 3 Vervollständigung (GAP-UX1 bis GAP-UX3, GAP-UX5, GAP-F3):**~~ ✅ **Erledigt**
   * M3-Überarbeitung von `RankingScreen` / `RankingTable` (`Scaffold`, `LazyColumn`, saubere Punktformatierung, Warnhinweis bei offenen Partien, Read-Only-Status bei serverseitig finalisiertem Turnier).
   * Manueller Spieler-Sync (`SyncPlayersUseCase`) & Ladeanzeige im `TournamentSetupScreen` sowie Statuspunkt bei `player.dirty`.
   * Spieler-Initialen unter Spaltennummern `1..N` im `CrosstableHeader`.
