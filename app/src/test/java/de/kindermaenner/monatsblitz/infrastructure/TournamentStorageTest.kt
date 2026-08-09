package de.kindermaenner.monatsblitz.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
    fun `saveTournamentState calls updateData`() = runTest {
        coEvery { dataStore.updateData(any()) } returns mockk()

        storage.saveTournamentState(456L, true)

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `resetAll calls updateData`() = runTest {
        coEvery { dataStore.updateData(any()) } returns mockk()

        storage.resetAll()

        coVerify { dataStore.updateData(any()) }
    }

    @Test
    fun `finalizeTournament calls updateData`() = runTest {
        coEvery { dataStore.updateData(any()) } returns mockk()

        storage.finalizeTournament()

        coVerify { dataStore.updateData(any()) }
    }
}
