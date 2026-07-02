package de.sgkoenigslutter.monatsblitz.infrastructure.api.impl

import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Player
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import de.sgkoenigslutter.monatsblitz.infrastructure.RemoteApiDataSource
import de.sgkoenigslutter.monatsblitz.infrastructure.api.MonatsblitzApi
import de.sgkoenigslutter.monatsblitz.infrastructure.api.dto.PlayerDto
import de.sgkoenigslutter.monatsblitz.infrastructure.api.dto.toPlayer
import java.time.LocalDate
import kotlin.collections.map

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
        // Tournament creation is not yet implemented in the API
        // For now, return a local tournament object
        return Tournament(
            Id = 1,
            Mode = mode,
            Date = LocalDate.now(),
            players = players,

            doubleRound = doubleRound);
    }
}
