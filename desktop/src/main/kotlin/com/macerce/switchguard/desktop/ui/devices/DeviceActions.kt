package com.macerce.switchguard.desktop.ui.devices

import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.api.TuyaClient
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceParser
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DeviceTimers
import com.macerce.switchguard.core.RepeatTimer
import com.macerce.switchguard.core.TimerEntry
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone

/**
 * Cihaz sayfasının bulut işlemleri (telefondaki `ui/Actions.kt`'in ilgili kısmı): zamanlayıcılar, canlı enerji isteği,
 * ham veri. Hepsi IO iş parçacığında çalışır ve sonucu [Result] olarak döner; motorun durumuna dokunmaz (eWeLink
 * oturum yenilemesi `EwelinkClient`'ın uygulama geneli kilidiyle motorla çakışmaz).
 */
class DeviceActions(
    private val store: Store,
    private val ewelink: () -> Ewelink = { RealEwelink(EwelinkClient(store)) },
    private val tuyaRaw: (String) -> JSONObject = { TuyaClient(store).rawProperties(it) },
) {
    /** Testlerde sahtesi verilebilen eWeLink çağrıları. */
    interface Ewelink {
        fun deviceParams(deviceId: String): JSONObject
        fun setTimers(deviceId: String, timers: JSONArray)
        fun requestLiveEnergy(device: DeviceSnapshot)
    }

    private class RealEwelink(private val client: EwelinkClient) : Ewelink {
        override fun deviceParams(deviceId: String) = client.getDeviceParams(deviceId)
        override fun setTimers(deviceId: String, timers: JSONArray) = client.setTimers(deviceId, timers)
        override fun requestLiveEnergy(device: DeviceSnapshot) = client.requestLiveEnergy(device)
    }

    /** Cihazdaki eWeLink zamanlayıcıları (uygulamanın tanımadıkları da dahil, korunmak üzere). */
    suspend fun loadTimers(deviceId: String): Result<List<TimerEntry>> = withContext(Dispatchers.IO) {
        runCatching { DeviceTimers.parse(ewelink().deviceParams(deviceId).optJSONArray("timers"), utcOffsetMinutes()) }
    }

    /**
     * Zamanlayıcıyı ekler/günceller ([timer] != null) ya da siler ([timer] == null, [id] ile).
     * Başka bir yerden (eWeLink uygulaması) yapılan değişiklikleri ezmemek için liste önce yeniden okunur.
     */
    suspend fun saveTimer(device: DeviceSnapshot, id: String, timer: RepeatTimer?): Result<List<TimerEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val client = ewelink()
                val offset = utcOffsetMinutes()
                val fresh = DeviceTimers.parse(client.deviceParams(device.id).optJSONArray("timers"), offset)
                val timers = DeviceTimers.rebuild(fresh, id, timer, device.isMultiChannel, offset)
                client.setTimers(device.id, timers)
                DeviceTimers.parse(timers, offset)
            }
        }

    /** Enerji ölçen cihazdan bir süre canlı güç ister (cihaz sayfası açıkken; yalnızca eWeLink). */
    suspend fun requestLiveEnergy(device: DeviceSnapshot): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { ewelink().requestLiveEnergy(device) }
    }

    /** Cihaz verisinin ham hali; destek/hata ayıklama için panoya kopyalanır. */
    suspend fun rawParams(device: DeviceSnapshot): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            when (device.cloud) {
                Cloud.TUYA -> tuyaRaw(device.id).toString(2)
                Cloud.DEMO -> JSONArray(DeviceParser.snapshotsToJson(listOf(device))).toString(2)
                Cloud.EWELINK -> ewelink().deviceParams(device.id).toString(2)
            }
        }
    }

    private fun utcOffsetMinutes() = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
}
