package de.kindermaenner.monatsblitz.domain.repository

import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Tournament
import kotlinx.coroutines.flow.Flow

interface TounamentRepository {
    fun observeTournaments(): Flow<List<Tournament>>
    fun observeTournament(id: Int): Flow<Tournament?>
    suspend fun createTournament(request: NewTournament): Tournament
    suspend fun refreshTournament(id : Int);
}
