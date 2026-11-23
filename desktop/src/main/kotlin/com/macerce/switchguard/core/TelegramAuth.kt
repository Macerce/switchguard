package com.macerce.switchguard.core

/**
 * Telegram'dan gelen komutlarda yetki ve komut yaşı (saf mantık).
 *
 * - Özel sohbet (kimlik > 0): listedeki kişi, aç/kapa izni açıksa kontrol edebilir (eskisi gibi).
 * - Grup (kimlik < 0): grubun her üyesi listedeki kişi sayılmaz. Grupta kontrol varsayılan olarak kapalıdır; açılsa da
 *   yalnızca seçilen kişiler ([Recipient.controllers], Telegram kullanıcı kimliği) aç/kapa yapabilir.
 * - Durumu değiştiren her komut (aç/kapat, süreli, hepsini, onay, döngü, sahne, evde/dışarıda) en çok
 *   [TelegramBot.SWITCH_MAX_AGE_MS] eski olabilir: telefon çevrimdışıyken yazılmış "hepsini kapat / evet" sonradan çalışmasın.
 * - "Hepsini kapat" onayı ve "hangi kanal?" sorusu sohbet + kişi anahtarıyla bekler: farklı sohbetlerdeki aynı adlı
 *   kişiler birbirinin onayını kullanamaz.
 */
object TelegramAuth {
    fun isGroup(chatId: Long): Boolean = chatId < 0

    /** Bu sohbette bu kişi cihazları kontrol edebilir mi ([globalControl]: Ayarlar'daki "Telegram'dan aç/kapa"). */
    fun canControl(recipient: Recipient?, fromId: Long, globalControl: Boolean): Boolean {
        if (!globalControl || recipient == null) return false
        if (!isGroup(recipient.id)) return recipient.control != false
        return recipient.control == true && fromId != 0L && fromId in recipient.controllers
    }

    /** Durumu değiştiren komut mu (daha kısa yaş sınırı ve kontrol izni gerekir). */
    fun changesState(intent: ChatCommand.Intent): Boolean = when (intent) {
        is ChatCommand.Intent.Switch, is ChatCommand.Intent.Batch, is ChatCommand.Intent.Timed,
        is ChatCommand.Intent.SwitchAll, is ChatCommand.Intent.Confirm, is ChatCommand.Intent.RunCycle,
        is ChatCommand.Intent.RunScene, is ChatCommand.Intent.SetPresence -> true
        else -> false
    }

    /** Komutun en çok ne kadar eski olabileceği. */
    fun maxAge(intent: ChatCommand.Intent): Long =
        if (changesState(intent)) TelegramBot.SWITCH_MAX_AGE_MS else TelegramBot.STATUS_MAX_AGE_MS

    /** Onay ve soru bekleme anahtarı: sohbet + kişi. */
    fun pendingKey(chatId: Long, fromId: Long): String = "tg:$chatId:$fromId"

    /** Gruplarda son yazan kişiler (seçim listesi için): en yeni başta, kişi başına bir kez, grup başına en çok [max]. */
    fun rememberSender(
        senders: Map<Long, List<Pair<Long, String>>>,
        chatId: Long,
        fromId: Long,
        name: String,
        max: Int = 20,
    ): Map<Long, List<Pair<Long, String>>> {
        if (!isGroup(chatId) || fromId == 0L) return senders
        val list = listOf(fromId to name) + senders[chatId].orEmpty().filter { it.first != fromId }
        return senders + (chatId to list.take(max))
    }
}
