# Monatsblitz Android App - Agent Guidance

## Project Overview
Android app for managing blitz chess tournaments. Players select participants, choose game mode, and track results on a tournament crosstable. Data connects to WordPress via REST API (infrastructure prepared but not yet integrated).

**Tech Stack:** Jetpack Compose, Navigation Compose, Kotlin 2.4.0, Gradle with version catalog

---

## Architecture Overview

### Three-Layer Structure
```
data/model/          → Domain models (Tournament, Player, GameMode, GameResult)
infrastructure/      → Data access (FakeRepo - currently mock data)
ui/                  → Compose screens, ViewModels, navigation, theme
```

### Data Flow
1. **Navigation Layer** (`ui/navigation/`) - Routes between Home and Tournament screens via `AppNavHost.kt`
2. **ViewModel Layer** (`ui/viewmodels/`) - `HomeViewModel` manages player selection, game mode, and tournament creation using `StateFlow<UiState>`
3. **Infrastructure Layer** (`infrastructure/FakeRepo.kt`) - Currently hardcoded player list and tournament factory
4. **UI Layer** (`ui/screens/`) - Compose components receive state from ViewModels via `collectAsState()`

**Key Pattern:** Data flows unidirectionally - UI listens to StateFlow, events call ViewModel methods which update state.

---

## Essential Domain Models

### Tournament.kt
- Stores player list, game mode, double-round flag
- `results: MutableMap<Pair<Int, Int>, GameResult>` - crosstable matrix (row, column) → result
- `getResult(row, col)` helper displays results in UI

### Player.kt
- `id`, `Name` (Nachname), `Vorname` (Vorname)
- Computed `fullName: String` property for display
- `PlayerDto` for API serialization, `NewPlayer` for create operations

### GameMode.kt (Enum)
```kotlin
BLITZ_3_2("3 + 2")     // 3 min + 2 sec increment
BLITZ_5_0("5 + 0")     // 5 min no increment
HANDICAP               // For uneven player levels
```

### GameResult.kt (Enum)
```kotlin
Loss("0"), Win("1"), Remis("1/2")
```

---

## Compose UI Patterns

### State Management Pattern
```kotlin
// In HomeViewModel
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState

// In HomeScreen Composable
val state by viewModel.uiState.collectAsState()
```

### Reusable Composables
- `ModeSelector` - Dropdown for GameMode selection (uses ExposedDropdownMenuBox)
- `CrosstableHeader`, `CrosstableRow`, `ResultCell` - Tournament display components with fixed widths (40dp cells)

### ViewModel Injection Pattern
ViewModels are instantiated at navigation composable with lambda dependencies:
```kotlin
HomeViewModel(
    getPlayers = { FakeRepo.getPlayers() },
    createTournament = { players, mode, double -> ... }
)
```

---

## Important Build Configuration

### Gradle Version Catalog (`gradle/libs.versions.toml`)
All dependencies use alias syntax from version catalog. To add dependencies:
1. Add version and library to `libs.versions.toml`
2. Add plugin alias if applicable
3. Reference via `libs.ALIAS` in `build.gradle.kts`

### Key Versions
- AGP 9.2.1, Kotlin 2.4.0
- Compose BOM 2026.06.00 (very recent)
- Retrofit 3.0.0 (prepared, not yet used)
- kotlinx-serialization 1.11.0 (for JSON)

### Build Commands
```bash
./gradlew build           # Full build
./gradlew installDebug    # Install on device/emulator
./gradlew runUnitTests    # Run tests
```

---

## Common Development Tasks

### Adding a New Screen
1. Create screen Composable in `ui/screens/NewScreen.kt`
2. Create UiState data class if needed (`NewScreenUiState.kt`)
3. Create ViewModel in `ui/viewmodels/NewViewModel.kt` extending `ViewModel()`
4. Add route to `ui/navigation/Routes.kt`
5. Add composable to `AppNavHost.kt` with ViewModel instantiation
6. Use `StateFlow` and `collectAsState()` for state binding

### Modifying Tournament Results
1. Update `Tournament.results` map (Pair<Int, Int> → GameResult)
2. Call `tournament.getResult(row, col)` in `ResultCell` to display
3. Add click handler in `CrosstableRow` to update results

### Connecting REST API (Future)
- Retrofit client prepared in dependencies
- Replace `FakeRepo.getPlayers()` with API call
- Use `kotlinx-serialization` with `@Serializable` annotations on DTOs
- Follow existing DTO pattern (e.g., `PlayerDto` already defined)

---

## Code Style & Conventions

- **German UI text** - All user-facing strings in German (Spieler, Turnier, Ergebnisse, etc.)
- **Kotlin DSL Gradle** - Use `build.gradle.kts` (not Groovy), prefer version catalog aliases
- **Compose Preview** - Add `@Preview` composables for isolated component testing
- **No global state** - Use ViewModel+StateFlow, never static fields for UI state
- **Descriptive widths/heights** - UI uses named constants (e.g., `CellWidth = 40.dp`, `NameWidth = 140.dp`)

---

## Next Priority Integration Points

1. **REST API Integration** - Replace FakeRepo with Retrofit calls to WordPress REST API
2. **Player Management** - Implement add/edit player screens with create operations
3. **Game Entry UI** - Build interface to enter game results (currently cells are clickable but non-functional)
4. **Result Calculation** - Compute tournament scores and rankings from crosstable
5. **Settings Screen** - Defined in Routes.kt but no implementation yet

---

## Testing Patterns

- Use `HomeViewModel` constructor injection for dependency mocking
- Test UI state changes via `collectAsState()` snapshots
- Leverage Compose's preview system for visual regression testing
- Mock `getPlayers()` and `createTournament()` lambdas in tests

