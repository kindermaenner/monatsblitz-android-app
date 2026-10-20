package de.kindermaenner.monatsblitz.infrastructure.api

import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerBatchItemDto
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
    fun `getPlayers calls api and returns players list`() = runTest {
        val players = listOf(
            PlayerDto(id = 1L, surname = "Carlsen", forename = "Magnus"),
            PlayerDto(id = 2L, surname = "Nakamura", forename = "Hikaru")
        )
        coEvery { api.getPlayers() } returns players

        val result = dataSource.getPlayers()

        assertEquals(players, result)
        coVerify { api.getPlayers() }
    }

    @Test
    fun `createPlayer calls api and returns created player`() = runTest {
        val newPlayer = NewPlayerDto(forename = "Magnus", surname = "Carlsen")
        val createdPlayer = PlayerBatchItemDto(success = true, playerId = 1L)
        coEvery { api.createPlayer(newPlayer) } returns createdPlayer

        val result = dataSource.createPlayer(newPlayer)

        assertEquals(createdPlayer, result)
        coVerify { api.createPlayer(newPlayer) }
    }

    @Test(expected = Exception::class)
    fun `getPlayers should throw exception if api fails`() = runTest {
        coEvery { api.getPlayers() } throws Exception("API Error")
        dataSource.getPlayers()
    }
}
