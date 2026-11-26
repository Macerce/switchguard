package com.macerce.switchguard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.macerce.switchguard.R
import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DeviceUsage
import com.macerce.switchguard.core.RuleToDevice
import com.macerce.switchguard.core.TimerEntry
import com.macerce.switchguard.data.Store
import android.widget.Toast
import kotlinx.coroutines.launch

/** Cihaz sayfasındaki "Bu cihazı neler kontrol ediyor?" satırı; dokununca sorgu sayfası açılır. */
@Composable
fun DeviceUsageRow(device: DeviceSnapshot, onClosed: () -> Unit = {}) {
    var open by rememberSaveable { mutableStateOf(false) }
    SectionCard {
        InfoRow(
            Icons.Rounded.Search,
            stringResource(R.string.usage_title),
            stringResource(R.string.usage_desc),
            Modifier.clickable { open = true },
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (open) DeviceUsageDialog(device) { open = false; onClosed() }
}

/**
 * Cihazı kontrol eden her şey tek sayfada: cihazın kendi zamanlayıcıları (eWeLink; buluttan o an okunur, kimin
 * yazdığıyla birlikte) ve bu telefondaki kurallar, sahneler, döngüler, gruplar.
 */
@Composable
fun DeviceUsageDialog(device: DeviceSnapshot, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val store = remember { Store.get(context) }
    val automations = remember { store.automations }
    val report = remember {
        DeviceUsage.find(device.id, automations, store.scenes, store.cycles, store.groups)
    }
    val devices = remember { store.snapshotsForRules }
    val ewelink = device.cloud == Cloud.EWELINK
    var timers by remember { mutableStateOf<List<TimerEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(device.id) {
        if (!ewelink) return@LaunchedEffect
        Actions.loadTimers(context, device.id)
            .onSuccess { timers = it }
            .onFailure { error = if (it is ApiException) "eWeLink ${it.code}" else resources.getString(R.string.state_no_internet) }
    }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    // Silinmek üzere seçilen zamanlayıcı: (kimlik, satır metni).
    var confirm by remember { mutableStateOf<Pair<String, String>?>(null) }
    confirm?.let { (id, label) ->
        AlertDialog(
            onDismissRequest = { if (!busy) confirm = null },
            title = { Text(stringResource(R.string.usage_timer_delete_title)) },
            text = { Text(stringResource(R.string.usage_timer_delete_text, label)) },
            confirmButton = {
                TextButton(enabled = !busy, modifier = Modifier.tvFocusRing(), onClick = {
                    busy = true
                    scope.launch {
                        Actions.saveTimer(context, device, id, null)
                            .onSuccess { fresh ->
                                timers = fresh
                                // Kuralın bu cihazda zamanlayıcısı kalmadıysa "cihaza yazılı" kaydından düş.
                                DeviceUsage.timerRule(id, automations)?.let { rule ->
                                    val left = fresh.any { e -> RuleToDevice.owns(e.timer?.id ?: e.raw.optString("mId"), rule.id) }
                                    if (!left) store.setRuleDevices(rule.id, store.ruleDevices(rule.id) - device.id)
                                }
                                Toast.makeText(context, resources.getString(R.string.usage_timer_deleted), Toast.LENGTH_SHORT).show()
                            }
                            .onFailure { Toast.makeText(context, resources.getString(R.string.control_failed, it.message ?: ""), Toast.LENGTH_LONG).show() }
                        busy = false
                        confirm = null
                    }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !busy, modifier = Modifier.tvFocusRing(), onClick = { confirm = null }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                AppTopBar(stringResource(R.string.usage_title), navigationIcon = { BackButton(onDismiss) }, small = true)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
                    Text(
                        device.name,
                        Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    // ---- Cihazın kendisinde (telefon kapalıyken de çalışır)
                    SectionTitle(stringResource(R.string.usage_device_timers))
                    SectionCard {
                        val list = timers
                        when {
                            !ewelink -> Hint(stringResource(R.string.usage_timers_unsupported))
                            error != null -> Text(
                                stringResource(R.string.timers_load_failed, error ?: ""), Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.error,
                            )
                            list == null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                            list.isEmpty() -> Hint(stringResource(R.string.usage_timers_none))
                            else -> list.forEachIndexed { i, e ->
                                if (i > 0) RowDivider(inset = 0.dp)
                                val t = e.timer
                                val id = t?.id ?: e.raw.optString("mId")
                                val owner = DeviceUsage.timerRule(id, automations)
                                val source = when {
                                    owner != null -> stringResource(R.string.usage_timer_rule, owner.name)
                                    DeviceUsage.isAppTimer(id) -> stringResource(R.string.usage_timer_orphan)
                                    id.startsWith(DeviceUsage.MANUAL_PREFIX) -> stringResource(R.string.usage_timer_manual)
                                    else -> stringResource(R.string.usage_timer_ewelink)
                                }
                                if (t == null) {
                                    InfoRow(Icons.Rounded.Schedule, stringResource(R.string.timer_other), source)
                                } else {
                                    val what = formatMinute(context, t.hour * 60 + t.minute) + " · " +
                                        (if (device.isMultiChannel) stringResource(R.string.channel_n, t.channel + 1) + " " else "") +
                                        stringResource(if (t.on) R.string.auto_action_turn_on else R.string.auto_action_turn_off)
                                    val off = if (t.enabled) "" else " · " + stringResource(R.string.state_off_title)
                                    val days = daysLabel(t.days)
                                    InfoRow(
                                        if (owner != null || DeviceUsage.isAppTimer(id)) Icons.Rounded.Memory else Icons.Rounded.Schedule,
                                        what + off,
                                        days + "\n" + source,
                                    ) {
                                        IconButton(enabled = !busy, modifier = Modifier.tvFocusRing(), onClick = { confirm = id to "$what · $days" }) {
                                            Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---- Bu telefonda (izleme açıkken çalışır)
                    if (report.rules.isEmpty() && report.scenes.isEmpty() && report.cycles.isEmpty()) {
                        SectionTitle(stringResource(R.string.usage_rules))
                        SectionCard { Hint(stringResource(R.string.usage_none)) }
                    }
                    if (report.rules.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.usage_rules))
                        SectionCard {
                            report.rules.forEachIndexed { i, u ->
                                if (i > 0) RowDivider(inset = 0.dp)
                                val (whenText, thenText) = describe(u.rule, devices)
                                val role = listOfNotNull(
                                    if (u.switches) stringResource(R.string.usage_switches) else null,
                                    if (u.watches) stringResource(R.string.usage_watches) else null,
                                ).joinToString(" · ")
                                val off = if (u.rule.enabled) "" else " · " + stringResource(R.string.state_off_title)
                                InfoRow(
                                    Icons.Rounded.Rule,
                                    u.rule.name + off,
                                    "$role\n${stringResource(R.string.auto_if)} $whenText\n${stringResource(R.string.auto_then)} $thenText",
                                )
                            }
                        }
                    }
                    if (report.scenes.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.scenes_title))
                        SectionCard {
                            report.scenes.forEachIndexed { i, (scene, acts) ->
                                if (i > 0) RowDivider(inset = 0.dp)
                                InfoRow(
                                    Icons.Rounded.AutoAwesome, scene.name,
                                    acts.joinToString(", ") { a ->
                                        (if (device.isMultiChannel) resources.getString(R.string.channel_n, a.channel + 1) + " " else "") +
                                            resources.getString(if (a.on) R.string.auto_action_turn_on else R.string.auto_action_turn_off)
                                    }.takeIf { acts.all { it.deviceId == device.id } },
                                )
                            }
                        }
                    }
                    if (report.cycles.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.cycles_title))
                        SectionCard {
                            report.cycles.forEachIndexed { i, c ->
                                if (i > 0) RowDivider(inset = 0.dp)
                                InfoRow(Icons.Rounded.Loop, c.name + if (c.enabled) "" else " · " + stringResource(R.string.state_off_title))
                            }
                        }
                    }
                    if (report.groups.isNotEmpty()) {
                        SectionTitle(stringResource(R.string.usage_groups))
                        SectionCard {
                            report.groups.forEachIndexed { i, g ->
                                if (i > 0) RowDivider(inset = 0.dp)
                                InfoRow(Icons.Rounded.Groups, g.name, stringResource(R.string.usage_group_desc))
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.usage_footer),
                        Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
