package de.kindermaenner.monatsblitz.ui.settings

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.model.TournamentDumpFileInfo
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentDumpRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncModesUseCase
import de.kindermaenner.monatsblitz.domain.usecase.ConnectionTestResult
import de.kindermaenner.monatsblitz.domain.usecase.TestBackendConnectionUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.RecreatePostsDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ReopenTournamentResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.VerifyAuthResponseDto
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
    private val tournamentDumpRepository = mockk<TournamentDumpRepository>(relaxed = true)
    private val monatsblitzApi = mockk<MonatsblitzApi>(relaxed = true)
    private val tournamentStorage = mockk<TournamentStorage>(relaxed = true)

    private val serverUrlFlow = MutableStateFlow("")
    private val apiKeyFlow = MutableStateFlow("")
    private val syncFlow = MutableStateFlow(true)
    private val offlineFlow = MutableStateFlow(false)
    private val maintenanceKeyFlow = MutableStateFlow("")

    @Before
    fun setup() {
        every { settingsRepository.serverUrl } returns serverUrlFlow
        every { settingsRepository.apiKey } returns apiKeyFlow
        every { settingsRepository.showDetailedSyncIndicators } returns syncFlow
        every { settingsRepository.standaloneOfflineMode } returns offlineFlow
        every { settingsRepository.maintenanceApiKey } returns maintenanceKeyFlow
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

    @Test
    fun `saveSettings with blank url or blank key returns early and invokes nothing`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        advanceUntilIdle()

        var successCalled = false
        // Blank URL
        viewModel.onServerUrlChanged("   ")
        viewModel.onApiKeyChanged("secret-key")
        viewModel.saveSettings(onSuccess = { successCalled = true })
        advanceUntilIdle()

        coVerify(exactly = 0) { testBackendConnectionUseCase(any(), any()) }
        coVerify(exactly = 0) { settingsRepository.setServerUrl(any()) }
        assertEquals(false, successCalled)
        assertEquals(false, viewModel.isSavedMessageVisible.value)

        // Blank API Key
        viewModel.onServerUrlChanged("https://test.de")
        viewModel.onApiKeyChanged("   ")
        viewModel.saveSettings(onSuccess = { successCalled = true })
        advanceUntilIdle()

        coVerify(exactly = 0) { testBackendConnectionUseCase(any(), any()) }
        coVerify(exactly = 0) { settingsRepository.setApiKey(any()) }
        assertEquals(false, successCalled)
    }

    @Test
    fun `saveSettings when connection test fails does not save and reports error`() = runTest {
        coEvery { testBackendConnectionUseCase("https://test.de", "wrong-key") } returns
            ConnectionTestResult.Error("API-Schlüssel ungültig (401)")

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        advanceUntilIdle()

        var successCalled = false
        viewModel.onServerUrlChanged("https://test.de")
        viewModel.onApiKeyChanged("wrong-key")
        viewModel.saveSettings(onSuccess = { successCalled = true })
        advanceUntilIdle()

        assertEquals(false, successCalled)
        assertEquals(false, viewModel.isSavedMessageVisible.value)
        val result = viewModel.testResult.value
        assertTrue(result is ConnectionTestResult.Error)
        assertEquals("API-Schlüssel ungültig (401)", (result as ConnectionTestResult.Error).message)
        coVerify(exactly = 0) { settingsRepository.setServerUrl(any()) }
        coVerify(exactly = 0) { settingsRepository.setApiKey(any()) }
    }

    @Test
    fun `saveSettings and testConnection gracefully catch sync exceptions`() = runTest {
        val faultySyncPlayers = mockk<de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase>()
        val faultySyncModes = mockk<SyncModesUseCase>()
        coEvery { faultySyncPlayers.invoke() } throws RuntimeException("Offline / DNS failure")
        coEvery { faultySyncModes.invoke() } throws RuntimeException("Offline / DNS failure")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            syncPlayersUseCase = faultySyncPlayers,
            syncModesUseCase = faultySyncModes
        )
        advanceUntilIdle()

        // 1. saveSettings despite sync error
        var successCalled = false
        viewModel.onServerUrlChanged("https://my-club.de")
        viewModel.onApiKeyChanged("secret")
        viewModel.saveSettings(onSuccess = { successCalled = true })
        advanceUntilIdle()

        assertTrue(successCalled)
        assertTrue(viewModel.isSavedMessageVisible.value)
        coVerify { settingsRepository.setServerUrl("https://my-club.de") }

        // 2. testConnection despite sync error
        viewModel.testConnection()
        advanceUntilIdle()

        assertTrue(viewModel.testResult.value is ConnectionTestResult.Success)
    }

    @Test
    fun `onQrScanError updates testResult with error message`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        advanceUntilIdle()

        viewModel.onQrScanError("QR-Code enthält keine gültigen Zugangsdaten")

        val result = viewModel.testResult.value
        assertTrue(result is ConnectionTestResult.Error)
        assertEquals("QR-Code enthält keine gültigen Zugangsdaten", (result as ConnectionTestResult.Error).message)
    }

    @Test
    fun `verifyAndActivateAdminKey activates key on success`() = runTest {
        coEvery { monatsblitzApi.verifyMaintenanceAuth("secret_key") } returns
            VerifyAuthResponseDto(valid = true, role = "admin")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.onMaintenanceKeyChanged("secret_key")
        viewModel.verifyAndActivateAdminKey()
        advanceUntilIdle()

        coVerify { settingsRepository.setMaintenanceApiKey("secret_key") }
        assertEquals("Admin-Modus erfolgreich freigeschaltet", viewModel.adminMessage.value)
        assertNull(viewModel.adminError.value)
    }

    @Test
    fun `verifyAndActivateAdminKey handles invalid key`() = runTest {
        coEvery { monatsblitzApi.verifyMaintenanceAuth("bad_key") } returns
            VerifyAuthResponseDto(valid = false, role = "none")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.onMaintenanceKeyChanged("bad_key")
        viewModel.verifyAndActivateAdminKey()
        advanceUntilIdle()

        coVerify(exactly = 0) { settingsRepository.setMaintenanceApiKey("bad_key") }
        assertEquals("Wartungs-Key wurde vom Server nicht akzeptiert", viewModel.adminError.value)
    }

    @Test
    fun `lockAdminMode resets key and state`() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase
        )
        advanceUntilIdle()

        viewModel.lockAdminMode()
        advanceUntilIdle()

        coVerify { settingsRepository.setMaintenanceApiKey("") }
        assertEquals("", viewModel.maintenanceKeyInput.value)
        assertEquals("Admin-Modus gesperrt", viewModel.adminMessage.value)
    }

    @Test
    fun `reopenTournament calls API and resets storage state`() = runTest {
        maintenanceKeyFlow.value = "admin_key"
        coEvery { monatsblitzApi.reopenTournament("admin_key", any()) } returns
            ReopenTournamentResponseDto(success = true, message = "Tournament reopened")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi,
            tournamentStorage = tournamentStorage
        )
        advanceUntilIdle()

        viewModel.reopenTournament(42L)
        advanceUntilIdle()

        coVerify { tournamentStorage.saveTournamentState(42L, false) }
        assertEquals("Tournament reopened", viewModel.adminMessage.value)
    }

    @Test
    fun `recreatePosts calls API and updates status message`() = runTest {
        maintenanceKeyFlow.value = "admin_key"
        coEvery { monatsblitzApi.recreatePostsWithKey("admin_key") } returns
            RecreatePostsDto(success = true, processed = 5, succeeded = 5, failed = 0)

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.recreatePosts()
        advanceUntilIdle()

        assertEquals("Aktualisiert: 5 von 5 Beiträgen (0 Fehler)", viewModel.adminMessage.value)
    }

    @Test
    fun `restoreDump calls repository and resets tournament storage`() = runTest {
        coEvery { tournamentDumpRepository.restoreDump(any()) } returns Result.success(42L)
        coEvery { tournamentDumpRepository.getDumpFiles() } returns listOf(
            TournamentDumpFileInfo(
                fileName = "tournament_42.json",
                filePath = "/path/to/tournament_42.json",
                tournamentId = 42L,
                timestamp = "20261108",
                sizeBytes = 1024L
            )
        )

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            tournamentDumpRepository = tournamentDumpRepository,
            tournamentStorage = tournamentStorage
        )
        advanceUntilIdle()

        viewModel.restoreDump("{\"tournamentId\": 42}")
        advanceUntilIdle()

        coVerify { tournamentStorage.saveTournamentState(42L, false) }
        assertTrue(viewModel.adminMessage.value?.contains("Turnier #42 erfolgreich aus Backup wiederhergestellt") == true)
        assertEquals(1, viewModel.dumps.value.size)
    }

    @Test
    fun `verifyAndActivateAdminKey with blank key sets error`() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase
        )
        advanceUntilIdle()

        viewModel.onMaintenanceKeyChanged("   ")
        viewModel.verifyAndActivateAdminKey()
        advanceUntilIdle()

        assertEquals("Bitte gib einen Wartungs-API-Key ein", viewModel.adminError.value)
    }

    @Test
    fun `verifyAndActivateAdminKey when api is null sets error`() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = null
        )
        advanceUntilIdle()

        viewModel.onMaintenanceKeyChanged("any_key")
        viewModel.verifyAndActivateAdminKey()
        advanceUntilIdle()

        assertEquals("API-Client nicht verfügbar", viewModel.adminError.value)
    }

    @Test
    fun `verifyAndActivateAdminKey when api throws sets error`() = runTest {
        coEvery { monatsblitzApi.verifyMaintenanceAuth("error_key") } throws RuntimeException("Timeout")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.onMaintenanceKeyChanged("error_key")
        viewModel.verifyAndActivateAdminKey()
        advanceUntilIdle()

        assertTrue(viewModel.adminError.value?.contains("Verbindung fehlgeschlagen") == true)
    }

    @Test
    fun `reopenTournament when key is blank sets unauthorized error`() = runTest {
        maintenanceKeyFlow.value = ""

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.reopenTournament(42L)
        advanceUntilIdle()

        assertEquals("Nicht autorisiert", viewModel.adminError.value)
    }

    @Test
    fun `reopenTournament when api returns failure sets error message`() = runTest {
        maintenanceKeyFlow.value = "admin_key"
        coEvery { monatsblitzApi.reopenTournament("admin_key", any()) } returns
            ReopenTournamentResponseDto(success = false, message = "Tournament already finalized")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.reopenTournament(42L)
        advanceUntilIdle()

        assertEquals("Fehler: Tournament already finalized", viewModel.adminError.value)
    }

    @Test
    fun `reopenTournament when api throws sets error message`() = runTest {
        maintenanceKeyFlow.value = "admin_key"
        coEvery { monatsblitzApi.reopenTournament("admin_key", any()) } throws RuntimeException("Server error 500")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.reopenTournament(42L)
        advanceUntilIdle()

        assertTrue(viewModel.adminError.value?.contains("Fehler beim Wiedereröffnen") == true)
    }

    @Test
    fun `recreatePosts when key is blank sets unauthorized error`() = runTest {
        maintenanceKeyFlow.value = ""

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.recreatePosts()
        advanceUntilIdle()

        assertEquals("Nicht autorisiert", viewModel.adminError.value)
    }

    @Test
    fun `recreatePosts when api throws sets error message`() = runTest {
        maintenanceKeyFlow.value = "admin_key"
        coEvery { monatsblitzApi.recreatePostsWithKey("admin_key") } throws RuntimeException("Fatal crash")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = monatsblitzApi
        )
        advanceUntilIdle()

        viewModel.recreatePosts()
        advanceUntilIdle()

        assertTrue(viewModel.adminError.value?.contains("Fehler beim Aktualisieren") == true)
    }

    @Test
    fun `restoreDump when repository is null sets error`() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            tournamentDumpRepository = null
        )
        advanceUntilIdle()

        viewModel.restoreDump("{}")
        advanceUntilIdle()

        assertEquals("Dump-Repository nicht verfügbar", viewModel.adminError.value)
    }

    @Test
    fun `restoreDump when repository returns failure sets error message`() = runTest {
        coEvery { tournamentDumpRepository.restoreDump(any()) } returns Result.failure(RuntimeException("Invalid JSON"))

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            tournamentDumpRepository = tournamentDumpRepository
        )
        advanceUntilIdle()

        viewModel.restoreDump("bad_json")
        advanceUntilIdle()

        assertTrue(viewModel.adminError.value?.contains("Import fehlgeschlagen: Invalid JSON") == true)
    }

    @Test
    fun `clearAdminFeedback clears both message and error`() = runTest {
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase
        )
        advanceUntilIdle()

        viewModel.onMaintenanceKeyChanged("   ")
        viewModel.verifyAndActivateAdminKey()
        advanceUntilIdle()
        assertEquals("Bitte gib einen Wartungs-API-Key ein", viewModel.adminError.value)

        viewModel.clearAdminFeedback()
        assertNull(viewModel.adminError.value)
        assertNull(viewModel.adminMessage.value)
    }

    @Test
    fun `loadDumps and getDumpFile call repository`() = runTest {
        val dummyFile = java.io.File.createTempFile("test_dump", ".json")
        dummyFile.deleteOnExit()
        coEvery { tournamentDumpRepository.getDumpFiles() } returns listOf(
            TournamentDumpFileInfo("dump.json", dummyFile.absolutePath, 1L, "20261108", 100L)
        )
        coEvery { tournamentDumpRepository.getDumpFile("dump.json") } returns dummyFile

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            tournamentDumpRepository = tournamentDumpRepository
        )
        advanceUntilIdle()

        viewModel.loadDumps()
        advanceUntilIdle()

        assertEquals(1, viewModel.dumps.value.size)
        val file = viewModel.getDumpFile("dump.json")
        assertEquals(dummyFile.absolutePath, file?.absolutePath)
    }

    @Test
    fun `settings setters update repository correctly`() = runTest {
        val rotationFlow = MutableStateFlow(listOf("3+2"))
        every { settingsRepository.rotationModes } returns rotationFlow

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase
        )
        advanceUntilIdle()

        viewModel.setPlayerSortField(PlayerSortField.FIRST_NAME)
        viewModel.setPlayerSortOrder(PlayerSortOrder.DESCENDING)
        viewModel.setUseShortPlayerNames(true)
        viewModel.setPreferRufnameAsShortName(true)
        viewModel.setAutoAdoptOpenTournament(true)
        viewModel.setAutoCleanupDumps(false)
        viewModel.setDumpRetentionMonths(6)
        viewModel.setActivePlayersCutoffMonths(24)
        viewModel.setModePreselectionType(ModePreselectionType.ROTATION)
        viewModel.setFixedMode("5+0")
        viewModel.setRotationModes(listOf("3+2", "5+0"))
        advanceUntilIdle()

        coVerify { settingsRepository.setPlayerSortField(PlayerSortField.FIRST_NAME) }
        coVerify { settingsRepository.setPlayerSortOrder(PlayerSortOrder.DESCENDING) }
        coVerify { settingsRepository.setUseShortPlayerNames(true) }
        coVerify { settingsRepository.setPreferRufnameAsShortName(true) }
        coVerify { settingsRepository.setAutoAdoptOpenTournament(true) }
        coVerify { settingsRepository.setAutoCleanupDumps(false) }
        coVerify { settingsRepository.setDumpRetentionMonths(6) }
        coVerify { settingsRepository.setActivePlayersCutoffMonths(24) }
        coVerify { settingsRepository.setModePreselectionType(ModePreselectionType.ROTATION) }
        coVerify { settingsRepository.setFixedMode("5+0") }
        coVerify { settingsRepository.setRotationModes(listOf("3+2", "5+0")) }

        // Test toggleRotationMode: adding a mode
        viewModel.toggleRotationMode("5+0")
        advanceUntilIdle()
        coVerify { settingsRepository.setRotationModes(listOf("3+2", "5+0")) }

        // Test toggleRotationMode: removing existing mode
        viewModel.toggleRotationMode("3+2")
        advanceUntilIdle()
        coVerify { settingsRepository.setRotationModes(emptyList()) }
    }
}
