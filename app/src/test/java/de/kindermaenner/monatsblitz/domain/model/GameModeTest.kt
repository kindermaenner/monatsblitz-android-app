package de.kindermaenner.monatsblitz.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameModeTest {

    @Test
    fun `fromDisplayName returns correct enum or null`() {
        assertEquals(GameMode.BLITZ_3_2, GameMode.fromDisplayName("3+2"))
        assertEquals(GameMode.BLITZ_5_0, GameMode.fromDisplayName("5+0"))
        assertEquals(GameMode.HANDICAP, GameMode.fromDisplayName("Handicap"))
        assertNull(GameMode.fromDisplayName("invalid"))
        assertNull(GameMode.fromDisplayName(""))
    }

    @Test
    fun `displayName returns correct string for each mode`() {
        assertEquals("3+2", GameMode.BLITZ_3_2.displayName)
        assertEquals("5+0", GameMode.BLITZ_5_0.displayName)
        assertEquals("Handicap", GameMode.HANDICAP.displayName)
    }

    @Test
    fun `fromString maps standard names or builds custom mode with trimmed name`() {
        assertEquals(GameMode.BLITZ_3_2, GameMode.fromString("3+2"))
        assertEquals(GameMode.BLITZ_3_2, GameMode.fromString("  BLITZ_3_2  "))
        assertEquals(GameMode.BLITZ_5_0, GameMode.fromString("5+0"))
        assertEquals(GameMode.HANDICAP, GameMode.fromString("Handicap"))
        
        val custom = GameMode.fromString("  10+0 Rapid  ")
        assertEquals("10+0 Rapid", custom.name)
        assertEquals("10+0 Rapid", custom.displayName)
    }

    @Test
    fun `equality and hashCode are case-insensitive`() {
        val modeLower = GameMode(name = "blitz 3+2")
        val modeUpper = GameMode(name = "BLITZ 3+2")

        assertEquals(modeLower, modeUpper)
        assertEquals(modeLower.hashCode(), modeUpper.hashCode())
    }
}
