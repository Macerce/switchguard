package com.macerce.switchguard.ui

import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ClockStep
import kotlinx.coroutines.delay
import java.text.DateFormatSymbols

/**
 * Saat seçtirir. Telefonda sistemin saat penceresi açılır. TV'de sistem penceresi saat kadranı olarak
 * açılıyor ve kadran kumandayla seçilemiyor (odak yalnız üstteki rakamlar arasında geziyor); orada
 * yukarı/aşağı tuşlarıyla değişen [TvTimeDialog] açılır. Pencereyi bu nesne kendisi gösterir.
 */
@Stable
class TimePicker internal constructor(private val context: Context, private val tv: Boolean) {
    internal class Request(val title: String, val minute: Int, val onPick: (Int) -> Unit)

    internal var request by mutableStateOf<Request?>(null)

    /** [minute] gün içindeki dakika (0..1439); seçilen değer [onPick]'e gelir. */
    fun pick(title: String, minute: Int, onPick: (Int) -> Unit) {
        if (tv) {
            request = Request(title, minute, onPick)
        } else {
            TimePickerDialog(
                context, { _, h, m -> onPick(h * 60 + m) },
                minute / 60, minute % 60, AndroidDateFormat.is24HourFormat(context),
            ).show()
        }
    }
}

@Composable
fun rememberTimePicker(): TimePicker {
    val context = LocalContext.current
    val tv = LocalIsTv.current
    val picker = remember(context, tv) { TimePicker(context, tv) }
    picker.request?.let { r ->
        TvTimeDialog(
            title = r.title,
            initial = r.minute,
            onPick = { picker.request = null; r.onPick(it) },
            onDismiss = { picker.request = null },
        )
    }
    return picker
}

/**
 * Kumandayla saat seçimi: saat ve dakika kutuları. Yukarı/aşağı değeri değiştirir (basılı tutunca hızla
 * akar), sol/sağ kutular arasında gezer. OK saatteyken dakikaya geçirir, dakikadayken kaydeder; Geri vazgeçer.
 */
@Composable
private fun TvTimeDialog(title: String, initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val is24 = remember(context) { AndroidDateFormat.is24HourFormat(context) }
    var minute by remember { mutableIntStateOf(initial) }
    val hourFocus = remember { FocusRequester() }
    val minuteFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Pencere ekrana yerleşmeden istenen odak kaybolur: kısa aralıklarla yeniden dene.
        for (attempt in 1..20) {
            val ok = runCatching { hourFocus.requestFocus() }.getOrNull()
            if (ok != null && ok != false) break
            delay(50)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    TimeField(
                        label = hourLabel(minute / 60, is24),
                        wide = !is24,
                        onStep = { minute = ClockStep.hour(minute, it) },
                        onOk = { minuteFocus.requestFocus() },
                        modifier = Modifier.focusRequester(hourFocus),
                    )
                    Text(":", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(horizontal = 20.dp))
                    TimeField(
                        label = "%02d".format(minute % 60),
                        wide = false,
                        onStep = { minute = ClockStep.minute(minute, it) },
                        onOk = { onPick(minute) },
                        modifier = Modifier.focusRequester(minuteFocus),
                    )
                }
                Text(
                    stringResource(R.string.tv_time_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.tvFocusRing(), onClick = { onPick(minute) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Tek bir sayı kutusu; üstündeki/altındaki oklar yalnız işaret (odak almaz, dokunulursa da çalışır). */
@Composable
private fun TimeField(label: String, wide: Boolean, onStep: (Int) -> Unit, onOk: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(20.dp)
    val arrowTint = if (focused) scheme.primary else scheme.onSurfaceVariant
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Arrow(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.action_increase), arrowTint) { onStep(+1) }
        Box(
            modifier
                .tvFocusRing(shape)
                .onPreviewKeyEvent { e ->
                    val delta = when (e.key) {
                        Key.DirectionUp -> +1
                        Key.DirectionDown -> -1
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> 0
                        else -> return@onPreviewKeyEvent false
                    }
                    // Yukarı/aşağı odağı başka öğeye taşımasın: hem basışı hem bırakışı yut.
                    if (e.type == KeyEventType.KeyDown) {
                        if (delta == 0) onOk() else onStep(delta)
                    }
                    true
                }
                .focusable(interactionSource = interaction)
                .clip(shape)
                .background(if (focused) scheme.primaryContainer else scheme.surfaceContainerHighest)
                .widthIn(min = if (wide) 168.dp else 112.dp)
                .height(96.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = if (wide) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
                color = if (focused) scheme.onPrimaryContainer else scheme.onSurface,
            )
        }
        Arrow(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.action_decrease), arrowTint) { onStep(-1) }
    }
}

@Composable
private fun Arrow(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Icon(
        icon, description, tint = tint,
        modifier = Modifier.size(40.dp).focusProperties { canFocus = false }.clickable(onClick = onClick),
    )
}

/** 24 saat düzeninde "07"; 12 saat düzeninde dilin ÖÖ/ÖS yazısıyla "7 AM". */
private fun hourLabel(hour: Int, is24: Boolean): String {
    if (is24) return "%02d".format(hour)
    val amPm = DateFormatSymbols.getInstance().amPmStrings
    return "${ClockStep.hour12(hour)} ${amPm[if (hour < 12) 0 else 1]}"
}
