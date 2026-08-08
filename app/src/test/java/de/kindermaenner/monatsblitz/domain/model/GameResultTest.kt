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
}
