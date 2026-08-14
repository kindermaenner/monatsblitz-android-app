package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase

class CrosstableViewModelFactory(
    private val tournamentRepository: TournamentRepository,
    private val setGameResultUseCase: SetGameResultUseCase,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase,
    private val tournamentId: Long

) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(
                CrosstableViewModel::class.java
            )
        ) {

            @Suppress("UNCHECKED_CAST")
            return CrosstableViewModel(
                tournamentRepository,
                setGameResultUseCase,
                createTournamentRankingsUseCase,
                tournamentId
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
