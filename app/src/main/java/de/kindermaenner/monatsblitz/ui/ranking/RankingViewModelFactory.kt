package de.kindermaenner.monatsblitz.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao

class RankingViewModelFactory(
    private val tournamentId: Long,
    private val tournamentPlayerDao: TournamentPlayerDao,
    private val playerDao: PlayerDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RankingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RankingViewModel(
                tournamentId = tournamentId,
                tournamentPlayerDao = tournamentPlayerDao,
                playerDao = playerDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
