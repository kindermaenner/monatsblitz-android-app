package de.sgkoenigslutter.monatsblitz.ui.screens

import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Player

data class HomeUiState(
    val players: List<Player> = emptyList(),
    val selectedPlayerIds: Set<Int> = emptySet(),

    val selectedMode: GameMode = GameMode.BLITZ_5_0,
    val doubleRound: Boolean = false,

    val isLoading: Boolean = false
)

