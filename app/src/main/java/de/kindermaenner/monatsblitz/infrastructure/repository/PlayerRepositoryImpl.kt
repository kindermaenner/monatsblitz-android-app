package de.kindermaenner.monatsblitz.infrastructure.repository

import retrofit2.HttpException
import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDomain
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.PlayerMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

class PlayerRepositoryImpl(
    private val api: MonatsblitzApi,
    private val playerDao: PlayerDao
) : PlayerRepository {

    override fun observePlayers(): Flow<List<Player>> =
        playerDao.observePlayers()
            .map { entities ->
                entities.map(PlayerMapper::toDomain)
            }

    override fun observePlayer(id: Long): Flow<Player?> =
        playerDao.observePlayer(id)
            .map { entity ->
                entity?.let(PlayerMapper::toDomain)
            }

    override suspend fun createPlayer(newPlayer: NewPlayer): Player {
        val createdPlayer = try {
            api.createPlayer(newPlayer.toDto())
        }  catch (e: IOException) {
            null
        } catch (e: HttpException) {
            null
        }
        val entity = if (createdPlayer != null)
            PlayerMapper.toEntity(createdPlayer.toDomain(), createdPlayer.id)
        else
            PlayerMapper.toEntity(newPlayer)
        val id = playerDao.insert(entity)
        return PlayerMapper.toDomain(entity.copy(id = id))
    }


    override suspend fun refreshPlayers() {
        val remote = api.getPlayers()

        val entities = remote.map { dto ->
            PlayerMapper.toEntity(
                dto,
                remoteId = dto.id
            )
        }
        playerDao.upsertAll(entities)
    }

    override suspend fun syncDirtyPlayers(): Boolean {
        val dirtyPlayers = playerDao.getDirtyPlayers()
        var allSynced = true

        for (dirtyPlayer in dirtyPlayers) {
            try {
                val createdPlayer = api.createPlayer(PlayerMapper.toDto(dirtyPlayer))
                val entity = PlayerMapper.toEntity(createdPlayer, createdPlayer.id, dirtyPlayer.id)
                playerDao.update(entity)
            } catch (e: Exception) {
                allSynced = false
            }
        }

        return allSynced
    }
}