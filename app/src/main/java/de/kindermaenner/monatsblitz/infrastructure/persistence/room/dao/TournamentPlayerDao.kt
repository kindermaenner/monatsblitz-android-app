package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef

@Dao
interface TournamentPlayerDao {

    @Insert
    suspend fun insertAll(
        refs: List<TournamentPlayerCrossRef>
    )

    @Query("""
        DELETE FROM tournament_player_cross_ref
        WHERE tournamentId = :tournamentId
    """)
    suspend fun deleteForTournament(tournamentId: Long)
}