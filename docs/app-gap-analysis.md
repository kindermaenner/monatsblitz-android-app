# App Gap-Analyse: Monatsblitz Android App

Diese Analyse gleicht den aktuellen Implementierungsstand der **Monatsblitz Android App** gegen die Spezifikationen in `docs/` ([`01-FunctionalRequirements.md`](./specs/01-FunctionalRequirements.md), [`02-TechnicalRequirements.md`](./specs/02-TechnicalRequirements.md), [`api-gap-analysis.md`](./api-gap-analysis.md)), die dokumentierten Architekturregeln ([`00-Architecture.md`](./architecture/00-Architecture.md) bis [`11-RisksAndTechnicalDebt.md`](./architecture/11-RisksAndTechnicalDebt.md), [`AGENT.md`](../AGENT.md)) sowie Best Practices für **Clean Architecture**, **UX (Material 3)** und **Android-Entwicklung** ab.

---

## 1. Management Summary & Kritikalitäts-Matrix

| ID | Bereich | Befund / Gap | Schweregrad |
| :--- | :--- | :--- | :---: |
| **GAP-F1** | Fachlogik & Sync | Asymmetrische Paarungs-Erzeugung vs. symmetrische Speicherung führt zu offenen `dirty=true`-Leichen (`"offen"`) und doppelten API-Payloads | 🔴 Kritisch |
| **GAP-F2** | Spezifikation / Sync | Initialer Turnier-/Ergebnisabgleich beim App-Start (§1.3) und Read-before-Write (§2.3.2) fehlen (Dokumentations-Diskrepanz V1 vs. V2) | 🟠 Hoch |
| **GAP-F3** | Spezifikation / UI | Detaillierte Sync-Indikatoren für **neue Spieler** werden in den Einstellungen versprochen, aber im Setup-Screen nicht angezeigt | 🟡 Mittel |
| **GAP-F4** | Datenmodell / DB | Namenseindeutigkeit in SQLite (`Index(vorname, name)`) ist ohne `COLLATE NOCASE` auf DB-Ebene case-sensitive | 🟡 Mittel |
| **GAP-A1** | Clean Architecture | **Schichtenverletzung im Domain-Layer:** 6 von 10 Use Cases importieren direkt Room-DAOs, Retrofit-APIs, DTOs oder DataStore aus `infrastructure` | 🔴 Kritisch |
| **GAP-A2** | Clean Architecture | **Schichtenverletzung im UI-Layer:** `RootViewModel`, `RankingViewModel`, `CrosstableViewModel` und `SettingsScreen` greifen direkt auf `infrastructure` (DAOs, API, Storage) zu | 🟠 Hoch |
| **GAP-A3** | Clean Architecture | Doppelte Verantwortlichkeit beim Speichern des Turnierzustands (`CreateTournamentUseCase` UND `TournamentRepositoryImpl`) | 🟡 Mittel |
| **GAP-A4** | Code-Hygiene | ~~Verwaiste Altlasten aus ADR-001 (`RootUiState`, `RootEffect`, `ErrorComponent`, `LoadingComponent`) noch im Codebestand~~ | ✅ Erledigt |
| **GAP-UX1** | UX / Material 3 | `RankingScreen` und `RankingTable` nutzen kein `Scaffold`, kein `LazyColumn` (nicht scrollbar!), keine M3-Typografie/Abstände und formatieren Punkte als `1.0` statt `1` / `½` | 🟠 Hoch |
| **GAP-UX2** | UX / Workflow | Kein manueller Spieler-Sync (z. B. Pull-to-Refresh / Sync-Button) und kein Lade-/Offline-Feedback im `TournamentSetupScreen` | 🟠 Hoch |
| **GAP-UX3** | UX / Workflow | Fehlende Warnung bei noch offenen Partien vor „Turnier abschließen“ sowie fehlender Read-Only-Zustand im `RankingScreen` | 🟡 Mittel |
| **GAP-UX4** | UX / Sicherheit | Zurück-Navigation in `CrosstableScreen` löscht sofort den lokalen Turnierstatus (`resetAll()`), selbst wenn das Turnier bereits auf dem Server existiert oder `FINALIZED` ist | 🟠 Hoch |
| **GAP-AND1** | Android Best Practice | Hintergrund-Sync läuft im `viewModelScope` von `CrosstableViewModel` und wird bei Screen-Wechsel (z. B. zur Rangliste) abgebrochen | 🟠 Hoch |
| **GAP-AND2** | Android Best Practice | `fallbackToDestructiveMigration(dropAllTables = true)` in `AppDatabase` birgt Risiko von komplettem Datenverlust bei App-Updates während eines Turniers | 🟠 Hoch |
| **GAP-AND3** | Android Best Practice | Nutzung von `collectAsState()` statt lifecycle-sensitivem `collectAsStateWithLifecycle()` in allen Screens | 🟡 Mittel |
| **GAP-AND4** | Android Best Practice | `RankingViewModel` lädt bei jeder Änderung in der Kreuztabelle ungefiltert `playerDao.getAllPlayers()` auf dem Collector-Pfad | 🟡 Mittel |

---

## 2. Funktionale & Technische Anforderungs-Gaps (`docs/specs/`)

### GAP-F1: Inkonsistenz zwischen Paarungs-Generierung, Ergebnis-Speicherung und Sync (🔴 Kritisch)
In der Zusammensicht von Paarungserzeugung, Punkteberechnung und API-Synchronisation besteht ein gravierender Widerspruch im Datenmodell:

1. **Asymmetrische Anlage mit `dirty = true`:**
   * [`CreateNewGamesUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/CreateNewGamesUseCase.kt) erzeugt bei Turnierstart nur Paarungen einer Dreiecksmatrix (`minOf(player1.id, player2.id)` gegen `maxOf(player1.id, player2.id)` mit `result = GameResult.Open`).
   * [`NewGame.toEntity()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/persistence/room/mapper/TournamentMapper.kt) setzt für alle diese initialen, noch ungespielten Partien sofort **`dirty = true`**.
2. **Symmetrisches Nachflicken bei Ergebniseingabe:**
   * [`SetGameResultUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/SetGameResultUseCase.kt) ruft `updateGameResult` **zweimal** auf: einmal für `(player1Id, player2Id, result)` und einmal für die Gegenrichtung `(player2Id, player1Id, result.opposite())`.
   * Da die Gegenrichtung `(maxId, minId)` initial nicht in der Datenbank existiert, legt [`TournamentRepositoryImpl.updateGameResult()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/repository/TournamentRepositoryImpl.kt) beim ersten Eintragen dynamisch einen **zweiten Datensatz** in der Tabelle `games` an.
   * [`CalculatePlayerPointsUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/CalculatePlayerPointsUseCase.kt) verlässt sich darauf, dass für jeden Spieler ein eigener Datensatz mit `it.player1Id == playerId` existiert.
3. **Folgen für Synchronisation & UI:**
   * Sobald das erste Ergebnis eingetragen wird, selektiert [`GameDao.getGamesForSync()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/persistence/room/dao/GameDao.kt) **alle** Spiele mit `dirty = 1` – darunter sämtliche noch gar nicht gespielten Partien (`GameResult.Open`), und sendet diese mit `result = "offen"` ([`MappingUtils.kt`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/api/dto/MappingUtils.kt)) an `POST /game`.
   * Gleichzeitig werden für jede gespielte Partie **beide** Richtungen (`(A, B, "1:0")` und `(B, A, "0:1")`) als separate Spiele an das WordPress-Backend übertragen.
   * Direkt nach Turnierstart meldet [`TournamentSyncManager.checkInitialStatus()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/sync/TournamentSyncManager.kt) sofort **`SyncStatus.FAILED`**, weil das neu angelegte Turnier und alle offenen Spiele mit `dirty = true` initialisiert wurden, aber beim Turnierstart kein initialer Sync ausgelöst wird.

> [!WARNING]
> **Empfehlung zu GAP-F1:**
> Entscheiden, ob eine Partie in Room kanonisch (1 Zeile pro Paarung) oder gerichtet (2 Zeilen pro Paarung) gespeichert wird, und sicherstellen, dass ungespielte Initial-Partien (`GameResult.Open`) beim Turnierstart entweder mit `dirty = false` angelegt oder in `getGamesForSync()` ausgefiltert werden, sofern noch kein Ergebnis vorliegt (außer beim bewussten Zurücksetzen eines bereits synchronisierten Spiels mit `remoteId != null`). Zudem sollte nach `createTournament` direkt ein initialer Hintergrund-Sync angestoßen werden, statt in `SyncStatus.FAILED` zu starten.

---

### GAP-F2: Diskrepanz in den Specs bzgl. App-Start-Sync & Read-before-Write (🟠 Hoch)
Zwischen den Anforderungsdokumenten in `docs/` und der Implementierung besteht eine Dokumentations- und Funktionslücke:
* **Konflikt- & Merge-Algorithmus:** [`02-TechnicalRequirements.md` §2.2 & §2.3.2](./specs/02-TechnicalRequirements.md) sowie [`01-FunctionalRequirements.md` §1.2](./specs/01-FunctionalRequirements.md) beschreiben den Zustand `Conflict`, den Konfliktdialog (`Force Read`, `Force Write`, `Keine Aktion`) und den `GET /games/{tournament_id}` Read-before-Write-Ablauf als aktive Anforderung. Demgegenüber stellen [`01-FunctionalRequirements.md` §1.4](./specs/01-FunctionalRequirements.md) und [`api-gap-analysis.md` §1](./api-gap-analysis.md) klar, dass Multi-Device & Merge auf **V2+** verschoben wurden. Im Code werden [`MonatsblitzApi.getGames()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/api/MonatsblitzApi.kt) und [`MonatsblitzApi.getTournaments()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/api/MonatsblitzApi.kt) nirgends aufgerufen.
* **Initialer Datenabgleich beim App-Start ([`01-FunctionalRequirements.md` §1.3](./specs/01-FunctionalRequirements.md)):**
  [`RootViewModel`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/root/RootViewModel.kt) prüft lediglich, ob ein lokal im DataStore gespeichertes Turnier auf dem Server bereits `finalized` ist (`isRemoteTournamentFinalized`). Falls die App neu installiert wurde oder der lokale Speicher leer ist, auf dem Server aber ein offenes Turnier existiert, wird dieses **nicht** vom Server geladen.

---

### GAP-F3: Fehlender Sync-Indikator für ungesyncte Spieler im Setup-Screen (🟡 Mittel)
* In [`SettingsScreen.kt`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/screens/SettingsScreen.kt) lautet die Beschreibung der Einstellung:
  *„Zeigt kleine Statuspunkte an ungespeicherten Zellen **und neuen Spielern**, bis sie synchronisiert sind.“*
* **Ist-Zustand:**
  1. Das Domain-Modell [`Player`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/model/Player.kt) enthält (anders als in [`02-TechnicalRequirements.md` §2.1](./specs/02-TechnicalRequirements.md) spezifiziert) weder `remoteKey`/`remoteId` noch ein `dirty`-Flag.
  2. Wenn im [`TournamentSetupScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/tournamentsetup/TournamentSetupScreen.kt) ein neuer Spieler angelegt wird, wird weder sofort [`SyncPlayersUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/SyncPlayersUseCase.kt) getriggert noch in `PlayerRow` ein Statuspunkt angezeigt.

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

### GAP-A1: Massive Schichtenverletzung im Domain-Layer (🔴 Kritisch)
Fast alle Use Cases im Package `de.kindermaenner.monatsblitz.domain.usecase` umgehen die Repository-Abstraktion und hängen direkt von `infrastructure` (Room-DAOs, Retrofit-Interfaces, Netzwerk-DTOs, DataStore) oder dem Android-SDK ab:

| Use Case (`domain.usecase`) | Unerlaubte Abhängigkeiten (`infrastructure` / `android`) |
| :--- | :--- |
| [`CreateTournamentUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/CreateTournamentUseCase.kt) | `infrastructure.TournamentStorage` |
| [`CreateTournamentRankingsUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/CreateTournamentRankingsUseCase.kt) | `infrastructure.persistence.room.dao.TournamentPlayerDao`, `toEntity()` |
| [`FinalizeTournamentUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/FinalizeTournamentUseCase.kt) | `MonatsblitzApi`, `CreateResultsDto`, `FinalizeRequestDto`, `FinalizeResponseDto` (als Rückgabetyp!), `ResultDto`, `PlayerDao`, `TournamentDao`, `TournamentPlayerDao`, `TournamentStorage` |
| [`SyncGameResultsUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/SyncGameResultsUseCase.kt) | `MonatsblitzApi`, `CreateGamesDto`, `GameDtoData`, `GameDao` |
| [`SyncPlayersUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/SyncPlayersUseCase.kt) | `PlayerRemoteDataSource`, `PlayerDao`, DTO-Mapper |
| [`SyncTournamentUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/SyncTournamentUseCase.kt) | `MonatsblitzApi`, `TournamentDao`, `toDto()` |
| [`SetGameResultUseCase`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/SetGameResultUseCase.kt) | `android.util.Log` (verhindert reine JVM-Unabhängigkeit ohne Mocking/Default-Values) |

### GAP-A2: Schichtenverletzung im UI-Layer (🟠 Hoch)
Auch der UI-Layer greift an mehreren Stellen direkt auf die Infrastruktur-Schicht zu, anstatt über Use Cases oder Domain-Interfaces zu gehen:
* [`RootViewModel`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/root/RootViewModel.kt) injiziert sich direkt `TournamentStorage`, `TournamentDao` und `MonatsblitzApi`, um den initialen App-Zustand zu bestimmen. Diese Logik gehört in einen dedizierten Domain-Use-Case (z. B. `GetInitialAppStateUseCase` / `SessionRepository`).
* [`RankingViewModel`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/ranking/RankingViewModel.kt) nutzt direkt `TournamentPlayerDao`, `PlayerDao` und den Room-Mapper `toDomain()`, anstatt das Ranking über `TournamentRepository` (oder einen `ObserveTournamentRankingUseCase`) zu beziehen.
* [`CrosstableViewModel`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/CrosstableViewModel.kt) und [`CrosstableUiState`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/CrosstableUiState.kt) importieren `AppSettingsStorage`, `TournamentSyncManager` und `SyncStatus` direkt aus `infrastructure`.
* [`SettingsScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/screens/SettingsScreen.kt) besitzt kein ViewModel und erhält direkt die `infrastructure`-Klasse [`AppSettingsStorage`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/settings/AppSettingsStorage.kt) als Parameter, auf der im Composable via `rememberCoroutineScope()` Schreiboperationen ausgeführt werden. Zudem liegt `SettingsScreen.kt` im inkonsistenten Package `ui.screens` statt feature-basiert unter `ui.settings` (vgl. [`ADR-003`](./architecture/adr/ADR-003-jetpack-navigation.md)).

### GAP-A3: Doppelte Zustands-Persistenz bei Turnieranlage (🟡 Mittel)
Beim Erstellen eines Turniers wird `tournamentStorage.saveTournamentState(tournamentId, false)` doppelt aufgerufen:
1. Innerhalb der Room-Transaktion in [`TournamentRepositoryImpl.createTournament()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/repository/TournamentRepositoryImpl.kt).
2. Unmittelbar danach erneut in [`CreateTournamentUseCase.invoke()`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/usecase/CreateTournamentUseCase.kt).

### GAP-A4: Tote Altlasten aus ADR-001 (✅ Erledigt)
Die nach [`ADR-001`](./architecture/adr/ADR-001-navigation-approach.md) bzw. [`ADR-003`](./architecture/adr/ADR-003-jetpack-navigation.md) verwaisten Dateien (`RootEffect.kt`, `RootUiState.kt`, `ErrorComponent.kt`, `LoadingComponent.kt`) wurden vollständig aus dem Codebestand entfernt.

---

## 4. UX & Material 3 Best Practices (`ADR-004`)

### GAP-UX1: `RankingScreen` und `RankingTable` sind gestalterisch unvollständig (🟠 Hoch)
Während [`TournamentSetupScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/tournamentsetup/TournamentSetupScreen.kt) und [`CrosstableScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/CrosstableScreen.kt) sauber nach Material 3 gestaltet sind, fällt der Ranking-Bereich stark ab:
* **Kein `Scaffold`:** [`RankingScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/ranking/RankingScreen.kt) nutzt ein bloßes `Column` statt `Scaffold`. Bei aktiviertem `enableEdgeToEdge()` in [`MainActivity`](../app/src/main/java/de/kindermaenner/monatsblitz/MainActivity.kt) fehlt dadurch die korrekte Berücksichtigung von System-Bar-Insets am unteren Bildschirmrand.
* **Nicht scrollbar & kein M3-Tabellenlayout:** [`RankingTable`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/ranking/components/RankingTable.kt) rendert alle Spieler in einer einfachen `Column` mit `rows.forEach` ohne `LazyColumn`, ohne `padding`, ohne `HorizontalDivider`, ohne Header-Hervorhebung und ohne M3-Typografie. Bei Turnieren mit vielen Teilnehmern oder im Querformat sind die unteren Plätze **nicht erreichbar (abgeschnitten)**.
* **Inkonsistente Punkte-Formatierung:** In [`CrosstableRow`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/components/CrosstableRow.kt) werden ganze Punkte sauber ohne `.0` formatiert (`2` statt `2.0`), in [`RankingTable`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/ranking/components/RankingTable.kt) hingegen roh via `"${row.points}"` (`2.0`) ausgegeben.

### GAP-UX2: Fehlendes Sync-Feedback und fehlender manueller Sync im `TournamentSetupScreen` (🟠 Hoch)
* Wenn der Turnierleiter die App im Turnierraum startet und erst kurz darauf WLAN aktiviert, gibt es im [`TournamentSetupScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/tournamentsetup/TournamentSetupScreen.kt) **keine Möglichkeit**, die Spielerliste manuell mit WordPress zu synchronisieren (weder Pull-to-Refresh noch ein Sync-Icon in der TopAppBar).
* Zudem wird `state.isLoading` aus [`TournamentSetupUiState`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/tournamentsetup/TournamentSetupUiState.kt) im [`TournamentSetupScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/tournamentsetup/TournamentSetupScreen.kt) überhaupt nicht ausgewertet.

### GAP-UX3: Finalisierungs-Absicherung und Read-Only-Modus im `RankingScreen` (🟡 Mittel)
* **Keine Warnung bei offenen Partien:** Der Turnierleiter kann im [`RankingScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/ranking/RankingScreen.kt) jederzeit auf „Turnier abschließen“ klicken, selbst wenn noch 0 oder nur ein Teil der Partien gespielt wurden. Der Bestätigungsdialog weist nicht darauf hin, falls noch offene Partien (`GameResult.Open`) existieren.
* **Kein Read-Only-Schutz im RankingScreen:** Wenn ein Turnier serverseitig bereits finalisiert wurde (`SyncStatus.FINALIZED` in der Kreuztabelle via HTTP 403), der Nutzer dann aber auf „Rangliste“ tippt, weiß [`RankingViewModel`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/ranking/RankingViewModel.kt) nichts vom `FINALIZED`-Status im `TournamentStorage` und bietet den Button „Turnier abschließen“ weiterhin aktiv an.

### GAP-UX4: Gefährliches Verhalten beim „Turnier abbrechen“ / Zurück-Button in `CrosstableScreen` (🟠 Hoch)
* In [`CrosstableScreen`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/CrosstableScreen.kt) führt sowohl der System-Back-Button als auch der Pfeil oben links zum Dialog *„Turnier abbrechen? Das aktuelle Turnier wird beendet.“*
* Bestätigt der Nutzer dies, ruft [`AppNavHost`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/navigation/AppNavHost.kt) `vm.resetTournament()` auf, was in [`TournamentStorage.resetAll()`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/TournamentStorage.kt) **den gesamten DataStore leert** (`it.clear()`):
  1. Da `TournamentStorage` und `AppSettingsStorage` in [`AppContainer`](../app/src/main/java/de/kindermaenner/monatsblitz/app/AppContainer.kt) **dieselbe DataStore-Instanz (`"tournament_state"`)** teilen, löscht `it.clear()` ungewollt auch alle Benutzereinstellungen (`AppSettingsStorage`)!
  2. Wurde das Turnier bereits per Auto-Sync an den WordPress-Server übertragen (`remoteId != null`), bleibt auf dem Server ein verwaistes Turnier mit Status `"open"` zurück, während die App lokal die Referenz verliert und für denselben Tag kein neues Turnier synchronisieren kann (wegen `UNIQUE KEY unique_tournament_date (year, month, day)` im WordPress-Plugin!).
  3. Ist das Turnier bereits im Status `FINALIZED` (Schreibschutz), heißt der Zurück-Button trotzdem *„Turnier abbrechen?“* statt schlicht zum Setup für ein neues Turnier zurückzukehren.

### GAP-UX5: Kreuztabellen-Orientierung und Ergebnisanzeige (🟢 Niedrig)
* **Halbpunkt-Darstellung:** In [`ResultPickerDialog`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/components/ResultCell.kt) wird `½` angezeigt, in der Zelle selbst ([`GameResult.Remis.displayName`](../app/src/main/java/de/kindermaenner/monatsblitz/domain/model/GameResult.kt)) jedoch der 3-stellige String `"1/2"`, was in schmalen Zellen (`44.dp`) unruhiger wirkt als `"½"`.
* **Spaltenüberschriften in der Kreuztabelle:** [`CrosstableHeader`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/components/CrosstableHeader.kt) zeigt in den Spaltenköpfen nur die laufenden Nummern `1..N`. Bei größeren Teilnehmerfeldern (>10 Spieler) verliert man beim horizontalen Scrollen leicht die Zuordnung, welcher Spieler hinter Spalte `14` steckt (Abhilfe: Namenskürzel z. B. `M.M.` unter/über der Spaltennummer oder Hervorhebung von aktiver Zeile/Spalte).

---

## 5. Android-Entwicklung Best Practices

### GAP-AND1: Hintergrund-Synchronisation an `viewModelScope` gekoppelt (🟠 Hoch)
* In [`CrosstableViewModel.setResult()`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/CrosstableViewModel.kt) wird `tournamentSyncManager?.triggerSync(tournament.id)` innerhalb von `viewModelScope.launch` ausgeführt.
* Navigiert der Nutzer nach der letzten Ergebniseingabe oder nach dem Zurückkehren aus dem Screen weg (und wird das ViewModel zerstört) oder schließt die App kurzzeitig, bricht die laufende Coroutine mitten im Netzwerk-Request ab (z. B. nachdem `POST /game` auf dem Server ankam, aber bevor `gameDao.markGameAsSynced()` lokal ausgeführt werden konnte).
* **Best Practice:** [`TournamentSyncManager`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/sync/TournamentSyncManager.kt) ist ein Singleton im [`AppContainer`](../app/src/main/java/de/kindermaenner/monatsblitz/app/AppContainer.kt) und sollte einen eigenen Application-weiten `CoroutineScope(SupervisorJob() + Dispatchers.IO)` (oder `WorkManager` für garantierte Hintergrund-Ausführung bei Netzwiederkehr gemäß [`01-FunctionalRequirements.md` §1.5](./specs/01-FunctionalRequirements.md)) nutzen.

### GAP-AND2: `fallbackToDestructiveMigration` und fehlender Schema-Export in Room (🟠 Hoch)
* [`AppDatabase`](../app/src/main/java/de/kindermaenner/monatsblitz/infrastructure/persistence/room/AppDatabase.kt) setzt `exportSchema = false` und `.fallbackToDestructiveMigration(dropAllTables = true)`.
* **Risiko:** Bei jedem zukünftigen App-Update mit einer Erhöhung der DB-Version werden sämtliche lokalen Turnier- und Spielerdaten kommentarlos gelöscht. Zudem können ohne `exportSchema = true` keine automatisierten Room-Migrationstests geschrieben werden.

### GAP-AND3: Fehlende Lifecycle-Awareness bei Flow-Collection in Compose (🟡 Mittel)
* In [`MainActivity.kt`](../app/src/main/java/de/kindermaenner/monatsblitz/MainActivity.kt), [`TournamentSetupScreen.kt`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/tournamentsetup/TournamentSetupScreen.kt), [`CrosstableScreen.kt`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/CrosstableScreen.kt), [`RankingScreen.kt`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/ranking/RankingScreen.kt) und [`SettingsScreen.kt`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/screens/SettingsScreen.kt) wird `collectAsState()` verwendet.
* **Best Practice:** Verwendung von `collectAsStateWithLifecycle()` (`androidx.lifecycle:lifecycle-runtime-compose`), damit Datenbank-Flows (`SharingStarted.WhileSubscribed(5000)`) tatsächlich pausiert werden, wenn die App im Hintergrund liegt.

### GAP-AND4: Ineffiziente Datenbankabfrage in `RankingViewModel` & Re-Allokationen in `CrosstableScreen` (🟡 Mittel)
* **Ungefiltertes Nachladen aller Spieler:** [`RankingViewModel.observeRanking()`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/ranking/RankingViewModel.kt) ruft bei jeder Emission `playerDao.getAllPlayers()` auf, anstatt per SQL-Join direkt die Turnierteilnehmer samt Rangliste aus Room zu beziehen.
* **Berechnung in der Composition:** In [`CrosstableScreen.kt`](../app/src/main/java/de/kindermaenner/monatsblitz/ui/crosstable/CrosstableScreen.kt) wird innerhalb des `LazyColumn` `items`-Blocks für jede Zeile eine $O(N)$-Schleife mit linearem Lookup `tournament.findGame(...)` ($O(M)$ über `games.values.firstOrNull`) während der UI-Composition ausgeführt (insgesamt $O(N^2 \cdot M)$ pro Recomposition/Scroll). Diese Aufbereitung gehört vorberechnet in das `CrosstableViewModel` bzw. `CrosstableUiState`.

### GAP-AND5: Fehlende Netzwerk-Konnektivitätsüberwachung (ConnectivityManager) (🟡 Mittel)
* Laut [`01-FunctionalRequirements.md` §1.5](./specs/01-FunctionalRequirements.md) gilt: *„War ein Gerät längere Zeit offline, holt der automatische Sync alle ungespeicherten Ergebnisse gebündelt nach, **sobald wieder eine Verbindung besteht** oder ein neues Ergebnis eingegeben wird.“*
* Aktuell wird der Sync **nur** bei einer neuen Ergebniseingabe oder manuell per Klick ausgelöst; es gibt keinen `ConnectivityManager.NetworkCallback`, der bei Rückkehr der Internetverbindung automatisch `tournamentSyncManager.triggerSync()` anstößt.

---

## 6. Empfohlene Roadmap (Architekturkonformes Vorgehen)

Um die identifizierten Gaps im Sinne von [`AGENT.md`](../AGENT.md) kontrolliert und ohne Regressionsrisiko aufzulösen, empfiehlt sich eine Aufteilung in vier abgeschlossene Arbeitspakete:

1. **Paket 1 – Fachliche Korrektheit & Datenintegrität (GAP-F1, GAP-UX4):**
   * Bereinigung des `dirty`-Verhaltens für initiale `Open`-Spiele und Klärung des kanonischen vs. gerichteten Speichermodells in `games`.
   * Trennung der DataStore-Preferences (oder gezieltes Löschen nur der Turnier-Keys in `TournamentStorage.resetAll()`, damit `AppSettingsStorage` erhalten bleibt) und sauberes Handling beim Verlassen eines bereits synchronisierten/finalisierten Turniers.
2. **Paket 2 – Clean Architecture Refactoring (GAP-A1 bis GAP-A4):**
   * Rückführung aller Infrastruktur-Abhängigkeiten aus `domain.usecase` und `ui.*` hinter die Repository-Interfaces (`PlayerRepository`, `TournamentRepository`, `SettingsRepository`).
   * Entfernung der verwaisten Dateien aus ADR-001.
3. **Paket 3 – Android Lifecycle & Resilienz (GAP-AND1 bis GAP-AND5):**
   * Eigener Application-Scope für `TournamentSyncManager` + optionaler `NetworkCallback` für Auto-Sync bei Verbindungsrückkehr.
   * Umstellung auf `collectAsStateWithLifecycle()` und Aktivierung von `exportSchema = true` in Room.
4. **Paket 4 – UX & Material 3 Vervollständigung (GAP-UX1 bis GAP-UX3, GAP-F3):**
   * M3-Überarbeitung von `RankingScreen` / `RankingTable` (`Scaffold`, `LazyColumn`, Punktformatierung, Warnhinweis bei offenen Partien, Read-Only-Status).
   * Manueller Spieler-Sync & Sync-Status im `TournamentSetupScreen`.
