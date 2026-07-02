package de.sgkoenigslutter.monatsblitz.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.sgkoenigslutter.monatsblitz.infrastructure.MonatsblitzRepository
import de.sgkoenigslutter.monatsblitz.ui.screens.HomeScreen
import de.sgkoenigslutter.monatsblitz.ui.screens.TournamentScreen
import de.sgkoenigslutter.monatsblitz.ui.viewmodels.HomeViewModel

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            val viewModel = remember {
                HomeViewModel(
                    getPlayers = {
                        MonatsblitzRepository.getPlayers()
                    },
                    createTournament = { players, mode, double ->
                        MonatsblitzRepository.createTournament(players, mode, double)
                    },
                    onTournamentCreated = { tournamentId ->
                        navController.navigate(Routes.tournamentPath(tournamentId))
                    }
                )
            }

            LaunchedEffect(Unit) {
                viewModel.loadPlayers()
            }

            HomeScreen(
                viewModel = viewModel
            )
        }

        composable(
            route = Routes.TOURNAMENT_WITH_ARG,
            arguments = listOf(navArgument(Routes.TOURNAMENT_ID_ARG) { type = NavType.IntType })
        ) { entry ->
            val tournamentId = entry.arguments?.getInt(Routes.TOURNAMENT_ID_ARG)
            val tournament = tournamentId?.let { MonatsblitzRepository.getTournament(it) }

            if (tournament == null) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
                Text("Turnier wurde nicht gefunden.")
            } else {
                TournamentScreen(tournament)
            }
        }
    }
}
