package com.macerce.switchguard.desktop.data

import com.macerce.switchguard.desktop.Log
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * sqlite-jdbc kendi yerel kitaplığını (DLL) ilk bağlantıda bir klasöre çıkarıp yükler; varsayılan klasör `%TEMP%`'tir.
 * `%TEMP%` temizleyicileri ve virüs tarayıcıları orada yeni çıkarılan DLL'leri silebilir ya da engelleyebilir
 * (`UnsatisfiedLinkError`: veritabanı açılamaz). [useDataDir] çıkarma yerini uygulamanın veri klasörü altına
 * (`<dataDir>\native`) alır; ilk veritabanı bağlantısından **önce** çağrılmalıdır (kitaplık süreç başına bir kez yüklenir).
 *
 * Önceki açılışlardan kalanlar silinir: sqlite-jdbc eski DLL'i yalnızca yanındaki `.lck` yoksa siler, zorla kapatılan
 * (Görev Yöneticisi, elektrik kesintisi) her açılış ise ikisini de bırakır; her biri ~1 MB birikirdi. Klasör yalnızca
 * sqlite-jdbc içindir ve tek örnek kilidi alındıktan sonra çağrılır: başka bir örnek bu dosyaları kullanıyor olamaz.
 */
object SqliteNative {
    const val PROPERTY = "org.sqlite.tmpdir"

    /** `<dataDir>\native` klasörünü kurar, önceki açılışların dosyalarını siler ve sqlite-jdbc'yi oraya yönlendirir; klasörü döner. */
    fun useDataDir(dataDir: Path): Path {
        val dir = dataDir.resolve("native")
        Files.createDirectories(dir) // sqlite-jdbc klasörü kendisi kurmaz
        removeLeftovers(dir)
        System.setProperty(PROPERTY, dir.toString())
        return dir
    }

    /**
     * Silinemeyen dosya (ör. virüs tarayıcısı o an tutuyor) yalnızca günlüğe yazılır; sonraki açılış yeniden dener.
     * Temizlik hiçbir zaman fırlatmaz: yönlendirme ([PROPERTY]) temizlik başarısız olsa da yapılır.
     */
    private fun removeLeftovers(dir: Path) {
        try {
            Files.newDirectoryStream(dir).use { files ->
                for (f in files) {
                    if (!Files.isRegularFile(f)) continue
                    runCatching { Files.delete(f) }.onFailure { Log.w(TAG, "could not remove old native file ${f.fileName}: $it") }
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "could not list $dir: $e")
        }
    }

    private const val TAG = "SqliteNative"
}
