package de.kindermaenner.monatsblitz.infrastructure.api

import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateModeResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ModeDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewModeDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ModeRemoteDataSourceTest {

    private val api = mockk<MonatsblitzApi>()
    private val dataSource = ModeRemoteDataSource(api)

    @Test
    fun `getModes calls api and returns modes list`() = runTest {
        val modes = listOf(
            ModeDto(id = 1L, name = "3+2"),
            ModeDto(id = 2L, name = "5+0")
        )
        coEvery { api.getModes() } returns modes

        val result = dataSource.getModes()

        assertEquals(modes, result)
        coVerify { api.getModes() }
    }

    @Test
    fun `createMode calls api and returns created mode`() = runTest {
        val newMode = NewModeDto(name = "10+0")
        val createdMode = CreateModeResponseDto(success = true, id = 10L, name = "10+0")
        coEvery { api.createMode(newMode) } returns createdMode

        val result = dataSource.createMode(newMode)

        assertEquals(createdMode, result)
        coVerify { api.createMode(newMode) }
    }
}
