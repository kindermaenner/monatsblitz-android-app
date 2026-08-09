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
        val string = "BLITZ_3_2"
        
        assertEquals(string, converters.fromGameMode(mode))
        assertEquals(mode, converters.toGameMode(string))
    }

    @Test
    fun `GameResult conversion works both ways`() {
        val result = GameResult.Remis
        val string = "Remis"
        
        assertEquals(string, converters.fromGameResult(result))
        assertEquals(result, converters.toGameResult(string))
    }
}
