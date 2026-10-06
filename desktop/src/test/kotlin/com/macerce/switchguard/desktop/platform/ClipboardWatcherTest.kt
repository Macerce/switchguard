package com.macerce.switchguard.desktop.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardWatcherTest {
    private var clipboard: () -> String? = { null }
    private val seen = mutableListOf<String>()
    private val watcher = ClipboardWatcher({ clipboard() }) { seen += it }

    @Test
    fun textAlreadyThereWhenWatchingStartsIsIgnored() {
        clipboard = { "eski adres" }
        watcher.poll()
        watcher.poll()

        assertEquals(emptyList<String>(), seen)
    }

    @Test
    fun newTextIsReportedOnce() {
        clipboard = { "eski adres" }
        watcher.poll()
        clipboard = { "https://127.0.0.1/?code=x" }
        watcher.poll()
        watcher.poll()

        assertEquals(listOf("https://127.0.0.1/?code=x"), seen)
    }

    @Test
    fun lockedClipboardIsIgnored() {
        watcher.poll()
        clipboard = { throw IllegalStateException("clipboard busy") }
        watcher.poll()
        clipboard = { "yeni" }
        watcher.poll()

        assertEquals(listOf("yeni"), seen)
    }

    @Test
    fun nullIsIgnored() {
        watcher.poll()
        clipboard = { null }
        watcher.poll()

        assertEquals(emptyList<String>(), seen)
    }
}
