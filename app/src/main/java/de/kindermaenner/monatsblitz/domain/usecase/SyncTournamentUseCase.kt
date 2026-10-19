package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import retrofit2.HttpException

class SyncTournamentUseCase(val monatsblitzApi: MonatsblitzApi, val tournamentDao: TournamentDao) {
    suspend operator fun invoke() {
        tournamentDao.getDirtyTournaments().forEach {
            val dto = de.kindermaenner.monatsblitz.infrastructure.api.dto.NewTournamentDto(
                date = it.tournament.date.toString(),
                mode = it.tournament.mode.displayName,
                roundCount = it.tournament.rounds,
                playerIds = it.players.mapNotNull { player -> player.remoteId }
            )
            try {
                val response = monatsblitzApi.createTournament(dto)
                if (response.success) {
                    tournamentDao.updateTournamentRemoteId(it.tournament.id, response.tournamentId)
                }
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    val existingOpenTournament = monatsblitzApi.getTournaments().firstOrNull { remote ->
                        remote.date == it.tournament.date.toString() &&
                            remote.status.equals("open", ignoreCase = true)
                    }
                    if (existingOpenTournament != null) {
                        tournamentDao.updateTournamentRemoteId(it.tournament.id, existingOpenTournament.id)
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }
        }
    }
}