package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateResultsDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeRequestDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ResultDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toDomain

class FinalizeTournamentUseCase(
    private val monatsblitzApi: MonatsblitzApi,
    private val tournamentDao: TournamentDao,
    private val tournamentPlayerDao: TournamentPlayerDao,
    private val playerDao: PlayerDao,
    private val tournamentStorage: TournamentStorage,
    private val syncTournamentUseCase: SyncTournamentUseCase,
    private val syncGameResultsUseCase: SyncGameResultsUseCase,
    private val syncPlayersUseCase: SyncPlayersUseCase,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase = CreateTournamentRankingsUseCase(tournamentPlayerDao)
) {
    suspend operator fun invoke(tournamentId: Long): FinalizeResponseDto {
        val initialTournament = tournamentDao.getTournament(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")

        // 1. Spieler synchronisieren, damit alle Spieler eine remoteId haben
        syncPlayersUseCase()

        // 2. Turnier remote anlegen, falls noch nicht geschehen
        if (initialTournament.tournament.remoteId == null) {
            syncTournamentUseCase()
        }

        val tournamentWithDetails = tournamentDao.getTournament(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")

        val remoteId = tournamentWithDetails.tournament.remoteId
            ?: throw IllegalStateException("Turnier konnte nicht mit dem Server synchronisiert werden")

        // 3. Spiele / Ergebnisse synchronisieren
        syncGameResultsUseCase()

        // 4. Rangliste berechnen und abspeichern
        createTournamentRankingsUseCase(tournamentWithDetails.toDomain())

        val rankings = tournamentPlayerDao.getRankingForTournament(tournamentId)
        val playerEntities = playerDao.getPlayersByIds(rankings.map { it.playerId }).associateBy { it.id }

        val resultDtos = rankings.map { entry ->
            val player = playerEntities[entry.playerId]
                ?: throw IllegalStateException("Spieler mit ID ${entry.playerId} nicht gefunden")
            val playerRemoteId = player.remoteId
                ?: throw IllegalStateException("Spieler ${player.vorname} ${player.name} hat keine Remote-ID")

            ResultDto(
                player_id = playerRemoteId.toInt(),
                points = entry.points ?: 0.0,
                rank = entry.rank ?: 0
            )
        }

        // 5. Gesamtergebnisse an Backend senden
        val createResultsResponse = monatsblitzApi.createResults(
            CreateResultsDto(
                tournament_id = remoteId.toInt(),
                results = resultDtos
            )
        )
        if (!createResultsResponse.success) {
            val errorMsg = createResultsResponse.errors?.joinToString() ?: "Ergebnisse konnten nicht übertragen werden"
            throw IllegalStateException(errorMsg)
        }

        // 6. Finalisierung auf dem Server aufrufen
        val finalizeResponse = monatsblitzApi.finalize(
            FinalizeRequestDto(tournamentId = remoteId.toInt())
        )
        if (!finalizeResponse.success) {
            throw IllegalStateException("Finalisierung auf dem Server fehlgeschlagen")
        }

        // 7. Lokal als finalisiert markieren
        tournamentStorage.finalizeTournament()

        return finalizeResponse
    }
}
