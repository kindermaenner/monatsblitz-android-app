package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("""
        SELECT * FROM games 
        WHERE tournamentId = :tournamentId
    """)
    fun observeGames(tournamentId: Long): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Update
    suspend fun update(game: GameEntity)

    @Query("""
        SELECT * FROM games
        WHERE tournamentId = :tournamentId
        AND player1Id = :player1Id
        AND player2Id = :player2Id
        AND leg = :leg
        LIMIT 1
    """)
    suspend fun getGame(
        tournamentId: Long,
        player1Id: Long,
        player2Id: Long,
        leg: Leg
    ): GameEntity?

    @Query("""
    SELECT * FROM games
    WHERE tournamentId = :tournamentId
      AND dirty = 1
""")
    suspend fun getDirtyGames(tournamentId: Long): List<GameEntity>

    @Query("""
    UPDATE games
    SET dirty = 0
    WHERE tournamentId = :tournamentId
        AND player1Id = :player1Id
        AND player2Id = :player2Id
        AND leg = :leg
""")
    suspend fun markGameAsSynced(tournamentId : Long, player1Id: Long, player2Id:Long, leg : Leg)

    suspend fun markGameAsSynced(game: GameEntity) {
        markGameAsSynced(
            game.tournamentId,
            game.player1Id,
            game.player2Id,
            game.leg
        )
    }

    @Transaction
    suspend fun markGamesAsSynced(games: List<GameEntity>) {
        games.forEach {
            markGameAsSynced(
                it.tournamentId,
                it.player1Id,
                it.player2Id,
                it.leg
            )
        }
    }
}
