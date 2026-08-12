package de.kindermaenner.monatsblitz.ui.tournament

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase

class TournamentViewModelFactory(
    private val tournamentRepository: TournamentRepository,
    private val setGameResultUseCase: SetGameResultUseCase,
    private val updateRankingUseCase: CreateTournamentRankingsUseCase,
    private val tournamentId: Long

) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(
                TournamentViewModel::class.java
            )
        ) {

            @Suppress("UNCHECKED_CAST")
            return TournamentViewModel(
                tournamentRepository,
                setGameResultUseCase,
                updateRankingUseCase,
                tournamentId
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}