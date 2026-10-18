package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerRankingEntry
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
        
        assertEquals(1L, domain.id)
        assertEquals(GameMode.BLITZ_3_2, domain.mode)
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
        assertEquals(1L, domain.id)
        assertEquals(date, domain.date)
    }

    @Test
    fun `Tournament toEntities maps correctly`() {
        val date = LocalDate.now()
        val domain = Tournament(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, players = listOf())
        
        val (entity, games) = domain.toEntities()
        
        assertEquals(1L, entity.id)
        assertEquals(GameMode.BLITZ_3_2, entity.mode)
        assertEquals(0, games.size)
    }

    @Test
    fun `NewTournament toEntity maps correctly`() {
        val date = LocalDate.now()
        val domain = NewTournament(mode = GameMode.BLITZ_5_0, date = date, rounds = 2, players = listOf(), games = listOf())
        
        val entity = domain.toEntity()
        
        assertEquals(GameMode.BLITZ_5_0, entity.mode)
        assertEquals(2, entity.rounds)
    }

    @Test
    fun `NewGame toEntity maps correctly`() {
        val domain = NewGame(player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Open)
        val entity = domain.toEntity(tournamentId = 5)
        
        assertEquals(5L, entity.tournamentId)
        assertEquals(1L, entity.player1Id)
        assertEquals(GameResult.Open, entity.result)
    }

    @Test
    fun `PlayerRankingEntry toEntity maps correctly`() {
        val domain = PlayerRankingEntry(playerId = 10, tournamentId = 100, points = 8.5, rank = 2)
        val entity = domain.toEntity()
        
        assertEquals(10L, entity.playerId)
        assertEquals(100L, entity.tournamentId)
        assertEquals(8.5, entity.points ?: 0.0, 0.0)
        assertEquals(2, entity.rank)
    }

    @Test
    fun `TournamentPlayerCrossRef toDomain maps correctly`() {
        val entity = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef(playerId = 20, tournamentId = 200, points = 5.0, rank = 1)
        val domain = entity.toDomain()

        assertEquals(20L, domain.playerId)
        assertEquals(200L, domain.tournamentId)
        assertEquals(5.0, domain.points, 0.0)
        assertEquals(1, domain.rank)
    }

    @Test
    fun `GameEntity toDomain maps all fields correctly`() {
        val entity = GameEntity(id = 42L, tournamentId = 1L, player1Id = 10L, player2Id = 20L, leg = 2, result = GameResult.Remis, dirty = true)
        val domain = entity.toDomain()

        assertEquals(42L, domain.id)
        assertEquals(10L, domain.player1Id)
        assertEquals(20L, domain.player2Id)
        assertEquals(2, domain.leg)
        assertEquals(GameResult.Remis, domain.result)
        assertEquals(true, domain.dirty)
    }

    @Test
    fun `Tournament toEntities with games maps all games to GameEntities`() {
        val game = de.kindermaenner.monatsblitz.domain.model.Game(id = 55L, player1Id = 1L, player2Id = 2L, leg = 1, result = GameResult.Loss)
        val domain = Tournament(
            id = 7L,
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.now(),
            rounds = 2,
            players = emptyList(),
            games = mapOf(55L to game)
        )

        val (entity, games) = domain.toEntities()

        assertEquals(7L, entity.id)
        assertEquals(1, games.size)
        assertEquals(55L, games[0].id)
        assertEquals(7L, games[0].tournamentId)
        assertEquals(1L, games[0].player1Id)
        assertEquals(2L, games[0].player2Id)
        assertEquals(1, games[0].leg)
        assertEquals(GameResult.Loss, games[0].result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `TournamentPlayerCrossRef toDomain throws IllegalArgumentException when rank is null`() {
        val entity = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef(playerId = 20, tournamentId = 200, points = 5.0, rank = null)
        entity.toDomain()
    }

    @Test
    fun `TournamentPlayerCrossRef toDomain defaults points to 0 when points is null`() {
        val entity = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef(playerId = 20, tournamentId = 200, points = null, rank = 3)
        val domain = entity.toDomain()

        assertEquals(0.0, domain.points, 0.0)
        assertEquals(3, domain.rank)
    }
}
