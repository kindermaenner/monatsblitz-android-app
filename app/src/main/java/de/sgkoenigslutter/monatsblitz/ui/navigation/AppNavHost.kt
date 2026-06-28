package de.sgkoenigslutter.monatsblitz.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import de.sgkoenigslutter.monatsblitz.infrastructure.FakeRepo
import de.sgkoenigslutter.monatsblitz.ui.screens.HomeScreen
import de.sgkoenigslutter.monatsblitz.ui.screens.TournamentScreen
import de.sgkoenigslutter.monatsblitz.ui.viewmodels.HomeViewModel

class AppState {
    var currentTournament: Tournament? = null
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val appState = remember { AppState() }

    NavHost(navController, startDestination = "home") {

        composable("home") {
            val viewModel = remember {
                HomeViewModel(
                    getPlayers = { FakeRepo.getPlayers() },
                    createTournament = { players, mode, double ->
                        appState.currentTournament = FakeRepo.createTournament(players, mode, double)
                    }
                )
            }

            HomeScreen(
                viewModel = viewModel,
                onStartTournament = {
                    navController.navigate("tournament")
                }
            )
        }

        composable("tournament") {
            TournamentScreen(appState.currentTournament!!)
        }
    }
}
