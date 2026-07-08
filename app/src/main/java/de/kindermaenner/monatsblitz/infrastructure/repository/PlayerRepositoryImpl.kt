package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.PlayerMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayerRepositoryImpl(
    private val api: MonatsblitzApi,
    private val playerDao: PlayerDao
) : PlayerRepository {

    override fun observePlayers(): Flow<List<Player>> =
        playerDao.observePlayers()
            .map { entities ->
                entities.map(PlayerMapper::toDomain)
            }

    override fun observePlayer(id: Int): Flow<Player?> =
        playerDao.observePlayer(id)
            .map { entity ->
                entity?.let(PlayerMapper::toDomain)
            }

    override suspend fun createPlayer(newPlayer: NewPlayer): Player {
        val dto = NewPlayerDto(
            forename = newPlayer.Vorname,
            surname = newPlayer.Name
        )

        val createdPlayer = api.createPlayer(dto)

        val entity = PlayerMapper.toEntity(createdPlayer)
        playerDao.insert(entity.copy(dirty = false))
        return  PlayerMapper.toDomain(entity)
    }

    override suspend fun refreshPlayers() {
        val remote = api.getPlayers()
        val entities = remote.map(PlayerMapper::toEntity)
        playerDao.insertAll(entities)
    }

    override suspend fun syncDirtyPlayers(): Boolean {
        val dirtyPlayers = playerDao.getDirtyPlayers()
        var allSynced = true

        for (dirtyPlayer in dirtyPlayers) {
            try {
                val dto = NewPlayerDto(
                    forename = dirtyPlayer.vorname,
                    surname = dirtyPlayer.name
                )
                val createdPlayer = api.createPlayer(dto)
                val entity = PlayerMapper.toEntity(createdPlayer)
                playerDao.insert(entity.copy(dirty = false))
            } catch (e: Exception) {
                allSynced = false
            }
        }

        return allSynced
    }
}