package de.kindermaenner.monatsblitz.domain.model

enum class PlayerWithdrawRule {
    YOUTH_DRAW,   // Ausstehende Partien als kampfloses Remis ("=") werten (0.5 Pkt. für beide)
    REGULAR_LOSS  // Ausstehende Partien als kampflos verloren ("-") werten (Gegner erhält "+")
}
