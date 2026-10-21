package de.kindermaenner.monatsblitz.domain.model

enum class GameResult(val displayName: String) {
    Open(" "),
    Loss("0"),
    Win("1"),
    Remis("1/2"),
    ForfeitWin("+"),
    ForfeitLoss("-"),
    ForfeitDraw("=");

    fun opposite(): GameResult = when (this) {
        Open -> Open
        Loss -> Win
        Win -> Loss
        Remis -> Remis
        ForfeitWin -> ForfeitLoss
        ForfeitLoss -> ForfeitWin
        ForfeitDraw -> ForfeitDraw
    }

    val points: Double
        get() = when (this) {
            Open -> 0.0
            Loss -> 0.0
            Win -> 1.0
            Remis -> 0.5
            ForfeitWin -> 1.0
            ForfeitLoss -> 0.0
            ForfeitDraw -> 0.5
        }

    companion object {
        fun fromDisplayName(name: String): GameResult? {
            return GameResult.entries.find { it.displayName == name }
        }
    }
}