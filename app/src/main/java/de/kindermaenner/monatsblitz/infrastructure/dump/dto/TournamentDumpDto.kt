package de.kindermaenner.monatsblitz.infrastructure.dump.dto

import kotlinx.serialization.Serializable

@Serializable
data class TournamentDumpDto(
    val dumpedAt: String,
    val tournamentId: Long,
    val mode: String,
    val date: String,
    val rounds: Int,
    val remoteId: Long?,
    val isLocalOnly: Boolean,
    val countsForRanking: Boolean,
    val label: String,
    val players: List<DumpPlayerDto>,
    val games: List<DumpGameDto>
)

@Serializable
data class DumpPlayerDto(
    val localId: Long,
    val remoteId: Long?,
    val vorname: String,
    val name: String,
    val rufname: String?,
    val hasLeft: Boolean,
    val points: Double?,
    val rank: Int?
)

@Serializable
data class DumpGameDto(
    val localGameId: Long,
    val player1Id: Long,
    val player2Id: Long,
    val leg: Int,
    val result: String,
    val remoteGameId: Long?,
    val dirty: Boolean
)
