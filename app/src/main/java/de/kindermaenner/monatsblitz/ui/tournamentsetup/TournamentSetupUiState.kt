package de.kindermaenner.monatsblitz.ui.tournamentsetup

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player

data class TournamentSetupUiState(
    val players: List<Player> = emptyList(),
    val selectedPlayerIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val showOnlySelected: Boolean = false,

    val selectedMode: GameMode = GameMode.BLITZ_5_0,
    val doubleRound: Boolean = false,

    val showAddPlayerDialog: Boolean = false,
    val newPlayerVorname: String = "",
    val newPlayerName: String = "",

    val isLoading: Boolean = false
)
