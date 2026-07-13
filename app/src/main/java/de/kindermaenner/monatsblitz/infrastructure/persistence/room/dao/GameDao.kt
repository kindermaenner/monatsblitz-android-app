package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("""
        SELECT * FROM games 
        WHERE tournamentId = :tournamentId
    """)
    fun observeGames(tournamentId: Long): Flow<List<GameEntity>>

    @Query("""
        SELECT * FROM games 
        WHERE id = :id
    """)
    fun observeGame(id : Long) : Flow<GameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity) : Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>) : List<Long>

    @Upsert
    suspend fun upsertAll(games: List<GameEntity>) : List<Long>

    @Update
    suspend fun update(game: GameEntity)

    @Query("""
        SELECT * FROM games
        WHERE id = :id
    """)
    suspend fun getGame(id : Long): GameEntity?

    @Query("""
    SELECT * FROM games
    WHERE tournamentId = :tournamentId
      AND dirty = 1
""")
    suspend fun getDirtyGamesForTournament(tournamentId: Long): List<GameEntity>

    @Query("""
    SELECT * FROM games
    WHERE dirty = 1
""")
    suspend fun getDirtyGames(): List<GameEntity>

    @Query("""
    UPDATE games
    SET dirty = 0
    WHERE id = :gameId
""")
    suspend fun markGameAsSynced(gameId : Long)

}
