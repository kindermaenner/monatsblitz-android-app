package de.kindermaenner.monatsblitz.ui.tournamentsetup

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import java.time.LocalDate

data class TournamentSetupUiState(
    val players: List<Player> = emptyList(),
    val selectedPlayerIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val showOnlySelected: Boolean = false,

    val selectedDate: LocalDate = LocalDate.now(),
    val selectedMode: GameMode = GameMode.BLITZ_5_0,
    val doubleRound: Boolean = false,

    val showAddPlayerDialog: Boolean = false,
    val newPlayerVorname: String = "",
    val newPlayerName: String = "",
    val addPlayerError: String? = null,

    val isLoading: Boolean = false,
    val isSyncingPlayers: Boolean = false,
    val showDetailedSyncIndicators: Boolean = true
)
