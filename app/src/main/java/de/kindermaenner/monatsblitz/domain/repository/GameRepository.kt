package de.kindermaenner.monatsblitz.domain.repository


import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.domain.model.NewGame
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeGames(tournamentId : Long): Flow<List<Game>>

    fun observeGame(tournamentId : Long, player1Id:Long, player2Id:Long, leg: Leg): Flow<Game?>

    suspend fun createGame(newGame: NewGame) : Game

    suspend fun updateGame(game: Game)
}

