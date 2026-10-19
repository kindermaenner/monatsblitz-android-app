package de.kindermaenner.monatsblitz.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TournamentStorageTest {

    private val dataStore = mockk<DataStore<Preferences>>()
    private val storage = TournamentStorage(dataStore)

    private val tournamentIdKey = longPreferencesKey("tournament_id")
    private val finalizedKey = booleanPreferencesKey("tournament_finalized")

    @Test
    fun `getTournamentState returns null when no id stored`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[tournamentIdKey] } returns null
        every { prefs[finalizedKey] } returns null
        every { dataStore.data } returns flowOf(prefs)

        val result = storage.getTournamentState().first()

        assertNull(result)
    }

    @Test
    fun `getTournamentState returns state when id stored`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[tournamentIdKey] } returns 123L
        every { prefs[finalizedKey] } returns true
        every { dataStore.data } returns flowOf(prefs)

        val result = storage.getTournamentState().first()

        assertEquals(123L, result?.tournamentId)
        assertEquals(true, result?.finalized)
    }

    @Test
    fun `getTournamentState defaults finalized to false when finalizedKey is null`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[tournamentIdKey] } returns 789L
        every { prefs[finalizedKey] } returns null
        every { dataStore.data } returns flowOf(prefs)

        val result = storage.getTournamentState().first()

        assertEquals(789L, result?.tournamentId)
        assertEquals(false, result?.finalized)
    }

    @Test
    fun `saveTournamentState updates preferences`() = runTest {
        val initialPrefs = mutablePreferencesOf()
        var updatedPrefs: Preferences? = null
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }

        storage.saveTournamentState(456L, finalized = true)

        assertEquals(456L, updatedPrefs?.get(tournamentIdKey))
        assertEquals(true, updatedPrefs?.get(finalizedKey))
    }

    @Test
    fun `resetAll removes only tournament keys and preserves other preferences`() = runTest {
        val otherKey = booleanPreferencesKey("show_detailed_sync_indicators")
        val initialPrefs = mutablePreferencesOf(
            tournamentIdKey to 100L,
            finalizedKey to true,
            otherKey to false
        )
        var updatedPrefs: Preferences? = null
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }

        storage.resetAll()

        assertNull(updatedPrefs?.get(tournamentIdKey))
        assertNull(updatedPrefs?.get(finalizedKey))
        assertEquals(false, updatedPrefs?.get(otherKey))
    }

    @Test
    fun `finalizeTournament sets finalized to true`() = runTest {
        val initialPrefs = mutablePreferencesOf(finalizedKey to false)
        var updatedPrefs: Preferences? = null
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }

        storage.finalizeTournament()

        assertEquals(true, updatedPrefs?.get(finalizedKey))
    }
}
