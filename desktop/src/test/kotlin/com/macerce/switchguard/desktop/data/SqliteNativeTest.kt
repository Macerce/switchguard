package com.macerce.switchguard.desktop.data

import org.junit.Assert.assertEquals
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
}
