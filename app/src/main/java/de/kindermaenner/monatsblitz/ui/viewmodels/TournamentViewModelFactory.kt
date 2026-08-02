package de.kindermaenner.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao

class TournamentViewModelFactory(
    private val tournamentRepository: TournamentRepository,
    private val gameDao: GameDao,
    private val tournamentId: Long

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
                gameDao,
                tournamentId
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}