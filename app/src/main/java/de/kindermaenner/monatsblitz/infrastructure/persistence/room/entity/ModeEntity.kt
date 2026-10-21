package de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "modes",
    indices = [Index(value = ["name"], unique = true)]
)
data class ModeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: Long? = null,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    val dirty: Boolean = false
)
