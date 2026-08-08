package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGamesDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDtoData
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toResultString
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao

class SyncGameResultsUseCase(val monatsblitzApi: MonatsblitzApi, val gameDao: GameDao) {
    suspend operator fun invoke() {
        val gamesByTournament = gameDao.getGamesForSync()
            .groupBy { it.tournamentRemoteId }
        gamesByTournament.forEach { (tournamentRemoteId, games) ->
            val data = games.map { syncData ->
                GameDtoData(
                    player1Id = syncData.player1Id.toInt(),
                    player2Id = syncData.player2Id.toInt(),
                    leg = syncData.leg,
                    result = syncData.result.toResultString()
                )
            }

            val response  = monatsblitzApi.createGames(CreateGamesDto(
                tournamentId = tournamentRemoteId,
                games = data
            ))
            if (response.success) {
                games.forEachIndexed { index, game ->
                    gameDao.markGameAsSynced(game.localGameId, response.items[index].gameId)
                }
            }
        }
    }
}