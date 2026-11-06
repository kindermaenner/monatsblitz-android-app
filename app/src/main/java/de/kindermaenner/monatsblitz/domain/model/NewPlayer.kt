package de.kindermaenner.monatsblitz.domain.model

data class NewPlayer(
    val name: String,
    val vorname: String,
    val rufname: String? = null,
    val isLocal: Boolean = false
)
