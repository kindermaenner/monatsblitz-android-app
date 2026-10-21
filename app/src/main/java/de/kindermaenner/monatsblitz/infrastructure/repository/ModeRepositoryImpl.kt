package de.kindermaenner.monatsblitz.infrastructure.repository

import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.repository.ModeRepository
import de.kindermaenner.monatsblitz.infrastructure.api.ModeRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewModeDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDomain
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.ModeDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.ModeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ModeRepositoryImpl(
    private val modeDao: ModeDao,
    private val remoteDataSource: ModeRemoteDataSource? = null
) : ModeRepository {

    companion object {
        private const val TAG = "ModeRepositoryImpl"
    }

    override fun observeModes(): Flow<List<GameMode>> =
        modeDao.observeModes().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getAllModes(): List<GameMode> {
        return modeDao.getAllModes().map { it.toDomain() }
    }

    override suspend fun createMode(name: String, permanent: Boolean): GameMode {
        val trimmedName = name.trim()
        if (!permanent) {
            return GameMode(name = trimmedName)
        }

        val existing = modeDao.findModeByName(trimmedName)
        if (existing != null) {
            return existing.toDomain()
        }

        val entity = ModeEntity(name = trimmedName, dirty = true)
        val id = modeDao.insert(entity)

        val remote = remoteDataSource
        if (remote != null) {
            try {
                val result = remote.createMode(NewModeDto(name = trimmedName))
                if (result.success && result.id != null) {
                    modeDao.setRemoteId(id, result.id)
                    return entity.copy(id = id, remoteId = result.id, dirty = false).toDomain()
                }
            } catch (e: Exception) {
                Log.w(TAG, "createMode: failed to sync with remote, will sync later: ${e.message}")
            }
        }

        return entity.copy(id = id).toDomain()
    }

    override suspend fun syncModes() {
        val remote = remoteDataSource
            ?: throw IllegalStateException("ModeRemoteDataSource ist nicht konfiguriert")

        val dirtyModes = modeDao.getDirtyModes()
        Log.i(TAG, "syncModes: ${dirtyModes.size} dirty modes to sync")
        dirtyModes.forEach {
            try {
                val result = remote.createMode(NewModeDto(name = it.name))
                if (result.success && result.id != null) {
                    modeDao.setRemoteId(modeId = it.id, remoteId = result.id)
                }
            } catch (e: Exception) {
                Log.w(TAG, "syncModes: failed to sync dirty mode ${it.name}: ${e.message}")
            }
        }

        val remoteModes = remote.getModes().map { it.toEntity() }
        Log.i(TAG, "syncModes: fetched ${remoteModes.size} remote modes")
        remoteModes.forEach { remoteMode ->
            val remoteId = remoteMode.remoteId ?: return@forEach
            val existingByRemoteId = modeDao.getModeByRemoteId(remoteId)
            if (existingByRemoteId != null) {
                modeDao.update(
                    existingByRemoteId.copy(
                        name = remoteMode.name.trim(),
                        dirty = false
                    )
                )
            } else {
                val existingByName = modeDao.findModeByName(remoteMode.name.trim())
                if (existingByName != null) {
                    modeDao.setRemoteId(modeId = existingByName.id, remoteId = remoteId)
                } else {
                    modeDao.insert(
                        remoteMode.copy(
                            name = remoteMode.name.trim(),
                            dirty = false
                        )
                    )
                }
            }
        }
    }
}
