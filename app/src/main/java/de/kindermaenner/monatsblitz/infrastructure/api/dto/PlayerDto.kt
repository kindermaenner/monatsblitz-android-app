package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.Player
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(val id: Int, val surname: String, val forename: String)

@Serializable
data class NewPlayerDto(val forename: String, val surname: String)

fun PlayerDto.toPlayer(): Player =
    Player(id = this.id, Name = this.surname, Vorname = this.forename)

fun Player.toPlayerDto(): PlayerDto =
    PlayerDto(id = this.id, surname = this.Name, forename = this.Vorname)
