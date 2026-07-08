package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {

    @Query("SELECT * FROM players")
    fun observePlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id IN (:ids)")
    suspend fun getPlayersByIds(ids: List<Int>): List<PlayerEntity>

    @Query("SELECT * FROM players WHERE id == :id")
    fun observePlayer(id: Int): Flow<PlayerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: PlayerEntity)

    @Query("SELECT * FROM players WHERE dirty = 1")
    suspend fun getDirtyPlayers(): List<PlayerEntity>

    @Query("""
    UPDATE players
    SET dirty = 0
    WHERE id = :playerId
""")
    suspend fun markPlayerAsClean(playerId: Int)
}