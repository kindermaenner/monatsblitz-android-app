package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase

class TournamentSetupViewModelFactory(
    private val playerRepository: PlayerRepository,
    private val createTournamentUseCase: CreateTournamentUseCase,
    private val addPlayerUseCase: AddPlayerUseCase,
    private val syncPlayersUseCase: SyncPlayersUseCase? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val tournamentRepository: TournamentRepository? = null
) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(TournamentSetupViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return TournamentSetupViewModel(
                playerRepository,
                createTournamentUseCase,
                addPlayerUseCase,
                syncPlayersUseCase,
                settingsRepository,
                tournamentRepository
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
