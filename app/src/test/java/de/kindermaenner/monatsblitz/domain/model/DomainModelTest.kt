package de.kindermaenner.monatsblitz.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DomainModelTest {

    @Test
    fun testGameDataClass() {
        val g1 = Game(id = 1L, player1Id = 2L, player2Id = 3L, leg = 1, result = GameResult.Win)
        val g2 = Game(id = 1L, player1Id = 2L, player2Id = 3L, leg = 1, result = GameResult.Win)
        val g3 = g1.copy(result = GameResult.Loss)

        assertEquals(g1, g2)
        assertEquals(g1.hashCode(), g2.hashCode())
        assertNotEquals(g1, g3)
        assertTrue(g1.toString().contains("Game"))
    }

    @Test
    fun testNewGameDataClass() {
        val ng1 = NewGame(player1Id = 2L, player2Id = 3L, leg = 1)
        val ng2 = NewGame(player1Id = 2L, player2Id = 3L, leg = 1)
        val ng3 = ng1.copy(leg = 2)

        assertEquals(ng1, ng2)
        assertEquals(ng1.hashCode(), ng2.hashCode())
        assertNotEquals(ng1, ng3)
        assertTrue(ng1.toString().contains("NewGame"))
    }

    @Test
    fun testNewPlayerDataClass() {
        val np1 = NewPlayer(name = "Mustermann", vorname = "Max", rufname = "Maxi", isLocal = false)
        val np2 = NewPlayer(name = "Mustermann", vorname = "Max", rufname = "Maxi", isLocal = false)
        val np3 = np1.copy(isLocal = true)

        assertEquals(np1, np2)
        assertEquals(np1.hashCode(), np2.hashCode())
        assertNotEquals(np1, np3)
        assertTrue(np1.toString().contains("NewPlayer"))
    }

    @Test
    fun testNewTournamentDataClass() {
        val nt1 = NewTournament(
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.of(2026, 11, 7),
            rounds = 1,
            players = emptyList(),
            games = emptyList(),
            isLocalOnly = false,
            countsForRanking = true,
            label = "A"
        )
        val nt2 = NewTournament(
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.of(2026, 11, 7),
            rounds = 1,
            players = emptyList(),
            games = emptyList(),
            isLocalOnly = false,
            countsForRanking = true,
            label = "A"
        )
        val nt3 = nt1.copy(label = "B")

        assertEquals(nt1, nt2)
        assertEquals(nt1.hashCode(), nt2.hashCode())
        assertNotEquals(nt1, nt3)
        assertTrue(nt1.toString().contains("NewTournament"))
    }

    @Test
    fun testPlayerRankingEntryAndTournamentRankingItem() {
        val pre1 = PlayerRankingEntry(playerId = 1L, tournamentId = 2L, points = 3.5, rank = 1)
        val pre2 = PlayerRankingEntry(playerId = 1L, tournamentId = 2L, points = 3.5, rank = 1)
        val pre3 = pre1.copy(rank = 2)

        assertEquals(pre1, pre2)
        assertEquals(pre1.hashCode(), pre2.hashCode())
        assertNotEquals(pre1, pre3)
        assertTrue(pre1.toString().contains("PlayerRankingEntry"))

        val tri1 = TournamentRankingItem(playerId = 1L, name = "Max", points = 3.5, rank = 1)
        val tri2 = TournamentRankingItem(playerId = 1L, name = "Max", points = 3.5, rank = 1)
        val tri3 = tri1.copy(rank = 2)

        assertEquals(tri1, tri2)
        assertEquals(tri1.hashCode(), tri2.hashCode())
        assertNotEquals(tri1, tri3)
        assertTrue(tri1.toString().contains("TournamentRankingItem"))
    }

    @Test
    fun testQrSetupConfigDataClass() {
        val q1 = QrSetupConfig(
            serverUrl = "https://example.com",
            apiKey = "key123",
            modePreselectionType = ModePreselectionType.FIXED,
            fixedMode = "Blitz 5+0",
            rotationModes = listOf("Blitz 3+2", "Blitz 5+0")
        )
        val q2 = QrSetupConfig(
            serverUrl = "https://example.com",
            apiKey = "key123",
            modePreselectionType = ModePreselectionType.FIXED,
            fixedMode = "Blitz 5+0",
            rotationModes = listOf("Blitz 3+2", "Blitz 5+0")
        )
        val q3 = q1.copy(serverUrl = "https://other.com")

        assertEquals(q1, q2)
        assertEquals(q1.hashCode(), q2.hashCode())
        assertNotEquals(q1, q3)
        assertTrue(q1.toString().contains("QrSetupConfig"))
    }

    @Test
    fun testDomainEnums() {
        PlayerWithdrawRule.entries.forEach {
            assertNotNull(PlayerWithdrawRule.valueOf(it.name))
        }
        PlayerFilterMode.entries.forEach {
            assertNotNull(PlayerFilterMode.valueOf(it.name))
        }
        PlayerSortField.entries.forEach {
            assertNotNull(PlayerSortField.valueOf(it.name))
        }
        PlayerSortOrder.entries.forEach {
            assertNotNull(PlayerSortOrder.valueOf(it.name))
        }
        ModePreselectionType.entries.forEach {
            assertNotNull(ModePreselectionType.valueOf(it.name))
        }
        SyncStatus.entries.forEach {
            assertNotNull(SyncStatus.valueOf(it.name))
        }
    }
}
