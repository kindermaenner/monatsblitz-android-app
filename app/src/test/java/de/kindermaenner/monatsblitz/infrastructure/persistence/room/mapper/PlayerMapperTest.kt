package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerMapperTest {

    @Test
    fun `toDomain maps PlayerEntity correctly`() {
        val entity = PlayerEntity(id = 1, name = "Carlsen", vorname = "Magnus", remoteId = 100, dirty = false)
        val domain = entity.toDomain()
        
        assertEquals(1L, domain.id)
        assertEquals("Carlsen", domain.Name)
        assertEquals("Magnus", domain.Vorname)
    }

    @Test
    fun `toEntity maps Player correctly`() {
        val player = Player(id = 1, Name = "Carlsen", Vorname = "Magnus")
        val entity = player.toEntity()
        
        assertEquals(1L, entity.id)
        assertEquals("Carlsen", entity.name)
        assertEquals("Magnus", entity.vorname)
        assertTrue(entity.dirty)
    }
}
