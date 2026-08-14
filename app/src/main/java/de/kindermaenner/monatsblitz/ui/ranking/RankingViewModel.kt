package de.kindermaenner.monatsblitz.ui.ranking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RankingViewModel(
    private val tournamentId: Long,
    private val tournamentPlayerDao: TournamentPlayerDao,
    private val playerDao: PlayerDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Loading)
    val uiState: StateFlow<RankingUiState> = _uiState

    init {
        observeRanking()
    }

    private fun observeRanking() {
        viewModelScope.launch {
            tournamentPlayerDao.observeRankingForTournament(tournamentId)
                .collect { entries ->

                    entries.forEach { x -> Log.i("RakingViewModel", "$x") }
                    val players = playerDao.getAllPlayers().associateBy { it.id }
                    val rows = entries.filter {e -> e.rank != null}.map { entry ->
                        val playerRankingEntry = entry.toDomain()
                        val player = players[entry.playerId]
                        RankingRowData(
                            name = if (player != null) "${player.vorname} ${player.name}" else "Unbekannt",
                            points = playerRankingEntry.points,
                            rank = playerRankingEntry.rank
                        )
                    }

                    _uiState.value = RankingUiState.Ready(rows)
                }
        }
    }
}