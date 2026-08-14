package de.kindermaenner.monatsblitz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.app.MonatsblitzApplication
import de.kindermaenner.monatsblitz.ui.MonatsblitzApp
import de.kindermaenner.monatsblitz.ui.root.RootViewModel
import de.kindermaenner.monatsblitz.ui.theme.MonatsblitzTheme

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        appContainer = (application as MonatsblitzApplication).appContainer
        
        enableEdgeToEdge()
        setContent {
            val rootViewModel: RootViewModel = viewModel(
                factory = appContainer.rootViewModelFactory
            )
            val initialRoute by rootViewModel.initialRoute.collectAsState()

            // Splash-Screen halten, bis initialRoute feststeht
            splashScreen.setKeepOnScreenCondition {
                initialRoute == null
            }

            if (initialRoute != null) {
                MonatsblitzTheme {
                    MonatsblitzApp(
                        appContainer = appContainer,
                        startDestination = initialRoute!!
                    )
                }
            }
        }
    }
}
