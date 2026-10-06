package com.macerce.switchguard.desktop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class SqliteNativeTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun nativeLibraryIsExtractedUnderTheDataFolder() {
        val previous = System.getProperty(SqliteNative.PROPERTY)
        try {
            val dir = SqliteNative.useDataDir(tmp.root.toPath())

            assertEquals(tmp.root.toPath().resolve("native"), dir)
            assertTrue(Files.isDirectory(dir))
            assertEquals(dir.toString(), System.getProperty("org.sqlite.tmpdir"))
        } finally {
            if (previous == null) System.clearProperty(SqliteNative.PROPERTY) else System.setProperty(SqliteNative.PROPERTY, previous)
        }
    }

    @Test
    fun leftoversOfEarlierRunsAreRemoved() {
        // Zorla kapatılan (Görev Yöneticisi, elektrik kesintisi) her açılıştan kalanlar: DLL + kilidi. sqlite-jdbc
        // kilidi duran DLL'i silmez; her biri ~1 MB birikirdi.
        val native = Files.createDirectories(tmp.root.toPath().resolve("native"))
        val oldDll = Files.write(native.resolve("sqlite-3.53.4.0-1111-sqlitejdbc.dll"), ByteArray(1024))
        val oldLock = Files.createFile(native.resolve("sqlite-3.53.4.0-1111-sqlitejdbc.dll.lck"))
        val otherRun = Files.write(native.resolve("sqlite-3.53.4.0-2222-sqlitejdbc.dll"), ByteArray(1024))
        val previous = System.getProperty(SqliteNative.PROPERTY)
        try {
            SqliteNative.useDataDir(tmp.root.toPath())

            assertFalse(Files.exists(oldDll))
            assertFalse(Files.exists(oldLock))
            assertFalse(Files.exists(otherRun))
            assertTrue(Files.isDirectory(native))
        } finally {
            if (previous == null) System.clearProperty(SqliteNative.PROPERTY) else System.setProperty(SqliteNative.PROPERTY, previous)
        }
    }
}
