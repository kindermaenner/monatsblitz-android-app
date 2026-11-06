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

        coEvery { repository.findPlayersByNameAndRufname(vorname, name, "") } returns emptyList()
        coEvery { repository.createPlayer(NewPlayer(name, vorname, null)) } returns expectedPlayer

        val result = useCase(vorname, name)

        assertEquals(expectedPlayer, result)
        coVerify { repository.createPlayer(NewPlayer(name = name, vorname = vorname, rufname = null)) }
    }

    @Test
    fun `invoke with new name and rufname should create player successfully`() = runTest {
        val vorname = "Michael"
        val name = "Schmidt"
        val rufname = "Michi"
        val expectedPlayer = Player(1, name, vorname, rufname = rufname)

        coEvery { repository.findPlayersByNameAndRufname(vorname, name, rufname) } returns emptyList()
        coEvery { repository.createPlayer(NewPlayer(name, vorname, rufname)) } returns expectedPlayer

        val result = useCase(vorname, name, rufname = rufname)

        assertEquals(expectedPlayer, result)
        coVerify { repository.createPlayer(NewPlayer(name = name, vorname = vorname, rufname = rufname)) }
    }

    @Test
    fun `invoke with existing name and empty rufname should throw IllegalArgumentException`() = runTest {
        val vorname = "Max"
        val name = "Mustermann"
        val existing = listOf(Player(1, name, vorname))

        coEvery { repository.findPlayersByNameAndRufname(vorname, name, "") } returns existing

        try {
            useCase(vorname, name)
            fail("Expected IllegalArgumentException was not thrown")
        } catch (e: IllegalArgumentException) {
            assertEquals("Ein Spieler mit dem Namen 'Max Mustermann' existiert bereits.", e.message)
        }

        coVerify(exactly = 0) { repository.createPlayer(any()) }
    }

    @Test
    fun `invoke with existing name and same rufname should throw IllegalArgumentException with rufname info`() = runTest {
        val vorname = "Michael"
        val name = "Schmidt"
        val rufname = "Michi"
        val existing = listOf(Player(1, name, vorname, rufname = rufname))

        coEvery { repository.findPlayersByNameAndRufname(vorname, name, rufname) } returns existing

        try {
            useCase(vorname, name, rufname = rufname)
            fail("Expected IllegalArgumentException was not thrown")
        } catch (e: IllegalArgumentException) {
            assertEquals("Ein Spieler mit dem Namen 'Michael Schmidt' (Rufname: Michi) existiert bereits.", e.message)
        }

        coVerify(exactly = 0) { repository.createPlayer(any()) }
    }

    @Test
    fun `invoke with same name but different rufname succeeds`() = runTest {
        val vorname = "Michael"
        val name = "Schmidt"
        val rufname = "Mike"
        val expectedPlayer = Player(2, name, vorname, rufname = rufname)

        // Mike does not exist yet (even if Michi exists)
        coEvery { repository.findPlayersByNameAndRufname(vorname, name, rufname) } returns emptyList()
        coEvery { repository.createPlayer(NewPlayer(name, vorname, rufname)) } returns expectedPlayer

        val result = useCase(vorname, name, rufname = rufname)

        assertEquals(expectedPlayer, result)
        coVerify { repository.createPlayer(NewPlayer(name = name, vorname = vorname, rufname = rufname)) }
    }

    @Test
    fun `invoke with whitespace should trim names before checking and creating`() = runTest {
        val vorname = "  Alice  "
        val name = "  Meier  "
        val rufname = "  Ali  "
        val expectedPlayer = Player(1, "Meier", "Alice", rufname = "Ali")

        coEvery { repository.findPlayersByNameAndRufname("Alice", "Meier", "Ali") } returns emptyList()
        coEvery { repository.createPlayer(NewPlayer("Meier", "Alice", "Ali")) } returns expectedPlayer

        val result = useCase(vorname, name, rufname)

        assertEquals(expectedPlayer, result)
        coVerify { repository.createPlayer(NewPlayer(name = "Meier", vorname = "Alice", rufname = "Ali")) }
    }

    @Test
    fun `invoke with isLocal true passes flag to NewPlayer`() = runTest {
        val expected = Player(1, "Meier", "Alice", isLocal = true)
        coEvery { repository.findPlayersByNameAndRufname("Alice", "Meier", "") } returns emptyList()
        coEvery { repository.createPlayer(NewPlayer(vorname = "Alice", name = "Meier", rufname = null, isLocal = true)) } returns expected

        val result = useCase("Alice", "Meier", isLocal = true)
        assertEquals(expected, result)
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
