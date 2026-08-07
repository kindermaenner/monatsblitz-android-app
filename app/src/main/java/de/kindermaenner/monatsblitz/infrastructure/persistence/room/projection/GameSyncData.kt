package de.kindermaenner.monatsblitz.infrastructure.persistence.room.projection

import de.kindermaenner.monatsblitz.domain.model.GameResult

data class GameSyncData(val localGameId: Long,
                        val tournamentRemoteId: Int,
                        val player1Id: Long,
                        val player2Id: Long,
                        val leg: Int,
                        val result: GameResult
)
