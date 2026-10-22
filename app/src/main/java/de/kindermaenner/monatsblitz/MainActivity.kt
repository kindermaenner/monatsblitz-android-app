package de.kindermaenner.monatsblitz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.app.MonatsblitzApplication
import de.kindermaenner.monatsblitz.ui.MonatsblitzApp
import de.kindermaenner.monatsblitz.ui.root.RootViewModel
import de.kindermaenner.monatsblitz.ui.theme.MonatsblitzTheme

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer
    private lateinit var rootViewModel: RootViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        appContainer = (application as MonatsblitzApplication).appContainer
        rootViewModel = ViewModelProvider(
            this,
            appContainer.rootViewModelFactory
        )[RootViewModel::class.java]

        val splashStartTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            val timedOut = System.currentTimeMillis() - splashStartTime > 2000L
            !timedOut && rootViewModel.initialRoute.value == null
        }

        enableEdgeToEdge()
        setContent {
            val initialRoute by rootViewModel.initialRoute.collectAsStateWithLifecycle()

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
