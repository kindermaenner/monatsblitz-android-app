package de.kindermaenner.monatsblitz.domain.repository

import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Tournament
import kotlinx.coroutines.flow.Flow

interface TournamentRepository {
    fun observeTournaments(): Flow<List<Tournament>>
    fun observeTournament(id: Long): Flow<Tournament?>
    suspend fun createTournament(request: NewTournament): Tournament

    suspend fun getTournamentById(id: Long): Tournament?

    suspend fun updateGameResult(
        tournamentId: Long,
        playerId1: Long,
        playerId2: Long,
        leg: Int,
        result: GameResult
    )
}
