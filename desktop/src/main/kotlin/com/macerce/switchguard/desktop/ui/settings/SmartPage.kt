package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.macerce.switchguard.desktop.ui.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.Watchdog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.PowerSource
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.StatusColors
import com.macerce.switchguard.desktop.ui.automations.LocationSetup
import com.macerce.switchguard.desktop.ui.energy.EnergyFormat
import com.macerce.switchguard.desktop.ui.report.ReportActions
import com.macerce.switchguard.desktop.ui.report.ReportWindow
import com.macerce.switchguard.desktop.ui.str
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.coroutines.resume

/**
 * Uyarılar → Akıllı uyarılar (telefon 2.15 `SmartPage`): elektrik kesintisi, evde/dışarıda ve tatil, tüketim (anormal
 * gün, tarife, aylık bütçe), konum, haftalık rapor ve Telegram'ın ek işleri (komutlar, aç/kapa izni, kademeli haber,
 * bildirimlerin iletilmesi, bekçi). Masaüstü farkı: "şarjdan kesinti" yalnızca pili olan bilgisayarda (dizüstü) görünür.
 */
@Composable
fun SmartPage(graph: AppGraph, strings: Strings, snackbar: SnackbarHostState? = null, onTelegram: () -> Unit) {
    val store = graph.store
    val version by store.version.collectAsState()
    val s = remember(version) { store }
    val scope = rememberCoroutineScope()
    var dialog by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<ReportDoc?>(null) }
    val locale = strings.locale

    PageIntro(str("smart_desc"))

    SectionTitle(str("smart_outage_section"))
    SectionCard {
        SwitchInfoRow(Icons.Rounded.ElectricBolt, str("smart_outage"), str("smart_outage_desc"), checked = s.outageDetect) { store.outageDetect = it }
        RowDivider()
        SwitchInfoRow(Icons.Rounded.Home, str("desktop_smart_same_power"), str("desktop_smart_same_power_desc"), checked = s.samePower) {
            store.samePower = it
        }
        if (remember { PowerSource.hasBattery }) {
            RowDivider()
            SwitchInfoRow(Icons.Rounded.BatteryChargingFull, str("desktop_smart_ac"), str("desktop_smart_ac_desc"), checked = s.chargerOutage) {
                store.chargerOutage = it
            }
        }
    }

    SectionTitle(str("presence_section"))
    SectionCard {
        SwitchInfoRow(Icons.Rounded.Home, str("presence_soften"), str("presence_soften_desc"), checked = s.softenAtHome) { store.softenAtHome = it }
        RowDivider()
        val onVacation = s.onVacation()
        InfoRow(
            Icons.Rounded.BeachAccess,
            str("vacation_title"),
            if (onVacation) strings.format("vacation_until", vacationDay(s.vacationUntil, strings)) else str("vacation_desc"),
            Modifier.clickable { dialog = "vacation" }.testTag("vacation"),
        ) {
            if (onVacation) TextButton(onClick = { graph.engine.setVacation(0) }) { Text(str("vacation_end_now")) }
        }
    }

    SectionTitle(str("smart_energy_section"))
    SectionCard {
        SwitchInfoRow(Icons.AutoMirrored.Rounded.TrendingUp, str("smart_anomaly"), str("smart_anomaly_desc"), checked = s.energyAnomaly) { store.energyAnomaly = it }
        RowDivider()
        InfoRow(
            Icons.Rounded.Payments, str("tariff_title"),
            if (s.threeZone) str("tariff_three_short") else if (s.energyPrice > 0) EnergyFormat.money(s.energyPrice, locale) + " / kWh" else str("tariff_none"),
            Modifier.clickable { dialog = "tariff" }.testTag("tariff"),
        )
        RowDivider()
        val budget = s.monthlyBudget
        InfoRow(
            Icons.Rounded.Savings, str("smart_budget"),
            if (budget > 0) strings.format("smart_budget_on", EnergyFormat.money(budget, locale)) else str("blind_alert_off"),
            Modifier.clickable { dialog = "budget" }.testTag("budget"),
        )
    }

    SectionTitle(str("location_title"))
    SectionCard {
        var locationVersion by remember { mutableStateOf(0) }
        key(locationVersion) {
            LocationSetup(store, strings, onChanged = { locationVersion++ }, modifier = Modifier.padding(16.dp))
        }
    }
    PageIntro(str("desktop_location_desc"))

    SectionTitle(str("smart_weekly_section"))
    SectionCard {
        SwitchInfoRow(Icons.Rounded.CalendarMonth, str("smart_weekly"), str("smart_weekly_desc"), checked = s.weeklyReport) { store.weeklyReport = it }
        RowDivider()
        InfoRow(
            Icons.Rounded.Visibility, str("smart_weekly_preview"), null,
            Modifier.clickable {
                scope.launch { preview = suspendCancellableCoroutine { c -> graph.engine.weeklyDoc { c.resume(it) } } }
            }.testTag("weekly-preview"),
        )
    }

    SectionTitle(str("smart_tg_section"))
    if (!s.escalationReady) {
        SectionCard {
            InfoRow(Icons.AutoMirrored.Rounded.Send, str("tg_title"), str("smart_tg_needs_setup"), Modifier.clickable(onClick = onTelegram))
        }
    } else {
        SectionCard {
            SwitchInfoRow(Icons.Rounded.Keyboard, str("smart_tg_commands"), str("smart_tg_commands_desc"), checked = s.telegramCommands) { store.telegramCommands = it }
            if (s.telegramCommands) {
                RowDivider()
                SwitchInfoRow(Icons.Rounded.PowerSettingsNew, str("smart_tg_control"), str("smart_tg_control_desc"), checked = s.telegramControl) {
                    store.telegramControl = it
                }
            }
            RowDivider()
            InfoRow(Icons.Rounded.Stairs, str("stages_title"), stagesSummary(s, strings), Modifier.clickable { dialog = "stages" }.testTag("stages"))
            RowDivider()
            SwitchInfoRow(Icons.AutoMirrored.Rounded.Send, str("smart_tg_forward"), str("smart_tg_forward_desc"), checked = s.telegramForward) {
                store.telegramForward = it
            }
            RowDivider()
            val current = if (s.watchdogMode == Store.WATCHDOG_WATCH) -s.watchdogMin else s.watchdogMode
            InfoRow(Icons.Rounded.MonitorHeart, str("smart_wd"), watchdogLabel(current, strings), Modifier.clickable { dialog = "watchdog" }.testTag("watchdog"))
        }
        PageIntro(str("smart_tg_commands_list"))
    }

    val close = { dialog = null }
    when (dialog) {
        "vacation" -> VacationDatePicker(s.vacationUntil, strings, onPick = { graph.engine.setVacation(it) }, onDismiss = close)
        "tariff" -> TariffDialog(store, strings, close)
        "budget" -> BudgetDialog(store, strings, close)
        "stages" -> StagesDialog(store, strings, close)
        "watchdog" -> {
            val modes = listOf(Store.WATCHDOG_OFF, Store.WATCHDOG_SEND) + Watchdog.WATCH_MINUTES.map { -it }
            val current = if (s.watchdogMode == Store.WATCHDOG_WATCH) -s.watchdogMin else s.watchdogMode
            ChoiceDialog(
                str("smart_wd"), str("smart_wd_desc"), modes, current, { watchdogLabel(it, strings) },
                onPick = {
                    if (it < 0) {
                        store.watchdogMin = -it
                        store.watchdogMode = Store.WATCHDOG_WATCH
                    } else store.watchdogMode = it
                },
                onDismiss = close,
            )
        }
    }
    preview?.let { doc ->
        ReportWindow(
            doc, onDismiss = { preview = null },
            onPdf = { ReportActions.savePdf(scope, strings, doc, snackbar) },
            onTelegram = if (s.escalationReady) ({ ReportActions.sendTelegram(scope, store, strings, doc, snackbar) }) else null,
        )
    }
}

/** Tatilin son günü (bitiş anı ertesi günün başı). */
private fun vacationDay(until: Long, strings: Strings): String = SimpleDateFormat("d MMM", strings.locale).format(Date(until - 1))

/** "Ahmet 5 dk → Ayşe 15 dk" */
private fun stagesSummary(s: Store, strings: Strings): String {
    val list = s.telegramRecipients.sortedBy { s.stageFor(it.id) }
    if (list.isEmpty()) return strings["blind_alert_off"]
    val min = strings["min_short"]
    return list.joinToString(" → ") { "${it.name} ${s.stageFor(it.id)} $min" }
}

/** Bekçi seçeneği: 0 kapalı, 1 sinyal ver, eksi değer = diğerini o kadar dakikayla denetle. */
private fun watchdogLabel(mode: Int, strings: Strings): String = when {
    mode == Store.WATCHDOG_OFF -> strings["blind_alert_off"]
    mode == Store.WATCHDOG_SEND -> strings["wd_send"]
    else -> strings.format("wd_watch", -mode)
}

/** Kademeli haber: her kişi/grup için ayrı bekleme süresi. Önce yakındaki kişi, kimse ilgilenmezse sıradaki. */
@Composable
private fun StagesDialog(store: Store, strings: Strings, onDismiss: () -> Unit) {
    val recipients = store.telegramRecipients
    val stages = remember { mutableStateMapOf<Long, Int>().apply { recipients.forEach { put(it.id, store.stageFor(it.id)) } } }
    val options = listOf(1, 2, 5, 10, 15, 20, 30, 45, 60, 90, 120)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["stages_title"]) },
        text = {
            Column(Modifier.width(460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings["stages_desc"])
                recipients.forEach { r ->
                    val value = stages[r.id] ?: store.escalateMin
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.name, Modifier.weight(1f), maxLines = 1)
                        TextButton(enabled = options.any { it < value }, onClick = { stages[r.id] = options.last { it < value } }) { Text("−") }
                        Text(strings.format("stages_after", value), style = MaterialTheme.typography.titleSmall)
                        TextButton(enabled = options.any { it > value }, onClick = { stages[r.id] = options.first { it > value } }) { Text("+") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { store.telegramStages = stages.toMap(); onDismiss() }) { Text(strings["action_save"]) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

private fun fmtPrice(v: Double) = if (v > 0) java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString() else ""
private fun num(v: String) = v.replace(',', '.').toDoubleOrNull() ?: 0.0

/** Varsayılan tarife: tek fiyat ya da üç zamanlı (gündüz 06–17, puant 17–22, gece 22–06). Cihazın kendi fiyatı önce gelir. */
@Composable
private fun TariffDialog(store: Store, strings: Strings, onDismiss: () -> Unit) {
    val symbol = EnergyFormat.currency()?.getSymbol(strings.locale) ?: ""
    var three by remember { mutableStateOf(store.threeZone) }
    var flat by remember { mutableStateOf(fmtPrice(store.energyPrice)) }
    var day by remember { mutableStateOf(fmtPrice(store.priceDay)) }
    var peak by remember { mutableStateOf(fmtPrice(store.pricePeak)) }
    var night by remember { mutableStateOf(fmtPrice(store.priceNight)) }
    @Composable
    fun field(label: String, value: String, tag: String, onChange: (String) -> Unit) = OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == ',' || it == '.' }.take(10)) },
        label = { Text(label) },
        singleLine = true,
        suffix = { Text("$symbol / kWh") },
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["tariff_title"]) },
        text = {
            Column(Modifier.width(460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(strings["tariff_desc"])
                Row(Modifier.fillMaxWidth().selectable(selected = !three, onClick = { three = false }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !three, onClick = null)
                    Text(strings["tariff_single"], Modifier.padding(start = 8.dp))
                }
                Row(Modifier.fillMaxWidth().selectable(selected = three, onClick = { three = true }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = three, onClick = null)
                    Text(strings["tariff_three"], Modifier.padding(start = 8.dp))
                }
                if (!three) field(strings["tariff_price"], flat, "price-flat") { flat = it }
                else {
                    field(strings["tariff_day"], day, "price-day") { day = it }
                    field(strings["tariff_peak"], peak, "price-peak") { peak = it }
                    field(strings["tariff_night"], night, "price-night") { night = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                store.threeZone = three
                store.energyPrice = num(flat)
                store.priceDay = num(day)
                store.pricePeak = num(peak)
                store.priceNight = num(night)
                onDismiss()
            }, modifier = Modifier.testTag("tariff-save")) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

@Composable
private fun BudgetDialog(store: Store, strings: Strings, onDismiss: () -> Unit) {
    val symbol = EnergyFormat.currency()?.getSymbol(strings.locale) ?: ""
    var text by remember { mutableStateOf(fmtPrice(store.monthlyBudget)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["smart_budget"]) },
        text = {
            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings["smart_budget_desc"])
                if (!store.hasAnyPrice) Text(strings["smart_budget_needs_price"], color = StatusColors.warning)
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() || it == ',' || it == '.' }.take(10) },
                    singleLine = true,
                    suffix = { Text(symbol) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { store.monthlyBudget = num(text); onDismiss() }) { Text(strings["action_save"]) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

/** Tatil bitiş günü seçimi (o günün sonuna kadar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VacationDatePicker(initial: Long, strings: Strings, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val tomorrow = System.currentTimeMillis() + 86_400_000L
    val state = rememberDatePickerState(initialSelectedDateMillis = utcDay(if (initial > 0) initial - 1 else tomorrow))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(endOfLocalDay(it)) }
                onDismiss()
            }) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    ) { DatePicker(state, title = { Text(strings["vacation_pick"], Modifier.padding(start = 24.dp, top = 16.dp)) }) }
}

/** DatePicker UTC gece yarısıyla çalışır: yerel günü UTC gününe çevirir. */
private fun utcDay(local: Long): Long {
    val l = Calendar.getInstance().apply { timeInMillis = local }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(l.get(Calendar.YEAR), l.get(Calendar.MONTH), l.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Seçilen (UTC) günün yerel gece yarısı sonrası: tatil o günün sonunda biter. */
private fun endOfLocalDay(utc: Long): Long {
    val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return Calendar.getInstance().apply {
        clear(); set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH)); add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
}
