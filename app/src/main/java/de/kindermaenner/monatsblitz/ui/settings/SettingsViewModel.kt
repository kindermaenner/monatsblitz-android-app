package de.kindermaenner.monatsblitz.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.model.QrSetupConfig
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.usecase.ConnectionTestResult
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.usecase.TestBackendConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val testBackendConnectionUseCase: TestBackendConnectionUseCase = TestBackendConnectionUseCase(),
    private val syncPlayersUseCase: SyncPlayersUseCase? = null
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
        settingsRepository.apiKey
    ) { url, key ->
        url.isNotBlank() && key.isNotBlank()
    }.stateIn(
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

    private var hasLoadedInitialSettings = false

    init {
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

    val playerSortField: StateFlow<PlayerSortField> =
        settingsRepository.playerSortField
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = PlayerSortField.LAST_NAME
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
}
