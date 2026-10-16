package com.macerce.switchguard.desktop.ui

import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.ExperimentalTestApi
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

/** Geçmiş sekmesinin görsel örnekleri (açık + koyu, TR; bugün, dün ve daha eski olaylar): `build/snapshots/history-*.png`. */
@OptIn(ExperimentalTestApi::class)
class HistorySnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var graph: AppGraph
    private val tr = Strings.load("tr")

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "tr"
        graph.store.enableDemo(tr.demoNames())
    }

    @After
    fun tearDown() = graph.shutdown()

    @Test
    fun mixedEvents() {
        val now = System.currentTimeMillis()
        val h = 3_600_000L
        val log = graph.eventLog
        val pumps = tr.demoNames().pumps
        val light = tr.demoNames().light
        log.add("", "SwitchGuard", EventKind.SYSTEM, "Bilgisayar 42 dk uykudaydı — bu sürede kısa olaylar kaçmış olabilir.", "", time = now - 4 * 24 * h)
        log.add(Demo.LIGHT, light, EventType.TURNED_ON.name, "$light: KAPALI → AÇIK", "ALARM", time = now - 26 * h)
        log.add(Demo.LIGHT, light, EventKind.REMINDER, "Hatırlatma: $light hâlâ AÇIK (30 dk)", "ALARM", time = now - 25 * h)
        log.add("", "SwitchGuard", EventKind.ESCALATION, "Alarm 10 dk kapatılmadı: 1 kişiye Telegram ile haber verildi", "", time = now - 25 * h + 60_000)
        log.add("", "SwitchGuard", EventKind.MONITOR_GAP, "İzleme çalışmadı: 02:10 – 02:40 (30 dk)", "NOTIFY", time = now - 3 * h)
        log.add("", "SwitchGuard", EventKind.MONITOR_BLIND, "İzleme çalışmıyor (başlangıç 02:10): cihazların durumu alınamıyor.", "ALARM", time = now - 3 * h + 1000)
        log.add(Demo.PUMPS, pumps, EventType.WENT_OFFLINE.name, "$pumps: ÇEVRİMİÇİ → ÇEVRİMDIŞI", "ALARM", time = now - 2 * h)
        log.add(Demo.PUMPS, pumps, EventType.CAME_ONLINE.name, "$pumps: ÇEVRİMDIŞI → ÇEVRİMİÇİ", "NOTIFY", time = now - 2 * h + 60_000)
        log.add(Demo.PUMPS, pumps, EventKind.AUTOMATION, "Otomasyon “Pompa durdu” ($pumps)", "ALARM", time = now - h)
        log.add("", "SwitchGuard", EventKind.DISMISSED, "Alarm kapatıldı · 2 olay · tepki süresi 1 dk 19 sn", "", time = now - h + 80_000)
        log.add(Demo.LIGHT, light, EventKind.SHORT_DROP, "$light: kısa bağlantı kopması yok sayıldı", "IGNORE", time = now - 30 * 60_000L)
        log.add(Demo.LIGHT, light, EventKind.USER, "$light: AÇIK → KAPALI", "", time = now - 10 * 60_000L)
        InteractiveSnapshots.render("history-export-light", 1100, 760, content = { HistoryScreen(graph) }) {
            waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("history-list")).fetchSemanticsNodes().isNotEmpty() }
            onNodeWithTag("history-export").performClick()
        }
        for (dark in listOf(false, true)) {
            val out = SettledSnapshots.render("history-" + if (dark) "dark" else "light", 1100, 760, dark) { HistoryScreen(graph) }
            assertTrue(Files.size(out) > 1000)
            SettledSnapshots.render("history-tall-" + if (dark) "dark" else "light", 1100, 1700, dark) { HistoryScreen(graph) }
        }
    }

    @Test
    fun empty() {
        SettledSnapshots.render("history-empty-light", 1100, 760, false) { HistoryScreen(graph) }
    }
}
