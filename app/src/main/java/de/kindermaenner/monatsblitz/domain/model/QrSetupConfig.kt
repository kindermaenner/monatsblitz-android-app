package de.kindermaenner.monatsblitz.domain.model

data class QrSetupConfig(
    val serverUrl: String,
    val apiKey: String,
    val modePreselectionType: ModePreselectionType? = null,
    val fixedMode: String? = null,
    val rotationModes: List<String>? = null
)
