package de.kindermaenner.monatsblitz.domain.model

data class Player(
    val id: Long,
    val Name : String,
    val Vorname : String,
    val displaySuffix: String? = null
) {
    val fullName: String
        get() = if (displaySuffix != null) "$Vorname $Name $displaySuffix" else "$Vorname $Name"
}

