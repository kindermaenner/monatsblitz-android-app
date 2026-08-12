package de.kindermaenner.monatsblitz.ui.navigation

sealed class AppRoute(val path: String) {

    data object Home : AppRoute("home")

    data class Tournament(val id: Long) : AppRoute("tournament/$id") {
        companion object {
            const val BASE = "tournament"
            const val ARG_ID = "id"
            const val FULL = "$BASE/{$ARG_ID}"
        }
    }

    data class Ranking(val tournamentId: Long) :
        AppRoute("ranking/$tournamentId") {

        companion object {
            const val ARG_ID = "tournamentId"
            const val TEMPLATE = "ranking/{$ARG_ID}"
        }
    }
}
