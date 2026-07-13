package de.kindermaenner.monatsblitz.domain.repository

interface SyncPlayerRepository {
    suspend fun syncPlayers()
}