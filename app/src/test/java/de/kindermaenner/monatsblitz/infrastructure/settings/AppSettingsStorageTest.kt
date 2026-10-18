package de.kindermaenner.monatsblitz.infrastructure.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsStorageTest {

    private val dataStore = mockk<DataStore<Preferences>>()
    private val key = booleanPreferencesKey("show_detailed_sync_indicators")

    @Test
    fun `showDetailedSyncIndicators returns default true when key not set`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[key] } returns null
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        val result = storage.showDetailedSyncIndicators.first()
        assertTrue("Default should be true", result)
    }

    @Test
    fun `showDetailedSyncIndicators returns stored boolean value`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[key] } returns false
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        val result = storage.showDetailedSyncIndicators.first()
        assertFalse("Should be false", result)
    }

    @Test
    fun `setShowDetailedSyncIndicators edits preferences`() = runTest {
        val initialPrefs = androidx.datastore.preferences.core.mutablePreferencesOf()
        var updatedPrefs: Preferences? = null
        every { dataStore.data } returns flowOf(mockk())
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }
        val storage = AppSettingsStorage(dataStore)

        storage.setShowDetailedSyncIndicators(false)

        assertEquals(false, updatedPrefs?.get(key))
    }
}
