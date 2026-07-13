package de.kindermaenner.monatsblitz.infrastructure.persistence.room

import androidx.room.TypeConverter
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import java.time.LocalDate

class Converters {

    // -------- LocalDate --------
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String =
        date.toString() // ISO-8601

    @TypeConverter
    fun toLocalDate(value: String): LocalDate =
        LocalDate.parse(value)


    // -------- GameMode --------
    @TypeConverter
    fun fromGameMode(mode: GameMode): String =
        mode.name

    @TypeConverter
    fun toGameMode(value: String): GameMode =
        GameMode.valueOf(value)

    // -------- GameResult --------
    @TypeConverter
    fun fromGameResult(result: GameResult): String =
        result.name

    @TypeConverter
    fun toGameResult(value: String): GameResult =
        GameResult.valueOf(value)
}
