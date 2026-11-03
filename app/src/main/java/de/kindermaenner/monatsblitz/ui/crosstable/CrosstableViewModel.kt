package de.kindermaenner.monatsblitz.ui.crosstable

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.PlayerWithdrawRule
import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.util.PlayerNameFormatter
import de.kindermaenner.monatsblitz.ui.crosstable.components.CrosstableRowData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

class CrosstableViewModel(
    private val repository: TournamentRepository,
    private val setGameResultUseCase: SetGameResultUseCase,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase,
    private val calculatePlayerPointsUseCase: CalculatePlayerPointsUseCase,
    tournamentId: Long,
    private val tournamentSyncService: TournamentSyncService? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val playerRepository: PlayerRepository? = null,
    private val addPlayerUseCase: AddPlayerUseCase? = null,
    private val syncPlayersUseCase: SyncPlayersUseCase? = null
) : ViewModel() {

    constructor(
        repository: TournamentRepository,
        setGameResultUseCase: SetGameResultUseCase,
        createTournamentRankingsUseCase: CreateTournamentRankingsUseCase,
        tournamentId: Long
    ) : this(
        repository,
        setGameResultUseCase,
        createTournamentRankingsUseCase,
        CalculatePlayerPointsUseCase(),
        tournamentId,
        null,
        null,
        null,
        null,
        null
    )

    init {
        viewModelScope.launch {
            tournamentSyncService?.checkInitialStatus(tournamentId)
        }
    }
    
    private val _selectedLeg = MutableStateFlow(1)
    val selectedLeg = _selectedLeg.asStateFlow()

    private val settingsFlow = combine(
        settingsRepository?.showDetailedSyncIndicators ?: flowOf(true),
        settingsRepository?.useShortPlayerNames ?: flowOf(true)
    ) { showDetailed, useShort -> showDetailed to useShort }
    
    val uiState =
        combine(
            repository.observeTournament(tournamentId),
            _selectedLeg,
            tournamentSyncService?.syncStatus ?: flowOf(SyncStatus.SYNCED),
            settingsFlow,
            playerRepository?.observePlayers() ?: flowOf(emptyList())
        ) { rawTournament, leg, syncStatus, (showDetailedSyncIndicators, useShortNames), allPlayers ->
            val tournament = rawTournament?.let { t ->
                val playerDisplayNames = t.players.associate { player ->
                    player.id to PlayerNameFormatter.formatDisplayName(
                        player = player,
                        allPlayers = t.players,
                        useShortName = useShortNames
                    )
                }
                val collator = Collator.getInstance(Locale.GERMAN).apply {
                    strength = Collator.SECONDARY
                }
                val sortedPlayers = t.players.sortedWith(
                    Comparator { p1, p2 ->
                        val n1 = playerDisplayNames[p1.id] ?: p1.fullName
                        val n2 = playerDisplayNames[p2.id] ?: p2.fullName
                        val res = collator.compare(n1, n2)
                        if (res != 0) return@Comparator res
                        val nameRes = collator.compare(p1.name, p2.name)
                        if (nameRes != 0) return@Comparator nameRes
                        val vornameRes = collator.compare(p1.vorname, p2.vorname)
                        if (vornameRes != 0) return@Comparator vornameRes
                        p1.id.compareTo(p2.id)
                    }
                )
                t.copy(players = sortedPlayers)
            }

            val pointsMap = tournament?.players?.associate { player ->
                player.id to calculatePlayerPointsUseCase(tournament, player.id)
            } ?: emptyMap()

            val effectiveLeg = if (tournament != null && leg > tournament.rounds) 1 else leg

            val rows = if (tournament != null) {
                buildRows(
                    tournament = tournament,
                    leg = effectiveLeg,
                    pointsMap = pointsMap,
                    showDetailedSyncIndicators = showDetailedSyncIndicators,
                    useShortNames = useShortNames,
                    enabled = syncStatus != SyncStatus.FINALIZED
                )
            } else {
                emptyList()
            }

            val participatingIds = tournament?.playerIds?.toSet() ?: emptySet()
            val germanCollator = Collator.getInstance(Locale.GERMAN).apply { strength = Collator.SECONDARY }
            val availablePlayers = allPlayers
                .filter { it.id !in participatingIds }
                .sortedWith(
                    Comparator { p1, p2 ->
                        val res = germanCollator.compare(p1.name, p2.name)
                        if (res != 0) res else germanCollator.compare(p1.vorname, p2.vorname)
                    }
                )

            CrosstableUiState(
                tournament = tournament,
                leg = effectiveLeg,
                playerPoints = pointsMap,
                rows = rows,
                isLoading = false,
                syncStatus = syncStatus,
                showDetailedSyncIndicators = showDetailedSyncIndicators,
                availablePlayers = availablePlayers
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CrosstableUiState()
        )

    private fun buildRows(
        tournament: Tournament,
        leg: Int,
        pointsMap: Map<Long, Double>,
        showDetailedSyncIndicators: Boolean,
        useShortNames: Boolean,
        enabled: Boolean
    ): List<CrosstableRowData> {
        val playerDisplayNames = tournament.players.associate { player ->
            player.id to PlayerNameFormatter.formatDisplayName(
                player = player,
                allPlayers = tournament.players,
                useShortName = useShortNames
            )
        }
        return tournament.playerIds.indices.map { rowIndex ->
            val playerId = tournament.playerIds[rowIndex]
            val points = pointsMap[playerId] ?: 0.0
            val results = mutableListOf<String>()
            val dirtyFlags = mutableListOf<Boolean>()

            for (columnIndex in tournament.playerIds.indices) {
                if (columnIndex == rowIndex) {
                    results.add("x")
                    dirtyFlags.add(false)
                } else {
                    val game = tournament.findGame(
                        playerId,
                        tournament.playerIds[columnIndex],
                        leg
                    )
                    results.add(game?.result?.displayName ?: "")
                    dirtyFlags.add(game?.dirty ?: false)
                }
            }

            val isWithdrawn = tournament.withdrawnPlayerIds.contains(playerId)

            CrosstableRowData(
                rowIndex = rowIndex,
                playerId = playerId,
                playerName = playerDisplayNames[playerId] ?: tournament.players.getOrNull(rowIndex)?.fullName ?: "",
                totalPoints = points,
                results = results,
                dirtyFlags = dirtyFlags,
                showDetailedSyncIndicators = showDetailedSyncIndicators,
                enabled = enabled,
                isWithdrawn = isWithdrawn
            )
        }
    }

    fun setResult(rowIndex: Int, columnIndex: Int, round: Int, result: GameResult) {
        val tournament = uiState.value.tournament ?: return
        if (uiState.value.syncStatus == SyncStatus.FINALIZED) return
        viewModelScope.launch {
            Log.i("CrosstableViewModel", "setGameResultUseCase: tournament=${tournament.id}, rowIndex=$rowIndex, columnIndex=$columnIndex, round=$round, result=$result")
            setGameResultUseCase(tournament.id, tournament.playerIds[rowIndex], tournament.playerIds[columnIndex], round, result)
            tournamentSyncService?.triggerSync(tournament.id)
        }
    }

    fun withdrawPlayer(playerId: Long, rule: PlayerWithdrawRule) {
        val tournament = uiState.value.tournament ?: return
        if (uiState.value.syncStatus == SyncStatus.FINALIZED) return
        viewModelScope.launch {
            Log.i("CrosstableViewModel", "withdrawPlayer: tournament=${tournament.id}, playerId=$playerId, rule=$rule")
            repository.withdrawPlayer(tournament.id, playerId, rule)
            tournamentSyncService?.triggerSync(tournament.id)
        }
    }

    fun addExistingPlayer(playerId: Long) {
        val tournament = uiState.value.tournament ?: return
        if (uiState.value.syncStatus == SyncStatus.FINALIZED) return
        viewModelScope.launch {
            Log.i("CrosstableViewModel", "addExistingPlayer: tournament=${tournament.id}, playerId=$playerId")
            if (!tournament.isLocalOnly) {
                runCatching { syncPlayersUseCase?.invoke() }
            }
            repository.addPlayerToTournament(tournament.id, playerId)
            tournamentSyncService?.triggerSync(tournament.id)
        }
    }

    fun addNewPlayer(
        vorname: String,
        name: String,
        isLocal: Boolean = false,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val tournament = uiState.value.tournament ?: return
        if (uiState.value.syncStatus == SyncStatus.FINALIZED) return
        val addPlayer = addPlayerUseCase ?: run {
            onError("Spieler anlegen nicht verfügbar")
            return
        }
        viewModelScope.launch {
            try {
                Log.i("CrosstableViewModel", "addNewPlayer: tournament=${tournament.id}, vorname=$vorname, name=$name, isLocal=$isLocal")
                val createdPlayer = addPlayer(vorname, name, isLocal = isLocal)
                if (!isLocal) {
                    runCatching { syncPlayersUseCase?.invoke() }
                }
                repository.addPlayerToTournament(tournament.id, createdPlayer.id)
                tournamentSyncService?.triggerSync(tournament.id)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Fehler beim Anlegen des Spielers")
            }
        }
    }

    private val _syncMessageEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val syncMessageEvent = _syncMessageEvent.asSharedFlow()

    fun triggerManualSync() {
        val tournament = uiState.value.tournament ?: return
        val currentStatus = uiState.value.syncStatus
        if (currentStatus == SyncStatus.SYNCING || currentStatus == SyncStatus.FINALIZED) {
            return
        }
        viewModelScope.launch {
            tournamentSyncService?.triggerSync(tournament.id)
            val newStatus = tournamentSyncService?.syncStatus?.value
            val error = runCatching { tournamentSyncService?.lastSyncError?.value }.getOrNull()
            if (newStatus == SyncStatus.FAILED) {
                _syncMessageEvent.emit(error ?: "Synchronisierung fehlgeschlagen.")
            } else if (newStatus == SyncStatus.SYNCED) {
                _syncMessageEvent.emit("Erfolgreich synchronisiert!")
            }
        }
    }

    fun prepareRankingsAndNavigate(onNavigate: () -> Unit) {
        val tournament = uiState.value.tournament ?: return
        viewModelScope.launch {
            createTournamentRankingsUseCase(tournament)
            onNavigate()
        }
    }

    fun selectLeg(leg: Int) {
        val tournament = uiState.value.tournament ?: return
        if (leg in 1..tournament.rounds) {
            _selectedLeg.value = leg
        }
    }

    fun updateTournamentRounds(rounds: Int) {
        val tournament = uiState.value.tournament ?: return
        if (uiState.value.syncStatus == SyncStatus.FINALIZED) return
        if (rounds !in 1..2) return
        if (rounds == tournament.rounds) return
        viewModelScope.launch {
            if (rounds < _selectedLeg.value) {
                _selectedLeg.value = 1
            }
            repository.updateTournamentRounds(tournament.id, rounds)
            tournamentSyncService?.triggerSync(tournament.id)
        }
    }

    fun resetTournament() {
        viewModelScope.launch {
            repository.resetTournament()
        }
    }
}
