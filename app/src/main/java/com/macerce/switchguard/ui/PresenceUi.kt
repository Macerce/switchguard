package com.macerce.switchguard.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store
import java.util.Calendar
import java.util.TimeZone

/** Evde / Dışarıda seçimi; tatildeyse "Tatil · 28 Eki'ye kadar" yazar ve seçim kilitlenir (tatil dışarıda sayılır). */
@Composable
fun PresenceCard(store: Store) {
    val context = LocalContext.current
    val vacation = store.onVacation()
    SectionCard(Modifier.padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (vacation) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HueIcon(Icons.Rounded.BeachAccess, Hue.AMBER, size = 30.dp)
                    Text(
                        stringResource(R.string.vacation_until, DateUtils.formatDateTime(context, store.vacationUntil - 1, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)),
                        style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp),
                    )
                }
            } else {
                val away = store.awayMode
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(false, true).forEachIndexed { i, a ->
                        SegmentedButton(
                            selected = away == a,
                            onClick = { Actions.setAway(context, a) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            icon = { Icon(if (a) Icons.AutoMirrored.Rounded.DirectionsWalk else Icons.Rounded.Home, null) },
                            modifier = Modifier.tvFocusRing(),
                        ) { Text(stringResource(if (a) R.string.presence_away else R.string.presence_home)) }
                    }
                }
            }
            Text(
                stringResource(if (vacation) R.string.vacation_effect else if (store.awayMode) R.string.presence_away_desc else R.string.presence_home_desc),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Tatil bitiş günü seçimi (o günün sonuna kadar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacationDatePicker(initial: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val tomorrow = System.currentTimeMillis() + 86_400_000L
    val state = rememberDatePickerState(initialSelectedDateMillis = utcDay(if (initial > 0) initial - 1 else tomorrow))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(modifier = Modifier.tvFocusRing(), onClick = {
                state.selectedDateMillis?.let { onPick(endOfLocalDay(it)) }
                onDismiss()
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) { DatePicker(state, title = { Text(stringResource(R.string.vacation_pick), Modifier.padding(start = 24.dp, top = 16.dp)) }) }
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
