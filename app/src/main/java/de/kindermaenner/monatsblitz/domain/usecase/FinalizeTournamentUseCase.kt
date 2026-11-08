package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository

class FinalizeTournamentUseCase(
    private val tournamentRepository: TournamentRepository,
    private val syncTournamentUseCase: SyncTournamentUseCase? = null,
    private val syncGameResultsUseCase: SyncGameResultsUseCase? = null,
    private val syncPlayersUseCase: SyncPlayersUseCase? = null,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase = CreateTournamentRankingsUseCase(tournamentRepository),
    private val createTournamentDumpUseCase: CreateTournamentDumpUseCase? = null
) {
    suspend operator fun invoke(tournamentId: Long) {
        val tournament = tournamentRepository.getTournamentById(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")

        // 0. Sicherheits-Netz: Lokalen Turnier-Dump vor Finalisierung sichern
        createTournamentDumpUseCase?.invoke(tournamentId)

        if (tournament.isLocalOnly) {
            createTournamentRankingsUseCase(tournament)
            tournamentRepository.finalizeLocalTournament(tournamentId)
            return
        }

        // 1. Spieler synchronisieren, damit alle Spieler eine remoteId haben
        syncPlayersUseCase?.invoke()

        // 2. Turnier remote anlegen, falls noch nicht geschehen
        syncTournamentUseCase?.invoke()

        // 3. Spiele / Ergebnisse synchronisieren
        syncGameResultsUseCase?.invoke()

        // 3b. Pre-Finalize Guard: Sicherstellen, dass alle Partien vollständig auf dem Server synchronisiert sind
        val unsyncedCount = tournamentRepository.getUnsyncedGamesCount(tournamentId)
        if (unsyncedCount > 0) {
            syncGameResultsUseCase?.invoke()
            val remainingUnsynced = tournamentRepository.getUnsyncedGamesCount(tournamentId)
            if (remainingUnsynced > 0) {
                throw IllegalStateException("Nicht alle Partien konnten mit dem Server synchronisiert werden ($remainingUnsynced ausstehend). Finalisierung abgebrochen.")
            }
        }

        // 4. Rangliste berechnen und abspeichern
        val updatedTournament = tournamentRepository.getTournamentById(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")
        createTournamentRankingsUseCase(updatedTournament)

        // 5. Gesamtergebnisse an Backend senden, Server-Finalisierung aufrufen & lokal als finalisiert markieren
        tournamentRepository.submitResultsAndFinalize(tournamentId)
    }
}

