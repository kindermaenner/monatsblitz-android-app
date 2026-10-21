package de.kindermaenner.monatsblitz.domain.model

data class GameMode(
    val name: String,
    val id: Long = 0,
    val remoteId: Long? = null,
    val dirty: Boolean = false
) {
    val displayName: String get() = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameMode) return false
        return name.equals(other.name, ignoreCase = true)
    }

    override fun hashCode(): Int = name.lowercase().hashCode()

    override fun toString(): String = name

    companion object {
        val BLITZ_3_2 = GameMode(name = "3+2")
        val BLITZ_5_0 = GameMode(name = "5+0")
        val HANDICAP = GameMode(name = "Handicap")

        val DEFAULT_MODES = emptyList<GameMode>()

        fun fromDisplayName(name: String): GameMode? {
            if (name.isBlank()) return null
            return when (name.trim()) {
                "BLITZ_3_2", "3+2" -> BLITZ_3_2
                "BLITZ_5_0", "5+0" -> BLITZ_5_0
                "HANDICAP", "Handicap" -> HANDICAP
                else -> null
            }
        }

        fun fromString(name: String): GameMode {
            return fromDisplayName(name) ?: GameMode(name = name.trim())
        }
    }
}
