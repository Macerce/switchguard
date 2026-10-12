package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvTest {
    @Test fun `duz alan oldugu gibi kalir`() = assertEquals("Rögar", Csv.field("Rögar", ';'))
    @Test fun `ayirici iceren alan tirnaklanir`() = assertEquals("\"a;b\"", Csv.field("a;b", ';'))
    @Test fun `tirnak iki kez yazilir`() = assertEquals("\"5\"\" boru\"", Csv.field("5\" boru", ','))
    @Test fun `satir sonu iceren alan tirnaklanir`() = assertEquals("\"a\nb\"", Csv.field("a\nb", ','))

    @Test
    fun `dosya BOM ile baslar, satirlar CRLF ile ayrilir`() {
        val text = Csv.write(listOf(listOf("Tarih", "Cihaz"), listOf("12.10.2026", "Rögar; K1")), ';')
        assertTrue(text.startsWith("\uFEFF"))
        assertEquals("\uFEFFTarih;Cihaz\r\n12.10.2026;\"Rögar; K1\"\r\n", text)
    }

    @Test
    fun `ondalik virgullu dillerde noktali virgul`() {
        assertEquals(';', Csv.separatorFor(decimalSeparator = ','))
        assertEquals(',', Csv.separatorFor(decimalSeparator = '.'))
    }

    @Test
    fun `aralik baslangici`() {
        val day = 24 * 3_600_000L
        val now = 100 * day
        assertEquals(now - day, HistoryRange.DAY.since(now))
        assertEquals(now - 7 * day, HistoryRange.WEEK.since(now))
        assertEquals(now - 30 * day, HistoryRange.MONTH.since(now))
        assertNull(HistoryRange.ALL.since(now))
    }

    @Test fun `formul gibi baslayan hucre metin olarak yazilir`() {
        assertEquals("'=HYPERLINK(1)", Csv.field("=HYPERLINK(1)", ';'))
        assertEquals("'+90 555", Csv.field("+90 555", ';'))
        assertEquals("'@x", Csv.field("@x", ';'))
        assertEquals("Rögar", Csv.field("Rögar", ';'))
    }
}
