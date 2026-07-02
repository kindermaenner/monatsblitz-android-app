package de.sgkoenigslutter.monatsblitz.infrastructure.api.dto

import de.sgkoenigslutter.monatsblitz.data.model.Player
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(val id: Int, val surname : String, val forename : String)

fun PlayerDto.toPlayer(): Player {
    return Player(
        id = this.id,
        Name = this.surname,
        Vorname = this.forename
    )
}

fun Player.toPlayerDto(): PlayerDto {
    return PlayerDto(
        id = this.id,
        surname = this.Name,
        forename = this.Vorname
    )
}