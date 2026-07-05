package de.kindermaenner.monatsblitz.ui.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.ui.screens.HomeScreen
import de.kindermaenner.monatsblitz.ui.screens.TournamentScreen
import de.kindermaenner.monatsblitz.ui.viewmodels.HomeViewModel
import de.kindermaenner.monatsblitz.infrastructure.MonatsblitzRepository
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tournamentStorage = remember { TournamentStorage(context) }
    
    // Determine start destination based on saved tournament state
    var startDestination by remember { mutableStateOf(Routes.HOME) }
    
    LaunchedEffect(Unit) {
        val state = tournamentStorage.getTournamentState().firstOrNull()
        if (state != null && !state.finalized) {
            startDestination = Routes.tournamentPath(state.tournamentId)
        }
    }

    NavHost(navController, startDestination = startDestination) {

        composable(Routes.HOME) {
            val viewModel = remember {
                HomeViewModel(
                    getPlayers = {
                        MonatsblitzRepository.getPlayers()
                    },
                    createTournament = { players, mode, double ->
                        val id = MonatsblitzRepository.createTournament(players, mode, double)
                        Log.i("AppNavHost", "Tournament created with ID: $id")
                        return@HomeViewModel id
                    },
                    tournamentStorage = tournamentStorage,
                    onTournamentCreated = { tournamentId ->
                        Log.i("AppNavHost", "Navigating to tournament with ID: $tournamentId")
                        navController.navigate(Routes.tournamentPath(tournamentId)) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
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
                TournamentScreen(
                    tournament = tournament,
                    onBackToHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
