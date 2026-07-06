package de.kindermaenner.monatsblitz.domain.model

enum class GameMode(
    val displayName: String
) {
    BLITZ_3_2("3+2"),
    BLITZ_5_0("5+0"),
    HANDICAP("Handicap");
    companion object {
        fun fromDisplayName(name: String): GameMode? {
            return entries.find { it.displayName == name }
        }
    }
}
