package com.macerce.switchguard.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import com.macerce.switchguard.R
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.StatusKind
import com.macerce.switchguard.data.StatusSummary
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.MainActivity

/**
 * İzleme durumunu uygulama dışında gösteren yüzeyler: ana ekran widget'ı ve Hızlı Ayarlar kutucuğu.
 * Servis durum değiştikçe [refresh] çağırır; ikisi de aynı metni gösterir.
 */
object StatusSurfaces {
    private var lastShown: Pair<StatusKind, String>? = null

    fun current(context: Context): Pair<StatusKind, String> {
        val store = Store.get(context)
        val alarms = store.pendingAlarm.size
        val connection = LiveState.connection.value
        val kind = StatusSummary.kind(store.monitoringEnabled, alarms, connection.state)
        val count = store.monitoredIds().size
        val res = context.resources
        val text = when (kind) {
            StatusKind.ALARM -> res.getQuantityString(R.plurals.alarm_banner_title, alarms, alarms)
            StatusKind.STOPPED -> context.getString(R.string.state_stopped)
            StatusKind.LIVE -> res.getQuantityString(R.plurals.status_live, count, count)
            StatusKind.POLLING -> res.getQuantityString(R.plurals.status_polling, count, count, store.pollIntervalSec)
            StatusKind.CONNECTING -> context.getString(R.string.state_connecting)
            StatusKind.PROBLEM -> context.getString(
                when (connection.state) {
                    ConnState.NO_INTERNET -> R.string.state_no_internet
                    ConnState.AUTH_ERROR -> R.string.state_auth_error
                    else -> R.string.state_error
                }
            )
        }
        return kind to text
    }

    /** Durum metni değiştiyse widget'ı ve kutucuğu tazeler (aynıysa hiçbir şey yapmaz). */
    fun refresh(context: Context, force: Boolean = false) {
        val now = current(context)
        if (!force && now == lastShown) return
        lastShown = now
        StatusWidget.update(context, now)
        runCatching { TileService.requestListeningState(context, ComponentName(context, MonitorTile::class.java)) }
    }

    fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 5,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

/** Ana ekran widget'ı: kalkan, uygulama adı, durum; alarmda kırmızı, sorunda turuncu. Dokununca uygulama açılır. */
class StatusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context, StatusSurfaces.current(context))
    }

    companion object {
        fun update(context: Context, status: Pair<StatusKind, String>) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, StatusWidget::class.java))
            if (ids.isEmpty()) return
            val (kind, text) = status
            val views = RemoteViews(context.packageName, R.layout.widget_status).apply {
                setTextViewText(R.id.widget_status, text)
                setOnClickPendingIntent(R.id.widget_root, StatusSurfaces.openApp(context))
                val strong = kind == StatusKind.ALARM || kind == StatusKind.PROBLEM
                setInt(
                    R.id.widget_root, "setBackgroundResource",
                    when (kind) {
                        StatusKind.ALARM -> R.drawable.widget_bg_alarm
                        StatusKind.PROBLEM -> R.drawable.widget_bg_problem
                        else -> R.drawable.widget_bg
                    }
                )
                setImageViewResource(R.id.widget_icon, if (kind == StatusKind.ALARM) R.drawable.ic_stat_alarm else R.drawable.ic_stat_guard)
                // Kırmızı/turuncu zeminde yazı ve simge beyaz; normalde temanın renkleri.
                val fg = if (strong) 0xFFFFFFFF.toInt() else context.getColor(R.color.widget_text)
                val fg2 = if (strong) 0xFFFFFFFF.toInt() else context.getColor(R.color.widget_text_secondary)
                setTextColor(R.id.widget_title, fg)
                setTextColor(R.id.widget_status, fg2)
                setInt(R.id.widget_icon, "setColorFilter", if (strong) 0xFFFFFFFF.toInt() else context.getColor(R.color.widget_icon))
            }
            manager.updateAppWidget(ids, views)
        }
    }
}

/**
 * Hızlı Ayarlar kutucuğu: izleme açıkken etkin. Dokununca izlemeyi açar/kapatır; çalan alarm varsa uygulamayı açar
 * (alarm, kapatma düğmesinin olduğu yerde görülsün). Android arka plandan başlatmaya izin vermezse uygulama açılır
 * ve izleme oradan başlar.
 */
class MonitorTile : TileService() {
    override fun onStartListening() = render()

    override fun onTileAdded() = render()

    override fun onClick() {
        val store = Store.get(this)
        when {
            store.pendingAlarm.isNotEmpty() -> openAppAndCollapse()
            store.monitoringEnabled -> MonitorService.send(this, MonitorService.ACTION_STOP)
            else -> runCatching { MonitorService.send(this, MonitorService.ACTION_START) }.onFailure {
                // Uygulama açılınca izleme açık görünür ve MainActivity servisi başlatır.
                store.monitoringEnabled = true
                openAppAndCollapse()
            }
        }
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val (kind, text) = StatusSurfaces.current(this)
        tile.state = if (kind == StatusKind.STOPPED) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
        tile.label = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = text
        tile.contentDescription = text
        tile.icon = Icon.createWithResource(this, if (kind == StatusKind.ALARM) R.drawable.ic_stat_alarm else R.drawable.ic_stat_guard)
        tile.updateTile()
    }

    // Intent'li sürüm yalnızca Android 14 öncesinde çağrılır (14+ PendingIntent'li sürümü kullanır).
    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openAppAndCollapse() {
        if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(StatusSurfaces.openApp(this))
        else startActivityAndCollapse(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
