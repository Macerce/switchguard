package com.macerce.switchguard.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.Actions
import com.macerce.switchguard.ui.formatWatts
import kotlinx.coroutines.runBlocking

/**
 * Grup widget'ı (büyük): üstte grup adı, kaç üyenin açık olduğu ve hepsini açıp kapatan düğme; altında gruptaki her
 * cihaz/kanal için ayrı kutu, dokununca yalnız o üye açılır/kapanır. Bağlantı widget kimliği → grup kimliği.
 */
class GroupWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = updateAll(context)

    override fun onDeleted(context: Context, ids: IntArray) {
        prefs(context).edit().apply { ids.forEach { remove(it.toString()) } }.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MEMBER && intent.action != ACTION_ALL) return super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val result = goAsync()
        Thread {
            try {
                val store = Store.get(context)
                if (intent.action == ACTION_ALL) {
                    val group = group(context, widgetId) ?: return@Thread
                    // Grup sanal cihaz gibi: Actions.setSwitch bütün üyelere dağıtır.
                    val virtual = store.snapshotsWithGroups.firstOrNull { it.id == group.deviceId }
                    if (virtual != null && virtual.online) runBlocking { Actions.setSwitch(context, virtual, 0, virtual.switches[0] != true) }
                } else {
                    val deviceId = intent.getStringExtra(EXTRA_DEVICE) ?: return@Thread
                    val channel = intent.getIntExtra(EXTRA_CHANNEL, 0)
                    val device = store.snapshots.firstOrNull { it.id == deviceId }
                    if (device != null && device.online) runBlocking { Actions.setSwitch(context, device, channel, device.switches[channel] != true) }
                }
                updateAll(context)
            } finally {
                result.finish()
            }
        }.start()
    }

    companion object {
        private const val ACTION_MEMBER = "com.macerce.switchguard.GROUP_WIDGET_MEMBER"
        private const val ACTION_ALL = "com.macerce.switchguard.GROUP_WIDGET_ALL"
        private const val EXTRA_DEVICE = "device"
        private const val EXTRA_CHANNEL = "channel"
        /** Büyük kutucuğa sığan en çok üye; fazlası "+N" ile uygulamayı açar. */
        private const val MAX_MEMBERS = 12

        private fun prefs(context: Context) = context.getSharedPreferences("switchguard_group_widgets", Context.MODE_PRIVATE)

        fun bind(context: Context, widgetId: Int, groupId: String) {
            prefs(context).edit().putString(widgetId.toString(), groupId).apply()
        }

        private fun group(context: Context, widgetId: Int): DeviceGroup? {
            val id = prefs(context).getString(widgetId.toString(), null) ?: return null
            return Store.get(context).groups.firstOrNull { it.id == id }
        }

        /** Durumlar değişince çağrılır ([DeviceWidget.updateAll] ile birlikte); widget yoksa hiçbir şey yapmaz. */
        fun updateAll(context: Context) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, GroupWidget::class.java))
                if (ids.isEmpty()) return
                val byId = Store.get(context).snapshots.associateBy { it.id }
                ids.forEach { manager.updateAppWidget(it, views(context, it, byId)) }
            }
        }

        private fun views(context: Context, widgetId: Int, byId: Map<String, DeviceSnapshot>): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_group)
            views.removeAllViews(R.id.widget_rows)
            val group = group(context, widgetId)
            if (group == null) {
                views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_group_choose))
                views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_group_reconfigure))
                views.setViewVisibility(R.id.widget_all, View.GONE)
                views.setOnClickPendingIntent(R.id.widget_header, StatusSurfaces.openApp(context))
                return views
            }
            val (onCount, total) = group.counts(byId)
            val anyOn = onCount > 0
            views.setTextViewText(R.id.widget_title, group.name)
            views.setTextViewText(
                R.id.widget_status,
                context.getString(R.string.widget_group_counts, onCount, total) + (group.watts(byId)?.let { " · " + formatWatts(it) } ?: ""),
            )
            views.setViewVisibility(R.id.widget_all, View.VISIBLE)
            views.setTextViewText(R.id.widget_all, context.getString(if (anyOn) R.string.widget_group_all_off else R.string.widget_group_all_on))
            views.setOnClickPendingIntent(R.id.widget_all, broadcast(context, widgetId, ACTION_ALL, "all", null))
            views.setOnClickPendingIntent(R.id.widget_header, StatusSurfaces.openApp(context))

            val members = group.members.filter { byId[it.deviceId]?.switches?.containsKey(it.channel) == true }
            val shown = members.take(MAX_MEMBERS)
            shown.chunked(2).forEachIndexed { r, pair ->
                val row = RemoteViews(context.packageName, R.layout.widget_group_row)
                pair.forEachIndexed { c, m ->
                    val d = byId.getValue(m.deviceId)
                    row.addView(R.id.widget_row, cell(context, widgetId, r * 2 + c, d, m.channel))
                }
                // Tek kalan üye satırı yarıya kadar doldursun.
                if (pair.size == 1) row.addView(R.id.widget_row, RemoteViews(context.packageName, R.layout.widget_group_cell).apply {
                    setViewVisibility(R.id.cell_root, View.INVISIBLE)
                })
                views.addView(R.id.widget_rows, row)
            }
            if (members.isEmpty()) views.setTextViewText(R.id.widget_status, context.getString(R.string.widget_group_empty))
            views.setViewVisibility(R.id.widget_more, if (members.size > MAX_MEMBERS) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_more, context.getString(R.string.widget_group_more, members.size - MAX_MEMBERS))
            views.setOnClickPendingIntent(R.id.widget_more, StatusSurfaces.openApp(context))
            return views
        }

        private fun cell(context: Context, widgetId: Int, index: Int, d: DeviceSnapshot, channel: Int): RemoteViews {
            val cell = RemoteViews(context.packageName, R.layout.widget_group_cell)
            val on = d.online && d.switches[channel] == true
            cell.setTextViewText(R.id.cell_title, if (d.isMultiChannel) context.getString(R.string.label_channel, d.name, channel + 1) else d.name)
            cell.setTextViewText(
                R.id.cell_status,
                context.getString(when {
                    !d.online -> R.string.label_offline
                    on -> R.string.label_on
                    else -> R.string.label_off
                }) + DeviceWidget.wattsSuffix(context, d, channel, emptyMap()),
            )
            val white = 0xFFFFFFFF.toInt()
            cell.setInt(R.id.cell_root, "setBackgroundResource", if (on) R.drawable.widget_bg_on else R.drawable.widget_cell_bg)
            cell.setTextColor(R.id.cell_title, if (on) white else context.getColor(R.color.widget_text))
            cell.setTextColor(R.id.cell_status, if (on) white else context.getColor(R.color.widget_text_secondary))
            cell.setInt(R.id.cell_icon, "setColorFilter", if (on) white else context.getColor(R.color.widget_icon))
            cell.setOnClickPendingIntent(R.id.cell_root, broadcast(context, widgetId, ACTION_MEMBER, "m$index", d.id to channel))
            return cell
        }

        private fun broadcast(context: Context, widgetId: Int, action: String, key: String, member: Pair<String, Int>?): PendingIntent {
            val intent = Intent(context, GroupWidget::class.java).setAction(action)
                // Her düğmenin PendingIntent'i ayrı kalsın.
                .setData(Uri.parse("switchguard://group-widget/$widgetId/$key"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            member?.let { intent.putExtra(EXTRA_DEVICE, it.first).putExtra(EXTRA_CHANNEL, it.second) }
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
