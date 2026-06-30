package de.sgkoenigslutter.monatsblitz.infrastructure.api.impl

import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Player
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import de.sgkoenigslutter.monatsblitz.infrastructure.RemoteApiDataSource
import de.sgkoenigslutter.monatsblitz.infrastructure.api.MonatsblitzApi

class RemoteApiDataSourceImpl(private val api: MonatsblitzApi) : RemoteApiDataSource {

    override fun fetchPlayers(): List<Player> {
        return try {
            // Note: Using blocking call - in production, use coroutines
            val dtos = api.getPlayers()
            dtos.map { dto ->
                Player(
                    id = dto.Id,
                    Name = dto.Name,
                    Vorname = dto.Vorname
                )
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch players from API", e)
        }
    }

    override fun createTournament(
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
