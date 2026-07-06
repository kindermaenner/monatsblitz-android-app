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
import androidx.lifecycle.lifecycleScope
import de.kindermaenner.monatsblitz.infrastructure.DefaultMonatsblitzRepository
import de.kindermaenner.monatsblitz.infrastructure.MonatsblitzRepository
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.ui.navigation.AppNavHost
import de.kindermaenner.monatsblitz.ui.theme.MonatsblitzTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: MonatsblitzRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "onCreate")
        repository = DefaultMonatsblitzRepository(
            TournamentStorage(applicationContext)
        )
        Log.i("MainActivity", "repository erzeugt")
        lifecycleScope.launch {
            Log.i("MainActivity", "initiialisiere repository")
            repository.init()
            Log.i("MainActivity", "Repository initialized")
            Log.i("MainActivity", "currentTournament: ${repository.currentTournament}")
        }
        Log.i("MainActivity", "nach lifecycleScope.launch")
        enableEdgeToEdge()
        setContent {
            MonatsblitzTheme {
                AppNavHost(repository)
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