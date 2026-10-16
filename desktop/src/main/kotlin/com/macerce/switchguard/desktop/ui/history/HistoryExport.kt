package com.macerce.switchguard.desktop.ui.history

import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Csv
import com.macerce.switchguard.core.HistoryRange
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.desktop.i18n.Strings
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DateFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Geçmişi Excel'de açılabilen bir CSV dosyasına yazar (telefondaki `ui/HistoryExport.kt`): seçili dilde, eskiden
 * yeniye; sütunlar tarih, saat, cihaz, olay, işlem. Ayırıcı dilin ondalık işaretine göre ([Csv.separatorFor]).
 */
object HistoryExport {

    /** "switchguard-gecmis-2026-10-12.csv" gibi önerilen dosya adı. */
    fun fileName(strings: Strings, now: Long = System.currentTimeMillis()): String =
        "switchguard-" + strings["export_file_word"] + "-" + SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(now)) + ".csv"

    /** [events] (yeniden eskiye, `EventLog.export` sırası) için CSV metni. */
    fun csv(events: List<LoggedEvent>, strings: Strings): String {
        val locale = strings.locale
        val eventStrings = strings.eventStrings()
        val date = DateFormat.getDateInstance(DateFormat.SHORT, locale)
        val time = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale)
        val header = listOf("export_col_date", "export_col_time", "export_col_device", "export_col_event", "export_col_action").map { strings[it] }
        val rows = events.asReversed().map { e ->
            val action = runCatching { AlertAction.valueOf(e.action) }.getOrNull()?.let { strings[it.titleKey()] }.orEmpty()
            listOf(date.format(Date(e.time)), time.format(Date(e.time)), e.deviceName, e.text(eventStrings), action)
        }
        val separator = Csv.separatorFor(DecimalFormatSymbols.getInstance(locale).decimalSeparator)
        return Csv.write(listOf(header) + rows, separator)
    }

    /** [deviceId] null ise tüm cihazlar. Dosyayı yazar ve yazılan kayıt sayısını döndürür; IO iş parçacığında çağrılır. */
    fun write(target: Path, log: EventLog, deviceId: String?, range: HistoryRange, strings: Strings, now: Long = System.currentTimeMillis()): Int {
        val events = log.export(deviceId, range.since(now))
        Files.write(target, csv(events, strings).toByteArray(Charsets.UTF_8))
        return events.size
    }

    /** Uzantısı yoksa ".csv" eklenir (Windows kaydetme penceresi yazılanı olduğu gibi döndürür). */
    fun withExtension(path: Path): Path =
        if (path.fileName.toString().endsWith(".csv", ignoreCase = true)) path else path.resolveSibling("${path.fileName}.csv")
}

/** Windows "Farklı kaydet" penceresi (ayarlardaki yedeklemeyle aynı yaklaşım); vazgeçilirse null. Arayüz iş parçacığından. */
fun chooseCsvTarget(defaultName: String, title: String): Path? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE).apply { file = defaultName }
    dialog.isVisible = true
    return HistoryExport.withExtension(Paths.get(dialog.directory ?: return null, dialog.file ?: return null))
}
