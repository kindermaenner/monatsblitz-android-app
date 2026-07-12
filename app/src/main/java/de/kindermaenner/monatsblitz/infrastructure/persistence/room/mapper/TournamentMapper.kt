package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateTournamentResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewTournamentDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import java.time.LocalDate

object TournamentMapper {
    fun toDomain(entity: TournamentEntity): Tournament =
        Tournament(
            Id =  entity.id,
            Mode =  entity.mode,
            Date  =  entity.date,
            players = emptyList(),
            doubleRound = entity.doubleRound,
            games = mutableMapOf()
        );

    fun toEntity(tournament : Tournament) : TournamentEntity =
        TournamentEntity(
            id = tournament.Id,
            remoteId  = null,
            mode =  tournament.Mode,
            date = tournament.Date,
            doubleRound = tournament.doubleRound
        );

    fun toEntity(newTournament: NewTournament, remoteId : Int?) : TournamentEntity =
        TournamentEntity(
            remoteId  = remoteId,
            mode =  newTournament.Mode,
            date = newTournament.Date,
            doubleRound = newTournament.doubleRound,
            dirty = remoteId==null
        )

    fun toNewDto(entity: TournamentEntity) : NewTournamentDto =
        NewTournamentDto(
            date = entity.date.toString(),
            mode = entity.mode.displayName,
            round_count = if (entity.doubleRound) 2 else 1
        )

}