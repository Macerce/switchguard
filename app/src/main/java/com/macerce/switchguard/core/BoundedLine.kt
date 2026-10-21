package com.macerce.switchguard.core

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Ev ağındaki aktarım bağlantıları için tek satır okuma. `BufferedReader.readLine` satırın tamamını belleğe alır;
 * ağdaki biri hiç bitmeyen bir satır gönderip uygulamanın belleğini doldurabilirdi. Burada sınır okurken uygulanır.
 */
object BoundedLine {
    /** "\n"e kadar okur (sondaki "\r" atılır). Satır [maxBytes]'ı aşarsa ya da hiç veri gelmezse null. */
    fun read(input: InputStream, maxBytes: Int): String? {
        val buf = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b < 0) {
                if (buf.size() == 0) return null
                break
            }
            if (b == '\n'.code) break
            if (buf.size() >= maxBytes) return null
            buf.write(b)
        }
        return buf.toString(Charsets.UTF_8.name()).removeSuffix("\r")
    }
}
