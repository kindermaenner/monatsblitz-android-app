package de.kindermaenner.monatsblitz.data.model

data class Player(val id: Int, val Name : String, val Vorname : String) {
    val fullName: String
        get() = "$Vorname $Name"
}

