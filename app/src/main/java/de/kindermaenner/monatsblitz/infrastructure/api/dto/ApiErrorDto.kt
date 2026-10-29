package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ApiErrorDto(
    val code: String? = null,
    val message: String? = null
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun extractMessage(rawJson: String?): String? {
            if (rawJson.isNullOrBlank()) return null
            return try {
                json.decodeFromString<ApiErrorDto>(rawJson).message?.trim()?.takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        }
    }
}

fun Throwable.toDetailedErrorMessage(): String {
    if (this is retrofit2.HttpException) {
        val rawBody = try { response()?.errorBody()?.string() } catch (_: Exception) { null }
        val pluginMessage = ApiErrorDto.extractMessage(rawBody)
        val httpInfo = "HTTP ${code()}" + (message().takeIf { it.isNotBlank() }?.let { " $it" } ?: "")
        return if (!pluginMessage.isNullOrBlank()) {
            "$httpInfo: $pluginMessage"
        } else if (!rawBody.isNullOrBlank()) {
            "$httpInfo: $rawBody"
        } else {
            httpInfo
        }
    }
    return message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName
}
