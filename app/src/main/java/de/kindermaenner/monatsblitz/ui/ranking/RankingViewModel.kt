package de.kindermaenner.monatsblitz.ui.ranking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RankingViewModel(
    private val tournamentId: Long,
    private val tournamentRepository: TournamentRepository,
    private val finalizeTournamentUseCase: FinalizeTournamentUseCase? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Loading)
    val uiState: StateFlow<RankingUiState> = _uiState

    init {
        observeRanking()
    }

    private fun observeRanking() {
        viewModelScope.launch {
            tournamentRepository.observeTournamentRanking(tournamentId)
                .collect { items ->
                    val rows = items.map { item ->
                        RankingRowData(
                            name = item.name,
                            points = item.points,
                            rank = item.rank
                        )
                    }

                    val currentReady = _uiState.value as? RankingUiState.Ready
                    _uiState.value = RankingUiState.Ready(
                        rows = rows,
                        isFinalizing = currentReady?.isFinalizing ?: false,
                        isFinalized = currentReady?.isFinalized ?: false,
                        finalizeError = currentReady?.finalizeError
                    )
                }
        }
    }


    fun finalizeTournament() {
        val currentState = _uiState.value
        if (currentState !is RankingUiState.Ready || currentState.isFinalizing) return

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