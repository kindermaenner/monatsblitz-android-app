package de.kindermaenner.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository

class TournamentViewModelFactory(
    private val tournamentRepository: TournamentRepository,
    private val tournamentId: Int

) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(
                TournamentViewModel::class.java
            )
        ) {

            @Suppress("UNCHECKED_CAST")
            return TournamentViewModel(
                tournamentRepository,
                tournamentId
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}