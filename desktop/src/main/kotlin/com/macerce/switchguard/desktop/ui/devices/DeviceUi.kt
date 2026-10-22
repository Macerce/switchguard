package com.macerce.switchguard.desktop.ui.devices

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import com.macerce.switchguard.desktop.ui.TextButton
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.LocalStrings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Cihaz kutucukları ve cihaz sayfasının ortak işlemleri (telefonda `Actions.setSwitch` + `Toast`). Anahtar komutu
 * motora gider ([send]; sonuç motor iş parçacığında, hata yoksa null), kısa mesaj ekranın Snackbar'ında gösterilir
 * ([message]). Cihazlar ekranı ve cihaz sayfası kendi örneğini verir.
 */
class DeviceUi(
    val send: (deviceId: String, channel: Int, on: Boolean, onResult: (Throwable?) -> Unit) -> Unit,
    val message: (String) -> Unit,
)

val LocalDeviceUi = staticCompositionLocalOf<DeviceUi> { error("LocalDeviceUi is not provided") }

/** Bir kanalın anahtar durumu: istek sürerken hedef değer gösterilir ve anahtar kilitlenir. */
class SwitchControl(val shown: Boolean, val busy: Boolean, val toggle: (Boolean) -> Unit)

/** Telefondaki `rememberSwitchControl`: istek motora gider; onay gelmezse 15 sn sonra bekleme bırakılır. */
@Composable
fun rememberSwitchControl(device: DeviceSnapshot, channel: Int, on: Boolean): SwitchControl {
    val ui = LocalDeviceUi.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var pending by remember(device.id, channel) { mutableStateOf<Boolean?>(null) }
    var timeout by remember(device.id, channel) { mutableStateOf<Job?>(null) }
    // Gerçek durum beklenen değere ulaşınca bekleme durumunu temizle.
    LaunchedEffect(on) { if (pending == on) pending = null }
    return SwitchControl(pending ?: on, pending != null) { target ->
        pending = target
        timeout?.cancel()
        timeout = scope.launch {
            delay(15_000)
            pending = null
        }
        // Sonuç motor iş parçacığında gelir ve hiç gelmeyebilir (izleme durursa): beklenmez.
        ui.send(device.id, channel, target) { error ->
            if (error != null) {
                scope.launch {
                    timeout?.cancel()
                    pending = null
                    ui.message(strings.format("control_failed", error.message ?: error.javaClass.simpleName))
                }
            }
        }
    }
}

/**
 * Başlık çubuğundaki simge düğmesi; fare üstünde durunca adı ipucu olarak görünür (telefonda uzun basınca çıkan
 * açıklamanın masaüstü karşılığı). Ad aynı zamanda erişilebilirlik açıklamasıdır.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HintIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    TooltipArea(
        tooltip = {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        },
        delayMillis = 500,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
    ) {
        // Masaüstü komut çubuğu düğmesi: simge + etiket (Fluent CommandBar), yalnız simge değil.
        TextButton(
            onClick = onClick, enabled = enabled, modifier = modifier,
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                if (content != null) content() else Icon(icon, null, Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** "118 W", "1,25 kW" (telefondaki `formatWatts`, seçili dilin sayı biçimiyle). */
fun formatWatts(w: Double, locale: Locale): String =
    if (w >= 1000) String.format(locale, "%.2f kW", w / 1000)
    else String.format(locale, "%.0f W", w)

@Composable
@ReadOnlyComposable
fun watts(w: Double): String = formatWatts(w, LocalStrings.current.locale)

/** "az önce", "3 dk önce", "2 sa 10 dk önce" (telefondaki `DateUtils.getRelativeTimeSpanString` karşılığı). */
fun relativeAgo(ms: Long, strings: Strings): String {
    if (ms < 60_000) return strings["desktop_just_now"]
    // Bir saatten kısa sürede saniyeler gösterilmez: metin 30 sn'de bir tazelenir.
    val rounded = if (ms < 3_600_000) ms / 60_000 * 60_000 else ms
    return strings.format("desktop_ago", DurationText.format(rounded, strings.durations()))
}
