package com.macerce.ewelinkalarm.core

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** eWeLink (CoolKit v2) imzası: Base64(HMAC-SHA256(appSecret, mesaj)). */
object Signer {
    fun sign(message: String, appSecret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(appSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }
}
