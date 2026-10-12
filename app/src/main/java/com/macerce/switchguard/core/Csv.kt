package com.macerce.switchguard.core

/**
 * Excel'in doğrudan açabildiği CSV. UTF-8 BOM'lu (Türkçe karakterler bozulmasın), satırlar CRLF ile ayrılır.
 * Ondalık ayırıcısı virgül olan dillerde (Türkçe gibi) Excel alan ayırıcısı olarak noktalı virgül bekler.
 */
object Csv {
    fun separatorFor(decimalSeparator: Char): Char = if (decimalSeparator == ',') ';' else ','

    /** Ayırıcı, tırnak ya da satır sonu içeren alan tırnaklanır; içteki tırnak iki kez yazılır. */
    fun field(value: String, separator: Char): String =
        if (value.any { it == separator || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\""
        else value

    fun write(rows: List<List<String>>, separator: Char): String = buildString {
        append('\uFEFF')
        rows.forEach { row ->
            append(row.joinToString(separator.toString()) { field(it, separator) })
            append("\r\n")
        }
    }
}

/** Dışa aktarılacak geçmişin aralığı. */
enum class HistoryRange(private val days: Int?) {
    DAY(1), WEEK(7), MONTH(30), ALL(null);

    /** Aralığın başlangıcı (ms); [ALL] için null. */
    fun since(now: Long): Long? = days?.let { now - it * 24 * 3_600_000L }
}
