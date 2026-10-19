package de.kindermaenner.monatsblitz.ui.ranking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class RankingViewModel(
    private val tournamentId: Long,
    private val tournamentRepository: TournamentRepository,
    private val finalizeTournamentUseCase: FinalizeTournamentUseCase? = null,
    private val tournamentSyncService: TournamentSyncService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Loading)
    val uiState: StateFlow<RankingUiState> = _uiState

    init {
        observeRanking()
    }

    private fun observeRanking() {
        val tournamentFlow = runCatching {
            tournamentRepository.observeTournament(tournamentId)
        }.getOrNull() ?: flowOf(null)
        val syncStatusFlow = tournamentSyncService?.syncStatus ?: flowOf(SyncStatus.SYNCED)

        viewModelScope.launch {
            combine(
                tournamentRepository.observeTournamentRanking(tournamentId),
                tournamentFlow,
                syncStatusFlow
            ) { items, tournament, syncStatus ->
                val rows = items.map { item ->
                    RankingRowData(
                        name = item.name,
                        points = item.points,
                        rank = item.rank
                    )
                }
                val openGamesCount = tournament?.games?.values?.count {
                    it.result == GameResult.Open
                } ?: 0
                val isReadOnly = syncStatus == SyncStatus.FINALIZED

                Triple(rows, openGamesCount, isReadOnly)
            }.collect { (rows, openGamesCount, isReadOnly) ->
                val currentReady = _uiState.value as? RankingUiState.Ready
                _uiState.value = RankingUiState.Ready(
                    rows = rows,
                    openGamesCount = openGamesCount,
                    isReadOnly = isReadOnly,
                    isFinalizing = currentReady?.isFinalizing ?: false,
                    isFinalized = currentReady?.isFinalized ?: false,
                    finalizeError = currentReady?.finalizeError
                )
            }
        }
    }

    fun finalizeTournament() {
        val currentState = _uiState.value
        if (currentState !is RankingUiState.Ready || currentState.isFinalizing || currentState.isReadOnly) return

        if (finalizeTournamentUseCase == null) {
            _uiState.value = currentState.copy(finalizeError = "Finalisierung nicht konfiguriert")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isFinalizing = true, finalizeError = null)
            try {
                finalizeTournamentUseCase(tournamentId)
                _uiState.value = (_uiState.value as? RankingUiState.Ready)?.copy(
                    isFinalizing = false,
                    isFinalized = true,
                    finalizeError = null
                ) ?: currentState
            } catch (e: Exception) {
                Log.e("RankingViewModel", "Error finalizing tournament", e)
                _uiState.value = (_uiState.value as? RankingUiState.Ready)?.copy(
                    isFinalizing = false,
                    finalizeError = e.message ?: "Unbekannter Fehler bei der Finalisierung"
                ) ?: currentState
            }
        }
    }

    fun clearFinalizeError() {
        val currentState = _uiState.value
        if (currentState is RankingUiState.Ready) {
            _uiState.value = currentState.copy(finalizeError = null)
        }
    }
}