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
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.GroupWidget
import com.macerce.switchguard.ui.theme.SwitchGuardTheme

/** Grup widget'ı eklenirken (ya da yeniden yapılandırılırken) hangi grubun gösterileceği seçilir. */
class GroupWidgetConfigActivity : ComponentActivity() {
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
                    val groups = remember { Store.get(this).groups }
                    Column(Modifier.safeDrawingPadding()) {
                        AppTopBar(stringResource(R.string.widget_group_choose), small = true)
                        if (groups.isEmpty()) Text(stringResource(R.string.widget_group_none), Modifier.padding(24.dp))
                        LazyColumn {
                            items(groups, key = { it.id }) { g ->
                                InfoRow(
                                    Icons.Rounded.Workspaces, g.name,
                                    pluralStringResource(R.plurals.widget_group_members, g.members.size, g.members.size),
                                    Modifier.clickable { choose(widgetId, g.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun choose(widgetId: Int, groupId: String) {
        GroupWidget.bind(this, widgetId, groupId)
        GroupWidget.updateAll(this)
        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }
}
