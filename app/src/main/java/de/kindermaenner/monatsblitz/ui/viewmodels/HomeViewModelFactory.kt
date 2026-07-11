package de.kindermaenner.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage

class HomeViewModelFactory(
    private val playerRepository: PlayerRepository,
    private val tournamentRepository: TournamentRepository,
    private val tournamentPreferences: TournamentStorage

) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                playerRepository,
                tournamentRepository,
                tournamentPreferences
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}