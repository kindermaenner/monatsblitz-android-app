package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateNewGamesUseCaseTest {

    private val useCase = CreateNewGamesUseCase()
    private val players = listOf(
        Player(1, "A", "A"),
        Player(2, "B", "B"),
        Player(3, "C", "C")
    )

    @Test
    fun `invoke with 1 round generates all pairings once`() {
        val games = useCase(players, 1)
        
        // n*(n-1)/2 = 3*2/2 = 3
        assertEquals(3, games.size)
        assertEquals(1, games.filter { it.player1Id == 1L && it.player2Id == 2L }.size)
        assertEquals(1, games.filter { it.player1Id == 1L && it.player2Id == 3L }.size)
        assertEquals(1, games.filter { it.player1Id == 2L && it.player2Id == 3L }.size)
        games.forEach { assertEquals(1, it.leg) }
        games.forEach { assertEquals(GameResult.Open, it.result) }
    }

    @Test
    fun `invoke with 2 rounds generates all pairings twice`() {
        val games = useCase(players, 2)
        
        assertEquals(6, games.size)
        assertEquals(1, games.filter { it.player1Id == 1L && it.player2Id == 2L && it.leg == 1 }.size)
        assertEquals(1, games.filter { it.player1Id == 1L && it.player2Id == 2L && it.leg == 2 }.size)
        games.forEach { assertEquals(GameResult.Open, it.result) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invoke with invalid rounds throws exception`() {
        useCase(players, 3)
    }
}
