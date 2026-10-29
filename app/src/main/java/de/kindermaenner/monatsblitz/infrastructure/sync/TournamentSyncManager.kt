package de.kindermaenner.monatsblitz.infrastructure.sync

import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.SyncGameResultsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncTournamentUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ApiErrorDto
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class TournamentSyncManager(
    private val tournamentStorage: TournamentStorage,
    private val tournamentDao: TournamentDao,
    private val gameDao: GameDao,
    private val syncTournamentUseCase: SyncTournamentUseCase,
    private val syncGameResultsUseCase: SyncGameResultsUseCase,
    private val syncPlayersUseCase: SyncPlayersUseCase,
    private val syncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : TournamentSyncService {

    companion object {
        private const val TAG = "TournamentSyncManager"
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCED)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    override val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private val syncMutex = Mutex()

    override suspend fun checkInitialStatus(tournamentId: Long) {
        syncScope.launch {
            checkInitialStatusInternal(tournamentId)
        }.join()
    }

    private suspend fun checkInitialStatusInternal(tournamentId: Long) {
        if (isFinalizedInStorage()) {
            _syncStatus.value = SyncStatus.FINALIZED
            return
        }

        val tournament = tournamentDao.getTournament(tournamentId)
        if (tournament?.tournament?.isLocalOnly == true) {
            _syncStatus.value = SyncStatus.SYNCED
            return
        }

        val isTournamentUnsynced = tournament != null && tournament.tournament.remoteId == null
        val dirtyGames = gameDao.getDirtyGamesForTournament(tournamentId)

        if (isTournamentUnsynced || dirtyGames.isNotEmpty()) {
            triggerSyncInternal(tournamentId)
        } else {
            _syncStatus.value = SyncStatus.SYNCED
        }
    }

    override suspend fun triggerSync(tournamentId: Long) {
        if (_syncStatus.value == SyncStatus.FINALIZED) return

        syncScope.launch {
            triggerSyncInternal(tournamentId)
        }.join()
    }

    private suspend fun triggerSyncInternal(tournamentId: Long) {
        if (_syncStatus.value == SyncStatus.FINALIZED) return

        val tournament = tournamentDao.getTournament(tournamentId)
        if (tournament?.tournament?.isLocalOnly == true) {
            _syncStatus.value = SyncStatus.SYNCED
            return
        }

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
            _lastSyncError.value = null
            Log.i(TAG, "Starting sync for tournament $tournamentId")
            syncPlayersUseCase()
            Log.i(TAG, "syncPlayersUseCase completed")
            syncTournamentUseCase()
            Log.i(TAG, "syncTournamentUseCase completed")
            syncGameResultsUseCase()
            Log.i(TAG, "syncGameResultsUseCase completed")
            val status = resolvePostSyncStatus(tournamentId)
            Log.i(TAG, "Sync resolved with status: $status")
            if (status == SyncStatus.SYNCED) {
                _lastSyncError.value = null
            }
            status
        } catch (e: HttpException) {
            val responseBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val errorMsg = when (e.code()) {
                401 -> "Authentifizierungsfehler (HTTP 401). Bitte API-Schlüssel in Einstellungen prüfen."
                403 -> "Turnier ist auf dem Server bereits abgeschlossen (HTTP 403)."
                404 -> "Server-Endpunkt nicht gefunden (HTTP 404). Bitte Server-Adresse in Einstellungen prüfen."
                409 -> "Turnier existiert bereits oder Konflikt auf dem Server (HTTP 409)."
                426 -> ApiErrorDto.extractMessage(responseBody)
                    ?: "Inkompatible API-Version zwischen App und WordPress-Plugin. Bitte prüfe Updates für App und Plugin."
                else -> "Server meldete HTTP ${e.code()}: ${responseBody ?: e.message()}"
            }
            Log.e(TAG, "Sync failed: $errorMsg (body=$responseBody)", e)
            _lastSyncError.value = errorMsg
            handleHttpException(e)
        } catch (e: UnknownHostException) {
            val errorMsg = "Server nicht erreichbar. Bitte Internetverbindung und Server-Adresse prüfen."
            Log.e(TAG, "Sync failed: $errorMsg", e)
            _lastSyncError.value = errorMsg
            SyncStatus.FAILED
        } catch (e: ConnectException) {
            val errorMsg = "Verbindung fehlgeschlagen. Bitte Server-Adresse und Netzwerk prüfen."
            Log.e(TAG, "Sync failed: $errorMsg", e)
            _lastSyncError.value = errorMsg
            SyncStatus.FAILED
        } catch (e: SocketTimeoutException) {
            val errorMsg = "Zeitüberschreitung bei Verbindung zum Server."
            Log.e(TAG, "Sync failed: $errorMsg", e)
            _lastSyncError.value = errorMsg
            SyncStatus.FAILED
        } catch (e: Exception) {
            val errorMsg = "Synchronisierung fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}"
            Log.e(TAG, "Sync failed: $errorMsg", e)
            _lastSyncError.value = errorMsg
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
            if (!isTournamentSynced && _lastSyncError.value == null) {
                _lastSyncError.value = "Turnier konnte nicht mit dem Server abgeglichen werden."
            } else if (remainingDirtyGames.isNotEmpty() && _lastSyncError.value == null) {
                _lastSyncError.value = "Einige Spielergebnisse konnten nicht synchronisiert werden."
            }
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
