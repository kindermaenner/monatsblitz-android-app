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
