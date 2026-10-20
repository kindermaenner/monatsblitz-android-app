package de.kindermaenner.monatsblitz.domain.util

import de.kindermaenner.monatsblitz.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerNameFormatterTest {

    @Test
    fun `formatShortName returns only vorname when unique in tournament`() {
        val p1 = Player(1, "Meier", "Alice")
        val p2 = Player(2, "Mueller", "Bob")
        val players = listOf(p1, p2)

        assertEquals("Alice", PlayerNameFormatter.formatShortName(p1, players))
        assertEquals("Bob", PlayerNameFormatter.formatShortName(p2, players))
    }

    @Test
    fun `formatShortName includes initial when same vorname but different initial`() {
        val p1 = Player(1, "Mueller", "Peter")
        val p2 = Player(2, "Schmidt", "Peter")
        val p3 = Player(3, "Weber", "Bob")
        val players = listOf(p1, p2, p3)

        assertEquals("Peter M.", PlayerNameFormatter.formatShortName(p1, players))
        assertEquals("Peter S.", PlayerNameFormatter.formatShortName(p2, players))
        assertEquals("Bob", PlayerNameFormatter.formatShortName(p3, players))
    }

    @Test
    fun `formatShortName returns full name when same vorname and same initial`() {
        val p1 = Player(1, "Mueller", "Peter")
        val p2 = Player(2, "Meier", "Peter")
        val p3 = Player(3, "Schmidt", "Peter")
        val players = listOf(p1, p2, p3)

        assertEquals("Peter Mueller", PlayerNameFormatter.formatShortName(p1, players))
        assertEquals("Peter Meier", PlayerNameFormatter.formatShortName(p2, players))
        assertEquals("Peter S.", PlayerNameFormatter.formatShortName(p3, players))
    }

    @Test
    fun `formatShortName handles empty vorname or empty name gracefully`() {
        val p1 = Player(1, "Carlsen", "")
        val p2 = Player(2, "", "Magnus")
        val p3 = Player(3, "", "")
        val players = listOf(p1, p2, p3)

        assertEquals("Carlsen", PlayerNameFormatter.formatShortName(p1, players))
        assertEquals("Magnus", PlayerNameFormatter.formatShortName(p2, players))
        assertEquals("Unbekannt", PlayerNameFormatter.formatShortName(p3, players))
    }

    @Test
    fun `formatDisplayName respects useShortName flag`() {
        val p1 = Player(1, "Mueller", "Peter")
        val p2 = Player(2, "Schmidt", "Peter")
        val players = listOf(p1, p2)

        assertEquals("Peter M.", PlayerNameFormatter.formatDisplayName(p1, players, useShortName = true))
        assertEquals("Peter Mueller", PlayerNameFormatter.formatDisplayName(p1, players, useShortName = false))
    }
}
