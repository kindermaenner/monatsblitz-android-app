package de.sgkoenigslutter.monatsblitz.ui.navigation

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val TOURNAMENT = "tournament"
    const val TOURNAMENT_ID_ARG = "tournamentId"

    const val TOURNAMENT_WITH_ARG = "$TOURNAMENT/{$TOURNAMENT_ID_ARG}"

    fun tournamentPath(tournamentId: Int): String = "$TOURNAMENT/$tournamentId"
}
