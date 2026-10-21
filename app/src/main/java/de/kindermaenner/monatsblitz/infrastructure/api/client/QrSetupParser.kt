package de.kindermaenner.monatsblitz.infrastructure.api.client

import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.model.QrSetupConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder

@Serializable
private data class QrSetupJsonDto(
    val url: String? = null,
    val key: String? = null,
    val mode_type: String? = null,
    val fixed_mode: String? = null,
    val rotation_modes: List<String>? = null
)

object QrSetupParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawValue: String): QrSetupConfig? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null

        // 1. Try parsing JSON format: {"url":"https://...","key":"...","mode_type":"...","fixed_mode":"...","rotation_modes":[...]}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val dto = json.decodeFromString<QrSetupJsonDto>(trimmed)
                val url = dto.url?.trim().orEmpty()
                val key = dto.key?.trim().orEmpty()
                if (url.isNotEmpty() && key.isNotEmpty()) {
                    val modeType = parseModeType(dto.mode_type)
                    val fixedMode = dto.fixed_mode?.trim()?.ifEmpty { null }
                    val rotationModes = dto.rotation_modes
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.ifEmpty { null }

                    return QrSetupConfig(
                        serverUrl = url,
                        apiKey = key,
                        modePreselectionType = modeType,
                        fixedMode = fixedMode,
                        rotationModes = rotationModes
                    )
                }
            } catch (_: Exception) {
                // Not valid JSON, continue to URI parsing
            }
        }

        // 2. Try parsing URI format: monatsblitz://setup?url=...&key=...&mode_type=...
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
                    val modeType = parseModeType(params["mode_type"])
                    val fixedMode = params["fixed_mode"]?.trim()?.ifEmpty { null }
                    val rotationModes = params["rotation_modes"]
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.ifEmpty { null }

                    return QrSetupConfig(
                        serverUrl = url,
                        apiKey = key,
                        modePreselectionType = modeType,
                        fixedMode = fixedMode,
                        rotationModes = rotationModes
                    )
                }
            } catch (_: Exception) {
                // Not valid URI format
            }
        }

        return null
    }

    private fun parseModeType(raw: String?): ModePreselectionType? {
        return when (raw?.trim()?.lowercase()) {
            "fixed" -> ModePreselectionType.FIXED
            "rotation" -> ModePreselectionType.ROTATION
            "none" -> ModePreselectionType.NONE
            else -> null
        }
    }
}
