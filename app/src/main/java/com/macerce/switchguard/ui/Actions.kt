package com.macerce.switchguard.ui

import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.api.TuyaClient
import com.macerce.switchguard.BuildConfig
import android.os.Build
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.macerce.switchguard.R
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceParser
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DeviceTimers
import com.macerce.switchguard.core.RepeatTimer
import com.macerce.switchguard.core.TimerEntry
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.DailySummary
import com.macerce.switchguard.service.MonitorService
import com.macerce.switchguard.service.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

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
        } else if (store.hasAnyAccount) {
            Thread {
                val ewelink = if (store.isLoggedIn) runCatching { EwelinkClient(store).getDevices() }.getOrNull() else emptyList()
                val tuya = if (store.hasTuya) runCatching { TuyaClient(store).getDevices() }.getOrNull() else emptyList()
                val old = store.snapshots
                // Hata veren bulutun eski listesi korunur.
                store.snapshots = (ewelink ?: old.filter { it.cloud == Cloud.EWELINK }) + (tuya ?: old.filter { it.cloud == Cloud.TUYA }) +
                    old.filter { it.cloud == Cloud.DEMO }
            }.start()
        }
    }

    /** Cihazı uygulamadan açar/kapatır. Bu değişiklik alarm çaldırmaz, geçmişe "uygulamadan" diye yazılır. */
    suspend fun setSwitch(context: Context, device: DeviceSnapshot, channel: Int, on: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val store = Store.get(context)
                LiveState.expected.expect(device.id, channel, on, System.currentTimeMillis())
                when (device.cloud) {
                    Cloud.TUYA -> TuyaClient(store).setSwitch(device, channel, on)
                    Cloud.EWELINK -> EwelinkClient(store).setSwitch(device, channel, on)
                    Cloud.DEMO -> Unit
                }
                val service = MonitorService.instance
                if (store.monitoringEnabled && service != null) {
                    service.onControlled(device.id, channel, on)
                } else {
                    // İzleme kapalı (ya da servis henüz yok): durumu doğrudan güncelle.
                    store.snapshots = store.snapshots.map {
                        if (it.id == device.id) Demo.recompute(it.copy(switches = it.switches + (channel to on))) else it
                    }
                }
            }
        }

    /** Günlük özeti hemen bildirim olarak gösterir; hiç kayıt yoksa false. */
    suspend fun previewSummary(context: Context): Boolean = withContext(Dispatchers.IO) {
        val lines = DailySummary.build(context) ?: return@withContext false
        Notifier(context, Store.get(context)).notifySummary(context.getString(R.string.summary_title), lines)
        true
    }

    /** Cihazdaki eWeLink zamanlayıcıları (uygulamanın tanımadıkları da dahil, korunmak üzere). */
    suspend fun loadTimers(context: Context, deviceId: String): Result<List<TimerEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val params = EwelinkClient(Store.get(context)).getDeviceParams(deviceId)
            DeviceTimers.parse(params.optJSONArray("timers"), utcOffsetMinutes())
        }
    }

    /**
     * Zamanlayıcıyı ekler/günceller ([timer] != null) ya da siler ([timer] == null, [id] ile).
     * Başka bir yerden (eWeLink uygulaması) yapılan değişiklikleri ezmemek için liste önce yeniden okunur.
     */
    suspend fun saveTimer(context: Context, device: DeviceSnapshot, id: String, timer: RepeatTimer?): Result<List<TimerEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = EwelinkClient(Store.get(context))
                val offset = utcOffsetMinutes()
                val fresh = DeviceTimers.parse(client.getDeviceParams(device.id).optJSONArray("timers"), offset)
                val timers = DeviceTimers.rebuild(fresh, id, timer, device.isMultiChannel, offset)
                client.setTimers(device.id, timers)
                DeviceTimers.parse(timers, offset)
            }
        }

    /** Enerji ölçen cihazdan bir süre canlı güç ister (detay ekranı açıkken). */
    suspend fun requestLiveEnergy(context: Context, device: DeviceSnapshot) = withContext(Dispatchers.IO) {
        runCatching { EwelinkClient(Store.get(context)).requestLiveEnergy(device) }
    }

    /** Cihaz verisinin ham hali; destek/hata ayıklama için panoya kopyalanır. */
    suspend fun rawParams(context: Context, deviceId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val store = Store.get(context)
            val device = store.snapshots.firstOrNull { it.id == deviceId }
            when (device?.cloud) {
                Cloud.TUYA -> TuyaClient(store).rawProperties(deviceId).toString(2)
                Cloud.DEMO -> org.json.JSONArray(DeviceParser.snapshotsToJson(listOf(device))).toString(2)
                else -> EwelinkClient(store).getDeviceParams(deviceId).toString(2)
            }
        }
    }

    /**
     * Tuya bilgilerini dener; başarılıysa kaydeder ve bulunan cihaz sayısını döndürür.
     * Başarısızsa önceki bilgiler geri yüklenir (çalışan bir bağlantı yanlış girişle bozulmasın).
     */
    suspend fun connectTuya(context: Context, accessId: String, secret: String, region: String): Result<Int> =
        withContext(Dispatchers.IO) {
            val store = Store.get(context)
            val old = Triple(store.tuyaAccessId, store.tuyaSecret, store.tuyaRegion)
            store.tuyaAccessId = accessId
            store.tuyaSecret = secret
            store.tuyaRegion = region
            store.tuyaToken = ""
            runCatching {
                val devices = TuyaClient(store).getDevices()
                store.snapshots = store.snapshots.filter { it.cloud != Cloud.TUYA } + devices
                if (store.monitoringEnabled) MonitorService.send(context, MonitorService.ACTION_RESTART)
                devices.size
            }.onFailure {
                store.tuyaAccessId = old.first
                store.tuyaSecret = old.second
                store.tuyaRegion = old.third
                store.tuyaToken = ""
            }
        }

    fun disconnectTuya(context: Context) {
        val store = Store.get(context)
        store.disconnectTuya()
        if (store.monitoringEnabled) {
            if (store.hasAnyAccount) MonitorService.send(context, MonitorService.ACTION_RESTART) else stopMonitoring(context)
        }
    }

    /** Hesapsız deneme için demo cihazlarını açar. */
    fun enableDemo(context: Context) {
        Store.get(context).enableDemo(
            Demo.Names(
                freezer = context.getString(R.string.demo_freezer),
                pumps = context.getString(R.string.demo_pumps),
                light = context.getString(R.string.demo_light),
                stalledPump = context.getString(R.string.demo_auto_pump),
                lightLeftOn = context.getString(R.string.demo_auto_light),
            )
        )
    }

    fun disableDemo(context: Context) {
        val store = Store.get(context)
        store.disableDemo()
        if (store.monitoringEnabled && !store.hasAnyAccount) stopMonitoring(context)
    }

    /** Demo cihazında "dışarıdan" bir olay canlandırır (bkz. MonitorService.demoSimulate). */
    fun demoSimulate(context: Context, op: String, deviceId: String, channel: Int = 0) =
        MonitorService.demo(context, op, deviceId, channel)

    private fun utcOffsetMinutes() = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000

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

    /** E-posta uygulamasını destek adresiyle açar; destek için sürüm ve cihaz bilgisi gövdeye eklenir. */
    fun emailSupport(context: Context): Boolean {
        val body = "\n\n—\nSwitchGuard ${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}"
        val uri = Uri.parse("mailto:$CONTACT_EMAIL?subject=" + Uri.encode("SwitchGuard") + "&body=" + Uri.encode(body))
        return runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }.isSuccess
    }

    const val CONTACT_EMAIL = "switchguardapp@gmail.com"
    const val DEV_PORTAL_URL = "https://dev.ewelink.cc"
    const val TUYA_PLATFORM_URL = "https://platform.tuya.com/cloud"
    private const val GUIDE_BASE = "https://github.com/Macerce/switchguard/blob/master/docs/guide/"

    /** Resimli kurulum rehberi (GitHub); [cloud] "ewelink" ya da "tuya", dil uygulamanın diline göre. */
    fun guideUrl(context: Context, cloud: String): String {
        val lang = if (context.resources.configuration.locales[0].language == "tr") "tr" else "en"
        return "$GUIDE_BASE$cloud.$lang.md"
    }
    const val PRIVACY_URL = "https://sites.google.com/view/switchguard-privacy"
}
