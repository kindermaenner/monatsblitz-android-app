package de.kindermaenner.monatsblitz.ui.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.screens.HomeScreen
import de.kindermaenner.monatsblitz.ui.screens.TournamentScreen
import de.kindermaenner.monatsblitz.ui.viewmodels.HomeViewModel
import de.kindermaenner.monatsblitz.ui.viewmodels.TournamentViewModel

@Composable
fun AppNavHost(navController: NavHostController,
               startDestination: String,
               appContainer: AppContainer
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        composable("home") {

            val viewModel: HomeViewModel =
                viewModel(
                    factory =
                        appContainer.homeViewModelFactory
                )


            HomeScreen(viewModel)
        }



        composable(
            "tournament/{tournamentId}",
            arguments = listOf(
                navArgument("tournamentId") {
                    type = NavType.IntType
                }
            )
        ) { entry ->
            val id = entry.arguments!!.getInt("tournamentId")

            val viewModel: TournamentViewModel =
                viewModel(
                    factory =
                        appContainer
                            .tournamentViewModelFactory(id)
                )
            TournamentScreen(viewModel)
        }
    }
}
