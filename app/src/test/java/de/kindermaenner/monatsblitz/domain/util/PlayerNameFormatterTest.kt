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

    @Test
    fun `formatShortName uses rufname when preferRufname is true`() {
        val p1 = Player(1, "Nguyen", "Pham Dai Thien", rufname = "Thien")
        val p2 = Player(2, "Mueller", "Bob")
        val players = listOf(p1, p2)

        assertEquals("Thien", PlayerNameFormatter.formatShortName(p1, players, preferRufname = true))
        assertEquals("Bob", PlayerNameFormatter.formatShortName(p2, players, preferRufname = true))
    }

    @Test
    fun `formatShortName ignores rufname when preferRufname is false`() {
        val p1 = Player(1, "Nguyen", "Pham Dai Thien", rufname = "Thien")
        val p2 = Player(2, "Mueller", "Bob")
        val players = listOf(p1, p2)

        assertEquals("Pham Dai Thien", PlayerNameFormatter.formatShortName(p1, players, preferRufname = false))
    }

    @Test
    fun `formatShortName disambiguates identical rufnamen with initial`() {
        val p1 = Player(1, "Mueller", "Michael", rufname = "Michi")
        val p2 = Player(2, "Schmidt", "Michael", rufname = "Michi")
        val players = listOf(p1, p2)

        assertEquals("Michi M.", PlayerNameFormatter.formatShortName(p1, players, preferRufname = true))
        assertEquals("Michi S.", PlayerNameFormatter.formatShortName(p2, players, preferRufname = true))
    }

    @Test
    fun `formatShortName falls back to base name and full surname when rufname and initial collision`() {
        val p1 = Player(1, "Mueller", "Michael", rufname = "Michi")
        val p2 = Player(2, "Meier", "Michael", rufname = "Michi")
        val players = listOf(p1, p2)

        assertEquals("Michi Mueller", PlayerNameFormatter.formatShortName(p1, players, preferRufname = true))
        assertEquals("Michi Meier", PlayerNameFormatter.formatShortName(p2, players, preferRufname = true))
    }
}
