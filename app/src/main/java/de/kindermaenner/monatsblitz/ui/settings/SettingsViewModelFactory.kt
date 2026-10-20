package de.kindermaenner.monatsblitz.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.usecase.TestBackendConnectionUseCase

class SettingsViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val testBackendConnectionUseCase: TestBackendConnectionUseCase = TestBackendConnectionUseCase(),
    private val syncPlayersUseCase: SyncPlayersUseCase? = null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                settingsRepository = settingsRepository,
                testBackendConnectionUseCase = testBackendConnectionUseCase,
                syncPlayersUseCase = syncPlayersUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
