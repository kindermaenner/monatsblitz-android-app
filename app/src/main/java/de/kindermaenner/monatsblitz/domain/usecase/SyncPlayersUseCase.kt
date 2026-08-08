package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.infrastructure.api.PlayerRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toEntity
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toNewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao

class SyncPlayersUseCase(private val remoteDataSource : PlayerRemoteDataSource,
                         private val playerDao : PlayerDao) {
    suspend operator fun invoke() {
        val dirtyPlayers = playerDao.getDirtyPlayers()

        dirtyPlayers.forEach {
            val result =
                remoteDataSource.createPlayer(it.toNewPlayerDto())

            playerDao.setRemoteId(
                playerId = it.id,
                remoteId = result.id.toLong()
            )
        }

        // 2. Remote Änderungen holen
        val remotePlayers =
            remoteDataSource.getPlayers().map { it.toEntity() }

        remotePlayers.forEach {
            val r = playerDao.getPlayerByRemoteId(it.remoteId!!)
            if (r == null) {
                playerDao.insert(it)
            } else {
                playerDao.update(it.copy(id = r.id))
            }
        }
    }
}