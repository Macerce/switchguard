package com.macerce.switchguard.core

/**
 * Widget yayınlarının doğrulanması. Widget alıcıları dışa kapalıdır (yalnızca sistem ve uygulamanın kendi
 * PendingIntent'leri ulaşır); bu kontroller ikinci savunma katmanıdır: gelen widget kimliği bu sağlayıcının gerçekten
 * eklenmiş widget'larından biri mi, grup widget'ında istenen cihaz/kanal o grubun üyesi mi.
 */
object WidgetGuard {
    /** AppWidgetManager.INVALID_APPWIDGET_ID. */
    const val INVALID_ID = 0

    /** [widgetId] bu sağlayıcının ekli widget'larından ([ownIds]) biri mi. */
    fun isOwnWidget(widgetId: Int, ownIds: IntArray): Boolean = widgetId != INVALID_ID && widgetId in ownIds

    /** Grup widget'ının üye düğmesi: yalnızca o grubun üyesi olan cihaz/kanal değiştirilebilir. */
    fun isGroupMember(group: DeviceGroup?, deviceId: String, channel: Int): Boolean =
        group != null && group.members.any { it.deviceId == deviceId && it.channel == channel }

    /** Yapılandırma ekranı yalnızca kendi sağlayıcımızın widget'ı için açılır (başka bir uygulama başka kimlik veremez). */
    fun isOwnProvider(providerClassName: String?, providerPackage: String?, ownPackage: String, expectedClassName: String): Boolean =
        providerPackage == ownPackage && providerClassName == expectedClassName
}
