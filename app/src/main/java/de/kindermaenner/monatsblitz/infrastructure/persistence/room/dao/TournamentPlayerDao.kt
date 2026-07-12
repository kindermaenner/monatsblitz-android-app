package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentPlayerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: TournamentPlayerCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(refs: List<TournamentPlayerCrossRef>)

    @Query("""
        SELECT playerId FROM tournament_players 
        WHERE tournamentId = :tournamentId
    """)
    suspend fun getPlayerIdsForTournament(tournamentId: Long): List<Long>

    @Query("""
        SELECT * FROM tournament_players
        WHERE tournamentId = :tournamentId
    """)
    fun observeRefs(tournamentId: Int): Flow<List<TournamentPlayerCrossRef>>
}