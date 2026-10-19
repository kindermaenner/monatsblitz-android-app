package de.kindermaenner.monatsblitz.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val showDetailedSyncIndicators: StateFlow<Boolean> =
        settingsRepository.showDetailedSyncIndicators
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    fun setShowDetailedSyncIndicators(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowDetailedSyncIndicators(enabled)
        }
    }
}
