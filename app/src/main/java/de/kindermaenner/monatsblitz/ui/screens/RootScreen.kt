package de.kindermaenner.monatsblitz.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.viewmodels.HomeViewModel
import de.kindermaenner.monatsblitz.ui.viewmodels.RootViewModel
import de.kindermaenner.monatsblitz.ui.viewmodels.TournamentViewModel

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
            LoadingScreen()
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

            ErrorScreen(current.message)
        }
    }
}