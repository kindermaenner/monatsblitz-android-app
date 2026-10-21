package de.kindermaenner.monatsblitz.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameResultTest {

    @Test
    fun `opposite returns correct inverse result`() {
        assertEquals(GameResult.Win, GameResult.Loss.opposite())
        assertEquals(GameResult.Loss, GameResult.Win.opposite())
        assertEquals(GameResult.Remis, GameResult.Remis.opposite())
        assertEquals(GameResult.Open, GameResult.Open.opposite())
        assertEquals(GameResult.ForfeitLoss, GameResult.ForfeitWin.opposite())
        assertEquals(GameResult.ForfeitWin, GameResult.ForfeitLoss.opposite())
        assertEquals(GameResult.ForfeitDraw, GameResult.ForfeitDraw.opposite())
    }

    @Test
    fun `fromDisplayName returns correct enum or null`() {
        assertEquals(GameResult.Win, GameResult.fromDisplayName("1"))
        assertEquals(GameResult.Loss, GameResult.fromDisplayName("0"))
        assertEquals(GameResult.Remis, GameResult.fromDisplayName("1/2"))
        assertEquals(GameResult.Open, GameResult.fromDisplayName(" "))
        assertEquals(GameResult.ForfeitWin, GameResult.fromDisplayName("+"))
        assertEquals(GameResult.ForfeitLoss, GameResult.fromDisplayName("-"))
        assertEquals(GameResult.ForfeitDraw, GameResult.fromDisplayName("="))
        assertNull(GameResult.fromDisplayName("invalid"))
        assertNull(GameResult.fromDisplayName(""))
    }

    @Test
    fun `displayName returns correct string for all results`() {
        assertEquals(" ", GameResult.Open.displayName)
        assertEquals("0", GameResult.Loss.displayName)
        assertEquals("1", GameResult.Win.displayName)
        assertEquals("1/2", GameResult.Remis.displayName)
        assertEquals("+", GameResult.ForfeitWin.displayName)
        assertEquals("-", GameResult.ForfeitLoss.displayName)
        assertEquals("=", GameResult.ForfeitDraw.displayName)
    }

    @Test
    fun `points returns correct value for each result`() {
        assertEquals(0.0, GameResult.Open.points, 0.0)
        assertEquals(0.0, GameResult.Loss.points, 0.0)
        assertEquals(1.0, GameResult.Win.points, 0.0)
        assertEquals(0.5, GameResult.Remis.points, 0.0)
        assertEquals(1.0, GameResult.ForfeitWin.points, 0.0)
        assertEquals(0.0, GameResult.ForfeitLoss.points, 0.0)
        assertEquals(0.5, GameResult.ForfeitDraw.points, 0.0)

        // Verify exhaustive mapping for every entry in GameResult
        val expected = mapOf(
            GameResult.Open to 0.0,
            GameResult.Loss to 0.0,
            GameResult.Win to 1.0,
            GameResult.Remis to 0.5,
            GameResult.ForfeitWin to 1.0,
            GameResult.ForfeitLoss to 0.0,
            GameResult.ForfeitDraw to 0.5
        )
        for (entry in GameResult.entries) {
            assertEquals("Points for $entry should match", expected[entry], entry.points)
        }
    }
}
