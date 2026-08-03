package de.kindermaenner.monatsblitz.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.root.components.ErrorComponent
import de.kindermaenner.monatsblitz.ui.home.HomeScreen
import de.kindermaenner.monatsblitz.ui.root.components.LoadingComponent
import de.kindermaenner.monatsblitz.ui.tournament.TournamentScreen
import de.kindermaenner.monatsblitz.ui.home.HomeViewModel
import de.kindermaenner.monatsblitz.ui.tournament.TournamentViewModel

@Composable
fun RootScreen(
    appContainer: AppContainer
) {

    val rootViewModel: RootViewModel = viewModel(
        factory = appContainer.rootViewModelFactory
    )

    val state by rootViewModel.uiState.collectAsState()

    when(val current = state) {

        RootUiState.Loading -> {
            LoadingComponent()
        }

        RootUiState.ReadyWithoutTournament -> {
            val homeViewModel: HomeViewModel = viewModel(
                factory = appContainer.homeViewModelFactory
            )

            HomeScreen(homeViewModel)
        }

        is RootUiState.ReadyWithTournament -> {

            val tournamentViewModel: TournamentViewModel = viewModel(
                factory = appContainer.tournamentViewModelFactory(
                    current.tournamentId
                )
            )

            TournamentScreen(tournamentViewModel)
        }

        is RootUiState.Error -> {

            ErrorComponent(current.message)
        }
    }
}