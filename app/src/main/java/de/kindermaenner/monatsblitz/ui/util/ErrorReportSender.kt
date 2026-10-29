package de.kindermaenner.monatsblitz.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import de.kindermaenner.monatsblitz.BuildConfig

object ErrorReportSender {

    const val DEVELOPER_EMAIL = "monatsblitz@kindermaenner.de"

    fun buildReportBody(
        actionTitle: String,
        errorMessage: String,
        serverUrl: String? = null
    ): String {
        return buildString {
            appendLine("=== Monatsblitz Fehlerbericht ===")
            appendLine("Aktion: $actionTitle")
            appendLine("Fehler: $errorMessage")
            if (!serverUrl.isNullOrBlank()) {
                appendLine("Server-Adresse: $serverUrl")
            }
            appendLine()
            appendLine("=== System-Informationen ===")
            appendLine("App-Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android-Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Gerät: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("=== Eigene Anmerkungen ===")
            appendLine("(Was hast du getan, als der Fehler aufgetreten ist?)")
            appendLine()
        }
    }

    fun sendErrorReport(
        context: Context,
        actionTitle: String,
        errorMessage: String,
        serverUrl: String? = null
    ) {
        val body = buildReportBody(actionTitle, errorMessage, serverUrl)
        val subject = "[Monatsblitz] Fehlerbericht: $actionTitle"

        val mailtoUri = Uri.parse("mailto:$DEVELOPER_EMAIL").buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()

        val intent = Intent(Intent.ACTION_SENDTO, mailtoUri)
        try {
            context.startActivity(Intent.createChooser(intent, "Fehlerbericht senden"))
        } catch (_: Exception) {
            copyToClipboard(context, body)
            Toast.makeText(context, "Keine E-Mail-App gefunden. Bericht wurde in die Zwischenablage kopiert.", Toast.LENGTH_LONG).show()
        }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Monatsblitz Fehlerbericht", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "In Zwischenablage kopiert", Toast.LENGTH_SHORT).show()
    }
}
