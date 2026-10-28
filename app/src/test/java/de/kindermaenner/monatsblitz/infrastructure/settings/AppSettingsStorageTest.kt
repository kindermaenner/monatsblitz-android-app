package de.kindermaenner.monatsblitz.infrastructure.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.mockk.coEvery
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
    private val syncKey = booleanPreferencesKey("show_detailed_sync_indicators")
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val apiKeyKey = stringPreferencesKey("api_key")
    private val standaloneOfflineModeKey = booleanPreferencesKey("standalone_offline_mode")

    @Test
    fun `showDetailedSyncIndicators returns default true when key not set`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[syncKey] } returns null
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        val result = storage.showDetailedSyncIndicators.first()
        assertTrue("Default should be true", result)
    }

    @Test
    fun `showDetailedSyncIndicators returns stored boolean value`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[syncKey] } returns false
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        val result = storage.showDetailedSyncIndicators.first()
        assertFalse("Should be false", result)
    }

    @Test
    fun `setShowDetailedSyncIndicators edits preferences`() = runTest {
        val initialPrefs = mutablePreferencesOf()
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

        assertEquals(false, updatedPrefs?.get(syncKey))
    }

    @Test
    fun `serverUrl returns stored string`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[serverUrlKey] } returns "https://example.com"
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        assertEquals("https://example.com", storage.serverUrl.first())
    }

    @Test
    fun `apiKey returns stored string`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[apiKeyKey] } returns "secret-key"
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        assertEquals("secret-key", storage.apiKey.first())
    }

    @Test
    fun `setServerUrl edits preferences`() = runTest {
        val initialPrefs = mutablePreferencesOf()
        var updatedPrefs: Preferences? = null
        every { dataStore.data } returns flowOf(mockk())
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }
        val storage = AppSettingsStorage(dataStore)

        storage.setServerUrl("https://new.site")

        assertEquals("https://new.site", updatedPrefs?.get(serverUrlKey))
    }

    @Test
    fun `setApiKey edits preferences`() = runTest {
        val initialPrefs = mutablePreferencesOf()
        var updatedPrefs: Preferences? = null
        every { dataStore.data } returns flowOf(mockk())
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }
        val storage = AppSettingsStorage(dataStore)

        storage.setApiKey("new-key")

        assertEquals("new-key", updatedPrefs?.get(apiKeyKey))
    }

    @Test
    fun `isConfigured returns false when url or key missing`() = runTest {
        val prefs1 = mockk<Preferences>()
        every { prefs1[serverUrlKey] } returns ""
        every { prefs1[apiKeyKey] } returns "key"
        every { prefs1[standaloneOfflineModeKey] } returns false
        every { dataStore.data } returns flowOf(prefs1)
        val storage1 = AppSettingsStorage(dataStore)
        assertFalse(storage1.isConfigured())

        val prefs2 = mockk<Preferences>()
        every { prefs2[serverUrlKey] } returns "https://example.com"
        every { prefs2[apiKeyKey] } returns null
        every { prefs2[standaloneOfflineModeKey] } returns false
        every { dataStore.data } returns flowOf(prefs2)
        val storage2 = AppSettingsStorage(dataStore)
        assertFalse(storage2.isConfigured())
    }

    @Test
    fun `isConfigured returns true when both url and key present`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[serverUrlKey] } returns "https://example.com"
        every { prefs[apiKeyKey] } returns "valid-key"
        every { prefs[standaloneOfflineModeKey] } returns false
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        assertTrue(storage.isConfigured())
        assertTrue(storage.isServerConfigured.first())
        assertTrue(storage.isConfigured.first())
    }

    @Test
    fun `isConfigured flow returns true when standaloneOfflineMode is true even without server`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[serverUrlKey] } returns ""
        every { prefs[apiKeyKey] } returns ""
        every { prefs[standaloneOfflineModeKey] } returns true
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        assertFalse(storage.isServerConfigured.first())
        assertTrue(storage.isConfigured.first())
        assertTrue(storage.isConfigured())
    }
}
