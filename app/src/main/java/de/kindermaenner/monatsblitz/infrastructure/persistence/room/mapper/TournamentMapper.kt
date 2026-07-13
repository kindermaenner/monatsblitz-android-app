package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails
import kotlin.Long


fun GameEntity.toDomain(): Game =
    Game(
        id = id,
        player1Id = player1Id,
        player2Id = player2Id,
        leg = leg,
        result = result
    )

fun Tournament.toEntities() : Pair<TournamentEntity, List<GameEntity>> {
    val tournamentEntity = TournamentEntity(
        id = Id,
        mode = Mode,
        date = Date,
        rounds = rounds,
        dirty = true,
        remoteId = null
    )
    val gameEntities = games.values.map {
        GameEntity(
            id = it.id,
            tournamentId = Id,
            player1Id = it.player1Id,
            player2Id = it.player2Id,
            leg = it.leg,
            result = it.result
        )
    }
    return tournamentEntity to gameEntities
}

fun TournamentEntity.toDomain(games: List<GameEntity>, players : List<Player>): Tournament =
    Tournament(
        Id =  id,
        Mode =  mode,
        Date  =  date,
        rounds = rounds,
        games = games.associateBy { it.id }
            .mapKeys { it.key }
            .mapValues { (_, gameEntity) ->
                gameEntity.toDomain()
            },
        players = players
    )

fun TournamentWithDetails.toDomain(): Tournament {
    return Tournament(
        Id = tournament.id,
        Mode = tournament.mode,
        Date = tournament.date,
        rounds = tournament.rounds,
        players = players.map { it -> it.toDomain() },
        games = games.associate { gameEntity ->
            gameEntity.id to gameEntity.toDomain()
        }
    )
}

fun NewTournament.toEntity() : TournamentEntity =
    TournamentEntity(
        mode = Mode,
        date = Date,
        rounds = rounds,
        dirty = true,
        remoteId = null
    )
fun NewGame.toEntity(tournamentId : Long) : GameEntity = GameEntity(
    tournamentId = tournamentId,
    player1Id = player1Id,
    player2Id = player2Id,
    leg = leg,
    result = result,
    dirty = true
)