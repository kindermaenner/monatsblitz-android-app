package de.kindermaenner.monatsblitz.domain.util

import de.kindermaenner.monatsblitz.domain.model.Player

object PlayerNameFormatter {

    fun formatShortName(
        player: Player,
        allPlayers: Collection<Player>,
        preferRufname: Boolean = true
    ): String {
        val baseName = effectiveBaseName(player, preferRufname)
        val nachname = player.name.trim()

        if (baseName.isBlank() || baseName.equals("Unbekannt", ignoreCase = true)) {
            return nachname.ifBlank { "Unbekannt" }
        }
        if (nachname.isBlank()) {
            return baseName
        }

        val sameBaseNamePlayers = allPlayers.filter {
            it.id != player.id && effectiveBaseName(it, preferRufname).equals(baseName, ignoreCase = true)
        }

        if (sameBaseNamePlayers.isEmpty()) {
            return baseName
        }

        val initial = nachname.first().uppercaseChar()
        val sameInitialPlayers = sameBaseNamePlayers.filter {
            it.name.trim().firstOrNull()?.uppercaseChar() == initial
        }

        return if (sameInitialPlayers.isEmpty()) {
            "$baseName $initial."
        } else {
            "$baseName $nachname"
        }
    }

    private fun effectiveBaseName(player: Player, preferRufname: Boolean): String {
        val rufname = player.rufname?.trim()
        if (preferRufname && !rufname.isNullOrBlank()) {
            return rufname
        }
        val vorname = player.vorname.trim()
        if (vorname.isNotBlank()) {
            return vorname
        }
        val nachname = player.name.trim()
        return nachname.ifBlank { "Unbekannt" }
    }

    fun formatDisplayName(
        player: Player,
        allPlayers: Collection<Player>,
        useShortName: Boolean,
        preferRufname: Boolean = true
    ): String {
        return if (useShortName) {
            formatShortName(player, allPlayers, preferRufname)
        } else {
            player.fullName
        }
    }
}
