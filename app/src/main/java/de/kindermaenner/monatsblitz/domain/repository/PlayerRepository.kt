package de.kindermaenner.monatsblitz.domain.repository

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun observePlayers(): Flow<List<Player>>

    fun observePlayer(id: Int): Flow<Player?>

    suspend fun createPlayer(newPlayer: NewPlayer) : Player

    suspend fun refreshPlayers()

    suspend fun syncDirtyPlayers() : Boolean
}
