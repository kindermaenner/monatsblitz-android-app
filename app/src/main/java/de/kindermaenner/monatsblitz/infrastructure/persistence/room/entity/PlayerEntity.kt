package de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId : Long? = null,
    val name: String,
    val vorname: String,
    val dirty : Boolean = true
)