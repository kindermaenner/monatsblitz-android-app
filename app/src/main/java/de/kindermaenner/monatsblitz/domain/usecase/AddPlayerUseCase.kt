package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository

class AddPlayerUseCase(private val playerRepository: PlayerRepository) {
    
    suspend operator fun invoke(
        vorname: String,
        name: String,
        rufname: String? = null,
        isLocal: Boolean = false
    ): Player {
        val trimmedVorname = vorname.trim()
        val trimmedName = name.trim()
        val trimmedRufname = rufname?.trim()?.takeIf { it.isNotBlank() }
        require(trimmedVorname.isNotBlank() && trimmedName.isNotBlank()) {
            "Vorname und Nachname dürfen nicht leer sein."
        }

        val existingPlayers = playerRepository.findPlayersByNameAndRufname(
            trimmedVorname,
            trimmedName,
            trimmedRufname ?: ""
        )
        require(existingPlayers.isEmpty()) {
            val rufnameInfo = trimmedRufname?.let { " (Rufname: $it)" } ?: ""
            "Ein Spieler mit dem Namen '$trimmedVorname $trimmedName'$rufnameInfo existiert bereits."
        }

        return playerRepository.createPlayer(
            NewPlayer(
                vorname = trimmedVorname,
                name = trimmedName,
                rufname = trimmedRufname,
                isLocal = isLocal
            )
        )
    }
}
