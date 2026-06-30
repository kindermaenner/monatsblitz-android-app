package de.sgkoenigslutter.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel : ViewModel() {
    private val _currentTournament = MutableStateFlow<Tournament?>(null)
    val currentTournament: StateFlow<Tournament?> = _currentTournament

    fun setTournament(tournament: Tournament) {
        _currentTournament.value = tournament
    }

    fun clearTournament() {
        _currentTournament.value = null
    }
}
