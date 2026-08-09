package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.GameResult
import org.junit.Assert.assertEquals
import org.junit.Test

class MappingUtilsTest {

    @Test
    fun `toGameResult maps API strings correctly`() {
        assertEquals(GameResult.Win, "1:0".toGameResult())
        assertEquals(GameResult.Loss, "0:1".toGameResult())
        assertEquals(GameResult.Remis, "0.5:0.5".toGameResult())
        assertEquals(GameResult.Remis, "0,5:0,5".toGameResult()) // Test comma replacement
        assertEquals(GameResult.ForfeitWin, "+:-".toGameResult())
        assertEquals(GameResult.ForfeitLoss, "-:+".toGameResult())
        assertEquals(GameResult.Open, "offen".toGameResult())
        assertEquals(GameResult.Open, "invalid".toGameResult())
    }

    @Test
    fun `toResultString maps enum to API string correctly`() {
        assertEquals("1:0", GameResult.Win.toResultString())
        assertEquals("0:1", GameResult.Loss.toResultString())
        assertEquals("0.5:0.5", GameResult.Remis.toResultString())
        assertEquals("+:-", GameResult.ForfeitWin.toResultString())
        assertEquals("-:+", GameResult.ForfeitLoss.toResultString())
        assertEquals("offen", GameResult.Open.toResultString())
    }
}
