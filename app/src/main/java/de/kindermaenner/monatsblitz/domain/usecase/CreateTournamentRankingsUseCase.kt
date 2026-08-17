package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.PlayerRankingEntry
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toEntity

class CreateTournamentRankingsUseCase(
    val tournamentPlayerDao: TournamentPlayerDao,
    val calculatePlayerPointsUseCase: CalculatePlayerPointsUseCase = CalculatePlayerPointsUseCase()
) {
    suspend operator fun invoke(tournament : Tournament) {
        val playerResult = mutableListOf<PlayerRankingEntry>()
        tournament.players.forEach { player ->
            val points = calculatePlayerPointsUseCase(tournament, player.id)
            playerResult.add(PlayerRankingEntry(playerId = player.id, tournamentId = tournament.Id, points = points))
        }
        val playerRanks = createDenseRanking(playerResult)

        val entities = playerRanks.map { rankEntry ->  rankEntry.toEntity()}
        tournamentPlayerDao.upsertAll(entities.toList())
    }

    internal fun createDenseRanking(rankings: List<PlayerRankingEntry>): List<PlayerRankingEntry> {
        val sortedRankings = rankings.sortedByDescending { it.points }

        val result = mutableListOf<PlayerRankingEntry>()
        var lastPoints: Double? = null
        var lastRank = 0

        for (p in sortedRankings) {
            val rank = if (lastPoints != null && p.points == lastPoints) {
                lastRank
            } else {
                lastRank + 1
            }

            result += PlayerRankingEntry(
                playerId = p.playerId,
                tournamentId = p.tournamentId,
                points = p.points,
                rank = rank
            )

            lastPoints = p.points
            lastRank = rank
        }

        return result
    }

    fun createCompetitionHandling(rankings: List<PlayerRankingEntry>): List<PlayerRankingEntry> {
        val sortedRankings = rankings.sortedByDescending { it.points }

        val result = mutableListOf<PlayerRankingEntry>()
        var lastPoints: Double? = null
        var lastRank = 0
        var itemsProcessed = 0

        for (p in sortedRankings) {
            itemsProcessed++

            val rank = if (lastPoints != null && p.points == lastPoints) {
                lastRank
            } else {
                itemsProcessed
            }

            result += PlayerRankingEntry(
                playerId = p.playerId,
                tournamentId = p.tournamentId,
                points = p.points,
                rank = rank
            )

            lastPoints = p.points
            lastRank = rank
        }

        return result
    }
}
