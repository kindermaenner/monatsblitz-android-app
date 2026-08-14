package de.kindermaenner.monatsblitz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.crosstable.CrosstableScreen
import de.kindermaenner.monatsblitz.ui.crosstable.CrosstableViewModel
import de.kindermaenner.monatsblitz.ui.ranking.RankingScreen
import de.kindermaenner.monatsblitz.ui.ranking.RankingViewModel
import de.kindermaenner.monatsblitz.ui.tournamentsetup.TournamentSetupScreen
import de.kindermaenner.monatsblitz.ui.tournamentsetup.TournamentSetupViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    appContainer: AppContainer,
    startDestination: AppRoute
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // TournamentSetup
        composable<AppRoute.TournamentSetup> {
            val vm: TournamentSetupViewModel = viewModel(
                factory = appContainer.tournamentSetupViewModelFactory
            )
            TournamentSetupScreen(
                viewModel = vm,
                onNavigateToCrosstable = { id ->
                    navController.navigate(AppRoute.Crosstable(id))
                }
            )
        }

        // Crosstable
        composable<AppRoute.Crosstable> { backStackEntry ->
            val route: AppRoute.Crosstable = backStackEntry.toRoute()
            
            val vm: CrosstableViewModel = viewModel(
                factory = appContainer.crosstableViewModelFactory(route.id)
            )
            CrosstableScreen(
                viewModel = vm,
                onNavigateToRanking = { id ->
                    navController.navigate(AppRoute.Ranking(id))
                },
                onBackToSetup = {
                    vm.resetTournament()
                    navController.navigate(AppRoute.TournamentSetup) {
                        popUpTo<AppRoute.TournamentSetup> { inclusive = true }
                    }
                }
            )
        }

        // Ranking
        composable<AppRoute.Ranking> { backStackEntry ->
            val route: AppRoute.Ranking = backStackEntry.toRoute()

            val vm: RankingViewModel = viewModel(
                factory = appContainer.rankingViewModelFactory(route.id)
            )

            RankingScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
