package de.kindermaenner.monatsblitz

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.app.MonatsblitzApplication
import de.kindermaenner.monatsblitz.ui.screens.RootScreen
import de.kindermaenner.monatsblitz.ui.theme.MonatsblitzTheme

class MainActivity : ComponentActivity() {

    private lateinit var appContainer: AppContainer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate")
        appContainer = (application as MonatsblitzApplication).appContainer
        Log.i("MainActivity", "repository erzeugt")

        Log.i("MainActivity", "nach lifecycleScope.launch")
        enableEdgeToEdge()
        setContent {
            MonatsblitzTheme {
                RootScreen(
                    appContainer = appContainer
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MonatsblitzTheme {
        Greeting("Android")
    }
}