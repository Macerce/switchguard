package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.str
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Hatırlatma seçenekleri (dk); 0 = kapalı. */
private val REMINDER_CHOICES = listOf(0, 10, 30, 60)

// ---------------------------------------------------------------- Alarm

/**
 * Ayarlar → Alarm (telefondaki AlarmPage): alarm ve bildirim sesi, ses düzeyi; alarm çalarken hatırlatma ve ikinci
 * kişiye haber verme (Telegram sayfasını [open] açar); test alarmı penceresi. [testAlarm] testlerde sahtedir
 * (bilgisayarda gerçekten alarm çalmasın).
 */
@Composable
fun AlarmPage(
    graph: AppGraph,
    strings: Strings,
    open: (SettingsPage) -> Unit,
    testAlarm: (silent: Boolean, delayMs: Long) -> Unit = graph.engine::testAlarm,
) {
    val store = graph.store
    val version by store.version.collectAsState()
    val alarmSound = remember(version) { store.alarmSoundUri }
    val notifySound = remember(version) { store.notifySoundUri }
    val volume = remember(version) { store.alarmVolume }
    val reminder = remember(version) { store.reminderMin }
    val escalation = remember(version, strings) { escalationSummary(store, strings) }
    var dialog by remember { mutableStateOf<String?>(null) } // "reminder" / "test"
    // Gecikmeli test alarmı kuruldu: satırda kısa süre "N saniye sonra çalacak" yazar (telefondaki Toast).
    var scheduled by remember { mutableStateOf(0) }
    LaunchedEffect(scheduled) {
        if (scheduled > 0) {
            delay(TEST_DELAY_SEC * 1000L)
            scheduled = 0
        }
    }

    SectionTitle(str("settings_sound"))
    SectionCard {
        SoundRows(graph, strings, alarmSound, notifySound)
        RowDivider()
        VolumeRow(volume) { store.alarmVolume = it }
    }

    SectionTitle(str("settings_when_alarm"))
    SectionCard {
        InfoRow(
            Icons.Rounded.Snooze, str("reminder"), reminderLabel(strings, reminder),
            Modifier.clickable { dialog = "reminder" },
        )
        RowDivider()
        InfoRow(
            Icons.AutoMirrored.Rounded.Send, str("tg_title"), escalation,
            Modifier.clickable { open(SettingsPage.TELEGRAM) },
        ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    SectionTitle(str("settings_try_it"))
    SectionCard {
        InfoRow(
            Icons.Rounded.NotificationsActive, str("test_alarm"),
            if (scheduled > 0) str("test_alarm_scheduled", TEST_DELAY_SEC) else str("test_alarm_desc"),
            Modifier.clickable { dialog = "test" },
        )
    }

    when (dialog) {
        "reminder" -> ChoiceDialog(
            str("reminder"), str("reminder_desc"), REMINDER_CHOICES, reminder, { reminderLabel(strings, it) },
            onPick = { store.reminderMin = it }, onDismiss = { dialog = null },
        )
        "test" -> TestAlarmDialog(
            onStart = { silent, delayMs ->
                testAlarm(silent, delayMs)
                if (delayMs > 0) scheduled++
            },
            onDismiss = { dialog = null },
        )
    }
}

/**
 * Ses düzeyi (telefondaki gibi simge + başlık, altında kaydırıcı). Sürüklerken yalnızca ekrandaki değer değişir;
 * kalıcı kayıt bırakınca bir kez yapılır: her sürükleme adımında ayar dosyası yazılmasın.
 */
@Composable
private fun VolumeRow(value: Float, onFinished: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                str("alarm_volume", (local * 100).roundToInt()),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        Slider(
            value = local,
            onValueChange = { local = it.coerceIn(0.05f, 1f) },
            onValueChangeFinished = { onFinished(local) },
            valueRange = 0.05f..1f,
            modifier = Modifier.padding(start = 40.dp).testTag("alarm-volume"),
        )
    }
}

// ---------------------------------------------------------------- Sessiz saatler

/** Ayarlar → Sessiz saatler: aç/kapa, başlangıç ve bitiş (saat penceresi), kritik cihaz açıklaması. */
@Composable
fun QuietPage(store: Store, strings: Strings) {
    val version by store.version.collectAsState()
    val q = remember(version) { store.quietHours }
    var picking by remember { mutableStateOf<String?>(null) } // "start" / "end"

    PageIntro(str("quiet_hours_desc"))
    SectionCard(Modifier.padding(top = 12.dp)) {
        SwitchInfoRow(Icons.Rounded.Bedtime, str("quiet_hours"), checked = q.enabled) {
            store.quietHours = store.quietHours.copy(enabled = it)
        }
        if (q.enabled) {
            RowDivider()
            InfoRow(Icons.Rounded.Schedule, str("quiet_start"), formatMinute(strings, q.startMinute), Modifier.clickable { picking = "start" })
            RowDivider()
            InfoRow(Icons.Rounded.Schedule, str("quiet_end"), formatMinute(strings, q.endMinute), Modifier.clickable { picking = "end" })
        }
    }
    PageIntro(str("settings_quiet_critical"), Modifier.padding(top = 8.dp))

    picking?.let { which ->
        val start = which == "start"
        TimeDialog(
            title = str(if (start) "quiet_start" else "quiet_end"),
            initial = if (start) q.startMinute else q.endMinute,
            locale = strings.locale,
            onPick = { m ->
                picking = null
                store.quietHours = store.quietHours.let { if (start) it.copy(startMinute = m) else it.copy(endMinute = m) }
            },
            onDismiss = { picking = null },
        )
    }
}

// ---------------------------------------------------------------- Günlük özet

/**
 * Ayarlar → Günlük özet: aç/kapa, gönderim saati, bugünün özetini hemen göster. Gönderimi motor yapar (her tick'te
 * saate bakar), ayar değişince başka bir şey çağırmak gerekmez. [preview] motorun `previewSummary`'si (testte sahte);
 * sonucu motor iş parçacığında verir, kayıt yoksa satırda açıklama görünür.
 */
@Composable
fun SummaryPage(store: Store, strings: Strings, preview: ((Boolean) -> Unit) -> Unit) {
    val version by store.version.collectAsState()
    val enabled = remember(version) { store.dailySummaryEnabled }
    val minute = remember(version) { store.dailySummaryMinute }
    var picking by remember { mutableStateOf(false) }
    var noData by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PageIntro(str("daily_summary_desc"))
    SectionCard(Modifier.padding(top = 12.dp)) {
        SwitchInfoRow(Icons.Rounded.Summarize, str("daily_summary"), checked = enabled) { store.dailySummaryEnabled = it }
        if (enabled) {
            RowDivider()
            InfoRow(Icons.Rounded.Schedule, str("daily_summary_time"), formatMinute(strings, minute), Modifier.clickable { picking = true })
            RowDivider()
            InfoRow(
                Icons.Rounded.Visibility, str("daily_summary_preview"), if (noData) str("summary_no_data") else null,
                Modifier.clickable {
                    scope.launch {
                        val ok = suspendCancellableCoroutine { c -> preview { c.resume(it) } }
                        noData = !ok
                    }
                },
            )
        }
    }
    PageIntro(str("desktop_summary_note"), Modifier.padding(top = 8.dp))

    if (picking) {
        TimeDialog(
            title = str("daily_summary_time"),
            initial = minute,
            locale = strings.locale,
            onPick = { picking = false; store.dailySummaryMinute = it },
            onDismiss = { picking = false },
        )
    }
}
