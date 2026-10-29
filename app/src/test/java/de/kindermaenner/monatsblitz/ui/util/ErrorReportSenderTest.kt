package de.kindermaenner.monatsblitz.ui.util

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ErrorReportSenderTest {

    @Test
    fun buildReportBody_containsAllExpectedSections() {
        val body = ErrorReportSender.buildReportBody(
            actionTitle = "Turnier finalisieren",
            errorMessage = "Serverfehler 500",
            serverUrl = "https://example.com"
        )

        assertTrue(body.contains("Aktion: Turnier finalisieren"))
        assertTrue(body.contains("Fehler: Serverfehler 500"))
        assertTrue(body.contains("Server-Adresse: https://example.com"))
        assertTrue(body.contains("=== Monatsblitz Fehlerbericht ==="))
        assertTrue(body.contains("=== System-Informationen ==="))
        assertTrue(body.contains("=== Eigene Anmerkungen ==="))
    }

    @Test
    fun buildReportBody_omitsServerUrlWhenNullOrBlank() {
        val bodyWithoutUrl = ErrorReportSender.buildReportBody(
            actionTitle = "Spieler laden",
            errorMessage = "Timeout",
            serverUrl = null
        )

        assertTrue(!bodyWithoutUrl.contains("Server-Adresse:"))

        val bodyWithBlankUrl = ErrorReportSender.buildReportBody(
            actionTitle = "Spieler laden",
            errorMessage = "Timeout",
            serverUrl = "   "
        )

        assertTrue(!bodyWithBlankUrl.contains("Server-Adresse:"))
    }

    @Test
    fun copyToClipboard_copiesTextSuccessfully() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ErrorReportSender.copyToClipboard(context, "Test Fehlerbericht")

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        assertNotNull(clip)
        assertEquals(1, clip?.itemCount)
        assertEquals("Test Fehlerbericht", clip?.getItemAt(0)?.text?.toString())
    }

    @Test
    fun sendErrorReport_copiesToClipboardWhenNoActivityAvailable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // In Robolectric without email intent receiver, sendErrorReport catches and copies to clipboard
        ErrorReportSender.sendErrorReport(
            context = context,
            actionTitle = "Turnier starten",
            errorMessage = "Netzwerkfehler",
            serverUrl = "https://example.com"
        )

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        assertNotNull(clip)
        val text = clip?.getItemAt(0)?.text?.toString() ?: ""
        assertTrue(text.contains("Netzwerkfehler"))
    }
}
