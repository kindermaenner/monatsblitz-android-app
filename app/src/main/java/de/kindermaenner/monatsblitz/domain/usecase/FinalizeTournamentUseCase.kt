package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository

class FinalizeTournamentUseCase(
    private val tournamentRepository: TournamentRepository,
    private val syncTournamentUseCase: SyncTournamentUseCase,
    private val syncGameResultsUseCase: SyncGameResultsUseCase,
    private val syncPlayersUseCase: SyncPlayersUseCase,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase = CreateTournamentRankingsUseCase(tournamentRepository)
) {
    suspend operator fun invoke(tournamentId: Long) {
        tournamentRepository.getTournamentById(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")

        // 1. Spieler synchronisieren, damit alle Spieler eine remoteId haben
        syncPlayersUseCase()

        // 2. Turnier remote anlegen, falls noch nicht geschehen
        syncTournamentUseCase()

        // 3. Spiele / Ergebnisse synchronisieren
        syncGameResultsUseCase()

        // 4. Rangliste berechnen und abspeichern
        val tournament = tournamentRepository.getTournamentById(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")
        createTournamentRankingsUseCase(tournament)

        // 5. Gesamtergebnisse an Backend senden, Server-Finalisierung aufrufen & lokal als finalisiert markieren
        tournamentRepository.submitResultsAndFinalize(tournamentId)
    }
}

