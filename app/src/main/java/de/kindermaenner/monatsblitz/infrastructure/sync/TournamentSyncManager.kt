package de.kindermaenner.monatsblitz.infrastructure.sync

import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.SyncGameResultsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncTournamentUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

class TournamentSyncManager(
    private val tournamentStorage: TournamentStorage,
    private val tournamentDao: TournamentDao,
    private val gameDao: GameDao,
    private val syncTournamentUseCase: SyncTournamentUseCase,
    private val syncGameResultsUseCase: SyncGameResultsUseCase,
    private val syncPlayersUseCase: SyncPlayersUseCase
) : TournamentSyncService {
    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val syncMutex = Mutex()

    override suspend fun checkInitialStatus(tournamentId: Long) {
        val state = tournamentStorage.getTournamentState().first()
        if (state?.finalized == true) {
            _syncStatus.value = SyncStatus.FINALIZED
            return
        }

        val tournament = tournamentDao.getTournament(tournamentId)
        val isTournamentUnsynced = tournament != null && tournament.tournament.remoteId == null
        val dirtyGames = gameDao.getDirtyGamesForTournament(tournamentId)

        if (isTournamentUnsynced || dirtyGames.isNotEmpty()) {
            triggerSync(tournamentId)
        } else {
            _syncStatus.value = SyncStatus.SYNCED
        }
    }

    override suspend fun triggerSync(tournamentId: Long) {
        if (_syncStatus.value == SyncStatus.FINALIZED) return

        syncMutex.withLock {
            if (isFinalizedInStorage()) {
                _syncStatus.value = SyncStatus.FINALIZED
                return
            }

            _syncStatus.value = SyncStatus.SYNCING
            _syncStatus.value = executeSyncSteps(tournamentId)
        }
    }

    private suspend fun isFinalizedInStorage(): Boolean {
        return tournamentStorage.getTournamentState().first()?.finalized == true
    }

    private suspend fun executeSyncSteps(tournamentId: Long): SyncStatus {
        return try {
            syncPlayersUseCase()
            syncTournamentUseCase()
            syncGameResultsUseCase()
            resolvePostSyncStatus(tournamentId)
        } catch (e: HttpException) {
            handleHttpException(e)
        } catch (_: Exception) {
            SyncStatus.FAILED
        }
    }

    private suspend fun resolvePostSyncStatus(tournamentId: Long): SyncStatus {
        val updatedTournament = tournamentDao.getTournament(tournamentId)
        val isTournamentSynced = updatedTournament == null || updatedTournament.tournament.remoteId != null
        val remainingDirtyGames = gameDao.getDirtyGamesForTournament(tournamentId)
        return if (isTournamentSynced && remainingDirtyGames.isEmpty()) {
            SyncStatus.SYNCED
        } else {
            SyncStatus.FAILED
        }
    }

    private suspend fun handleHttpException(e: HttpException): SyncStatus {
        return if (e.code() == 403) {
            tournamentStorage.finalizeTournament()
            SyncStatus.FINALIZED
        } else {
            SyncStatus.FAILED
        }
    }
}
