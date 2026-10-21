package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TournamentPlayerDao {

    @Insert
    suspend fun insertAll(
        refs: List<TournamentPlayerCrossRef>
    )

    @Upsert
    suspend fun upsertAll(
        refs: List<TournamentPlayerCrossRef>
    )
    @Query("""
        UPDATE tournament_player_cross_ref
        SET hasLeft = :hasLeft
        WHERE tournamentId = :tournamentId AND playerId = :playerId
    """)
    suspend fun setPlayerWithdrawn(tournamentId: Long, playerId: Long, hasLeft: Boolean = true)

    @Query("""
        DELETE FROM tournament_player_cross_ref
        WHERE tournamentId = :tournamentId
    """)
    suspend fun deleteForTournament(tournamentId: Long)

    @Query("""
    SELECT * FROM tournament_player_cross_ref
    WHERE tournamentId = :tournamentId
    ORDER BY rank ASC
""")
    fun observeRankingForTournament(tournamentId: Long): Flow<List<TournamentPlayerCrossRef>>

    @Query("""
    SELECT r.playerId, r.points, r.rank, r.hasLeft, p.vorname, p.name
    FROM tournament_player_cross_ref r
    LEFT JOIN players p ON p.id = r.playerId
    WHERE r.tournamentId = :tournamentId AND r.rank IS NOT NULL
    ORDER BY r.rank ASC
""")
    fun observeRankingWithPlayers(tournamentId: Long): Flow<List<TournamentRankingRowEntity>>

    @Query("""
    SELECT * FROM tournament_player_cross_ref
    WHERE tournamentId = :tournamentId
    ORDER BY rank ASC
""")
    suspend fun getRankingForTournament(tournamentId: Long): List<TournamentPlayerCrossRef>
}

data class TournamentRankingRowEntity(
    val playerId: Long,
    val points: Double?,
    val rank: Int,
    val vorname: String?,
    val name: String?,
    val hasLeft: Boolean = false
)