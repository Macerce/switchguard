package com.macerce.switchguard.desktop.platform

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

class AutostartTest {
    private val name = "SwitchGuardTest"
    private val runKey = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private val command = "\"C:\\x\\SwitchGuard.exe\" --tray"

    private val windows = System.getProperty("os.name").startsWith("Windows")
    private val autostart = Autostart(name)

    private fun stored(): String? =
        if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, runKey, name)) {
            Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, runKey, name)
        } else {
            null
        }

    @Before
    fun requireWindows() {
        assumeTrue(windows)
        cleanUp()
    }

    @After
    fun cleanUp() {
        // Yalnızca test değerini siler; gerçek "SwitchGuard" değerine asla dokunulmaz.
        if (windows && Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, runKey, name)) {
            Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, runKey, name)
        }
    }

    @Test
    fun setEnabledWritesRunValue() {
        autostart.setEnabled(true, command)

        assertEquals(command, stored())
        assertTrue(autostart.isEnabled())
    }

    @Test
    fun disableRemovesValue() {
        autostart.setEnabled(true, command)
        autostart.setEnabled(false)

        assertNull(stored())
        assertFalse(autostart.isEnabled())
    }

    @Test
    fun enablingRepairsAStaleCommand() {
        autostart.setEnabled(true, "\"C:\\old\\SwitchGuard.exe\" --tray")
        autostart.setEnabled(true, command)

        assertEquals(command, stored())
    }

    @Test
    fun disablingWhenAbsentIsHarmless() {
        autostart.setEnabled(false)
        assertFalse(autostart.isEnabled())
    }

    @Test
    fun enablingWithoutACommandWritesNothing() {
        autostart.setEnabled(true, null)
        assertNull(stored())
    }
}

class AutostartCommandTest {
    private val property = "jpackage.app-path"

    private fun withAppPath(value: String?, block: () -> Unit) {
        val old = System.getProperty(property)
        try {
            if (value == null) System.clearProperty(property) else System.setProperty(property, value)
            block()
        } finally {
            if (old == null) System.clearProperty(property) else System.setProperty(property, old)
        }
    }

    @Test
    fun defaultCommandQuotesThePathAndAddsTray() =
        withAppPath("C:\\Users\\Ali Veli\\AppData\\Local\\SwitchGuard\\SwitchGuard.exe") {
            assertEquals(
                "\"C:\\Users\\Ali Veli\\AppData\\Local\\SwitchGuard\\SwitchGuard.exe\" --tray",
                Autostart.defaultCommand(),
            )
        }

    @Test
    fun defaultCommandIsNullWhenNotPackaged() = withAppPath(null) {
        assertNull(Autostart.defaultCommand())
    }

    @Test
    fun unavailableWhenNotPackaged() = withAppPath(null) {
        assertFalse(Autostart().available)
    }
}
