package de.kindermaenner.monatsblitz.infrastructure.api.client

import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.model.QrSetupConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLDecoder

@Serializable
private data class QrSetupJsonDto(
    val url: String? = null,
    val key: String? = null,
    @SerialName("mode_type")
    val modeType: String? = null,
    @SerialName("fixed_mode")
    val fixedMode: String? = null,
    @SerialName("rotation_modes")
    val rotationModes: List<String>? = null
)

object QrSetupParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawValue: String): QrSetupConfig? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null

        return parseJson(trimmed) ?: parseUri(trimmed)
    }

    private fun parseJson(trimmed: String): QrSetupConfig? {
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return null
        }
        return try {
            val dto = json.decodeFromString<QrSetupJsonDto>(trimmed)
            val url = dto.url?.trim().orEmpty()
            val key = dto.key?.trim().orEmpty()
            if (url.isNotEmpty() && key.isNotEmpty()) {
                val modeType = parseModeType(dto.modeType)
                val fixedMode = dto.fixedMode?.trim()?.ifEmpty { null }
                val rotationModes = dto.rotationModes
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.ifEmpty { null }

                QrSetupConfig(
                    serverUrl = url,
                    apiKey = key,
                    modePreselectionType = modeType,
                    fixedMode = fixedMode,
                    rotationModes = rotationModes
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseUri(trimmed: String): QrSetupConfig? {
        if (!trimmed.contains("?")) {
            return null
        }
        return try {
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

                QrSetupConfig(
                    serverUrl = url,
                    apiKey = key,
                    modePreselectionType = modeType,
                    fixedMode = fixedMode,
                    rotationModes = rotationModes
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
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
