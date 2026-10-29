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
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDetailedErrorMessage
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

        observeRankingItems(tournamentFlow)
        observeOpenGames(tournamentFlow)
        observeSyncStatus()
    }

    private fun observeRankingItems(tournamentFlow: kotlinx.coroutines.flow.Flow<de.kindermaenner.monatsblitz.domain.model.Tournament?>) {
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
                    mapToRankingRow(item, tournament, shortNameMap)
                }
                val count = tournament?.games?.values?.count {
                    it.result == GameResult.Open
                }
                if (count != null) {
                    openGamesCount = count
                }
                val currentReady = _uiState.value as? RankingUiState.Ready
                _uiState.value = RankingUiState.Ready(
                    rows = rows,
                    openGamesCount = count ?: currentReady?.openGamesCount ?: openGamesCount,
                    isReadOnly = currentReady?.isReadOnly ?: isReadOnly,
                    isFinalizing = currentReady?.isFinalizing ?: false,
                    isFinalized = currentReady?.isFinalized ?: false,
                    finalizeError = currentReady?.finalizeError,
                    tournament = tournament ?: currentReady?.tournament
                )
            }.collect {}
        }
    }

    private fun mapToRankingRow(
        item: de.kindermaenner.monatsblitz.domain.model.TournamentRankingItem,
        tournament: de.kindermaenner.monatsblitz.domain.model.Tournament?,
        shortNameMap: Map<Long, String>
    ): RankingRowData {
        val baseName = shortNameMap[item.playerId] ?: item.name
        val displayName = if (tournament?.withdrawnPlayerIds?.contains(item.playerId) == true && !baseName.endsWith("(z)")) {
            "$baseName (z)"
        } else {
            baseName
        }
        return RankingRowData(
            name = displayName,
            points = item.points,
            rank = item.rank
        )
    }

    private fun observeOpenGames(tournamentFlow: kotlinx.coroutines.flow.Flow<de.kindermaenner.monatsblitz.domain.model.Tournament?>) {
        viewModelScope.launch {
            tournamentFlow.collect { tournament ->
                val count = tournament?.games?.values?.count {
                    it.result == GameResult.Open
                } ?: 0
                openGamesCount = count
                val currentReady = _uiState.value as? RankingUiState.Ready
                if (currentReady != null) {
                    _uiState.value = currentReady.copy(openGamesCount = count, tournament = tournament)
                }
            }
        }
    }

    private fun observeSyncStatus() {
        val syncService = tournamentSyncService ?: return
        viewModelScope.launch {
            syncService.syncStatus.collect { syncStatus ->
                val readOnly = syncStatus == SyncStatus.FINALIZED
                isReadOnly = readOnly
                val currentReady = _uiState.value as? RankingUiState.Ready
                if (currentReady != null) {
                    _uiState.value = currentReady.copy(isReadOnly = readOnly)
                }
            }
        }
    }

    fun finalizeTournament() {
        val currentState = _uiState.value
        if (currentState !is RankingUiState.Ready || currentState.isFinalizing || currentState.isReadOnly || currentState.openGamesCount > 0) return

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
                    finalizeError = e.toDetailedErrorMessage()
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
