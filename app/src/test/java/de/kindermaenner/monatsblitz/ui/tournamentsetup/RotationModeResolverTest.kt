package de.kindermaenner.monatsblitz.ui.tournamentsetup

import org.junit.Assert.assertEquals
import org.junit.Test

class RotationModeResolverTest {

    @Test
    fun `returns empty string when rotation modes list is empty`() {
        val next = determineNextRotationMode(emptyList(), listOf("3+2"))
        assertEquals("", next)
    }

    @Test
    fun `returns first mode when no recent tournaments exist`() {
        val rotationModes = listOf("3+2", "5+0")
        val next = determineNextRotationMode(rotationModes, emptyList())
        assertEquals("3+2", next)
    }

    @Test
    fun `returns second mode when last tournament was first mode`() {
        val rotationModes = listOf("3+2", "5+0")
        val next = determineNextRotationMode(rotationModes, listOf("3+2"))
        assertEquals("5+0", next)
    }

    @Test
    fun `wraps around to first mode when last tournament was last mode`() {
        val rotationModes = listOf("3+2", "5+0")
        val next = determineNextRotationMode(rotationModes, listOf("5+0", "3+2"))
        assertEquals("3+2", next)
    }

    @Test
    fun `works with N modes in rotation`() {
        val rotationModes = listOf("3+2", "5+0", "10+0")
        assertEquals("5+0", determineNextRotationMode(rotationModes, listOf("3+2")))
        assertEquals("10+0", determineNextRotationMode(rotationModes, listOf("5+0", "3+2")))
        assertEquals("3+2", determineNextRotationMode(rotationModes, listOf("10+0", "5+0", "3+2")))
    }

    @Test
    fun `ignores one-shot modes and finds previous rotation mode`() {
        val rotationModes = listOf("3+2", "5+0")
        // The most recent tournament was a one-shot ("Jubiläumsblitz"), the one before was "3+2"
        val recentTournaments = listOf("Jubiläumsblitz", "3+2", "5+0")
        val next = determineNextRotationMode(rotationModes, recentTournaments)
        assertEquals("5+0", next)
    }

    @Test
    fun `ignores multiple one-shot modes`() {
        val rotationModes = listOf("3+2", "5+0")
        val recentTournaments = listOf("Weihnachtsblitz", "Jubiläumsblitz", "5+0")
        val next = determineNextRotationMode(rotationModes, recentTournaments)
        assertEquals("3+2", next)
    }

    @Test
    fun `case-insensitive matching for mode names`() {
        val rotationModes = listOf("3+2", "5+0")
        val next = determineNextRotationMode(rotationModes, listOf("3+2"))
        assertEquals("5+0", next)
    }
}
