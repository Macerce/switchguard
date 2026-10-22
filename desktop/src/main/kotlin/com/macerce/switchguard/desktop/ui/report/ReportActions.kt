package com.macerce.switchguard.desktop.ui.report

import androidx.compose.material3.SnackbarHostState
import com.macerce.switchguard.api.TelegramClient
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.ReportText
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.report.ReportPdf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

/** Rapor penceresinin düğmeleri: PDF olarak kaydet (telefondaki `Actions.writeReportPdf`), Telegram'a gönder. */
object ReportActions {
    /** "switchguard-haftalik-rapor.pdf" gibi önerilen ad. */
    fun fileName(doc: ReportDoc): String =
        "switchguard-" + doc.title.lowercase().replace(Regex("[^\\p{L}\\p{Nd}]+"), "-").trim('-') + ".pdf"

    fun writePdf(strings: Strings, doc: ReportDoc, target: Path) {
        val pdf = ReportPdf.render(strings, doc, "SwitchGuard · " + doc.title)
        try {
            Files.newOutputStream(target).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }

    /** Kaydetme penceresini açar, PDF'i arka planda yazar; sonuç [snackbar]'da. */
    fun savePdf(scope: CoroutineScope, strings: Strings, doc: ReportDoc, snackbar: SnackbarHostState?) {
        val target = chooseSaveTarget(fileName(doc), strings["report_save_pdf"], "pdf") ?: return
        scope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { writePdf(strings, doc, target) }.onFailure { Log.w(TAG, "pdf failed", it) }.isSuccess }
            snackbar?.showSnackbar(strings[if (ok) "energy_report_done" else "energy_report_failed"])
            if (ok) openFile(target)
        }
    }

    fun sendTelegram(scope: CoroutineScope, store: Store, strings: Strings, doc: ReportDoc, snackbar: SnackbarHostState?) {
        val text = ReportText.telegramHtml(doc, strings["report_actions_title"], strings["report_what_to_do_short"])
        val token = store.telegramToken
        val recipients = store.telegramRecipients
        scope.launch {
            val sent = withContext(Dispatchers.IO) { recipients.count { r -> runCatching { TelegramClient.send(token, r.id, text, html = true) }.isSuccess } }
            snackbar?.showSnackbar(strings[if (sent > 0) "tg_test_sent" else "energy_report_failed"])
        }
    }

    /** Kaydedilen dosyayı varsayılan uygulamayla açar (PDF görüntüleyici, Excel); açılamazsa sessiz geçer. */
    fun openFile(path: Path) {
        runCatching { if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(path.toFile()) }
    }

    private const val TAG = "ReportActions"
}
