package com.macerce.switchguard.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.DeviceWidget
import com.macerce.switchguard.ui.theme.SwitchGuardTheme

/** Cihaz düğmesi widget'ı eklenirken (ya da yeniden yapılandırılırken) hangi cihazın/kanalın açılıp kapanacağı seçilir. */
class DeviceWidgetConfigActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Vazgeçilirse widget eklenmesin.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return finish()
        enableEdgeToEdge()
        setContent {
            SwitchGuardTheme {
                Surface(Modifier.fillMaxSize()) {
                    val store = remember { Store.get(this) }
                    // Her anahtar bir seçenek: tek kanallı cihaz bir satır, çok kanallı cihaz kanal başına bir satır.
                    val choices = remember {
                        store.deviceLayout.sections(store.snapshots).flatMap { it.devices }.flatMap { d ->
                            d.switches.keys.sorted().map { ch -> d to ch }
                        }
                    }
                    Column(Modifier.safeDrawingPadding()) {
                        AppTopBar(stringResource(R.string.widget_device_choose), small = true)
                        if (choices.isEmpty()) {
                            Text(stringResource(R.string.no_devices), Modifier.padding(24.dp))
                        }
                        LazyColumn {
                            item {
                                InfoRow(
                                    Icons.Rounded.Home, stringResource(R.string.widget_presence_choice), stringResource(R.string.widget_presence_choice_desc),
                                    Modifier.clickable { chooseRaw(widgetId, DeviceWidget.PRESENCE) },
                                )
                            }
                            items(store.scenes, key = { "scene/" + it.id }) { scene ->
                                InfoRow(
                                    Icons.Rounded.PlayArrow, scene.name, stringResource(R.string.scenes_title),
                                    Modifier.clickable { chooseRaw(widgetId, DeviceWidget.SCENE + scene.id) },
                                )
                            }
                            items(choices, key = { "${it.first.id}/${it.second}" }) { (d, ch) ->
                                InfoRow(
                                    Icons.Rounded.PowerSettingsNew,
                                    if (d.isMultiChannel) stringResource(R.string.label_channel, d.name, ch + 1) else d.name,
                                    null,
                                    Modifier.clickable { choose(widgetId, d.id, ch) },
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.widget_device_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
            }
        }
    }

    private fun choose(widgetId: Int, deviceId: String, channel: Int) = chooseRaw(widgetId, "$deviceId|$channel")

    private fun chooseRaw(widgetId: Int, raw: String) {
        DeviceWidget.bindRaw(this, widgetId, raw)
        DeviceWidget.updateAll(this)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
