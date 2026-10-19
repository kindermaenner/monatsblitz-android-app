package de.kindermaenner.monatsblitz.ui.settings

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
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
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    @Test
    fun `showDetailedSyncIndicators reflects repository state`() = runTest {
        val stateFlow = MutableStateFlow(true)
        every { settingsRepository.showDetailedSyncIndicators } returns stateFlow

        val viewModel = SettingsViewModel(settingsRepository)
        backgroundScope.launch { viewModel.showDetailedSyncIndicators.collect() }
        advanceUntilIdle()

        assertEquals(true, viewModel.showDetailedSyncIndicators.value)

        stateFlow.value = false
        advanceUntilIdle()

        assertEquals(false, viewModel.showDetailedSyncIndicators.value)
    }

    @Test
    fun `setShowDetailedSyncIndicators delegates to repository`() = runTest {
        every { settingsRepository.showDetailedSyncIndicators } returns MutableStateFlow(true)

        val viewModel = SettingsViewModel(settingsRepository)
        viewModel.setShowDetailedSyncIndicators(false)
        advanceUntilIdle()

        coVerify(exactly = 1) { settingsRepository.setShowDetailedSyncIndicators(false) }
    }

    @Test
    fun `SettingsViewModelFactory creates SettingsViewModel and throws for unknown class`() {
        every { settingsRepository.showDetailedSyncIndicators } returns MutableStateFlow(true)
        val factory = SettingsViewModelFactory(settingsRepository)

        val vm = factory.create(SettingsViewModel::class.java)
        assertNotNull(vm)

        class UnknownViewModel : ViewModel()
        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }
}
