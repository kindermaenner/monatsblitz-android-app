package de.kindermaenner.monatsblitz.domain.repository


import de.kindermaenner.monatsblitz.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeGames(tournamentId : Int): Flow<List<Game>>

    fun observeGame(tournamentId : Int, player1Id:Int, player2Id:Int): Flow<Game?>

    suspend fun createGame(newGame: Game)

    suspend fun updateGame(game: Game)
}

