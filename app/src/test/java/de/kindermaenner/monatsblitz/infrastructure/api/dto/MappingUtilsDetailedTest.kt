package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class MappingUtilsDetailedTest {

    @Test
    fun `NewPlayer toDto maps correctly`() {
        val domain = NewPlayer(Name = "Surname", Vorname = "Forename")
        val dto = domain.toDto()
        
        assertEquals("Forename", dto.forename)
        assertEquals("Surname", dto.surname)
    }

    @Test
    fun `NewTournament toDto maps correctly`() {
        val date = LocalDate.of(2026, 8, 9)
        val domain = NewTournament(
            Mode = GameMode.BLITZ_3_2,
            Date = date,
            rounds = 2,
            players = listOf(),
            games = listOf()
        )
        val dto = domain.toDto()
        
        assertEquals("2026-08-09", dto.date)
        assertEquals("3+2", dto.mode)
        assertEquals(2, dto.round_count)
    }

    @Test
    fun `PlayerEntity toNewPlayerDto maps correctly`() {
        val entity = PlayerEntity(id = 1, name = "Name", vorname = "Vorname", dirty = true)
        val dto = entity.toNewPlayerDto()
        
        assertEquals("Vorname", dto.forename)
        assertEquals("Name", dto.surname)
    }

    @Test
    fun `PlayerDto toEntity maps correctly`() {
        val dto = PlayerDto(id = 100, surname = "Surname", forename = "Forename")
        val entity = dto.toEntity()
        
        assertEquals(100L, entity.remoteId)
        assertEquals("Surname", entity.name)
        assertEquals("Forename", entity.vorname)
        assertFalse(entity.dirty)
    }
}
