package de.kindermaenner.monatsblitz.domain.model

data class Player(val id: Long, val Name : String, val Vorname : String) {
    val fullName: String
        get() = "$Vorname $Name"
}

