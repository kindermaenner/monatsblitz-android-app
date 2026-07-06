package de.kindermaenner.monatsblitz.infrastructure

import android.util.Log
import de.kindermaenner.monatsblitz.BuildConfig
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.api.client.RetrofitClient
import de.kindermaenner.monatsblitz.infrastructure.api.impl.RemoteApiDataSourceImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

/**
 * Local repository cache in front of the remote API layer.
 * Remote calls are routed through this repository and cached in-memory.
 */
class DefaultMonatsblitzRepository(val storage: TournamentStorage) : MonatsblitzRepository {

    private val remoteApi: RemoteApiDataSource by lazy {
        val api = RetrofitClient.createApi(BuildConfig.API_KEY)
        RemoteApiDataSourceImpl(api)
    }

    private var playersCache: List<Player>? = null
    private var currentTournament_ : Tournament? = null

    public override val currentTournament: Tournament?
        get() {
            return currentTournament_
        }

    override suspend fun init() {
        val state = storage.getTournamentState().firstOrNull()
        Log.i("MonatsblitzRepository", "Initializing repository. Saved tournament state: $state")
        if (state == null) {
            Log.i("MonatsblitzRepository", "No saved tournament state found.")
            return
        }
        Log.i("MonatsblitzRepository","getting players for tournament ${state.tournamentId} with player IDs: ${state.playerIds}")
        getPlayers(forceRefresh = true).filter { player -> state.playerIds.contains(player.id) }.let { players ->
            currentTournament_ = remoteApi.getTournamentById(state.tournamentId, players)
            Log.i("MonatsblitzRepository", "Initialized tournament: $currentTournament_")
        }
    }

    override suspend fun getPlayers(forceRefresh: Boolean): List<Player> {
        Log.i("MonatsblitzRepository", "Fetching players (forceRefresh=$forceRefresh)")
        if (playersCache == null || forceRefresh) {
            playersCache = try {
                remoteApi.fetchPlayers()
            } catch (e: Exception) {
                Log.e("MonatsblitzRepository", "Fetching players failed: ${e.message}. Falling back to fake data.", e)
                // Fallback to Fake data if API fails
                FakeRemoteApiDataSource().fetchPlayers()
            }
        }

        return playersCache.orEmpty()
    }

    override suspend fun createTournament(
        playerIds: List<Int>,
        mode: GameMode,
        doubleRound: Boolean
    ): Tournament? {
        Log.i("MonatsblitzRepository", "Creating tournament with players: $playerIds, mode: $mode, doubleRound: $doubleRound")
        val playersById = getPlayers().associateBy { it.id }
        val selectedPlayers = playerIds.mapNotNull { id -> playersById[id] }

        currentTournament_ = remoteApi.createTournament(
            players = selectedPlayers,
            mode = mode,
            doubleRound = doubleRound
        )

        currentTournament_?.let { tournament ->
            storage.saveTournamentState(
                tournamentId = tournament.Id,
                playerIds = selectedPlayers.map { it.id },
                finalized = false
            )
        }

        return currentTournament_
    }

    override suspend fun finalizeTournament() {
        currentTournament_?.let { _ ->
            storage.finalizeTournament()
        }
    }
    override fun getTournamentState(): Flow<TournamentState?> {
        return storage.getTournamentState()
    }
}

interface RemoteApiDataSource {
    suspend fun fetchPlayers(): List<Player>

    suspend fun createTournament(
        players: List<Player>,
        mode: GameMode,
        doubleRound: Boolean
    ): Tournament

    suspend fun getTournamentById(tournamentId: Int, players: List<Player>): Tournament?
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

    override suspend fun fetchPlayers(): List<Player> = players

    override suspend fun createTournament(
        players: List<Player>,
        mode: GameMode,
        doubleRound: Boolean
    ): Tournament {
        val tournament = Tournament(
            Id = nextTournamentId,
            Mode = mode,
            Date = LocalDate.now(),
            players = players,
            doubleRound = doubleRound
        )

        nextTournamentId += 1
        return tournament
    }

    override suspend fun getTournamentById(tournamentId: Int, players: List<Player>): Tournament? {
        // For the fake implementation, we can return a dummy tournament or null
        return null
    }
}
