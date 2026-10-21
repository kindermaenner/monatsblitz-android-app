package de.kindermaenner.monatsblitz.ui.ranking

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase
import de.kindermaenner.monatsblitz.domain.util.PlayerNameFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class RankingViewModel(
    private val tournamentId: Long,
    private val tournamentRepository: TournamentRepository,
    private val finalizeTournamentUseCase: FinalizeTournamentUseCase? = null,
    private val tournamentSyncService: TournamentSyncService? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<RankingUiState>(RankingUiState.Loading)
    val uiState: StateFlow<RankingUiState> = _uiState

    init {
        observeRanking()
    }

    private var openGamesCount = 0
    private var isReadOnly = false

    private fun observeRanking() {
        val tournamentFlow = runCatching { tournamentRepository.observeTournament(tournamentId) }
            .getOrNull() ?: flowOf(null)

        viewModelScope.launch {
            combine(
                tournamentRepository.observeTournamentRanking(tournamentId),
                tournamentFlow,
                settingsRepository?.useShortPlayerNames ?: flowOf(true)
            ) { items, tournament, useShortNames ->
                val shortNameMap = if (useShortNames && tournament != null) {
                    tournament.players.associate { player ->
                        player.id to PlayerNameFormatter.formatDisplayName(
                            player = player,
                            allPlayers = tournament.players,
                            useShortName = true
                        )
                    }
                } else emptyMap()

                val rows = items.map { item ->
                    val baseName = shortNameMap[item.playerId] ?: item.name
                    val displayName = if (tournament?.withdrawnPlayerIds?.contains(item.playerId) == true && !baseName.endsWith("(z)")) {
                        "$baseName (z)"
                    } else {
                        baseName
                    }
                    RankingRowData(
                        name = displayName,
                        points = item.points,
                        rank = item.rank
                    )
                }
                val currentReady = _uiState.value as? RankingUiState.Ready
                _uiState.value = RankingUiState.Ready(
                    rows = rows,
                    openGamesCount = currentReady?.openGamesCount ?: openGamesCount,
                    isReadOnly = currentReady?.isReadOnly ?: isReadOnly,
                    isFinalizing = currentReady?.isFinalizing ?: false,
                    isFinalized = currentReady?.isFinalized ?: false,
                    finalizeError = currentReady?.finalizeError
                )
            }.collect {}
        }

        viewModelScope.launch {
            tournamentFlow.collect { tournament ->
                val count = tournament?.games?.values?.count {
                    it.result == GameResult.Open
                } ?: 0
                openGamesCount = count
                val currentReady = _uiState.value as? RankingUiState.Ready
                if (currentReady != null) {
                    _uiState.value = currentReady.copy(openGamesCount = count)
                }
            }
        }

        if (tournamentSyncService != null) {
            viewModelScope.launch {
                tournamentSyncService.syncStatus.collect { syncStatus ->
                    val readOnly = syncStatus == SyncStatus.FINALIZED
                    isReadOnly = readOnly
                    val currentReady = _uiState.value as? RankingUiState.Ready
                    if (currentReady != null) {
                        _uiState.value = currentReady.copy(isReadOnly = readOnly)
                    }
                }
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
