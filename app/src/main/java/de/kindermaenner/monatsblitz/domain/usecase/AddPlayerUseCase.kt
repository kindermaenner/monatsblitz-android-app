package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository

class AddPlayerUseCase(private val playerRepository: PlayerRepository) {
    
    suspend operator fun invoke(vorname: String, name: String): Player {
        val existingPlayers = playerRepository.findPlayersByName(vorname, name)
        
        val suffix = if (existingPlayers.isEmpty()) {
            null
        } else {
            "(${existingPlayers.size + 1})"
        }
        
        return playerRepository.createPlayer(
            NewPlayer(
                Vorname = vorname,
                Name = name,
                displaySuffix = suffix
            )
        )
    }
}
