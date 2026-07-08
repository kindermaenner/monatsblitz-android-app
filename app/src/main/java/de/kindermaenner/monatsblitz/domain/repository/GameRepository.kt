package de.kindermaenner.monatsblitz.domain.repository


import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.domain.model.NewGame
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeGames(tournamentId : Int): Flow<List<Game>>

    fun observeGame(tournamentId : Int, player1Id:Int, player2Id:Int, leg: Leg): Flow<Game?>

    suspend fun createGame(newGame: NewGame) : Game

    suspend fun updateGame(game: Game)
}

