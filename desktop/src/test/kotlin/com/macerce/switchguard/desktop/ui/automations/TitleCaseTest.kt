package com.macerce.switchguard.desktop.ui.automations

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleCaseTest {
    @Test
    fun everyWordStartsWithACapitalTurkishStyle() {
        assertEquals("Açılınca", titleCase("açılınca", "tr"))
        assertEquals("Şu Süre Açık Kalınca", titleCase("şu süre açık kalınca:", "tr"))
        assertEquals("Güç Şunun Üstüne Çıkınca", titleCase("güç şunun üstüne çıkınca:", "tr"))
        assertEquals("İnternet İlk İş", titleCase("internet ilk iş", "tr")) // Türkçe: i → İ
    }

    @Test
    fun englishToo() {
        assertEquals("Turns On", titleCase("turns on", "en"))
        assertEquals("Stays On For", titleCase("stays on for", "en"))
        assertEquals("Internet", titleCase("internet", "en")) // İngilizcede i → I
    }
}
