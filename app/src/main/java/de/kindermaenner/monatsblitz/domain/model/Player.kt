package de.kindermaenner.monatsblitz.domain.model

data class Player(
    val id: Long,
    val name: String,
    val vorname: String,
    val dirty: Boolean = false
) {
    val fullName: String
        get() = "$vorname $name"
}

