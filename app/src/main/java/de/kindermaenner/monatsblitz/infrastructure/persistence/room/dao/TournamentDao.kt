package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentDao {

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournament(id: Long): TournamentEntity?

    @Query("SELECT * FROM tournaments WHERE remoteId = :remoteId")
    suspend fun getTournamentByRemoteId(remoteId: Int): TournamentEntity?

    @Query("SELECT * FROM tournaments")
    fun observeTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    fun observeTournament(id: Long): Flow<TournamentEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tournament: TournamentEntity) : Long

    @Update
    suspend fun update(tournament: TournamentEntity)

    @Delete
    suspend fun delete(tournament: TournamentEntity)

    @Query("""
    UPDATE tournaments
    SET dirty = 0
    WHERE id = :tournamentId
""")
    suspend fun markTournamentAsClean(tournamentId: Long)
}