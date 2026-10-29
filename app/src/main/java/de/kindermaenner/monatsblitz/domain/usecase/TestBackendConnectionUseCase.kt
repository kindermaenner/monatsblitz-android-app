package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.client.RetrofitClient
import de.kindermaenner.monatsblitz.infrastructure.api.client.UrlNormalizer
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ApiErrorDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface ConnectionTestResult {
    data class Success(val playerCount: Int) : ConnectionTestResult
    data class Error(val message: String) : ConnectionTestResult
}

class TestBackendConnectionUseCase(
    private val apiFactory: (apiKey: String, baseUrl: String) -> MonatsblitzApi = { key, url ->
        RetrofitClient.createApi(apiKey = key, baseUrl = url)
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(rawUrl: String, rawApiKey: String): ConnectionTestResult = withContext(ioDispatcher) {
        val url = rawUrl.trim()
        val apiKey = rawApiKey.trim()

        if (url.isEmpty()) {
            return@withContext ConnectionTestResult.Error("Bitte gib eine Server-Adresse ein.")
        }
        if (apiKey.isEmpty()) {
            return@withContext ConnectionTestResult.Error("Bitte gib einen API-Schlüssel ein.")
        }

        val normalizedUrl = UrlNormalizer.normalize(url)

        try {
            val api = apiFactory(apiKey, normalizedUrl)
            val players = api.getPlayers()
            ConnectionTestResult.Success(playerCount = players.size)
        } catch (e: Exception) {
            mapExceptionToError(e)
        }
    }

    private fun mapExceptionToError(e: Exception): ConnectionTestResult.Error = when (e) {
        is HttpException -> mapHttpException(e)
        is UnknownHostException -> ConnectionTestResult.Error("Server nicht gefunden. Bitte Adresse prüfen.")
        is ConnectException -> ConnectionTestResult.Error("Verbindung fehlgeschlagen. Bitte Server-Adresse und Netzwerk prüfen.")
        is SocketTimeoutException -> ConnectionTestResult.Error("Zeitüberschreitung bei Verbindung zum Server.")
        is IOException -> ConnectionTestResult.Error("Netzwerkfehler: ${e.message ?: "Verbindung fehlgeschlagen"}")
        else -> ConnectionTestResult.Error("Fehler: ${e.message ?: "Unerwarteter Fehler"}")
    }

    private fun mapHttpException(e: HttpException): ConnectionTestResult.Error = when (e.code()) {
        401, 403 -> ConnectionTestResult.Error("Server erreichbar, aber API-Schlüssel ungültig (${e.code()}).")
        404 -> ConnectionTestResult.Error("Server erreichbar, aber Monatsblitz-Endpunkt nicht gefunden (404). Ist das Plugin aktiviert?")
        426 -> {
            val rawBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val message = ApiErrorDto.extractMessage(rawBody)
                ?: "Inkompatible API-Version zwischen App und WordPress-Plugin. Bitte prüfe Updates für App und Plugin."
            ConnectionTestResult.Error(message)
        }
        else -> ConnectionTestResult.Error("Server meldete HTTP ${e.code()}: ${e.message()}")
    }
}
