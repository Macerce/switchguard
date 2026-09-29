package com.macerce.switchguard.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.MonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Arayüzden tetiklenen işlemler. Ağ çağrıları IO dispatcher'da çalışır. */
object Actions {

    fun startMonitoring(context: Context) = MonitorService.send(context, MonitorService.ACTION_START)
    fun stopMonitoring(context: Context) = MonitorService.send(context, MonitorService.ACTION_STOP)
    fun dismissAlarm(context: Context) = MonitorService.send(context, MonitorService.ACTION_DISMISS)
    fun testAlarm(context: Context) = MonitorService.send(context, MonitorService.ACTION_TEST)

    /** Giriş veya hesap ayarı değişince izleme açıksa bağlantıyı tazele. */
    fun afterLogin(context: Context) {
        if (Store.get(context).monitoringEnabled) MonitorService.send(context, MonitorService.ACTION_RESTART)
        else refreshBlocking(context)
    }

    /** Cihaz listesini tazeler: izleme açıksa servis yapar, değilse doğrudan çekilir. */
    suspend fun refresh(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { refreshBlocking(context) }
    }

    private fun refreshBlocking(context: Context) {
        val store = Store.get(context)
        if (store.monitoringEnabled) {
            MonitorService.send(context, MonitorService.ACTION_SYNC)
        } else if (store.isLoggedIn) {
            Thread { runCatching { store.snapshots = EwelinkClient(store).getDevices() } }.start()
        }
    }

    /** Cihazı uygulamadan açar/kapatır. Bu değişiklik alarm çaldırmaz, geçmişe "uygulamadan" diye yazılır. */
    suspend fun setSwitch(context: Context, device: DeviceSnapshot, channel: Int, on: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val store = Store.get(context)
                LiveState.expected.expect(device.id, channel, on, System.currentTimeMillis())
                EwelinkClient(store).setSwitch(device, channel, on)
                if (store.monitoringEnabled) {
                    MonitorService.send(context, MonitorService.ACTION_SYNC)
                } else {
                    // İzleme kapalı: durumu doğrudan güncelle.
                    store.snapshots = store.snapshots.map {
                        if (it.id == device.id) it.copy(switches = it.switches + (channel to on)) else it
                    }
                }
            }
        }

    fun notificationsEnabled(context: Context) =
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun batteryExempt(context: Context) =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    @android.annotation.SuppressLint("BatteryLife") // Uygulamanın temel işlevi sürekli izleme.
    fun requestBatteryExemption(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        runCatching { context.startActivity(direct) }
            .onFailure { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }

    fun openNotificationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }

    fun openUrl(context: Context, url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    const val DEV_PORTAL_URL = "https://dev.ewelink.cc"
    const val PRIVACY_URL = "https://github.com/macerce/switchguard/blob/main/docs/privacy-policy.md"
}
