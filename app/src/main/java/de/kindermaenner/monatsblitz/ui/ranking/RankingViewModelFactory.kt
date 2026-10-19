package de.kindermaenner.monatsblitz.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase

class RankingViewModelFactory(
    private val tournamentId: Long,
    private val tournamentRepository: TournamentRepository,
    private val finalizeTournamentUseCase: FinalizeTournamentUseCase? = null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RankingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RankingViewModel(
                tournamentId = tournamentId,
                tournamentRepository = tournamentRepository,
                finalizeTournamentUseCase = finalizeTournamentUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

