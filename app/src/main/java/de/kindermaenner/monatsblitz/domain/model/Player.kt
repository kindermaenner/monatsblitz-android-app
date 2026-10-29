package de.kindermaenner.monatsblitz.domain.model

import java.time.LocalDate

data class Player(
    val id: Long,
    val name: String,
    val vorname: String,
    val dirty: Boolean = false,
    val isLocal: Boolean = false,
    val lastParticipated: String? = null
) {
    val fullName: String
        get() = "$vorname $name"

    fun isActive(cutoffDate: LocalDate): Boolean {
        val dateStr = lastParticipated ?: return true
        val parsedDate = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return true
        return !parsedDate.isBefore(cutoffDate)
    }
}

