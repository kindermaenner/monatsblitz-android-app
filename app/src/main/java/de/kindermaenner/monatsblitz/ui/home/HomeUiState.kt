package de.kindermaenner.monatsblitz.ui.home

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player

data class HomeUiState(
    val players: List<Player> = emptyList(),
    val selectedPlayerIds: Set<Long> = emptySet(),

    val selectedMode: GameMode = GameMode.BLITZ_5_0,
    val doubleRound: Boolean = false,

    val isLoading: Boolean = false
)