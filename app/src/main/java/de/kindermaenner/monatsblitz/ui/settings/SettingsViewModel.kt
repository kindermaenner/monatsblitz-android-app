package de.kindermaenner.monatsblitz.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.model.QrSetupConfig
import de.kindermaenner.monatsblitz.domain.repository.ModeRepository
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.usecase.ConnectionTestResult
import de.kindermaenner.monatsblitz.domain.usecase.SyncModesUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.model.TournamentDumpFileInfo
import de.kindermaenner.monatsblitz.domain.repository.TournamentDumpRepository
import de.kindermaenner.monatsblitz.domain.usecase.TestBackendConnectionUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ReopenTournamentRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val testBackendConnectionUseCase: TestBackendConnectionUseCase = TestBackendConnectionUseCase(),
    private val syncPlayersUseCase: SyncPlayersUseCase? = null,
    private val syncModesUseCase: SyncModesUseCase? = null,
    private val modeRepository: ModeRepository? = null,
    private val tournamentDumpRepository: TournamentDumpRepository? = null,
    private val monatsblitzApi: MonatsblitzApi? = null,
    private val tournamentStorage: TournamentStorage? = null
) : ViewModel() {

    private val _serverUrlInput = MutableStateFlow("")
    val serverUrlInput: StateFlow<String> = _serverUrlInput.asStateFlow()

    private val _apiKeyInput = MutableStateFlow("")
    val apiKeyInput: StateFlow<String> = _apiKeyInput.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

    private val _isSavedMessageVisible = MutableStateFlow(false)
    val isSavedMessageVisible: StateFlow<Boolean> = _isSavedMessageVisible.asStateFlow()

    val isConfigured: StateFlow<Boolean> = combine(
        settingsRepository.serverUrl,
        settingsRepository.apiKey,
        settingsRepository.standaloneOfflineMode
    ) { url, key, offline ->
        (url.isNotBlank() && key.isNotBlank()) || offline
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val isServerConfigured: StateFlow<Boolean> = combine(
        settingsRepository.serverUrl,
        settingsRepository.apiKey
    ) { url, key ->
        url.isNotBlank() && key.isNotBlank()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val standaloneOfflineMode: StateFlow<Boolean> =
        settingsRepository.standaloneOfflineMode
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    val showDetailedSyncIndicators: StateFlow<Boolean> =
        settingsRepository.showDetailedSyncIndicators
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    val availableModes: StateFlow<List<GameMode>> =
        (modeRepository?.observeModes() ?: flowOf(emptyList()))
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val modePreselectionType: StateFlow<ModePreselectionType> =
        settingsRepository.modePreselectionType
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ModePreselectionType.NONE
            )

    val fixedMode: StateFlow<String> =
        settingsRepository.fixedMode
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ""
            )

    val rotationModes: StateFlow<List<String>> =
        settingsRepository.rotationModes
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _dumps = MutableStateFlow<List<TournamentDumpFileInfo>>(emptyList())
    val dumps: StateFlow<List<TournamentDumpFileInfo>> = _dumps.asStateFlow()

    private val _maintenanceKeyInput = MutableStateFlow("")
    val maintenanceKeyInput: StateFlow<String> = _maintenanceKeyInput.asStateFlow()

    val isAdminMode: StateFlow<Boolean> = settingsRepository.maintenanceApiKey
        .map { it.isNotBlank() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _isAdminVerifying = MutableStateFlow(false)
    val isAdminVerifying: StateFlow<Boolean> = _isAdminVerifying.asStateFlow()

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    private val _adminError = MutableStateFlow<String?>(null)
    val adminError: StateFlow<String?> = _adminError.asStateFlow()

    private var hasLoadedInitialSettings = false

    init {
        loadDumps()
        viewModelScope.launch {
            combine(settingsRepository.serverUrl, settingsRepository.apiKey) { url, key ->
                Pair(url, key)
            }.collect { (url, key) ->
                if (!hasLoadedInitialSettings) {
                    _serverUrlInput.value = url
                    _apiKeyInput.value = key
                    hasLoadedInitialSettings = true
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.maintenanceApiKey.collect { key ->
                _maintenanceKeyInput.value = key
            }
        }
    }

    fun onServerUrlChanged(newUrl: String) {
        _serverUrlInput.value = newUrl
        _testResult.value = null
        _isSavedMessageVisible.value = false
    }

    fun onApiKeyChanged(newKey: String) {
        _apiKeyInput.value = newKey
        _testResult.value = null
        _isSavedMessageVisible.value = false
    }

    fun applyQrConfig(config: QrSetupConfig) {
        _serverUrlInput.value = config.serverUrl
        _apiKeyInput.value = config.apiKey
        _testResult.value = null
        _isSavedMessageVisible.value = false
        setStandaloneOfflineMode(false)
        if (config.modePreselectionType != null) {
            setModePreselectionType(config.modePreselectionType)
        }
        if (config.fixedMode != null) {
            setFixedMode(config.fixedMode)
        }
        if (config.rotationModes != null) {
            setRotationModes(config.rotationModes)
        }
        saveSettings()
    }

    fun onQrScanError(message: String) {
        _testResult.value = ConnectionTestResult.Error(message)
    }

    fun saveSettings(onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val trimmedUrl = _serverUrlInput.value.trim()
            val trimmedKey = _apiKeyInput.value.trim()
            if (trimmedUrl.isEmpty() || trimmedKey.isEmpty()) {
                return@launch
            }

            _isTestingConnection.value = true
            _testResult.value = null

            val result = testBackendConnectionUseCase(
                rawUrl = trimmedUrl,
                rawApiKey = trimmedKey
            )
            _testResult.value = result
            _isTestingConnection.value = false

            if (result is ConnectionTestResult.Success) {
                settingsRepository.setServerUrl(trimmedUrl)
                settingsRepository.setApiKey(trimmedKey)
                _isSavedMessageVisible.value = true

                try {
                    syncPlayersUseCase?.invoke()
                    syncModesUseCase?.invoke()
                } catch (_: Exception) {
                    // Offline-first
                }

                onSuccess?.invoke()
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _testResult.value = null
            val result = testBackendConnectionUseCase(
                rawUrl = _serverUrlInput.value,
                rawApiKey = _apiKeyInput.value
            )
            _testResult.value = result
            _isTestingConnection.value = false

            if (result is ConnectionTestResult.Success) {
                try {
                    syncPlayersUseCase?.invoke()
                    syncModesUseCase?.invoke()
                } catch (_: Exception) {
                    // Offline-first
                }
            }
        }
    }

    fun dismissSavedMessage() {
        _isSavedMessageVisible.value = false
    }

    fun setShowDetailedSyncIndicators(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowDetailedSyncIndicators(enabled)
        }
    }

    fun setStandaloneOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStandaloneOfflineMode(enabled)
        }
    }

    val playerSortField: StateFlow<PlayerSortField> =
        settingsRepository.playerSortField
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = PlayerSortField.FIRST_NAME
            )

    val playerSortOrder: StateFlow<PlayerSortOrder> =
        settingsRepository.playerSortOrder
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = PlayerSortOrder.ASCENDING
            )

    val useShortPlayerNames: StateFlow<Boolean> =
        settingsRepository.useShortPlayerNames
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    val preferRufnameAsShortName: StateFlow<Boolean> =
        settingsRepository.preferRufnameAsShortName
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    fun setPlayerSortField(field: PlayerSortField) {
        viewModelScope.launch {
            settingsRepository.setPlayerSortField(field)
        }
    }

    fun setPlayerSortOrder(order: PlayerSortOrder) {
        viewModelScope.launch {
            settingsRepository.setPlayerSortOrder(order)
        }
    }

    fun setUseShortPlayerNames(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseShortPlayerNames(enabled)
        }
    }

    fun setPreferRufnameAsShortName(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPreferRufnameAsShortName(enabled)
        }
    }

    val autoAdoptOpenTournament: StateFlow<Boolean> =
        settingsRepository.autoAdoptOpenTournament
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )

    fun setAutoAdoptOpenTournament(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoAdoptOpenTournament(enabled)
        }
    }

    val activePlayersCutoffMonths: StateFlow<Int> =
        settingsRepository.activePlayersCutoffMonths
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 12
            )

    val autoCleanupDumps: StateFlow<Boolean> =
        settingsRepository.autoCleanupDumps
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    val dumpRetentionMonths: StateFlow<Int> =
        settingsRepository.dumpRetentionMonths
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 3
            )

    fun setAutoCleanupDumps(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoCleanupDumps(enabled)
        }
    }

    fun setDumpRetentionMonths(months: Int) {
        viewModelScope.launch {
            settingsRepository.setDumpRetentionMonths(months)
        }
    }

    fun setActivePlayersCutoffMonths(months: Int) {
        viewModelScope.launch {
            settingsRepository.setActivePlayersCutoffMonths(months)
        }
    }

    fun setModePreselectionType(type: ModePreselectionType) {
        viewModelScope.launch {
            settingsRepository.setModePreselectionType(type)
        }
    }

    fun setFixedMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setFixedMode(mode)
        }
    }

    fun setRotationModes(modes: List<String>) {
        viewModelScope.launch {
            settingsRepository.setRotationModes(modes)
        }
    }

    fun toggleRotationMode(modeName: String) {
        viewModelScope.launch {
            val current = settingsRepository.rotationModes.first().toMutableList()
            if (current.contains(modeName)) {
                current.remove(modeName)
                settingsRepository.setRotationModes(current)
            } else {
                current.add(modeName)
                settingsRepository.setRotationModes(current)
            }
        }
    }

    fun loadDumps() {
        viewModelScope.launch {
            _dumps.value = tournamentDumpRepository?.getDumpFiles() ?: emptyList()
        }
    }

    fun getDumpFile(fileName: String): File? {
        return runBlocking { tournamentDumpRepository?.getDumpFile(fileName) }
    }

    fun onMaintenanceKeyChanged(key: String) {
        _maintenanceKeyInput.value = key
        _adminError.value = null
        _adminMessage.value = null
    }

    fun clearAdminFeedback() {
        _adminMessage.value = null
        _adminError.value = null
    }

    fun verifyAndActivateAdminKey() {
        val key = _maintenanceKeyInput.value.trim()
        if (key.isBlank()) {
            _adminError.value = "Bitte gib einen Wartungs-API-Key ein"
            return
        }

        viewModelScope.launch {
            _isAdminVerifying.value = true
            _adminError.value = null
            _adminMessage.value = null
            try {
                val api = monatsblitzApi
                if (api == null) {
                    _adminError.value = "API-Client nicht verfügbar"
                    return@launch
                }
                val response = api.verifyMaintenanceAuth(key)
                if (response.valid) {
                    settingsRepository.setMaintenanceApiKey(key)
                    _adminMessage.value = "Admin-Modus erfolgreich freigeschaltet"
                } else {
                    _adminError.value = "Wartungs-Key wurde vom Server nicht akzeptiert"
                }
            } catch (e: Exception) {
                _adminError.value = "Verbindung fehlgeschlagen: ${e.message ?: UNKNOWN_ERROR}"
            } finally {
                _isAdminVerifying.value = false
            }
        }
    }

    fun lockAdminMode() {
        viewModelScope.launch {
            settingsRepository.setMaintenanceApiKey("")
            _maintenanceKeyInput.value = ""
            _adminMessage.value = "Admin-Modus gesperrt"
            _adminError.value = null
        }
    }

    fun reopenTournament(tournamentId: Long) {
        viewModelScope.launch {
            _isAdminVerifying.value = true
            _adminError.value = null
            _adminMessage.value = null
            try {
                val key = settingsRepository.maintenanceApiKey.first().trim()
                val api = monatsblitzApi
                if (api == null || key.isBlank()) {
                    _adminError.value = "Nicht autorisiert"
                    return@launch
                }
                val response = api.reopenTournament(key, ReopenTournamentRequestDto(tournamentId))
                if (response.success) {
                    tournamentStorage?.saveTournamentState(tournamentId, false)
                    _adminMessage.value = response.message.ifBlank { "Turnier #$tournamentId wurde wieder geöffnet" }
                } else {
                    _adminError.value = "Fehler: ${response.message}"
                }
            } catch (e: Exception) {
                _adminError.value = "Fehler beim Wiedereröffnen: ${e.message ?: UNKNOWN_ERROR}"
            } finally {
                _isAdminVerifying.value = false
            }
        }
    }

    fun recreatePosts() {
        viewModelScope.launch {
            _isAdminVerifying.value = true
            _adminError.value = null
            _adminMessage.value = null
            try {
                val key = settingsRepository.maintenanceApiKey.first().trim()
                val api = monatsblitzApi
                if (api == null || key.isBlank()) {
                    _adminError.value = "Nicht autorisiert"
                    return@launch
                }
                val response = api.recreatePostsWithKey(key)
                _adminMessage.value = "Aktualisiert: ${response.succeeded} von ${response.processed} Beiträgen (${response.failed} Fehler)"
            } catch (e: Exception) {
                _adminError.value = "Fehler beim Aktualisieren: ${e.message ?: UNKNOWN_ERROR}"
            } finally {
                _isAdminVerifying.value = false
            }
        }
    }

    fun restoreDump(jsonContent: String) {
        viewModelScope.launch {
            _isAdminVerifying.value = true
            _adminError.value = null
            _adminMessage.value = null
            try {
                val repo = tournamentDumpRepository
                if (repo == null) {
                    _adminError.value = "Dump-Repository nicht verfügbar"
                    return@launch
                }
                val result = repo.restoreDump(jsonContent)
                result.fold(
                    onSuccess = { tId ->
                        tournamentStorage?.saveTournamentState(tId, false)
                        loadDumps()
                        _adminMessage.value = "Turnier #$tId erfolgreich aus Backup wiederhergestellt! Es kann nun synchronisiert werden."
                    },
                    onFailure = { e ->
                        _adminError.value = "Import fehlgeschlagen: ${e.message ?: "Ungültiges Backup-Format"}"
                    }
                )
            } finally {
                _isAdminVerifying.value = false
            }
        }
    }

    companion object {
        private const val UNKNOWN_ERROR = "Unbekannter Fehler"
    }
}
