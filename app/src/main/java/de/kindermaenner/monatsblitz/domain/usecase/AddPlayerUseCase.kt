package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository

class AddPlayerUseCase(private val playerRepository: PlayerRepository) {
    
    suspend operator fun invoke(vorname: String, name: String, isLocal: Boolean = false): Player {
        val trimmedVorname = vorname.trim()
        val trimmedName = name.trim()
        require(trimmedVorname.isNotBlank() && trimmedName.isNotBlank()) {
            "Vorname und Nachname dürfen nicht leer sein."
        }

        val existingPlayers = playerRepository.findPlayersByName(trimmedVorname, trimmedName)
        require(existingPlayers.isEmpty()) {
            "Ein Spieler mit dem Namen '$trimmedVorname $trimmedName' existiert bereits."
        }

        return playerRepository.createPlayer(
            NewPlayer(
                vorname = trimmedVorname,
                name = trimmedName,
                isLocal = isLocal
            )
        )
    }
}
