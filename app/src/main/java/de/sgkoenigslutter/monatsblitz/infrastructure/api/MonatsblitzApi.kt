package de.sgkoenigslutter.monatsblitz.infrastructure.api

import de.sgkoenigslutter.monatsblitz.data.model.PlayerDto
import retrofit2.http.GET

interface MonatsblitzApi {
    @GET("players")
    fun getPlayers(): List<PlayerDto>
}
