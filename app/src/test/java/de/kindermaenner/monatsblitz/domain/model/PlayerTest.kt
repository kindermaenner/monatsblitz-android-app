package de.kindermaenner.monatsblitz.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlayerTest {

    @Test
    fun `fullName returns correct combination of Vorname and Name`() {
        val player = Player(id = 1, name = "Carlsen", vorname = "Magnus")
        assertEquals("Magnus Carlsen", player.fullName)
    }

    @Test
    fun `fullName handles empty strings`() {
        val player = Player(id = 2, name = "", vorname = "")
        assertEquals(" ", player.fullName)
    }

    @Test
    fun `isActive returns true when lastParticipated is null`() {
        val player = Player(id = 3, name = "Neu", vorname = "Spieler", lastParticipated = null)
        assertTrue(player.isActive(LocalDate.of(2025, 1, 1)))
    }

    @Test
    fun `isActive returns true when lastParticipated is on or after cutoff`() {
        val cutoff = LocalDate.of(2025, 10, 1)
        val playerOnCutoff = Player(id = 4, name = "A", vorname = "A", lastParticipated = "2025-10-01")
        val playerAfterCutoff = Player(id = 5, name = "B", vorname = "B", lastParticipated = "2026-03-15")

        assertTrue(playerOnCutoff.isActive(cutoff))
        assertTrue(playerAfterCutoff.isActive(cutoff))
    }

    @Test
    fun `isActive returns false when lastParticipated is before cutoff`() {
        val cutoff = LocalDate.of(2025, 10, 1)
        val player = Player(id = 6, name = "Alt", vorname = "Spieler", lastParticipated = "2024-05-20")

        assertFalse(player.isActive(cutoff))
    }
}
