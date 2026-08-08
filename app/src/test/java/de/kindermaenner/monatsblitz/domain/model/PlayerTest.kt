package de.kindermaenner.monatsblitz.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTest {

    @Test
    fun `fullName returns correct combination of Vorname and Name`() {
        val player = Player(id = 1, Name = "Carlsen", Vorname = "Magnus")
        assertEquals("Magnus Carlsen", player.fullName)
    }

    @Test
    fun `fullName handles empty strings`() {
        val player = Player(id = 2, Name = "", Vorname = "")
        assertEquals(" ", player.fullName)
    }
}
