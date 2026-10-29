package de.kindermaenner.monatsblitz.ui.tournamentsetup

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerFilterMode
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.repository.RemoteOpenTournamentInfo
import java.time.LocalDate

data class TournamentSetupUiState(
    val players: List<Player> = emptyList(),
    val selectedPlayerIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val playerFilterMode: PlayerFilterMode = PlayerFilterMode.ACTIVE,
    val showOnlySelected: Boolean = false,
    val activeCutoffMonths: Int = 12,
    val sortField: PlayerSortField = PlayerSortField.FIRST_NAME,
    val sortOrder: PlayerSortOrder = PlayerSortOrder.ASCENDING,

    val selectedDate: LocalDate = LocalDate.now(),
    val selectedMode: GameMode? = null,
    val availableModes: List<GameMode> = emptyList(),
    val doubleRound: Boolean = false,
    val isLocalOnly: Boolean = false,
    val isServerConfigured: Boolean = true,
    val isStandaloneOffline: Boolean = false,

    val showAddPlayerDialog: Boolean = false,
    val newPlayerVorname: String = "",
    val newPlayerName: String = "",
    val newPlayerIsLocal: Boolean = false,
    val addPlayerError: String? = null,

    val showAddModeDialog: Boolean = false,
    val showConfirmAddModeDialog: Boolean = false,
    val newModeName: String = "",
    val newModePermanent: Boolean = true,
    val addModeError: String? = null,

    val remoteOpenTournament: RemoteOpenTournamentInfo? = null,
    val adoptError: String? = null,

    val isLoading: Boolean = false,
    val isSyncingPlayers: Boolean = false,
    val isSyncingModes: Boolean = false,
    val showDetailedSyncIndicators: Boolean = true
)
