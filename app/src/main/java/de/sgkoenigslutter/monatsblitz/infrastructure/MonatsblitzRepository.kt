package de.sgkoenigslutter.monatsblitz.infrastructure

import de.sgkoenigslutter.monatsblitz.BuildConfig
import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Player
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import de.sgkoenigslutter.monatsblitz.infrastructure.api.RetrofitClient
import de.sgkoenigslutter.monatsblitz.infrastructure.api.impl.RemoteApiDataSourceImpl

/**
 * Local repository cache in front of the remote API layer.
 * Remote calls are routed through this repository and cached in-memory.
 */
object MonatsblitzRepository {

    private val remoteApi: RemoteApiDataSource by lazy {
        val api = RetrofitClient.createApi(BuildConfig.API_KEY)
        RemoteApiDataSourceImpl(api)
    }

    private val lock = Any()
    private var playersCache: List<Player>? = null
    private val tournamentCache: MutableMap<Int, Tournament> = mutableMapOf()

    fun getPlayers(forceRefresh: Boolean = false): List<Player> = synchronized(lock) {
        if (playersCache == null || forceRefresh) {
            playersCache = try {
                remoteApi.fetchPlayers()
            } catch (e: Exception) {
                // Fallback to Fake data if API fails
                FakeRemoteApiDataSource().fetchPlayers()
            }
        }

        playersCache.orEmpty()
    }

    fun createTournament(
        playerIds: List<Int>,
        mode: GameMode,
        doubleRound: Boolean
    ): Tournament = synchronized(lock) {
        val playersById = getPlayers().associateBy { it.id }
        val selectedPlayers = playerIds.mapNotNull { id -> playersById[id] }

        val tournament = remoteApi.createTournament(
            players = selectedPlayers,
            mode = mode,
            doubleRound = doubleRound
        )

        tournamentCache[tournament.Id] = tournament
        tournament
    }

    fun getTournament(tournamentId: Int): Tournament? = synchronized(lock) {
        tournamentCache[tournamentId]
    }
}

interface RemoteApiDataSource {
    fun fetchPlayers(): List<Player>

    fun createTournament(
        players: List<Player>,
        mode: GameMode,
        doubleRound: Boolean
    ): Tournament
}

class FakeRemoteApiDataSource : RemoteApiDataSource {

    private val players = listOf(
        Player(1, "Max", "Muller"),
        Player(2, "Peter", "Meier"),
        Player(3, "Hans", "Schulz"),
        Player(4, "Klaus", "Bertram"),
        Player(5, "Fiete", "Klose"),
        Player(6, "Bert", "Brot")
    )

    private var nextTournamentId = 1

    override fun fetchPlayers(): List<Player> = players

    override fun createTournament(
        players: List<Player>,
        mode: GameMode,
        doubleRound: Boolean
    ): Tournament {
        val tournament = Tournament(
            Id = nextTournamentId,
            Name = "Monatsblitz ${mode.displayName}",
            players = players,
            doubleRound = doubleRound
        )

        nextTournamentId += 1
        return tournament
    }
}