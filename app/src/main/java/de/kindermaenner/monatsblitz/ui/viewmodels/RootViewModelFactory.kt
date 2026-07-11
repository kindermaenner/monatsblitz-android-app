package de.kindermaenner.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage

class RootViewModelFactory(
    private val tournamentPreferences: TournamentStorage,
    private val playerRepository: PlayerRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(RootViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RootViewModel(
                tournamentPreferences,
                playerRepository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}