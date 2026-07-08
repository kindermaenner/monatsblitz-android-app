package de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.kindermaenner.monatsblitz.domain.model.GameMode
import java.time.LocalDate

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val mode: GameMode,

    val date: LocalDate,

    val doubleRound: Boolean,

    val dirty : Boolean = true
)