package com.macerce.switchguard.ui

import android.content.Context
import android.net.Uri
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Csv
import com.macerce.switchguard.core.HistoryRange
import com.macerce.switchguard.data.EventLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Geçmişi Excel'de açılabilen bir CSV dosyasına yazar (seçili dilde, eskiden yeniye). */
object HistoryExport {

    /** "switchguard-gecmis-2026-10-12.csv" gibi önerilen dosya adı. */
    fun fileName(context: Context): String =
        "switchguard-" + context.getString(R.string.export_file_word) + "-" +
            SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) + ".csv"

    /** [deviceId] null ise tüm cihazlar. Yazılan kayıt sayısını döndürür. */
    suspend fun write(context: Context, uri: Uri, deviceId: String?, range: HistoryRange): Int = withContext(Dispatchers.IO) {
        val locale = context.resources.configuration.locales[0]
        val strings = EventLog.strings(context)
        val events = EventLog.get(context).export(deviceId, range.since(System.currentTimeMillis()))
        val date = DateFormat.getDateInstance(DateFormat.SHORT, locale)
        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale)
        val header = listOf(
            R.string.export_col_date, R.string.export_col_time, R.string.export_col_device,
            R.string.export_col_event, R.string.export_col_action,
        ).map(context::getString)
        val rows = events.asReversed().map { e ->
            val action = runCatching { AlertAction.valueOf(e.action) }.getOrNull()?.let { context.getString(it.title()) }.orEmpty()
            listOf(date.format(Date(e.time)), time.format(Date(e.time)), e.deviceName, e.text(strings), action)
        }
        val separator = Csv.separatorFor(DecimalFormatSymbols.getInstance(locale).decimalSeparator)
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("no output stream")
        out.use { it.write(Csv.write(listOf(header) + rows, separator).toByteArray(Charsets.UTF_8)) }
        events.size
    }
}
