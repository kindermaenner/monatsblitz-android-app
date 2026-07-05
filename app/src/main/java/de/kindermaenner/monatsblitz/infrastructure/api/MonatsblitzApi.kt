package de.kindermaenner.monatsblitz.infrastructure.api

import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateTournamentResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewTournamentDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto
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

    @POST("tournament")
    suspend fun createTournament(
        @Body request: NewTournamentDto
    ): CreateTournamentResponseDto

    @GET(value = "/games/{tournament_id}")
    suspend fun getGames(
        @Path("tournament_id") tournamentId: Int
    ): List<GameDto>

    @POST(value = "/game")
    suspend fun createGame(
        @Body request: GameDto
    ): GameDto


}
