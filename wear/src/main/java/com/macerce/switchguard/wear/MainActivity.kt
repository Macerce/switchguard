package com.macerce.switchguard.wear

import android.Manifest
import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.phone.interactions.notifications.BridgingConfig
import androidx.wear.phone.interactions.notifications.BridgingManager

/** Saat uygulaması açılınca: telefondaki bildirimlerin saate köprülenmesini kapatır (alarm iki kez gelmesin). */
class WearApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { BridgingManager.fromContext(this).setConfig(BridgingConfig.Builder(this, false).build()) }
    }
}

/** Ana ekran: izleme durumu, çalan alarm (Kapat), cihazlar (dokununca aç/kapa). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearState.load(this)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        setContent { MaterialTheme { Home() } }
    }

    override fun onResume() {
        super.onResume()
        PhoneLink.readCurrent(this)
        PhoneLink.sync(this)
    }
}

@Composable
private fun Home() {
    val context = LocalContext.current
    val status by WearState.status.collectAsStateWithLifecycle()
    ScalingLazyColumn(Modifier.fillMaxSize()) {
        item { ListHeader { Text(stringResource(R.string.app_name)) } }
        if (!WearState.received) {
            item { Text(stringResource(R.string.no_phone), textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp)) }
            return@ScalingLazyColumn
        }
        item {
            Text(status.state, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
        }
        if (status.alarming) {
            item {
                Button(
                    onClick = { PhoneLink.dismiss(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.dismiss)) },
                    secondaryLabel = { Text(status.alarm.last(), maxLines = 2) },
                )
            }
        }
        item {
            SwitchButton(
                checked = status.away,
                enabled = !status.vacation,
                onCheckedChange = { PhoneLink.setAway(context, it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(if (status.away) R.string.away else R.string.home)) },
                secondaryLabel = if (status.vacation) ({ Text(stringResource(R.string.vacation)) }) else null,
            )
        }
        if (status.scenes.isNotEmpty()) item { ListHeader { Text(stringResource(R.string.scenes)) } }
        items(status.scenes, key = { "scene/" + it.first }) { (id, name) ->
            Button(
                onClick = { PhoneLink.runScene(context, id) },
                colors = ButtonDefaults.filledTonalButtonColors(),
                modifier = Modifier.fillMaxWidth(),
                label = { Text(name, maxLines = 1) },
            )
        }
        if (status.devices.isNotEmpty()) item { ListHeader { Text(stringResource(R.string.devices)) } }
        status.devices.forEach { d ->
            items(d.switches.entries.toList(), key = { "${d.id}/${it.key}" }) { (ch, on) ->
                val name = if (d.switches.size > 1) "${d.name} ${ch + 1}" else d.name
                SwitchButton(
                    checked = on,
                    enabled = d.online,
                    onCheckedChange = { PhoneLink.toggle(context, d.id, ch, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(name, maxLines = 1) },
                    secondaryLabel = {
                        Text(
                            when {
                                !d.online -> stringResource(R.string.offline)
                                d.watts != null -> String.format(LocalConfiguration.current.locales[0], "%.0f W", d.watts)
                                else -> stringResource(if (on) R.string.on else R.string.off)
                            },
                        )
                    },
                )
            }
        }
    }
}
