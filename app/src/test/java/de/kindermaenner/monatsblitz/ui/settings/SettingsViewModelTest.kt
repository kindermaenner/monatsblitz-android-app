package de.kindermaenner.monatsblitz.ui.settings

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncModesUseCase
import de.kindermaenner.monatsblitz.domain.usecase.ConnectionTestResult
import de.kindermaenner.monatsblitz.domain.usecase.TestBackendConnectionUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val testBackendConnectionUseCase = mockk<TestBackendConnectionUseCase>()
    private val syncPlayersUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase>(relaxed = true)

    private val serverUrlFlow = MutableStateFlow("")
    private val apiKeyFlow = MutableStateFlow("")
    private val syncFlow = MutableStateFlow(true)
    private val offlineFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        every { settingsRepository.serverUrl } returns serverUrlFlow
        every { settingsRepository.apiKey } returns apiKeyFlow
        every { settingsRepository.showDetailedSyncIndicators } returns syncFlow
        every { settingsRepository.standaloneOfflineMode } returns offlineFlow
        coEvery { testBackendConnectionUseCase(any(), any()) } returns ConnectionTestResult.Success(playerCount = 5)
    }

    @Test
    fun `initial inputs are populated from repository`() = runTest {
        serverUrlFlow.value = "https://my-club.de"
        apiKeyFlow.value = "my-secret-key"

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        advanceUntilIdle()

        assertEquals("https://my-club.de", viewModel.serverUrlInput.value)
        assertEquals("my-secret-key", viewModel.apiKeyInput.value)
    }

    @Test
    fun `onServerUrlChanged and onApiKeyChanged update input state and clear results`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        advanceUntilIdle()

        viewModel.onServerUrlChanged("https://new-url.de")
        viewModel.onApiKeyChanged("new-key")

        assertEquals("https://new-url.de", viewModel.serverUrlInput.value)
        assertEquals("new-key", viewModel.apiKeyInput.value)
        assertNull(viewModel.testResult.value)
    }

    @Test
    fun `saveSettings saves trimmed url and api key to repository`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        advanceUntilIdle()

        viewModel.onServerUrlChanged("  https://my-club.de/  ")
        viewModel.onApiKeyChanged("  secret-key  ")
        viewModel.saveSettings()
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setServerUrl("https://my-club.de/") }
        coVerify(exactly = 1) { settingsRepository.setApiKey("secret-key") }
        assertTrue(viewModel.isSavedMessageVisible.value)

        viewModel.dismissSavedMessage()
        assertEquals(false, viewModel.isSavedMessageVisible.value)
    }

    @Test
    fun `testConnection runs usecase and updates state`() = runTest {
        coEvery { testBackendConnectionUseCase("https://my-club.de", "my-key") } returns
            ConnectionTestResult.Success(playerCount = 14)

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        advanceUntilIdle()

        viewModel.onServerUrlChanged("https://my-club.de")
        viewModel.onApiKeyChanged("my-key")
        viewModel.testConnection()
        advanceUntilIdle()

        assertEquals(false, viewModel.isTestingConnection.value)
        val result = viewModel.testResult.value
        assertTrue(result is ConnectionTestResult.Success)
        assertEquals(14, (result as ConnectionTestResult.Success).playerCount)
    }

    @Test
    fun `applyQrConfig updates inputs and triggers connection test`() = runTest {
        coEvery { testBackendConnectionUseCase("https://sc-example.de", "qr-key") } returns
            ConnectionTestResult.Success(playerCount = 10)

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase, syncPlayersUseCase)
        advanceUntilIdle()

        viewModel.applyQrConfig(de.kindermaenner.monatsblitz.domain.model.QrSetupConfig("https://sc-example.de", "qr-key"))
        advanceUntilIdle()

        assertEquals("https://sc-example.de", viewModel.serverUrlInput.value)
        assertEquals("qr-key", viewModel.apiKeyInput.value)
        assertTrue(viewModel.testResult.value is ConnectionTestResult.Success)
        coVerify(exactly = 1) { settingsRepository.setServerUrl("https://sc-example.de") }
        coVerify(exactly = 1) { settingsRepository.setApiKey("qr-key") }
        coVerify(atLeast = 1) { syncPlayersUseCase() }
    }

    @Test
    fun `applyQrConfig applies mode preselection settings when provided`() = runTest {
        coEvery { testBackendConnectionUseCase("https://sc-example.de", "qr-key") } returns
            ConnectionTestResult.Success(playerCount = 10)

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase, syncPlayersUseCase)
        advanceUntilIdle()

        viewModel.applyQrConfig(
            de.kindermaenner.monatsblitz.domain.model.QrSetupConfig(
                serverUrl = "https://sc-example.de",
                apiKey = "qr-key",
                modePreselectionType = de.kindermaenner.monatsblitz.domain.model.ModePreselectionType.ROTATION,
                fixedMode = "3+2",
                rotationModes = listOf("3+2", "5+0")
            )
        )
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setModePreselectionType(de.kindermaenner.monatsblitz.domain.model.ModePreselectionType.ROTATION) }
        coVerify(exactly = 1) { settingsRepository.setFixedMode("3+2") }
        coVerify(exactly = 1) { settingsRepository.setRotationModes(listOf("3+2", "5+0")) }
    }

    @Test
    fun `showDetailedSyncIndicators reflects repository state`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        backgroundScope.launch { viewModel.showDetailedSyncIndicators.collect() }
        advanceUntilIdle()

        assertEquals(true, viewModel.showDetailedSyncIndicators.value)

        syncFlow.value = false
        advanceUntilIdle()

        assertEquals(false, viewModel.showDetailedSyncIndicators.value)
    }

    @Test
    fun `setShowDetailedSyncIndicators delegates to repository`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        viewModel.setShowDetailedSyncIndicators(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setShowDetailedSyncIndicators(false) }
    }

    @Test
    fun `SettingsViewModelFactory creates SettingsViewModel and throws for unknown class`() {
        val factory = SettingsViewModelFactory(settingsRepository, testBackendConnectionUseCase)

        val vm = factory.create(SettingsViewModel::class.java)
        assertNotNull(vm)

        class UnknownViewModel : ViewModel()
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }

    @Test
    fun `setModePreselectionType delegates to repository`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        viewModel.setModePreselectionType(ModePreselectionType.ROTATION)
        advanceUntilIdle()

        coVerify { settingsRepository.setModePreselectionType(ModePreselectionType.ROTATION) }
    }

    @Test
    fun `setFixedMode delegates to repository`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        viewModel.setFixedMode("10+0")
        advanceUntilIdle()

        coVerify { settingsRepository.setFixedMode("10+0") }
    }

    @Test
    fun `toggleRotationMode toggles mode in rotationModes list`() = runTest {
        every { settingsRepository.rotationModes } returns MutableStateFlow(listOf("3+2", "5+0"))
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        viewModel.toggleRotationMode("10+0")
        advanceUntilIdle()
        coVerify { settingsRepository.setRotationModes(listOf("3+2", "5+0", "10+0")) }

        viewModel.toggleRotationMode("5+0")
        advanceUntilIdle()
        coVerify { settingsRepository.setRotationModes(listOf("3+2")) }
    }

    @Test
    fun `saveSettings triggers syncModesUseCase on success`() = runTest {
        val syncModes = mockk<SyncModesUseCase>(relaxed = true)
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            syncPlayersUseCase = syncPlayersUseCase,
            syncModesUseCase = syncModes
        )
        viewModel.onServerUrlChanged("https://test.de")
        viewModel.onApiKeyChanged("secret")
        viewModel.saveSettings()
        advanceUntilIdle()

        coVerify { syncModes() }
    }

    @Test
    fun `setStandaloneOfflineMode delegates to repository`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        viewModel.setStandaloneOfflineMode(true)
        advanceUntilIdle()

        coVerify { settingsRepository.setStandaloneOfflineMode(true) }
    }
}
