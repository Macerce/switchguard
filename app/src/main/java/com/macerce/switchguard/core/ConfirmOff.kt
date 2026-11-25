package com.macerce.switchguard.core

/**
 * "Kapatırken onay iste": derin dondurucu, pompa gibi kritik cihazlar elle kapatılırken önce sorulur (uygulama,
 * bekleme ekranı, sesli komut/Telegram). Açmak sorulmaz; otomasyonlar ve döngüler hiç sormaz.
 * Grup ya da alan kapatılırken, kapanacak üyelerden biri onaylıysa sorulur. Saf; veriler dışarıdan verilir.
 */
object ConfirmOff {
    fun needed(deviceId: String, on: Boolean, confirmIds: Set<String>, groups: List<DeviceGroup>, byId: Map<String, DeviceSnapshot>): Boolean {
        if (on || confirmIds.isEmpty()) return false
        if (!DeviceGroup.isGroup(deviceId)) return deviceId in confirmIds
        val group = groups.firstOrNull { it.deviceId == deviceId } ?: return false
        return group.targets(byId, false).any { (d, _) -> d.id in confirmIds }
    }
}
