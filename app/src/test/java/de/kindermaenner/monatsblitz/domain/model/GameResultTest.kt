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
    }

    @Test
    fun `fromDisplayName returns correct enum or null`() {
        assertEquals(GameResult.Win, GameResult.fromDisplayName("1"))
        assertEquals(GameResult.Loss, GameResult.fromDisplayName("0"))
        assertEquals(GameResult.Remis, GameResult.fromDisplayName("1/2"))
        assertEquals(GameResult.Open, GameResult.fromDisplayName(" "))
        assertNull(GameResult.fromDisplayName("invalid"))
    }

    @Test
    fun `points returns correct value for each result`() {
        assertEquals(0.0, GameResult.Open.points, 0.0)
        assertEquals(0.0, GameResult.Loss.points, 0.0)
        assertEquals(1.0, GameResult.Win.points, 0.0)
        assertEquals(0.5, GameResult.Remis.points, 0.0)
        assertEquals(1.0, GameResult.ForfeitWin.points, 0.0)
        assertEquals(0.0, GameResult.ForfeitLoss.points, 0.0)
    }
}
