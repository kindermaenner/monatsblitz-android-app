package de.kindermaenner.monatsblitz.domain.model

data class TournamentDumpFileInfo(
    val fileName: String,
    val filePath: String,
    val tournamentId: Long,
    val timestamp: String,
    val sizeBytes: Long
)
