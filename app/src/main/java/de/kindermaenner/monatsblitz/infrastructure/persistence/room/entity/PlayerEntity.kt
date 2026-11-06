package de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [Index(value = ["vorname", "name", "rufname"])]
)
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: Long? = null,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val vorname: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE, defaultValue = "")
    val rufname: String = "",
    val dirty: Boolean = true,
    val isLocal: Boolean = false,
    val lastParticipated: String? = null
)