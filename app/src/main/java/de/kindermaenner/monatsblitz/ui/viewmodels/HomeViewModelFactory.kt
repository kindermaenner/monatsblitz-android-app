package de.kindermaenner.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase

class HomeViewModelFactory(
    private val playerRepository: PlayerRepository,
    private val createTournamentUseCase: CreateTournamentUseCase
) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                playerRepository,
                createTournamentUseCase) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}