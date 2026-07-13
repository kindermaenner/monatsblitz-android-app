package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import java.time.LocalDate

class CreateTournamentUseCase(
    private val tournamentStorage: TournamentStorage,
    private val tournamentRepository: TournamentRepository,
    private val createNewGamesUseCase : CreateNewGamesUseCase) {
    suspend operator fun invoke(players: List<Player>,
                                mode : GameMode,
                                date : LocalDate,
                                rounds: Int) : Tournament {
        val games = createNewGamesUseCase(players, rounds)
        val newTournament = NewTournament(
            Mode = mode,
            Date = date,
            rounds = rounds,
            players = players,
            games = games
        )
        val tournament = tournamentRepository.createTournament(newTournament)
        tournamentStorage.saveTournamentState(tournament.Id, false)
        return tournament
    }
}