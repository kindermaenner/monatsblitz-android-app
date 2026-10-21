package de.kindermaenner.monatsblitz.domain.repository

import de.kindermaenner.monatsblitz.domain.model.GameMode
import kotlinx.coroutines.flow.Flow

interface ModeRepository {
    fun observeModes(): Flow<List<GameMode>>
    suspend fun getAllModes(): List<GameMode>
    suspend fun createMode(name: String, permanent: Boolean): GameMode
    suspend fun syncModes()
}
