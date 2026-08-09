package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreateTournamentUseCaseTest {

    private val tournamentStorage = mockk<TournamentStorage>(relaxed = true)
    private val tournamentRepository = mockk<TournamentRepository>()
    private val createNewGamesUseCase = mockk<CreateNewGamesUseCase>()
    
    private val useCase = CreateTournamentUseCase(
        tournamentStorage,
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
        coVerify { tournamentRepository.createTournament(match { 
            it.Mode == mode && it.players == players && it.games == newGames 
        }) }
        coVerify { tournamentStorage.saveTournamentState(100, false) }
    }
}
