package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.UnknownHostException

class TestBackendConnectionUseCaseTest {

    private val api = mockk<MonatsblitzApi>()
    private val useCase = TestBackendConnectionUseCase(apiFactory = { _, _ -> api })

    @Test
    fun `returns error when url is blank`() = runTest {
        val result = useCase("   ", "key123")
        assertTrue(result is ConnectionTestResult.Error)
        assertEquals("Bitte gib eine Server-Adresse ein.", (result as ConnectionTestResult.Error).message)
    }

    @Test
    fun `returns error when apiKey is blank`() = runTest {
        val result = useCase("https://example.com", "")
        assertTrue(result is ConnectionTestResult.Error)
        assertEquals("Bitte gib einen API-Schlüssel ein.", (result as ConnectionTestResult.Error).message)
    }

    @Test
    fun `returns success with playerCount when api returns players`() = runTest {
        coEvery { api.getPlayers() } returns listOf(
            PlayerDto(id = 1, surname = "Meier", forename = "Alice"),
            PlayerDto(id = 2, surname = "Mueller", forename = "Bob")
        )

        val result = useCase("https://example.com", "key123")
        assertTrue(result is ConnectionTestResult.Success)
        assertEquals(2, (result as ConnectionTestResult.Success).playerCount)
    }

    @Test
    fun `returns error when api throws 401 HttpException`() = runTest {
        val response = Response.error<List<PlayerDto>>(401, "".toResponseBody(null))
        coEvery { api.getPlayers() } throws HttpException(response)

        val result = useCase("https://example.com", "invalid-key")
        assertTrue(result is ConnectionTestResult.Error)
        assertTrue((result as ConnectionTestResult.Error).message.contains("API-Schlüssel ungültig"))
    }

    @Test
    fun `returns error when api throws 404 HttpException`() = runTest {
        val response = Response.error<List<PlayerDto>>(404, "".toResponseBody(null))
        coEvery { api.getPlayers() } throws HttpException(response)

        val result = useCase("https://example.com", "key123")
        assertTrue(result is ConnectionTestResult.Error)
        assertTrue((result as ConnectionTestResult.Error).message.contains("404"))
    }

    @Test
    fun `returns error when api throws 426 HttpException with json message in body`() = runTest {
        val errorJson = """{"code":"rest_api_version_mismatch","message":"Plugin erwartet API-Version 2"}"""
        val response = Response.error<List<PlayerDto>>(426, errorJson.toResponseBody(null))
        coEvery { api.getPlayers() } throws HttpException(response)

        val result = useCase("https://example.com", "key123")
        assertTrue(result is ConnectionTestResult.Error)
        assertEquals("Plugin erwartet API-Version 2", (result as ConnectionTestResult.Error).message)
    }

    @Test
    fun `returns fallback error when api throws 426 HttpException with empty body`() = runTest {
        val response = Response.error<List<PlayerDto>>(426, "".toResponseBody(null))
        coEvery { api.getPlayers() } throws HttpException(response)

        val result = useCase("https://example.com", "key123")
        assertTrue(result is ConnectionTestResult.Error)
        assertTrue((result as ConnectionTestResult.Error).message.contains("Inkompatible API-Version"))
    }

    @Test
    fun `returns error when host unknown`() = runTest {
        coEvery { api.getPlayers() } throws UnknownHostException("unknown host")

        val result = useCase("https://unknown.invalid", "key123")
        assertTrue(result is ConnectionTestResult.Error)
        assertEquals("Server nicht gefunden. Bitte Adresse prüfen.", (result as ConnectionTestResult.Error).message)
    }
}
