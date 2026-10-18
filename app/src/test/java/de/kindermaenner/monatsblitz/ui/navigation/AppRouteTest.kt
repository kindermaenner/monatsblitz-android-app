package de.kindermaenner.monatsblitz.ui.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {

    private val json = Json

    @Test
    fun `AppRoute serialization and equality works for all routes`() {
        assertEquals(AppRoute.TournamentSetup, AppRoute.TournamentSetup)
        assertEquals(AppRoute.Settings, AppRoute.Settings)
        assertEquals(AppRoute.Crosstable(123L), AppRoute.Crosstable(123L))
        assertEquals(AppRoute.Ranking(456L), AppRoute.Ranking(456L))

        val setupJson = json.encodeToString(AppRoute.TournamentSetup)
        val decodedSetup = json.decodeFromString<AppRoute.TournamentSetup>(setupJson)
        assertEquals(AppRoute.TournamentSetup, decodedSetup)

        val settingsJson = json.encodeToString(AppRoute.Settings)
        val decodedSettings = json.decodeFromString<AppRoute.Settings>(settingsJson)
        assertEquals(AppRoute.Settings, decodedSettings)

        val crossJson = json.encodeToString(AppRoute.Crosstable(789L))
        val decodedCross = json.decodeFromString<AppRoute.Crosstable>(crossJson)
        assertEquals(789L, decodedCross.id)

        val rankJson = json.encodeToString(AppRoute.Ranking(999L))
        val decodedRank = json.decodeFromString<AppRoute.Ranking>(rankJson)
        assertEquals(999L, decodedRank.id)
    }
}
