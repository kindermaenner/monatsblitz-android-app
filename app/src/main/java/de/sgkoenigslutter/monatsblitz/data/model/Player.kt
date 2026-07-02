package de.sgkoenigslutter.monatsblitz.data.model

import kotlinx.serialization.Serializable

data class Player(val id: Int, val Name : String, val Vorname : String) {
    val fullName: String
        get() = "$Vorname $Name"
}

data class NewPlayer(val Name : String, val Vorname : String )

@Serializable
data class PlayerDto(val id: Int, val surname : String, val forename : String)

