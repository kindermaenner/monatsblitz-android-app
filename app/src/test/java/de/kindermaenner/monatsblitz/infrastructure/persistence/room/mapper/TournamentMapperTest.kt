package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TournamentMapperTest {

    @Test
    fun `toDomain maps TournamentEntity and details correctly`() {
        val date = LocalDate.now()
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val gameEntity = GameEntity(id = 10, tournamentId = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win)
        val player = Player(1, "N", "V")
        
        val domain = tournamentEntity.toDomain(listOf(gameEntity), listOf(player))
        
        assertEquals(1L, domain.Id)
        assertEquals(GameMode.BLITZ_3_2, domain.Mode)
        assertEquals(1, domain.games.size)
        assertEquals(GameResult.Win, domain.games[10L]?.result)
        assertEquals(1, domain.players.size)
    }

    @Test
    fun `toDomain from TournamentWithDetails maps correctly`() {
        val date = LocalDate.now()
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = 100L)
        val details = TournamentWithDetails(
            tournament = tournamentEntity,
            players = listOf(),
            games = listOf()
        )
        
        val domain = details.toDomain()
        assertEquals(1L, domain.Id)
        assertEquals(date, domain.Date)
    }
}
