package de.sgkoenigslutter.monatsblitz.infrastructure.api.impl

import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Player
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import de.sgkoenigslutter.monatsblitz.infrastructure.RemoteApiDataSource
import de.sgkoenigslutter.monatsblitz.infrastructure.api.MonatsblitzApi

class RemoteApiDataSourceImpl(private val api: MonatsblitzApi) : RemoteApiDataSource {

    override suspend fun fetchPlayers(): List<Player> {
        return try {
            api.getPlayers().map { dto ->
                Player(
                    id = dto.id,
                    Name = dto.surname,
                    Vorname = dto.forename
                )
            }
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
            Id = System.currentTimeMillis().toInt(),
            Name = "Monatsblitz ${mode.displayName}",
            players = players,
            doubleRound = doubleRound
        )
    }
}
