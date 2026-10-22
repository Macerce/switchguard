package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import com.macerce.switchguard.desktop.ui.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.macerce.switchguard.desktop.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.LocalDarkTheme
import com.macerce.switchguard.desktop.ui.str
import kotlinx.coroutines.delay
import java.text.DateFormatSymbols
import java.time.LocalTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Gün içindeki dakikayı (0..1439) adım adım değiştirme; telefondaki `core/ClockStep` + klavyeden rakam yazma.
 * Saat ve dakika ayrı ayrı başa döner, biri değişirken öteki korunur (23 → 0 olunca dakika aynı kalır).
 */
internal object TimeStep {
    private const val DAY = 24 * 60

    fun hour(minuteOfDay: Int, delta: Int): Int {
        val m = minuteOfDay.mod(DAY)
        return ((m / 60 + delta).mod(24)) * 60 + m % 60
    }

    fun minute(minuteOfDay: Int, delta: Int): Int {
        val m = minuteOfDay.mod(DAY)
        return (m / 60) * 60 + (m % 60 + delta).mod(60)
    }

    /** 12 saat düzeninde gösterilecek saat: 0 ve 12 → 12. */
    fun hour12(hour: Int): Int = (hour + 11) % 12 + 1

    /**
     * Yazılan rakam: önceki rakamla ([previous], bu kutuda az önce yazılan) birlikte [range] içindeyse iki basamaklı
     * değer, değilse yalnızca bu rakam; o da aralık dışındaysa null (yok sayılır). "2" sonra "3" → 23, "7" sonra "5" → 5.
     */
    fun typed(previous: Int?, digit: Int, range: IntRange): Int? {
        if (previous != null) {
            val two = previous * 10 + digit
            if (two in range) return two
        }
        return digit.takeIf { it in range }
    }

    /** Saati [hour] (0..23) yapar, dakika korunur. */
    fun withHour(minuteOfDay: Int, hour: Int): Int = hour.mod(24) * 60 + minuteOfDay.mod(DAY) % 60

    /** Dakikayı [minute] (0..59) yapar, saat korunur. */
    fun withMinute(minuteOfDay: Int, minute: Int): Int = (minuteOfDay.mod(DAY) / 60) * 60 + minute.mod(60)

    /** 12 saat düzeninde yazılan saat ([h12] 1..12): ÖÖ/ÖS korunur. */
    fun withHour12(minuteOfDay: Int, h12: Int): Int {
        val pm = minuteOfDay.mod(DAY) >= 12 * 60
        return withHour(minuteOfDay, h12 % 12 + if (pm) 12 else 0)
    }
}

/** Dilin kısa saat biçimi 24 saatlik mi ("HH:mm") yoksa ÖÖ/ÖS'li mi ("h:mm a"). */
internal fun is24Hour(locale: Locale): Boolean =
    DateTimeFormatterBuilder.getLocalizedDateTimePattern(null, FormatStyle.SHORT, IsoChronology.INSTANCE, locale).contains('H')

/** Gün içindeki dakika, seçili dilin kısa saat biçimiyle ("21:00", "9:00 PM"); telefondaki `formatMinute`. */
internal fun formatMinute(strings: Strings, minute: Int): String {
    val m = minute.mod(24 * 60)
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(strings.locale).format(LocalTime.of(m / 60, m % 60))
}

/**
 * Saat seçimi (telefonda sistemin saat penceresi, TV'de kumanda penceresi): saat ve dakika kutuları. Fareyle kutunun
 * üstündeki/altındaki oklara basılır (basılı tutunca hızla akar) ya da tekerlek çevrilir; klavyeyle ↑ ↓ değeri
 * değiştirir, ← → ve Tab kutular arasında gezer, rakam yazılabilir, Enter kaydeder, Esc vazgeçer.
 */
@Composable
fun TimeDialog(title: String, initial: Int, locale: Locale, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val is24 = remember(locale) { is24Hour(locale) }
    var minute by remember { mutableIntStateOf(initial.mod(24 * 60)) }
    val hourFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Pencere yerleşmeden istenen odak kaybolabilir: kısa aralıklarla yeniden dene.
        for (attempt in 1..20) {
            if (runCatching { hourFocus.requestFocus() }.isSuccess) break
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
                        label = hourLabel(minute / 60, is24, locale),
                        description = str("desktop_time_hour"),
                        wide = !is24,
                        tag = "time-hour",
                        onStep = { minute = TimeStep.hour(minute, it) },
                        onDigit = { previous, d ->
                            if (is24) {
                                TimeStep.typed(previous, d, 0..23)?.also { minute = TimeStep.withHour(minute, it) }
                            } else {
                                TimeStep.typed(previous, d, 1..12)?.also { minute = TimeStep.withHour12(minute, it) }
                            }
                        },
                        onEnter = { onPick(minute) },
                        modifier = Modifier.focusRequester(hourFocus),
                    )
                    Text(":", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(horizontal = 16.dp))
                    TimeField(
                        label = "%02d".format(minute % 60),
                        description = str("desktop_time_minute"),
                        wide = false,
                        tag = "time-minute",
                        onStep = { minute = TimeStep.minute(minute, it) },
                        onDigit = { previous, d -> TimeStep.typed(previous, d, 0..59)?.also { minute = TimeStep.withMinute(minute, it) } },
                        onEnter = { onPick(minute) },
                    )
                }
                Text(
                    str("desktop_time_hint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp).widthIn(max = 360.dp),
                )
            }
        },
        confirmButton = { Button(onClick = { onPick(minute) }) { Text(str("action_save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str("action_cancel")) } },
    )
}

/**
 * Tek sayı kutusu. Odaklanınca vurgulanır. [onDigit] yazılan rakamı (ve bu kutuda az önce yazılanı) alır, kabul
 * edilen değeri döndürür; iki basamak yazılınca ya da odak gidince yeniden başlanır.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TimeField(
    label: String,
    description: String,
    wide: Boolean,
    tag: String,
    onStep: (Int) -> Unit,
    onDigit: (previous: Int?, digit: Int) -> Int?,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    var typed by remember { mutableStateOf<Int?>(null) }
    val shape = RoundedCornerShape(8.dp)
    val arrowTint = if (focused) scheme.primary else scheme.onSurfaceVariant
    val self = remember { FocusRequester() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RepeatArrow(Icons.Rounded.KeyboardArrowUp, str("action_increase"), arrowTint) { typed = null; onStep(+1) }
        Box(
            modifier
                .focusRequester(self)
                .onFocusChanged { if (!it.isFocused) typed = null }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val digit = digitOf(e.key)
                    when {
                        e.key == Key.DirectionUp -> { typed = null; onStep(+1) }
                        e.key == Key.DirectionDown -> { typed = null; onStep(-1) }
                        e.key == Key.DirectionRight -> focusManager.moveFocus(FocusDirection.Next)
                        e.key == Key.DirectionLeft -> focusManager.moveFocus(FocusDirection.Previous)
                        e.key == Key.Enter || e.key == Key.NumPadEnter -> onEnter()
                        digit != null -> {
                            val previous = typed
                            val accepted = onDigit(previous, digit)
                            // İki basamak tamamlanınca baştan başla ("2","3" → 23, ardından "1" → 1); tek rakam
                            // kabul edildiyse bir sonraki rakamla birleşebilir.
                            typed = when {
                                accepted == null -> null
                                previous != null && accepted == previous * 10 + digit -> null
                                else -> accepted
                            }
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                }
                .focusable(interactionSource = interaction)
                .onPointerEvent(PointerEventType.Scroll) { e ->
                    val dy = e.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (dy != 0f) {
                        typed = null
                        onStep(if (dy < 0) +1 else -1)
                    }
                }
                .pointerInput(Unit) { detectTapGestures { self.requestFocus() } }
                .clip(shape)
                .background(
                    when {
                        focused -> scheme.primaryContainer
                        // Pencere zemininden ayrılsın: açık temada beyaz, koyuda bir ton açık.
                        LocalDarkTheme.current -> scheme.surfaceContainerHighest
                        else -> scheme.surfaceContainerLowest
                    },
                )
                .semantics { contentDescription = description }
                .testTag(tag)
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
        RepeatArrow(Icons.Rounded.KeyboardArrowDown, str("action_decrease"), arrowTint) { typed = null; onStep(-1) }
    }
}

/** Ok düğmesi: tıklama bir adım; basılı tutulunca kısa bir beklemeden sonra hızla adımlar. Tab ile odak almaz. */
@Composable
private fun RepeatArrow(icon: ImageVector, description: String, tint: androidx.compose.ui.graphics.Color, onStep: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val step by rememberUpdatedState(onStep)
    var repeated by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        repeated = false
        delay(450)
        repeated = true
        while (true) {
            step()
            delay(70)
        }
    }
    IconButton(
        // Basılı tutup akıttıktan sonra bırakmak bir adım daha eklemesin.
        onClick = { if (!repeated) step() else repeated = false },
        interactionSource = interaction,
        modifier = Modifier.focusProperties { canFocus = false },
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(32.dp))
    }
}

private fun digitOf(key: Key): Int? = when (key) {
    Key.Zero, Key.NumPad0 -> 0
    Key.One, Key.NumPad1 -> 1
    Key.Two, Key.NumPad2 -> 2
    Key.Three, Key.NumPad3 -> 3
    Key.Four, Key.NumPad4 -> 4
    Key.Five, Key.NumPad5 -> 5
    Key.Six, Key.NumPad6 -> 6
    Key.Seven, Key.NumPad7 -> 7
    Key.Eight, Key.NumPad8 -> 8
    Key.Nine, Key.NumPad9 -> 9
    else -> null
}

/** 24 saat düzeninde "07"; 12 saat düzeninde dilin ÖÖ/ÖS yazısıyla "7 AM". */
private fun hourLabel(hour: Int, is24: Boolean, locale: Locale): String {
    if (is24) return "%02d".format(hour)
    val amPm = DateFormatSymbols.getInstance(locale).amPmStrings
    return "${TimeStep.hour12(hour)} ${amPm[if (hour < 12) 0 else 1]}"
}
