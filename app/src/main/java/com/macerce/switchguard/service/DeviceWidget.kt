package com.macerce.switchguard.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.Actions
import kotlinx.coroutines.runBlocking

/**
 * Cihaz düğmesi widget'ı: her widget bir cihaza (çok kanallıda bir kanala) bağlanır, dokununca açar/kapatır.
 * Bağlantılar ayrı bir dosyada tutulur (widget kimliği → "cihaz|kanal"); yedeğe girmez, widget'lar cihaza özeldir.
 */
class DeviceWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)

    override fun onDeleted(context: Context, ids: IntArray) {
        prefs(context).edit().apply { ids.forEach { remove(it.toString()) } }.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return super.onReceive(context, intent)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val raw = rawBinding(context, id) ?: return
        val result = goAsync()
        Thread {
            try {
                val store = Store.get(context)
                when {
                    raw == PRESENCE -> Actions.setAway(context, !store.awayMode, by = context.getString(R.string.widget_device_label))
                    raw.startsWith(SCENE) -> store.scenes.firstOrNull { it.id == raw.removePrefix(SCENE) }?.let { scene ->
                        runBlocking { Actions.runScene(context, scene, context.getString(R.string.widget_device_label)) }
                    }
                    else -> {
                        val (deviceId, channel) = binding(context, id) ?: return@Thread
                        val device = store.snapshots.firstOrNull { it.id == deviceId }
                        if (device != null && device.online) {
                            val on = device.switches[channel] != true
                            runBlocking { Actions.setSwitch(context, device, channel, on) }
                        }
                    }
                }
                updateAll(context)
            } finally {
                result.finish()
            }
        }.start()
    }

    companion object {
        private const val ACTION_TOGGLE = "com.macerce.switchguard.WIDGET_TOGGLE"
        /** Bağlantı türleri: cihaz "id|kanal", sahne "scene:id", evde/dışarıda "presence". */
        const val SCENE = "scene:"
        const val PRESENCE = "presence"

        private fun prefs(context: Context) = context.getSharedPreferences("switchguard_widgets", Context.MODE_PRIVATE)

        fun bind(context: Context, widgetId: Int, deviceId: String, channel: Int) {
            prefs(context).edit().putString(widgetId.toString(), "$deviceId|$channel").apply()
        }

        fun bindRaw(context: Context, widgetId: Int, raw: String) {
            prefs(context).edit().putString(widgetId.toString(), raw).apply()
        }

        private fun rawBinding(context: Context, widgetId: Int): String? = prefs(context).getString(widgetId.toString(), null)

        fun binding(context: Context, widgetId: Int): Pair<String, Int>? {
            val raw = rawBinding(context, widgetId) ?: return null
            if (raw == PRESENCE || raw.startsWith(SCENE)) return null
            val id = raw.substringBeforeLast('|')
            return id to (raw.substringAfterLast('|').toIntOrNull() ?: 0)
        }

        /** Durumlar değişince çağrılır; widget yoksa hiçbir şey yapmaz. */
        fun updateAll(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, DeviceWidget::class.java))
                if (ids.isEmpty()) return
                val devices = Store.get(context).snapshots.associateBy { it.id }
                ids.forEach { manager.updateAppWidget(it, views(context, it, devices)) }
            }
        }

        private fun views(context: Context, widgetId: Int, devices: Map<String, DeviceSnapshot>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_device)
            val raw = rawBinding(context, widgetId)
            if (raw == PRESENCE || raw?.startsWith(SCENE) == true) return specialViews(context, widgetId, raw, views)
            val bound = binding(context, widgetId)
            val device = bound?.let { devices[it.first] }
            if (bound == null || device == null) {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_device_choose))
                views.setTextViewText(R.id.widget_status, context.getString(if (bound == null) R.string.widget_device_tap else R.string.device_missing))
                views.setOnClickPendingIntent(R.id.widget_root, StatusSurfaces.openApp(context))
                return views
            }
            val channel = bound.second
            val on = device.switches[channel] == true
            val title = if (device.isMultiChannel) context.getString(R.string.label_channel, device.name, channel + 1) else device.name
            val state = when {
                !device.online -> context.getString(R.string.label_offline)
                on -> context.getString(R.string.label_on)
                else -> context.getString(R.string.label_off)
            }
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_status, state)
            views.setInt(R.id.widget_root, "setBackgroundResource", if (on && device.online) R.drawable.widget_bg_on else R.drawable.widget_bg)
            val fg = if (on && device.online) 0xFFFFFFFF.toInt() else context.getColor(R.color.widget_text)
            views.setTextColor(R.id.widget_title, fg)
            views.setTextColor(R.id.widget_status, if (on && device.online) 0xFFFFFFFF.toInt() else context.getColor(R.color.widget_text_secondary))
            views.setInt(R.id.widget_icon, "setColorFilter", if (on && device.online) 0xFFFFFFFF.toInt() else context.getColor(R.color.widget_icon))
            setToggle(context, widgetId, views)
            return views
        }

        private fun setToggle(context: Context, widgetId: Int, views: RemoteViews) {
            val toggle = Intent(context, DeviceWidget::class.java).setAction(ACTION_TOGGLE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getBroadcast(context, 1000 + widgetId, toggle, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        }

        /** Sahne düğmesi (dokununca çalışır) ya da Evde/Dışarıda düğmesi (dokununca değişir; dışarıdayken renkli). */
        private fun specialViews(context: Context, widgetId: Int, raw: String, views: RemoteViews): RemoteViews {
            val store = Store.get(context)
            if (raw == PRESENCE) {
                val away = store.isAway()
                views.setTextViewText(R.id.widget_title, context.getString(if (away) R.string.presence_away else R.string.presence_home))
                views.setTextViewText(R.id.widget_status, context.getString(if (store.onVacation()) R.string.vacation_title else R.string.widget_presence_tap))
                views.setImageViewResource(R.id.widget_icon, if (away) R.drawable.ic_widget_away else R.drawable.ic_widget_home)
                colors(context, views, strong = away)
            } else {
                val scene = store.scenes.firstOrNull { it.id == raw.removePrefix(SCENE) }
                views.setTextViewText(R.id.widget_title, scene?.name ?: context.getString(R.string.device_missing))
                views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_scene_tap))
                views.setImageViewResource(R.id.widget_icon, R.drawable.ic_widget_scene)
                colors(context, views, strong = false)
            }
            setToggle(context, widgetId, views)
            return views
        }

        private fun colors(context: Context, views: RemoteViews, strong: Boolean) {
            views.setInt(R.id.widget_root, "setBackgroundResource", if (strong) R.drawable.widget_bg_on else R.drawable.widget_bg)
            val white = 0xFFFFFFFF.toInt()
            views.setTextColor(R.id.widget_title, if (strong) white else context.getColor(R.color.widget_text))
            views.setTextColor(R.id.widget_status, if (strong) white else context.getColor(R.color.widget_text_secondary))
            views.setInt(R.id.widget_icon, "setColorFilter", if (strong) white else context.getColor(R.color.widget_icon))
        }
    }
}
