package de.kindermaenner.monatsblitz.domain.repository

import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Tournament
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {
    fun observeTournaments(): Flow<List<Tournament>>
    fun observeTournament(id: Int): Flow<Tournament?>
    suspend fun createTournament(request: NewTournament): Tournament
    suspend fun refreshTournament(id: Int)

    suspend fun syncTournament(id: Int)
}
