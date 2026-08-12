package de.kindermaenner.monatsblitz.ui.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.navigation.AppNavHost
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import de.kindermaenner.monatsblitz.ui.root.components.ErrorComponent
import de.kindermaenner.monatsblitz.ui.root.components.LoadingComponent


@Composable
fun RootScreen( appContainer: AppContainer) {
    val navController = rememberNavController()

    // RootViewModel wird hier erzeugt, nicht im NavHost
    val rootViewModel: RootViewModel = viewModel(
        factory = appContainer.rootViewModelFactory
    )

    // --- UI-State beobachten ---
    val uiState by rootViewModel.uiState.collectAsState()

    // Navigation-Events beobachten
    LaunchedEffect(Unit) {
        if (uiState is RootUiState.Ready) {
            rootViewModel.effect.collect { effect ->
                when (effect) {
                    is RootEffect.Navigate -> {
                        navController.navigate(effect.route.path)
                    }
                }
            }
        }
    }

    when (uiState) {

        RootUiState.Loading -> {
            LoadingComponent()
        }

        is RootUiState.Ready -> {
            val tournamentId = (uiState as RootUiState.Ready).tournamentId

            AppNavHost(
                navController = navController,
                appContainer = appContainer,
                startDestination = if (tournamentId != null)
                    AppRoute.Tournament(tournamentId).path
                else
                    AppRoute.Home.path
            )
        }

        is RootUiState.Error -> {
            ErrorComponent((uiState as RootUiState.Error).message)
        }
    }
}
