package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.ModeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeDtoTest {

    @Test
    fun `toEntity maps ModeDto to ModeEntity with dirty false`() {
        val dto = ModeDto(id = 12L, name = "5+3")
        val entity = dto.toEntity()

        assertEquals(12L, entity.remoteId)
        assertEquals("5+3", entity.name)
        assertFalse(entity.dirty)
    }

    @Test
    fun `toDomain maps ModeEntity to GameMode`() {
        val entity = ModeEntity(id = 1L, remoteId = 12L, name = "5+3", dirty = false)
        val domain = entity.toDomain()

        assertEquals(1L, domain.id)
        assertEquals(12L, domain.remoteId)
        assertEquals("5+3", domain.name)
        assertFalse(domain.dirty)
    }

    @Test
    fun `toEntity maps GameMode to ModeEntity`() {
        val domain = GameMode(id = 3L, remoteId = 44L, name = "15+10", dirty = true)
        val entity = domain.toEntity()

        assertEquals(3L, entity.id)
        assertEquals(44L, entity.remoteId)
        assertEquals("15+10", entity.name)
        assertTrue(entity.dirty)
    }
}
