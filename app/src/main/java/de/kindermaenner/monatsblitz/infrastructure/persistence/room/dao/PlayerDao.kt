package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players")
    fun observePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id IN (:ids)")
    suspend fun getPlayersByIds(ids: List<Long>): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id == :id")
    suspend fun getPlayerById(id : Long) : PlayerEntity?

    @Query("SELECT * FROM players WHERE id == :id")
    fun observePlayer(id: Long): Flow<PlayerEntity?>

    @Query("SELECT * FROM players WHERE remoteId == :remoteId")
    fun getPlayerByRemoteId(remoteId : Int) : PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>) : List<Long>

    @Upsert
    suspend fun upsertAll(players: List<PlayerEntity>) : List<Long>

    @Update
    suspend fun update(player: PlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity) : Long

    @Query("SELECT * FROM players WHERE dirty = 1")
    suspend fun getDirtyPlayers(): List<PlayerEntity>

    @Query("""
    UPDATE players
    SET dirty = 0
    WHERE id = :playerId
""")
    suspend fun markPlayerAsClean(playerId: Int)
}