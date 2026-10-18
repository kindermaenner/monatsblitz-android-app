package de.kindermaenner.monatsblitz.infrastructure.sync

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

enum class SyncStatus {
    SYNCED,
    SYNCING,
    FAILED,
    FINALIZED
}

class TournamentSyncManager(
    private val tournamentStorage: TournamentStorage,
    private val tournamentDao: TournamentDao,
    private val gameDao: GameDao,
    private val syncTournamentUseCase: SyncTournamentUseCase,
    private val syncGameResultsUseCase: SyncGameResultsUseCase,
    private val syncPlayersUseCase: SyncPlayersUseCase
) {
    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val syncMutex = Mutex()

    suspend fun checkInitialStatus(tournamentId: Long) {
        val state = tournamentStorage.getTournamentState().first()
        if (state?.finalized == true) {
            _syncStatus.value = SyncStatus.FINALIZED
            return
        }

        val dirtyGames = gameDao.getDirtyGamesForTournament(tournamentId)
        val dirtyTournaments = tournamentDao.getDirtyTournaments()
        if (dirtyGames.isNotEmpty() || dirtyTournaments.any { it.tournament.id == tournamentId }) {
            _syncStatus.value = SyncStatus.FAILED
        } else {
            _syncStatus.value = SyncStatus.SYNCED
        }
    }

    suspend fun triggerSync(tournamentId: Long) {
        if (_syncStatus.value == SyncStatus.FINALIZED) return

        syncMutex.withLock {
            val state = tournamentStorage.getTournamentState().first()
            if (state?.finalized == true) {
                _syncStatus.value = SyncStatus.FINALIZED
                return
            }

            _syncStatus.value = SyncStatus.SYNCING

            try {
                // 1. Spieler synchronisieren (damit remoteIds existieren)
                syncPlayersUseCase()

                // 2. Turnier remote anlegen, falls noch nicht geschehen
                syncTournamentUseCase()

                // 3. Spiele / Ergebnisse synchronisieren
                syncGameResultsUseCase()

                // 4. Prüfen, ob alle Spiele dieses Turniers synchronisiert wurden
                val remainingDirtyGames = gameDao.getDirtyGamesForTournament(tournamentId)
                if (remainingDirtyGames.isEmpty()) {
                    _syncStatus.value = SyncStatus.SYNCED
                } else {
                    _syncStatus.value = SyncStatus.FAILED
                }
            } catch (e: HttpException) {
                if (e.code() == 403) {
                    tournamentStorage.finalizeTournament()
                    _syncStatus.value = SyncStatus.FINALIZED
                } else {
                    _syncStatus.value = SyncStatus.FAILED
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.FAILED
            }
        }
    }
}
