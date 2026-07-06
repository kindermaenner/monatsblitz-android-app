package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    fun observeGames(tournamentId: Int): Flow<List<GameEntity>>

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
        tournamentId: Int,
        player1Id: Int,
        player2Id: Int,
        leg: Leg
    ): GameEntity?
}