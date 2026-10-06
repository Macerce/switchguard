package com.macerce.switchguard.desktop.data

import java.nio.file.Files
import java.nio.file.Path

/**
 * sqlite-jdbc kendi yerel kitaplığını (DLL) ilk bağlantıda bir klasöre çıkarıp yükler; varsayılan klasör `%TEMP%`'tir.
 * `%TEMP%` temizleyicileri ve virüs tarayıcıları orada yeni çıkarılan DLL'leri silebilir ya da engelleyebilir
 * (`UnsatisfiedLinkError`: veritabanı açılamaz). [useDataDir] çıkarma yerini uygulamanın veri klasörü altına
 * (`<dataDir>\native`) alır; ilk veritabanı bağlantısından **önce** çağrılmalıdır (kitaplık süreç başına bir kez yüklenir).
 */
object SqliteNative {
    const val PROPERTY = "org.sqlite.tmpdir"

    /** `<dataDir>\native` klasörünü kurar ve sqlite-jdbc'yi oraya yönlendirir; klasörü döner. */
    fun useDataDir(dataDir: Path): Path {
        val dir = dataDir.resolve("native")
        Files.createDirectories(dir) // sqlite-jdbc klasörü kendisi kurmaz
        System.setProperty(PROPERTY, dir.toString())
        return dir
    }
}
