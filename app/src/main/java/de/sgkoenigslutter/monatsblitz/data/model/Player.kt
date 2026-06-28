package de.sgkoenigslutter.monatsblitz.data.model

data class Player(val id: Int, val Name : String, val Vorname : String) {
    val fullName: String
        get() = "$Vorname $Name"
}

data class NewPlayer(val Name : String, val Vorname : String )

data class PlayerDto(val Id: Int, val Name : String, val Vorname : String)

