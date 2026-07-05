package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.data.model.GameMode
import de.kindermaenner.monatsblitz.data.model.Player
import de.kindermaenner.monatsblitz.data.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toPlayer
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toTournament
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TournamentPlayerMappingTest {

    @Test
    fun player_roundtrip() {
        val p = Player(id = 5, Name = "Müller", Vorname = "Max")
        val dto = p.toPlayerDto()
        val p2 = dto.toPlayer()
        assertEquals(p.id, p2.id)
        assertEquals(p.Name, p2.Name)
        assertEquals(p.Vorname, p2.Vorname)
    }

    @Test
    fun tournament_mapping_roundtrip() {
        val p1 = Player(id = 1, Name = "A", Vorname = "One")
        val p2 = Player(id = 2, Name = "B", Vorname = "Two")
        val date = LocalDate.of(2026,7,3)
        val t = Tournament(
            Id = 7,
            Mode = GameMode.BLITZ_5_0,
            Date = date,
            players = listOf(p1, p2),
            doubleRound = true
        )

        val dto = t.toDto()
        assertEquals("2026-07-03", dto.date)
        assertEquals(2, dto.round_count)

        val t2 = dto.toTournament(players = listOf(p1, p2))
        assertEquals(t.Id, t2.Id)
        assertEquals(t.doubleRound, t2.doubleRound)
        assertEquals(t.Mode, t2.Mode)
    }
}
