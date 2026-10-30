package de.kindermaenner.monatsblitz.infrastructure.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.ui.ranking.RankingRowData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.io.OutputStream
import java.time.LocalDate

@Implements(FileProvider::class)
class ShadowFileProvider {
    companion object {
        @JvmStatic
        @Implementation
        fun getUriForFile(context: Context, authority: String, file: File): Uri {
            return Uri.parse("content://$authority/${file.name}")
        }
    }
}

@Implements(PdfDocument::class)
class ShadowPdfDocument {
    @Implementation
    fun __constructor__() {
        // Suppress nativeCreateDocument
    }

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        val constructor = PdfDocument.Page::class.java.getDeclaredConstructor(
            Canvas::class.java,
            PdfDocument.PageInfo::class.java
        )
        constructor.isAccessible = true
        return constructor.newInstance(Canvas(), pageInfo)
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {}

    @Implementation
    fun writeTo(out: OutputStream) {
        out.write(byteArrayOf(0x25, 0x50, 0x44, 0x46)) // %PDF
    }

    @Implementation
    fun close() {}
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowPdfDocument::class, ShadowFileProvider::class])
class TournamentPdfGeneratorTest {

    @Test
    fun buildCrosstableRows_formatsCrosstableWithResultsAndDiagonalMarks() {
        val p1 = Player(1L, "Carlsen", "Magnus")
        val p2 = Player(2L, "Nakamura", "Hikaru")
        val p3 = Player(3L, "Ding", "Liren")
        val players = listOf(p1, p2, p3)

        val gamesList = listOf(
            Game(id = 101, player1Id = 1L, player2Id = 2L, leg = 1, result = GameResult.Win),
            Game(id = 102, player1Id = 2L, player2Id = 3L, leg = 1, result = GameResult.Remis),
            Game(id = 103, player1Id = 3L, player2Id = 1L, leg = 1, result = GameResult.ForfeitDraw)
        )
        val gamesMap = gamesList.associateBy { it.id }

        val tournament = Tournament(
            id = 1L,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.of(2026, 10, 21),
            rounds = 1,
            players = players,
            games = gamesMap,
            withdrawnPlayerIds = setOf(3L),
            isLocalOnly = true
        )

        val rankingRows = listOf(
            RankingRowData(rank = 1, name = "Magnus Carlsen", points = 1.5),
            RankingRowData(rank = 2, name = "Hikaru Nakamura", points = 0.5),
            RankingRowData(rank = 3, name = "Liren Ding", points = 0.5)
        )

        val rows = TournamentPdfGenerator.buildCrosstableRows(tournament, rankingRows)

        assertEquals(3, rows.size)

        // Row 0 (Carlsen): vs p1 = "x", vs p2 = "1", vs p3 = "="
        val r0 = rows[0]
        assertEquals(1L, r0.playerId)
        assertEquals("Magnus Carlsen", r0.playerName)
        assertEquals(1.5, r0.totalPoints, 0.001)
        assertEquals(listOf("x", "1", "="), r0.results)
        assertEquals(false, r0.isWithdrawn)

        // Row 1 (Nakamura): vs p1 = "0", vs p2 = "x", vs p3 = "½"
        val r1 = rows[1]
        assertEquals(2L, r1.playerId)
        assertEquals(0.5, r1.totalPoints, 0.001)
        assertEquals(listOf("0", "x", "½"), r1.results)
        assertEquals(false, r1.isWithdrawn)

        // Row 2 (Ding): vs p1 = "=", vs p2 = "½", vs p3 = "x"
        val r2 = rows[2]
        assertEquals(3L, r2.playerId)
        assertEquals(0.5, r2.totalPoints, 0.001)
        assertEquals(listOf("=", "½", "x"), r2.results)
        assertTrue(r2.isWithdrawn)
    }

    @Test
    fun buildCrosstableRows_handlesAllGameResultVariants() {
        val p1 = Player(1L, "Alpha", "Avery")
        val p2 = Player(2L, "Beta", "Brooke")
        val p3 = Player(3L, "Gamma", "VeryLongFirstAndLastNameThatWillBeClipped")
        val players = listOf(p1, p2, p3)

        val gamesList = listOf(
            Game(id = 201, player1Id = 1L, player2Id = 2L, leg = 1, result = GameResult.ForfeitWin),
            Game(id = 202, player1Id = 1L, player2Id = 3L, leg = 1, result = GameResult.ForfeitLoss),
            Game(id = 203, player1Id = 2L, player2Id = 3L, leg = 1, result = GameResult.Open)
        )

        val tournament = Tournament(
            id = 2L,
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.of(2026, 10, 22),
            rounds = 2,
            players = players,
            games = gamesList.associateBy { it.id },
            withdrawnPlayerIds = emptySet(),
            isLocalOnly = false
        )

        val rankingRows = listOf(
            RankingRowData(rank = 1, name = "Avery Alpha", points = 1.0),
            RankingRowData(rank = 2, name = "VeryLongFirstAndLastNameThatWillBeClipped Gamma", points = 1.0),
            RankingRowData(rank = 3, name = "Brooke Beta", points = 0.0)
        )

        val rows = TournamentPdfGenerator.buildCrosstableRows(tournament, rankingRows)
        assertEquals(3, rows.size)

        // Alpha: vs 1='x', vs 2='+', vs 3='-'
        assertEquals(listOf("x", "+", "-"), rows[0].results)
        // Beta: vs 1='-', vs 2='x', vs 3=''
        assertEquals(listOf("-", "x", ""), rows[1].results)
        // Gamma: vs 1='+', vs 2='', vs 3='x'
        assertEquals(listOf("+", "", "x"), rows[2].results)
    }

    @Test
    fun generatePdf_createsValidPdfFileOnDisk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val p1 = Player(1L, "Doe", "John")
        val p2 = Player(2L, "Smith", "Jane")
        val players = listOf(p1, p2)

        val games = listOf(
            Game(id = 1L, player1Id = 1L, player2Id = 2L, leg = 1, result = GameResult.Win)
        ).associateBy { it.id }

        val tournament = Tournament(
            id = 42L,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.of(2026, 10, 29),
            rounds = 1,
            players = players,
            games = games,
            withdrawnPlayerIds = setOf(2L),
            isLocalOnly = true
        )

        val rankingRows = listOf(
            RankingRowData(rank = 1, name = "John Doe", points = 1.0),
            RankingRowData(rank = 2, name = "Jane Smith (z)", points = 0.0)
        )

        val file = TournamentPdfGenerator.generatePdf(context, tournament, rankingRows)

        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.contains("Turnier_2026-10-29_42.pdf"))
    }

    @Test
    fun generatePdf_handlesDoubleRoundAndRemoteTournamentWithoutErrors() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val p1 = Player(1L, "One", "Player")
        val p2 = Player(2L, "Two", "Player")
        val players = listOf(p1, p2)

        val tournament = Tournament(
            id = 99L,
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.of(2026, 11, 1),
            rounds = 2,
            players = players,
            games = emptyMap(),
            withdrawnPlayerIds = emptySet(),
            isLocalOnly = false
        )

        val rankingRows = listOf(
            RankingRowData(rank = 1, name = "Player One", points = 2.5),
            RankingRowData(rank = 2, name = "Player Two", points = 1.5)
        )

        val file = TournamentPdfGenerator.generatePdf(context, tournament, rankingRows)
        assertNotNull(file)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun sharePdf_startsSendIntentWithFileUri() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val p1 = Player(1L, "Doe", "John")
        val tournament = Tournament(
            id = 10L,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = listOf(p1),
            games = emptyMap()
        )
        val rankingRows = listOf(RankingRowData(rank = 1, name = "John Doe", points = 0.0))

        TournamentPdfGenerator.generateAndSharePdf(context, tournament, rankingRows)

        val shadowApp = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
        val startedIntent = shadowApp.nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(android.content.Intent.ACTION_CHOOSER, startedIntent.action)
    }
}
