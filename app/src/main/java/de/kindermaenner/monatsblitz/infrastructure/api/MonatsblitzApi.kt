package de.kindermaenner.monatsblitz.infrastructure.api

import de.kindermaenner.monatsblitz.infrastructure.api.dto.BatchResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGameDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGamesDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateResultDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateResultsDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateTournamentResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateYearPageDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewTournamentDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.RecreatePostsDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ResultDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.UpdateGameDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MonatsblitzApi {

    // Player interface
    @GET("players")
    suspend fun getPlayers(): List<PlayerDto>

    @POST("player")
    suspend fun createPlayer(
        @Body request: NewPlayerDto
    ): PlayerDto

    // tournament interface

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


    @GET(value = "games/{tournament_id}")
    suspend fun getGames(
        @Path("tournament_id") tournamentId: Int
    ): List<GameDto>

    // Game Interface

    @POST(value = "game")
    suspend fun createGame(
        @Body request: CreateGameDto
    ): GameDto

    @POST(value = "game")
    suspend fun updateGame(
        @Body request: UpdateGameDto
    ): GameDto

    @POST(value = "game")
    suspend fun createGames(
        @Body request: CreateGamesDto
    ): BatchResponseDto<GameDto>

    // Result Interface

    @GET(value = "results/{tournament_id}")
    suspend fun getResults(
        @Path("tournament_id") tournamentId: Int
    ): List<ResultDto>

    @POST(value = "result")
    suspend fun createResult(
        @Body request: CreateResultDto
    ): ResultDto

    @POST(value = "result")
    suspend fun createResults(
        @Body request: CreateResultsDto
    ): BatchResponseDto<ResultDto>


    // Finalizeation Interface

    @POST(value = "finalize")
    suspend fun finalize(
        @Body request: FinalizeDto
    ): FinalizeDto

    @POST(value = "buildYearPage")
    suspend fun createYearPage(
        @Body request: CreateYearPageDto
    ): CreateYearPageDto

    @POST(value = "recreatePosts")
    suspend fun recreatePosts(): RecreatePostsDto


}