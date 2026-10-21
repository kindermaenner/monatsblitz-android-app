package de.kindermaenner.monatsblitz.ui.tournamentsetup

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.repository.RemoteOpenTournamentInfo
import java.time.LocalDate

data class TournamentSetupUiState(
    val players: List<Player> = emptyList(),
    val selectedPlayerIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val showOnlySelected: Boolean = false,
    val sortField: PlayerSortField = PlayerSortField.LAST_NAME,
    val sortOrder: PlayerSortOrder = PlayerSortOrder.ASCENDING,

    val selectedDate: LocalDate = LocalDate.now(),
    val selectedMode: GameMode? = null,
    val doubleRound: Boolean = false,

    val showAddPlayerDialog: Boolean = false,
    val newPlayerVorname: String = "",
    val newPlayerName: String = "",
    val addPlayerError: String? = null,

    val remoteOpenTournament: RemoteOpenTournamentInfo? = null,
    val adoptError: String? = null,

    val isLoading: Boolean = false,
    val isSyncingPlayers: Boolean = false,
    val showDetailedSyncIndicators: Boolean = true
)
