package de.kindermaenner.monatsblitz.domain.model

enum class GameResult(val displayName: String) {
    Open(" "),
    Loss("0"),
    Win("1"),
    Remis("1/2"),
    ForfeitWin("+"),
    ForfeitLoss("-");

    fun opposite(): GameResult = when (this) {
        Open -> Open
        Loss -> Win
        Win -> Loss
        Remis -> Remis
        ForfeitWin -> ForfeitLoss
        ForfeitLoss -> ForfeitWin
    }

    companion object {
        fun fromDisplayName(name: String): GameResult? {
            return GameResult.entries.find { it.displayName == name }
        }
    }
}