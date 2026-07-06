package de.kindermaenner.monatsblitz.domain.model

enum class GameResult(val displayName: String) {
    Open(" "),
    Loss("0"),
    Win("1"),
    Remis("1/2"),
    ForfeitWin("+"),
    ForfeitLoss("-");

    companion object {
        fun fromDisplayName(name: String): GameResult? {
            return GameResult.entries.find { it.displayName == name }
        }
    }
}