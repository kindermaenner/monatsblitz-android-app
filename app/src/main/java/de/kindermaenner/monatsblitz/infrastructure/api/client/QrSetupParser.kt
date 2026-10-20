package de.kindermaenner.monatsblitz.infrastructure.api.client

import de.kindermaenner.monatsblitz.domain.model.QrSetupConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder

@Serializable
private data class QrSetupJsonDto(
    val url: String? = null,
    val key: String? = null
)

object QrSetupParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawValue: String): QrSetupConfig? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null

        // 1. Try parsing JSON format: {"url":"https://...","key":"..."}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val dto = json.decodeFromString<QrSetupJsonDto>(trimmed)
                val url = dto.url?.trim().orEmpty()
                val key = dto.key?.trim().orEmpty()
                if (url.isNotEmpty() && key.isNotEmpty()) {
                    return QrSetupConfig(serverUrl = url, apiKey = key)
                }
            } catch (_: Exception) {
                // Not valid JSON, continue to URI parsing
            }
        }

        // 2. Try parsing URI format: monatsblitz://setup?url=...&key=...
        if (trimmed.contains("?")) {
            try {
                val queryString = trimmed.substringAfter("?")
                val params = queryString.split("&").associate { pair ->
                    val parts = pair.split("=", limit = 2)
                    val k = URLDecoder.decode(parts[0], "UTF-8")
                    val v = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                    k to v
                }
                val url = params["url"]?.trim().orEmpty()
                val key = params["key"]?.trim().orEmpty()
                if (url.isNotEmpty() && key.isNotEmpty()) {
                    return QrSetupConfig(serverUrl = url, apiKey = key)
                }
            } catch (_: Exception) {
                // Not valid URI format
            }
        }

        return null
    }
}
