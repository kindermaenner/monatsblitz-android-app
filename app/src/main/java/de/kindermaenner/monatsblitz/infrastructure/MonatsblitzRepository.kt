package de.kindermaenner.monatsblitz.infrastructure

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import kotlinx.coroutines.flow.Flow

interface MonatsblitzRepository {

    suspend fun init()
    suspend fun getPlayers(forceRefresh: Boolean): List<Player>

    suspend fun getPlayers(): List<Player> {
        return getPlayers(forceRefresh = false)
    }

    suspend fun createTournament(
        playerIds: List<Int>,
        mode: GameMode,
        doubleRound: Boolean
    ): Tournament?

    val currentTournament: Tournament?

    suspend fun finalizeTournament()

    fun getTournamentState(): Flow<TournamentState?>
}
