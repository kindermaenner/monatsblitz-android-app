package de.kindermaenner.monatsblitz.infrastructure.repository

import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.infrastructure.api.PlayerRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toEntity
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toNewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toDomain
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlayerRepositoryImpl(
    private val playerDao: PlayerDao,
    private val remoteDataSource: PlayerRemoteDataSource? = null
) : PlayerRepository {

    companion object {
        private const val TAG = "PlayerRepositoryImpl"
    }

    override fun observePlayers(): Flow<List<Player>> =
        playerDao.observePlayers()
            .map { entities ->
                entities.map { it.toDomain() }
            }

    override fun observePlayer(id: Long): Flow<Player?> =
        playerDao.observePlayer(id)
            .map { entity ->
                entity?.toDomain()
            }

    override suspend fun findPlayersByName(vorname: String, name: String): List<Player> =
        playerDao.findPlayersByName(vorname, name).map { it.toDomain() }

    override suspend fun createPlayer(newPlayer: NewPlayer): Player {
        val e = newPlayer.toEntity()
        val id = playerDao.insert(e)
        return e.copy(id = id).toDomain()
    }

    override suspend fun promotePlayer(id: Long) {
        playerDao.promoteLocalPlayer(id)
    }

    override suspend fun syncPlayers() {
        val remote = remoteDataSource
            ?: throw IllegalStateException("PlayerRemoteDataSource ist nicht konfiguriert")

        val dirtyPlayers = playerDao.getDirtyPlayers()
        Log.i(TAG, "syncPlayers: ${dirtyPlayers.size} dirty players to sync")
        dirtyPlayers.forEach {
            Log.i(TAG, "syncPlayers: creating player on server: ${it.vorname} ${it.name}")
            val result = remote.createPlayer(it.toNewPlayerDto())
            Log.i(TAG, "syncPlayers: server result: $result")
            if (result.success && result.playerId != null) {
                playerDao.setRemoteId(
                    playerId = it.id,
                    remoteId = result.playerId
                )
            }
        }

        val remotePlayers = remote.getPlayers().map { it.toEntity() }
        Log.i(TAG, "syncPlayers: fetched ${remotePlayers.size} remote players")
        remotePlayers.forEach { remotePlayer ->
            val remoteId = remotePlayer.remoteId ?: return@forEach
            val existingByRemoteId = playerDao.getPlayerByRemoteId(remoteId)
            if (existingByRemoteId != null) {
                playerDao.update(
                    existingByRemoteId.copy(
                        name = remotePlayer.name.trim(),
                        vorname = remotePlayer.vorname.trim(),
                        lastParticipated = remotePlayer.lastParticipated,
                        dirty = false,
                        isLocal = false
                    )
                )
            } else {
                val existingByName = playerDao.findPlayersByName(
                    remotePlayer.vorname.trim(),
                    remotePlayer.name.trim()
                ).firstOrNull()
                if (existingByName != null) {
                    playerDao.setRemoteId(playerId = existingByName.id, remoteId = remoteId)
                } else {
                    playerDao.insert(
                        remotePlayer.copy(
                            name = remotePlayer.name.trim(),
                            vorname = remotePlayer.vorname.trim(),
                            dirty = false,
                            isLocal = false
                        )
                    )
                }
            }
        }
    }
}
