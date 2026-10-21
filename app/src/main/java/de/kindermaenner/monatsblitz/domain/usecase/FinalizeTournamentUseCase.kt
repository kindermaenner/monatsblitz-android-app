package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository

class FinalizeTournamentUseCase(
    private val tournamentRepository: TournamentRepository,
    private val syncTournamentUseCase: SyncTournamentUseCase? = null,
    private val syncGameResultsUseCase: SyncGameResultsUseCase? = null,
    private val syncPlayersUseCase: SyncPlayersUseCase? = null,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase = CreateTournamentRankingsUseCase(tournamentRepository)
) {
    suspend operator fun invoke(tournamentId: Long) {
        val tournament = tournamentRepository.getTournamentById(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")

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

        // 4. Rangliste berechnen und abspeichern
        val updatedTournament = tournamentRepository.getTournamentById(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")
        createTournamentRankingsUseCase(updatedTournament)

        // 5. Gesamtergebnisse an Backend senden, Server-Finalisierung aufrufen & lokal als finalisiert markieren
        tournamentRepository.submitResultsAndFinalize(tournamentId)
    }
}

