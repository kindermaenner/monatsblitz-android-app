package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao

class SyncTournamentUseCase(val monatsblitzApi: MonatsblitzApi, val tournamentDao: TournamentDao) {
    suspend operator fun invoke() {
        tournamentDao.getDirtyTournaments().forEach {
            val newTournament = NewTournament(
                Mode = it.tournament.mode,
                Date = it.tournament.date,
                rounds = it.tournament.rounds,
                players = listOf(),
                games = listOf()
            )
            val response = monatsblitzApi.createTournament(newTournament.toDto())
            if (response.success) {
                tournamentDao.updateTournamentRemoteId(it.tournament.id, response.tournament_id)
            }
        }
    }
}