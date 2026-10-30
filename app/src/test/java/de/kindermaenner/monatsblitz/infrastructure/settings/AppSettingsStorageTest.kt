package de.kindermaenner.monatsblitz.infrastructure.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
    private val activePlayersCutoffMonthsKey = intPreferencesKey("active_players_cutoff_months")

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

    @Test
    fun `activePlayersCutoffMonths returns default 12 when not set`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[activePlayersCutoffMonthsKey] } returns null
        every { dataStore.data } returns flowOf(prefs)
        val storage = AppSettingsStorage(dataStore)

        assertEquals(12, storage.activePlayersCutoffMonths.first())
    }

    @Test
    fun `setActivePlayersCutoffMonths edits preferences`() = runTest {
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

        storage.setActivePlayersCutoffMonths(6)

        assertEquals(6, updatedPrefs?.get(activePlayersCutoffMonthsKey))
    }

    @Test
    fun `playerSortField and playerSortOrder return default and stored values`() = runTest {
        val sortFieldKey = stringPreferencesKey("player_sort_field")
        val sortOrderKey = stringPreferencesKey("player_sort_order")

        val initialPrefs = mutablePreferencesOf()
        var updatedPrefs: Preferences? = null
        every { dataStore.data } returns flowOf(initialPrefs)
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }
        val storage = AppSettingsStorage(dataStore)

        assertEquals(de.kindermaenner.monatsblitz.domain.model.PlayerSortField.FIRST_NAME, storage.playerSortField.first())
        assertEquals(de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder.ASCENDING, storage.playerSortOrder.first())

        storage.setPlayerSortField(de.kindermaenner.monatsblitz.domain.model.PlayerSortField.LAST_NAME)
        assertEquals(de.kindermaenner.monatsblitz.domain.model.PlayerSortField.LAST_NAME.name, updatedPrefs?.get(sortFieldKey))

        storage.setPlayerSortOrder(de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder.DESCENDING)
        assertEquals(de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder.DESCENDING.name, updatedPrefs?.get(sortOrderKey))
    }

    @Test
    fun `useShortPlayerNames and autoAdoptOpenTournament read and write correctly`() = runTest {
        val shortNamesKey = booleanPreferencesKey("use_short_player_names")
        val autoAdoptKey = booleanPreferencesKey("auto_adopt_open_tournament")

        val initialPrefs = mutablePreferencesOf()
        var updatedPrefs: Preferences? = null
        every { dataStore.data } returns flowOf(initialPrefs)
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }
        val storage = AppSettingsStorage(dataStore)

        assertTrue(storage.useShortPlayerNames.first())
        assertFalse(storage.autoAdoptOpenTournament.first())

        storage.setUseShortPlayerNames(false)
        assertEquals(false, updatedPrefs?.get(shortNamesKey))

        storage.setAutoAdoptOpenTournament(true)
        assertEquals(true, updatedPrefs?.get(autoAdoptKey))
    }

    @Test
    fun `mode preselection and rotation settings read and write correctly`() = runTest {
        val modeTypeKey = stringPreferencesKey("mode_preselection_type")
        val fixedModeKey = stringPreferencesKey("fixed_mode")
        val rotationModesKey = stringPreferencesKey("rotation_modes")

        val initialPrefs = mutablePreferencesOf()
        var updatedPrefs: Preferences? = null
        every { dataStore.data } returns flowOf(initialPrefs)
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }
        val storage = AppSettingsStorage(dataStore)

        assertEquals(de.kindermaenner.monatsblitz.domain.model.ModePreselectionType.NONE, storage.modePreselectionType.first())
        assertEquals("", storage.fixedMode.first())
        assertEquals(emptyList<String>(), storage.rotationModes.first())

        storage.setModePreselectionType(de.kindermaenner.monatsblitz.domain.model.ModePreselectionType.ROTATION)
        assertEquals(de.kindermaenner.monatsblitz.domain.model.ModePreselectionType.ROTATION.name, updatedPrefs?.get(modeTypeKey))

        storage.setFixedMode("3+2")
        assertEquals("3+2", updatedPrefs?.get(fixedModeKey))

        storage.setRotationModes(listOf("3+2", "5+0"))
        assertEquals("3+2;5+0", updatedPrefs?.get(rotationModesKey))
    }

    @Test
    fun `setStandaloneOfflineMode and isServerConfigured suspend helper operate as expected`() = runTest {
        val initialPrefs = mutablePreferencesOf()
        var updatedPrefs: Preferences? = null
        every { dataStore.data } returns flowOf(initialPrefs)
        coEvery { dataStore.updateData(any()) } coAnswers {
            val transform = firstArg<suspend (Preferences) -> Preferences>()
            val updated = transform(initialPrefs)
            updatedPrefs = updated
            updated
        }
        val storage = AppSettingsStorage(dataStore)

        storage.setStandaloneOfflineMode(true)
        assertEquals(true, updatedPrefs?.get(standaloneOfflineModeKey))

        assertFalse(storage.isServerConfigured())
    }

    @Test
    fun `getServerUrlSync and getApiKeySync return cached or datastore values`() = runTest {
        val prefs = mockk<Preferences>()
        every { prefs[serverUrlKey] } returns "https://sync.example.com"
        every { prefs[apiKeyKey] } returns "sync-key"
        every { dataStore.data } returns flowOf(prefs)

        val storage = AppSettingsStorage(dataStore)
        assertEquals("https://sync.example.com", storage.getServerUrlSync())
        assertEquals("sync-key", storage.getApiKeySync())

        // Subsequent call returns cached value
        assertEquals("https://sync.example.com", storage.getServerUrlSync())
        assertEquals("sync-key", storage.getApiKeySync())
    }
}
