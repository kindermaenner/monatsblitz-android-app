package de.kindermaenner.monatsblitz.infrastructure.api

import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRemoteDataSourceTest {

    private val api = mockk<MonatsblitzApi>()
    private val dataSource = PlayerRemoteDataSource(api)

    @Test
    fun `getPlayers calls api and returns list`() = runTest {
        val players = listOf(PlayerDto(id = 1, surname = "Carlsen", forename = "Magnus"))
        coEvery { api.getPlayers() } returns players

        val result = dataSource.getPlayers()

        assertEquals(players, result)
        coVerify { api.getPlayers() }
    }

    @Test
    fun `createPlayer calls api and returns created player`() = runTest {
        val newPlayer = NewPlayerDto(forename = "Magnus", surname = "Carlsen")
        val createdPlayer = PlayerDto(id = 1, surname = "Carlsen", forename = "Magnus")
        coEvery { api.createPlayer(newPlayer) } returns createdPlayer

        val result = dataSource.createPlayer(newPlayer)

        assertEquals(createdPlayer, result)
        coVerify { api.createPlayer(newPlayer) }
    }
}
