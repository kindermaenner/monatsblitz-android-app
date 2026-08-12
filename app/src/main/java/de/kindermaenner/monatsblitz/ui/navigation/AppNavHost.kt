package de.kindermaenner.monatsblitz.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.home.HomeScreen
import de.kindermaenner.monatsblitz.ui.home.HomeViewModel
import de.kindermaenner.monatsblitz.ui.tournament.TournamentScreen
import de.kindermaenner.monatsblitz.ui.tournament.TournamentViewModel


@Composable
fun AppNavHost(
    navController: NavHostController,
    appContainer: AppContainer,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Home
        composable(AppRoute.Home.path) {
            val vm: HomeViewModel = viewModel(
                factory = appContainer.homeViewModelFactory
            )
            HomeScreen(vm)
        }

        // Tournament (mit Parameter)
        composable(AppRoute.Tournament.FULL,
            arguments = listOf(
            navArgument(AppRoute.Tournament.ARG_ID) {
                type = NavType.LongType
            }
        )) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(AppRoute.Tournament.ARG_ID)
                ?: error("Tournament ID missing")
            Log.i("AppNavHost", "Tournament ID: $id")


            val vm: TournamentViewModel = viewModel(
                factory = appContainer.tournamentViewModelFactory(id)
            )
            TournamentScreen(vm)
        }
    }
}
