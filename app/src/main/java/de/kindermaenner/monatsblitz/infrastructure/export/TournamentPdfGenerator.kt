package de.kindermaenner.monatsblitz.infrastructure.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.ui.crosstable.components.CrosstableRowData
import de.kindermaenner.monatsblitz.ui.ranking.RankingRowData
import java.io.File
import java.io.FileOutputStream
import java.time.format.DateTimeFormatter

object TournamentPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun generatePdf(
        context: Context,
        tournament: Tournament,
        rankingRows: List<RankingRowData>,
        crosstableRows: List<CrosstableRowData> = buildCrosstableRows(tournament, rankingRows)
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var currentY = MARGIN

        // Paints
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionHeaderPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val textBoldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(235, 238, 242)
            style = Paint.Style.FILL
        }

        val altRowPaint = Paint().apply {
            color = Color.rgb(248, 249, 250)
            style = Paint.Style.FILL
        }

        // 1. Header
        val title = if (tournament.isLocalOnly) "Schachturnier (Lokal)" else "Monatsblitz Turnier"
        canvas.drawText(title, MARGIN, currentY + 16f, titlePaint)
        currentY += 24f

        val formattedDate = tournament.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val roundsText = if (tournament.rounds == 2) "Doppelrunde" else "1 Runde"
        val subtitle = "Datum: $formattedDate   |   Modus: ${tournament.mode.displayName}   |   Format: $roundsText"
        canvas.drawText(subtitle, MARGIN, currentY + 10f, subtitlePaint)
        currentY += 18f

        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
        currentY += 14f

        // 2. Rangliste Table
        canvas.drawText("Endstand / Rangliste", MARGIN, currentY + 11f, sectionHeaderPaint)
        currentY += 16f

        val rankColW = 35f
        val pointsColW = 55f
        val nameColW = 200f
        val rankRowH = 16f

        // Header row
        canvas.drawRect(MARGIN, currentY, MARGIN + rankColW + nameColW + pointsColW, currentY + rankRowH, headerBgPaint)
        canvas.drawText("Pl.", MARGIN + 6f, currentY + 11f, textBoldPaint)
        canvas.drawText("Spieler", MARGIN + rankColW + 6f, currentY + 11f, textBoldPaint)
        canvas.drawText("Punkte", MARGIN + rankColW + nameColW + 6f, currentY + 11f, textBoldPaint)
        currentY += rankRowH

        rankingRows.forEachIndexed { index, row ->
            if (index % 2 == 1) {
                canvas.drawRect(MARGIN, currentY, MARGIN + rankColW + nameColW + pointsColW, currentY + rankRowH, altRowPaint)
            }
            canvas.drawText("${row.rank}.", MARGIN + 6f, currentY + 11f, textPaint)
            canvas.drawText(row.name, MARGIN + rankColW + 6f, currentY + 11f, textPaint)
            val pointsStr = if (row.points % 1.0 == 0.0) "${row.points.toInt()}" else "${row.points}"
            canvas.drawText(pointsStr, MARGIN + rankColW + nameColW + 6f, currentY + 11f, textBoldPaint)
            canvas.drawLine(MARGIN, currentY + rankRowH, MARGIN + rankColW + nameColW + pointsColW, currentY + rankRowH, linePaint)
            currentY += rankRowH
        }

        currentY += 18f

        // 3. Crosstable
        if (crosstableRows.isNotEmpty() && currentY + 60f < PAGE_HEIGHT - MARGIN) {
            canvas.drawText("Kreuztabelle", MARGIN, currentY + 11f, sectionHeaderPaint)
            currentY += 16f

            val ctNameColW = 120f
            val ctPtsColW = 40f
            val availableW = (PAGE_WIDTH - 2 * MARGIN) - ctNameColW - ctPtsColW
            val numPlayers = crosstableRows.size
            val cellW = (availableW / numPlayers).coerceIn(16f, 32f)
            val totalCtW = ctNameColW + (cellW * numPlayers) + ctPtsColW
            val ctRowH = 15f

            // CT Header
            canvas.drawRect(MARGIN, currentY, MARGIN + totalCtW, currentY + ctRowH, headerBgPaint)
            canvas.drawText("Spieler", MARGIN + 4f, currentY + 11f, textBoldPaint)
            for (i in 1..numPlayers) {
                canvas.drawText("$i", MARGIN + ctNameColW + (i - 1) * cellW + 4f, currentY + 11f, textBoldPaint)
            }
            canvas.drawText("Pkt.", MARGIN + ctNameColW + numPlayers * cellW + 4f, currentY + 11f, textBoldPaint)
            currentY += ctRowH

            crosstableRows.forEachIndexed { index, row ->
                if (index % 2 == 1) {
                    canvas.drawRect(MARGIN, currentY, MARGIN + totalCtW, currentY + ctRowH, altRowPaint)
                }
                val pName = "${index + 1}. ${row.playerName}"
                val clippedName = if (pName.length > 18) pName.substring(0, 16) + "…" else pName
                canvas.drawText(clippedName, MARGIN + 4f, currentY + 11f, textPaint)

                row.results.forEachIndexed { colIndex, res ->
                    canvas.drawText(res, MARGIN + ctNameColW + colIndex * cellW + 4f, currentY + 11f, textPaint)
                }

                val ptsStr = if (row.totalPoints % 1.0 == 0.0) "${row.totalPoints.toInt()}" else "${row.totalPoints}"
                canvas.drawText(ptsStr, MARGIN + ctNameColW + numPlayers * cellW + 4f, currentY + 11f, textBoldPaint)
                canvas.drawLine(MARGIN, currentY + ctRowH, MARGIN + totalCtW, currentY + ctRowH, linePaint)
                currentY += ctRowH
            }
        }

        // Footnote
        val hasWithdrawn = rankingRows.any { it.name.endsWith("(z)") } || crosstableRows.any { it.isWithdrawn }
        if (hasWithdrawn) {
            currentY += 12f
            canvas.drawText("*(z) = vorzeitig beendet / zurückgetreten*", MARGIN, currentY + 9f, subtitlePaint)
        }

        // Footer at bottom
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            isAntiAlias = true
        }
        canvas.drawText("Erstellt mit Monatsblitz Android App", MARGIN, PAGE_HEIGHT - MARGIN + 10f, footerPaint)

        document.finishPage(page)

        // Save to cache dir
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "Turnier_${tournament.date}_${tournament.id}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun generateAndSharePdf(
        context: Context,
        tournament: Tournament,
        rankingRows: List<RankingRowData>
    ) {
        val file = generatePdf(context, tournament, rankingRows)
        sharePdf(context, file)
    }

    internal fun buildCrosstableRows(
        tournament: Tournament,
        rankingRows: List<RankingRowData>
    ): List<CrosstableRowData> {
        val playerMap = tournament.players.associateBy { it.id }
        val playerIds = tournament.playerIds
        return playerIds.mapIndexed { index, playerId ->
            val player = playerMap[playerId]
            val results = mutableListOf<String>()
            val playerPts = rankingRows.find { 
                it.name.startsWith(player?.fullName ?: "") || 
                it.name.startsWith(player?.vorname ?: "") ||
                it.name.startsWith(player?.name ?: "")
            }?.points ?: 0.0

            for (colPlayerId in playerIds) {
                if (colPlayerId == playerId) {
                    results.add("x")
                } else {
                    val game = tournament.findGame(playerId, colPlayerId, 1)
                    val res = when (game?.result) {
                        de.kindermaenner.monatsblitz.domain.model.GameResult.Win -> "1"
                        de.kindermaenner.monatsblitz.domain.model.GameResult.Loss -> "0"
                        de.kindermaenner.monatsblitz.domain.model.GameResult.Remis -> "½"
                        de.kindermaenner.monatsblitz.domain.model.GameResult.ForfeitWin -> "+"
                        de.kindermaenner.monatsblitz.domain.model.GameResult.ForfeitLoss -> "-"
                        de.kindermaenner.monatsblitz.domain.model.GameResult.ForfeitDraw -> "="
                        de.kindermaenner.monatsblitz.domain.model.GameResult.Open, null -> ""
                    }
                    results.add(res)
                }
            }
            CrosstableRowData(
                rowIndex = index,
                playerId = playerId,
                playerName = player?.fullName ?: "",
                totalPoints = playerPts,
                results = results,
                isWithdrawn = tournament.withdrawnPlayerIds.contains(playerId)
            )
        }
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Turnierergebnisse")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Turnierergebnisse teilen oder drucken").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
