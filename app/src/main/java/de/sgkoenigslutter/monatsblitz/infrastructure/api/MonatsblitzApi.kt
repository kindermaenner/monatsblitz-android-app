package de.sgkoenigslutter.monatsblitz.infrastructure.api

import de.sgkoenigslutter.monatsblitz.infrastructure.api.dto.NewTournamentDto
import de.sgkoenigslutter.monatsblitz.infrastructure.api.dto.PlayerDto
import de.sgkoenigslutter.monatsblitz.infrastructure.api.dto.TournamentDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MonatsblitzApi {
    @GET("players")
    suspend fun getPlayers(): List<PlayerDto>

    @GET(value = "tournaments")
    suspend fun getTournaments(): List<TournamentDto>

    @GET("tournaments/{id}")
    suspend fun getTournament(
        @Path("id") id: Int
    ): TournamentDto

    @POST("tournaments")
    suspend fun createTournament(
        @Body request: NewTournamentDto
    ): TournamentDto
}
