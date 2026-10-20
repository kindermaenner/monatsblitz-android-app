package de.kindermaenner.monatsblitz.domain.util

import de.kindermaenner.monatsblitz.domain.model.Player

object PlayerNameFormatter {

    fun formatShortName(player: Player, allPlayers: Collection<Player>): String {
        val vorname = player.vorname.trim()
        val nachname = player.name.trim()

        if (vorname.isBlank()) {
            return nachname.ifBlank { "Unbekannt" }
        }
        if (nachname.isBlank()) {
            return vorname
        }

        val sameFirstNamePlayers = allPlayers.filter {
            it.id != player.id && it.vorname.trim().equals(vorname, ignoreCase = true)
        }

        if (sameFirstNamePlayers.isEmpty()) {
            return vorname
        }

        val initial = nachname.first().uppercaseChar()
        val sameInitialPlayers = sameFirstNamePlayers.filter {
            it.name.trim().firstOrNull()?.uppercaseChar() == initial
        }

        return if (sameInitialPlayers.isEmpty()) {
            "$vorname $initial."
        } else {
            "$vorname $nachname"
        }
    }

    fun formatDisplayName(
        player: Player,
        allPlayers: Collection<Player>,
        useShortName: Boolean
    ): String {
        return if (useShortName) {
            formatShortName(player, allPlayers)
        } else {
            player.fullName
        }
    }
}
