package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toDomain
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayerRepositoryImpl(
    private val playerDao: PlayerDao
) : PlayerRepository {

    override fun observePlayers(): Flow<List<Player>> =
        playerDao.observePlayers()
            .map { entities ->
                entities.map { it -> it.toDomain()}
            }

    override fun observePlayer(id: Long): Flow<Player?> =
        playerDao.observePlayer(id)
            .map { entity ->
                entity?.toDomain()
            }

    override suspend fun createPlayer(newPlayer: NewPlayer): Player {
        val e = newPlayer.toEntity()
        val id = playerDao.insert(e)
        return e.copy(id = id).toDomain()
    }

}