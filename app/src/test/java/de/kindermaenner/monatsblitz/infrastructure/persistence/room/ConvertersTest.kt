package de.kindermaenner.monatsblitz.infrastructure.persistence.room

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `LocalDate conversion works both ways`() {
        val date = LocalDate.of(2026, 8, 9)
        val string = "2026-08-09"

        assertEquals(string, converters.fromLocalDate(date))
        assertEquals(date, converters.toLocalDate(string))
    }

    @Test
    fun `GameMode conversion works both ways`() {
        val mode = GameMode.BLITZ_3_2
        val string = "3+2"

        assertEquals(string, converters.fromGameMode(mode))
        assertEquals(mode, converters.toGameMode(string))
    }

    @Test
    fun `GameMode conversion supports legacy enum strings`() {
        assertEquals(GameMode.BLITZ_3_2, converters.toGameMode("BLITZ_3_2"))
        assertEquals(GameMode.BLITZ_5_0, converters.toGameMode("BLITZ_5_0"))
        assertEquals(GameMode.HANDICAP, converters.toGameMode("HANDICAP"))
    }

    @Test
    fun `GameMode conversion supports dynamic custom modes`() {
        val customMode = GameMode(name = "10+0")
        assertEquals("10+0", converters.fromGameMode(customMode))
        assertEquals(customMode, converters.toGameMode("10+0"))
    }

    @Test
    fun `GameResult conversion works both ways`() {
        val result = GameResult.Remis
        val string = "Remis"

        assertEquals(string, converters.fromGameResult(result))
        assertEquals(result, converters.toGameResult(string))
    }
}
