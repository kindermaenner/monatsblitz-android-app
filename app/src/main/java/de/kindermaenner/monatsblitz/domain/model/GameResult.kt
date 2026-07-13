package de.kindermaenner.monatsblitz.domain.model

enum class GameResult(val displayName: String) {
    Open("offen"),
    Loss("0:1"),
    Win("1:0"),
    Remis("0.5:0.5"),
    ForfeitWin("+:-"),
    ForfeitLoss("-:+");

    companion object {
        fun fromDisplayName(name: String): GameResult? {
            return GameResult.entries.find { it.displayName == name }
        }
    }
}