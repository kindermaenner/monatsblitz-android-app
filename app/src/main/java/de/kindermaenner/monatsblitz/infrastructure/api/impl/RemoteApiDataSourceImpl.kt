package de.kindermaenner.monatsblitz.infrastructure.api.impl

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewTournamentDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toPlayer
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toTournament
import de.kindermaenner.monatsblitz.infrastructure.RemoteApiDataSource
import java.time.LocalDate

class RemoteApiDataSourceImpl(private val api: MonatsblitzApi) : RemoteApiDataSource {

    override suspend fun fetchPlayers(): List<Player> {
        return try {
            api.getPlayers().map { dto -> dto.toPlayer()}
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch players from API", e)
        }
    }

    override suspend fun createTournament(
        players: List<Player>,
        mode: GameMode,
        doubleRound: Boolean
    ): Tournament {
        val newTournamentDto = NewTournamentDto(
            date = LocalDate.now().toString(),
            mode = mode.name,
            round_count = if (doubleRound) 2 else 1
        )
        val result = api.createTournament(newTournamentDto)
        val dto = TournamentDto(
            id = result.tournament_id,
            date = newTournamentDto.date,
            mode = mode.displayName,
            round_count = newTournamentDto.round_count
        )
        return  dto.toTournament(players)
    }

    override suspend fun getTournamentById(tournamentId: Int,  players: List<Player>): Tournament? {
        // For the fake implementation, we can return a dummy tournament or null
        return api.getTournament(tournamentId)?.toTournament(players)
    }
}
