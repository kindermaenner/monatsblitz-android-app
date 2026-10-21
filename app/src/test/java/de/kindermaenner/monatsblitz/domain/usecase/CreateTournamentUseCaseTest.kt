package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreateTournamentUseCaseTest {

    private val tournamentRepository = mockk<TournamentRepository>()
    private val createNewGamesUseCase = mockk<CreateNewGamesUseCase>()

    private val useCase = CreateTournamentUseCase(
        tournamentRepository,
        createNewGamesUseCase
    )

    @Test
    fun `invoke calls dependencies and returns tournament`() = runTest {
        val players = listOf(Player(1, "A", "A"))
        val mode = GameMode.BLITZ_3_2
        val date = LocalDate.now()
        val rounds = 1
        val newGames = listOf(NewGame(1, 2, 1, GameResult.Open))
        val expectedTournament = Tournament(100, mode, date, rounds, players)

        every { createNewGamesUseCase(players, rounds) } returns newGames
        coEvery { tournamentRepository.createTournament(any()) } returns expectedTournament

        val result = useCase(players, mode, date, rounds)

        assertEquals(expectedTournament, result)
        coVerify {
            tournamentRepository.createTournament(match {
                it.mode == mode && it.players == players && it.games == newGames
            })
        }
    }

    @Test
    fun `invoke with 2 rounds should pass 2 to createNewGamesUseCase`() = runTest {
        val players = listOf(Player(1, "A", "A"))
        val rounds = 2

        every { createNewGamesUseCase(players, 2) } returns emptyList()
        coEvery { tournamentRepository.createTournament(any()) } returns Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 2, players)

        useCase(players, GameMode.BLITZ_3_2, LocalDate.now(), rounds)

        coVerify { createNewGamesUseCase(players, 2) }
    }

    @Test
    fun `invoke with isLocalOnly false promotes local players`() = runTest {
        val playerRepo = mockk<PlayerRepository>(relaxed = true)
        val localUseCase = CreateTournamentUseCase(tournamentRepository, createNewGamesUseCase, playerRepo)
        val p1 = Player(1, "A", "A", isLocal = true)
        val p2 = Player(2, "B", "B", isLocal = false)
        val players = listOf(p1, p2)

        every { createNewGamesUseCase(players, 1) } returns emptyList()
        coEvery { tournamentRepository.createTournament(any()) } returns Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, players)

        localUseCase(players, GameMode.BLITZ_3_2, LocalDate.now(), 1, isLocalOnly = false)

        coVerify(exactly = 1) { playerRepo.promotePlayer(1L) }
        coVerify(exactly = 0) { playerRepo.promotePlayer(2L) }
    }

    @Test
    fun `invoke with isLocalOnly true does not promote local players`() = runTest {
        val playerRepo = mockk<PlayerRepository>(relaxed = true)
        val localUseCase = CreateTournamentUseCase(tournamentRepository, createNewGamesUseCase, playerRepo)
        val p1 = Player(1, "A", "A", isLocal = true)
        val players = listOf(p1)

        every { createNewGamesUseCase(players, 1) } returns emptyList()
        coEvery { tournamentRepository.createTournament(any()) } returns Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, players)

        localUseCase(players, GameMode.BLITZ_3_2, LocalDate.now(), 1, isLocalOnly = true)

        coVerify(exactly = 0) { playerRepo.promotePlayer(any()) }
    }
}
