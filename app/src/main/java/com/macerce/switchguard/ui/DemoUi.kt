package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.service.MonitorService

/**
 * Demo cihazının detay ekranındaki simülasyon düğmeleri: cihazın kendisinde olmuş gibi
 * bir olay canlandırır, böylece kurallar ve alarm gerçek bir cihazdaki gibi denenebilir.
 */
@Composable
fun DemoSection(device: DeviceSnapshot) {
    val context = LocalContext.current
    fun simulate(op: String, channel: Int = 0) = Actions.demoSimulate(context, op, device.id, channel)

    SectionTitle(stringResource(R.string.section_demo))
    SectionCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.demo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            device.switches.keys.sorted().forEach { ch ->
                DemoButton(
                    if (device.isMultiChannel) stringResource(R.string.demo_toggle_channel, ch + 1)
                    else stringResource(R.string.demo_toggle),
                ) { simulate(MonitorService.DEMO_TOGGLE, ch) }
            }
            DemoButton(stringResource(if (device.online) R.string.demo_disconnect else R.string.demo_reconnect)) {
                simulate(MonitorService.DEMO_ONLINE)
            }
            // Güç düşüşü yalnızca açık ve güç ölçen kanallarda anlamlı.
            device.switches.keys.sorted()
                .filter { device.switches[it] == true && device.channelPowerOf(it) != null }
                .forEach { ch ->
                    val stalled = device.channelPowerOf(ch) == Demo.STALLED_WATTS
                    val label = when {
                        device.isMultiChannel && stalled -> stringResource(R.string.demo_power_restore, ch + 1)
                        device.isMultiChannel -> stringResource(R.string.demo_power_drop, ch + 1)
                        stalled -> stringResource(R.string.demo_power_restore_single)
                        else -> stringResource(R.string.demo_power_drop_single)
                    }
                    DemoButton(label) { simulate(MonitorService.DEMO_POWER, ch) }
                }
        }
    }
}

@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}
