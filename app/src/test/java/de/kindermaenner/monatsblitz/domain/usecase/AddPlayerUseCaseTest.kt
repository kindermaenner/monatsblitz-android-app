package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AddPlayerUseCaseTest {

    private val repository = mockk<PlayerRepository>()
    private val useCase = AddPlayerUseCase(repository)

    @Test
    fun `invoke with new name should create player successfully`() = runTest {
        val vorname = "Alice"
        val name = "Meier"
        val expectedPlayer = Player(1, name, vorname)

        coEvery { repository.findPlayersByName(vorname, name) } returns emptyList()
        coEvery { repository.createPlayer(NewPlayer(name, vorname)) } returns expectedPlayer

        val result = useCase(vorname, name)

        assertEquals(expectedPlayer, result)
        coVerify { repository.createPlayer(NewPlayer(Name = name, Vorname = vorname)) }
    }

    @Test
    fun `invoke with existing name should throw IllegalArgumentException and not create player`() = runTest {
        val vorname = "Max"
        val name = "Mustermann"
        val existing = listOf(Player(1, name, vorname))

        coEvery { repository.findPlayersByName(vorname, name) } returns existing

        try {
            useCase(vorname, name)
            fail("Expected IllegalArgumentException was not thrown")
        } catch (e: IllegalArgumentException) {
            assertEquals("Ein Spieler mit dem Namen 'Max Mustermann' existiert bereits.", e.message)
        }

        coVerify(exactly = 0) { repository.createPlayer(any()) }
    }

    @Test
    fun `invoke with whitespace should trim names before checking and creating`() = runTest {
        val vorname = "  Alice  "
        val name = "  Meier  "
        val expectedPlayer = Player(1, "Meier", "Alice")

        coEvery { repository.findPlayersByName("Alice", "Meier") } returns emptyList()
        coEvery { repository.createPlayer(NewPlayer("Meier", "Alice")) } returns expectedPlayer

        val result = useCase(vorname, name)

        assertEquals(expectedPlayer, result)
        coVerify { repository.createPlayer(NewPlayer(Name = "Meier", Vorname = "Alice")) }
    }

    @Test
    fun `invoke with blank vorname or name should throw IllegalArgumentException`() = runTest {
        try {
            useCase("", "Meier")
            fail("Expected IllegalArgumentException for blank vorname")
        } catch (e: IllegalArgumentException) {
            assertEquals("Vorname und Nachname dürfen nicht leer sein.", e.message)
        }

        try {
            useCase("Alice", "   ")
            fail("Expected IllegalArgumentException for blank name")
        } catch (e: IllegalArgumentException) {
            assertEquals("Vorname und Nachname dürfen nicht leer sein.", e.message)
        }

        coVerify(exactly = 0) { repository.createPlayer(any()) }
    }
}
