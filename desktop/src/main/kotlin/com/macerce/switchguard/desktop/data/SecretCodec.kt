package com.macerce.switchguard.desktop.data

import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.Win32Exception
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

/** Gizli alanların diskte nasıl saklanacağı: [encode] yazarken, [decode] okurken. */
interface SecretCodec {
    fun encode(plain: String): String

    /** Saklanan değeri açar; çözülemiyorsa (ör. başka bilgisayarın ya da kullanıcının şifrelediği değer) null. */
    fun decode(stored: String): String?
}

/**
 * Windows DPAPI (kullanıcı kapsamı): değer yalnızca aynı Windows kullanıcısı tarafından açılabilir.
 * Saklanan biçim: `"dpapi:" + Base64(blob)`. Önek taşımayan değer olduğu gibi döner (düz yazılmış eski kayıt).
 */
class DpapiCodec : SecretCodec {
    override fun encode(plain: String): String =
        PREFIX + Base64.getEncoder().encodeToString(Crypt32Util.cryptProtectData(plain.toByteArray(UTF_8)))

    override fun decode(stored: String): String? {
        if (!stored.startsWith(PREFIX)) return stored
        return try {
            String(Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(stored.substring(PREFIX.length))), UTF_8)
        } catch (_: Win32Exception) {
            null // başka kullanıcı/bilgisayar ya da bozuk blob
        } catch (_: IllegalArgumentException) {
            null // geçersiz Base64
        }
    }

    companion object {
        const val PREFIX = "dpapi:"
    }
}

/** Değeri aynen saklar; yalnızca testler için. */
object PlainCodec : SecretCodec {
    override fun encode(plain: String) = plain
    override fun decode(stored: String): String? = stored
}
