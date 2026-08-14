package de.kindermaenner.monatsblitz.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.navigation.AppNavHost
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute

@Composable
fun MonatsblitzApp(
    appContainer: AppContainer,
    startDestination: AppRoute
) {
    val navController = rememberNavController()

    AppNavHost(
        navController = navController,
        appContainer = appContainer,
        startDestination = startDestination
    )
}
