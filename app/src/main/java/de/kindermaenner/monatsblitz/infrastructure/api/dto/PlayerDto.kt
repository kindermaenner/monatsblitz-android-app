package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(val id: Int, val surname: String, val forename: String)

@Serializable
data class NewPlayerDto(val forename: String, val surname: String)
