package de.sgkoenigslutter.monatsblitz.infrastructure.api

import de.sgkoenigslutter.monatsblitz.data.model.PlayerDto
import retrofit2.Call
import retrofit2.http.GET

interface MonatsblitzApi {
    @GET("players")
    suspend fun getPlayers(): List<PlayerDto>

}
