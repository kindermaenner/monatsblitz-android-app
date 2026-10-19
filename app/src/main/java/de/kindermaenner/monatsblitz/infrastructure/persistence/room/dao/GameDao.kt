package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.projection.GameSyncData
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

    @Upsert
    suspend fun upsert(game: GameEntity) : Long

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
    SET dirty = 0, remoteId = :remoteId
    WHERE id = :gameId
""")
    suspend fun markGameAsSynced(gameId : Long, remoteId : Int)

    @Query("""
        SELECT * FROM games
        WHERE tournamentId = :tournamentId
          AND player1Id = :playerId1
          AND player2Id = :playerId2
          AND leg = :leg
    """)
    suspend fun getGameByPlayers(tournamentId: Long, playerId1: Long, playerId2: Long, leg: Int): GameEntity?

    @Query("""
    SELECT 
        g.id AS localGameId,
        g.remoteId AS remoteGameId,
        t.remoteId AS tournamentRemoteId,
        p1.remoteId AS player1Id,
        p2.remoteId AS player2Id,
        g.leg,
        g.result
    FROM games g
    INNER JOIN tournaments t 
        ON g.tournamentId = t.id
    INNER JOIN players p1
        ON g.player1Id = p1.id
    INNER JOIN players p2
        ON g.player2Id = p2.id
    WHERE g.dirty = 1
      AND (g.result != 'Open' OR g.remoteId IS NOT NULL)
      AND t.remoteId IS NOT NULL
      AND p1.remoteId IS NOT NULL
      AND p2.remoteId IS NOT NULL
""")
    suspend fun getGamesForSync(): List<GameSyncData>
}
