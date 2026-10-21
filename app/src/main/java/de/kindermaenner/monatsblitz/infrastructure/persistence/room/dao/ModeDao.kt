package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.ModeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {

    @Query("SELECT * FROM modes ORDER BY id ASC")
    fun observeModes(): Flow<List<ModeEntity>>

    @Query("SELECT * FROM modes ORDER BY id ASC")
    suspend fun getAllModes(): List<ModeEntity>

    @Query("SELECT * FROM modes WHERE id = :id")
    suspend fun getModeById(id: Long): ModeEntity?

    @Query("SELECT * FROM modes WHERE LOWER(name) = LOWER(:name)")
    suspend fun findModeByName(name: String): ModeEntity?

    @Query("SELECT * FROM modes WHERE remoteId = :remoteId")
    suspend fun getModeByRemoteId(remoteId: Long): ModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mode: ModeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modes: List<ModeEntity>): List<Long>

    @Upsert
    suspend fun upsertAll(modes: List<ModeEntity>): List<Long>

    @Update
    suspend fun update(mode: ModeEntity)

    @Query("SELECT * FROM modes WHERE dirty = 1")
    suspend fun getDirtyModes(): List<ModeEntity>

    @Query("UPDATE modes SET dirty = 0, remoteId = :remoteId WHERE id = :modeId")
    suspend fun setRemoteId(modeId: Long, remoteId: Long)

    @Query("DELETE FROM modes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM modes")
    suspend fun deleteAll()
}
