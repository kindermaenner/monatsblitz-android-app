package de.kindermaenner.monatsblitz.domain.usecase
import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository

class SetGameResultUseCase(private val repository: TournamentRepository) {
    suspend operator fun invoke(tournamentId : Long, player1Id : Long, player2Id : Long, leg : Int, result : GameResult) {
        Log.i("SetGameResultUseCase", "invoke: tournamentId=${tournamentId}, player1Id=${player1Id}, player2Id=${player2Id}, leg=${leg}, result=${result}")
        val minPlayerId = minOf(player1Id, player2Id)
        val maxPlayerId = maxOf(player1Id, player2Id)
        val canonicalResult = if (player1Id <= player2Id) result else result.opposite()
        repository.updateGameResult(tournamentId, minPlayerId, maxPlayerId, leg, canonicalResult)
    }
}