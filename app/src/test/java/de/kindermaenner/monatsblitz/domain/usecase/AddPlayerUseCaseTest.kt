package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddPlayerUseCaseTest {

    private val repository = mockk<PlayerRepository>()
    private val useCase = AddPlayerUseCase(repository)

    @Test
    fun `invoke with new name should create player without suffix`() = runTest {
        val vorname = "Alice"
        val name = "Meier"
        val expectedPlayer = Player(1, name, vorname, null)

        coEvery { repository.findPlayersByName(vorname, name) } returns emptyList()
        coEvery { repository.createPlayer(any()) } returns expectedPlayer

        val result = useCase(vorname, name)

        assertEquals(expectedPlayer, result)
        coVerify { repository.createPlayer(match { it.displaySuffix == null }) }
    }

    @Test
    fun `invoke with existing name should create player with suffix`() = runTest {
        val vorname = "Max"
        val name = "Mustermann"
        val existing = listOf(Player(1, name, vorname, null))
        val expectedPlayer = Player(2, name, vorname, "(2)")

        coEvery { repository.findPlayersByName(vorname, name) } returns existing
        coEvery { repository.createPlayer(any()) } returns expectedPlayer

        val result = useCase(vorname, name)

        assertEquals(expectedPlayer, result)
        coVerify { repository.createPlayer(match { it.displaySuffix == "(2)" }) }
    }

    @Test
    fun `invoke with multiple existing names should increment suffix`() = runTest {
        val vorname = "Uwe"
        val name = "Schmidt"
        val existing = listOf(
            Player(1, name, vorname, null),
            Player(2, name, vorname, "(2)")
        )
        
        coEvery { repository.findPlayersByName(vorname, name) } returns existing
        coEvery { repository.createPlayer(any()) } returns mockk()

        useCase(vorname, name)

        coVerify { repository.createPlayer(match { it.displaySuffix == "(3)" }) }
    }
}
