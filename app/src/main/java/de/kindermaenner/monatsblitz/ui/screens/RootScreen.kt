package de.kindermaenner.monatsblitz.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph
import androidx.navigation.compose.rememberNavController
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.navigation.AppNavHost
import de.kindermaenner.monatsblitz.ui.viewmodels.RootViewModel

@Composable
fun RootScreen(
    appContainer: AppContainer
) {

    val rootViewModel: RootViewModel = viewModel(
        factory = appContainer.rootViewModelFactory
    )

    val navController =
        rememberNavController()


    val state by rootViewModel
        .uiState
        .collectAsState()


    when(val current = state) {


        RootUiState.Loading -> {
            LoadingScreen()
        }


        RootUiState.ReadyWithoutTournament -> {
            AppNavHost(
                navController,
                "home",
                appContainer
            )
        }


        is RootUiState.ReadyWithTournament -> {

            AppNavHost(
                navController,
                "tournament/${current.tournamentId}",
                appContainer
            )
        }


        is RootUiState.Error -> {

            ErrorScreen(current.message)
        }
    }
}