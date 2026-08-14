package de.kindermaenner.monatsblitz.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object TournamentSetup : AppRoute

    @Serializable
    data class Crosstable(val id: Long) : AppRoute

    @Serializable
    data class Ranking(val id: Long) : AppRoute
}
