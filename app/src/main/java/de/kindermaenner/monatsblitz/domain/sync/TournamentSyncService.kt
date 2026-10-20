package de.kindermaenner.monatsblitz.domain.sync

import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

interface TournamentSyncService {
    val syncStatus: StateFlow<SyncStatus>
    val lastSyncError: StateFlow<String?>

    suspend fun checkInitialStatus(tournamentId: Long)

    suspend fun triggerSync(tournamentId: Long)
}
