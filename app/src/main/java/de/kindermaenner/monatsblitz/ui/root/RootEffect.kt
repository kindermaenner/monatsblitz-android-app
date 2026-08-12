package de.kindermaenner.monatsblitz.ui.root

import de.kindermaenner.monatsblitz.ui.navigation.AppRoute

sealed interface RootEffect {
    data class Navigate(val route: AppRoute) : RootEffect
}
