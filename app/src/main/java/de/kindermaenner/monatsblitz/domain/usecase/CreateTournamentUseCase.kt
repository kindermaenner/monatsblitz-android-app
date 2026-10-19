package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import java.time.LocalDate

class CreateTournamentUseCase(
    private val tournamentRepository: TournamentRepository,
    private val createNewGamesUseCase: CreateNewGamesUseCase
) {
    suspend operator fun invoke(
        players: List<Player>,
        mode: GameMode,
        date: LocalDate,
        rounds: Int
    ): Tournament {
        val games = createNewGamesUseCase(players, rounds)
        val newTournament = NewTournament(
            mode = mode,
            date = date,
            rounds = rounds,
            players = players,
            games = games
        )
        return tournamentRepository.createTournament(newTournament)
    }
}