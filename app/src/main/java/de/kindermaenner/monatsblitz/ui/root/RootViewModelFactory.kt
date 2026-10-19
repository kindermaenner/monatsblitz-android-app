package de.kindermaenner.monatsblitz.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase

class RootViewModelFactory(
    private val tournamentRepository: TournamentRepository,
    private val syncPlayersUseCase: SyncPlayersUseCase
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(RootViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RootViewModel(
                tournamentRepository,
                syncPlayersUseCase
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}