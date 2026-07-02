package de.sgkoenigslutter.monatsblitz.data.model

import java.time.LocalDate

data class Tournament(val Id: Int,
                      val Mode : GameMode,
                      val Date : LocalDate,
                      val players: List<Player>,
                      val doubleRound: Boolean,
                      val results: MutableMap<Pair<Int, Int>, GameResult> =  mutableMapOf(),
) {
    fun getResult(
        rowIndex: Int,
        columnIndex: Int
    ): String {
        return results[rowIndex to columnIndex]?.displayName?:"";
    }
}

