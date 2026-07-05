package de.kindermaenner.monatsblitz.ui.screens

import de.kindermaenner.monatsblitz.data.model.GameMode
import de.kindermaenner.monatsblitz.data.model.Player

data class HomeUiState(
    val players: List<Player> = emptyList(),
    val selectedPlayerIds: Set<Int> = emptySet(),

    val selectedMode: GameMode = GameMode.BLITZ_5_0,
    val doubleRound: Boolean = false,

    val isLoading: Boolean = false
)

