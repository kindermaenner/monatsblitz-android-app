package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerRankingEntry
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails


fun GameEntity.toDomain(): Game =
    Game(
        id = id,
        player1Id = player1Id,
        player2Id = player2Id,
        leg = leg,
        result = result,
        dirty = dirty
    )

fun Tournament.toEntities() : Pair<TournamentEntity, List<GameEntity>> {
    val tournamentEntity = TournamentEntity(
        id = id,
        mode = mode,
        date = date,
        rounds = rounds,
        dirty = true,
        remoteId = null
    )
    val gameEntities = games.values.map {
        GameEntity(
            id = it.id,
            tournamentId = id,
            player1Id = it.player1Id,
            player2Id = it.player2Id,
            leg = it.leg,
            result = it.result,
            dirty = it.dirty
        )
    }
    return tournamentEntity to gameEntities
}

fun TournamentEntity.toDomain(games: List<GameEntity>, players : List<Player>): Tournament =
    Tournament(
        id = id,
        mode = mode,
        date = date,
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
        id = tournament.id,
        mode = tournament.mode,
        date = tournament.date,
        rounds = tournament.rounds,
        players = players.map { it.toDomain() },
        games = games.associate { gameEntity ->
            gameEntity.id to gameEntity.toDomain()
        },
        withdrawnPlayerIds = playerRefs.filter { it.hasLeft }.map { it.playerId }.toSet(),
        isLocalOnly = tournament.isLocalOnly
    )
}

fun NewTournament.toEntity() : TournamentEntity =
    TournamentEntity(
        mode = mode,
        date = date,
        rounds = rounds,
        dirty = !isLocalOnly,
        remoteId = null,
        isLocalOnly = isLocalOnly
    )
fun NewGame.toEntity(tournamentId : Long) : GameEntity = GameEntity(
    tournamentId = tournamentId,
    player1Id = player1Id,
    player2Id = player2Id,
    leg = leg,
    result = result,
    dirty = result != de.kindermaenner.monatsblitz.domain.model.GameResult.Open
)

fun PlayerRankingEntry.toEntity() : TournamentPlayerCrossRef =
    TournamentPlayerCrossRef(
        tournamentId = tournamentId,
        playerId = playerId,
        points = points,
        rank = rank
    )

fun TournamentPlayerCrossRef.toDomain() : PlayerRankingEntry {
    val nonNullRank = requireNotNull(rank) { "Points and rank must not be null for PlayerRankingEntry" }
    return PlayerRankingEntry(
        tournamentId = tournamentId,
        playerId = playerId,
        points = points ?: 0.0,
        rank = nonNullRank
    )
}