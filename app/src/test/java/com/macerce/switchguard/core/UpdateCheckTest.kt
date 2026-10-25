package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckTest {

    private val releases = """[
      {"tag_name":"desktop-v0.7.2","html_url":"https://x/d072","draft":false,"prerelease":false,
       "assets":[{"name":"SwitchGuard-0.7.2-Kurulum.exe","browser_download_url":"https://x/d072.exe"}]},
      {"tag_name":"v2.17.0-beta","html_url":"https://x/beta","draft":false,"prerelease":true,"assets":[]},
      {"tag_name":"v2.16.1","html_url":"https://x/v2161","draft":true,"prerelease":false,"assets":[]},
      {"tag_name":"v2.16.0","html_url":"https://x/v2160","draft":false,"prerelease":false,
       "assets":[{"name":"SwitchGuard-Watch-2.15.1.apk","browser_download_url":"https://x/w.apk"},
                 {"name":"SwitchGuard-2.16.0.apk","browser_download_url":"https://x/2160.apk"}]},
      {"tag_name":"v2.9.0","html_url":"https://x/v290","draft":false,"prerelease":false,"assets":[]},
      {"tag_name":"desktop-v0.7.1","html_url":"https://x/d071","draft":false,"prerelease":false,"assets":[]}
    ]"""

    @Test
    fun phoneSeesOnlyPublishedPhoneTags() {
        val r = UpdateCheck.newer(releases, UpdateCheck.PHONE_TAG, "2.15.2", "SwitchGuard-2.16.0.apk")
        assertEquals(UpdateCheck.Release("2.16.0", "https://x/v2160", "https://x/2160.apk"), r)
        assertNull(UpdateCheck.newer(releases, UpdateCheck.PHONE_TAG, "2.16.0"))
        assertNull(UpdateCheck.newer(releases, UpdateCheck.PHONE_TAG, "2.16.0-kisisel"))
    }

    @Test
    fun desktopPicksHighestDesktopTagAndItsInstaller() {
        val r = UpdateCheck.newer(releases, UpdateCheck.DESKTOP_TAG, "0.7.0", ".exe")
        assertEquals(UpdateCheck.Release("0.7.2", "https://x/d072", "https://x/d072.exe"), r)
        assertNull(UpdateCheck.newer(releases, UpdateCheck.DESKTOP_TAG, "0.7.2", ".exe"))
    }

    @Test
    fun versionsCompareNumerically() {
        assertTrue(UpdateCheck.compare("2.10.0", "2.9.9") > 0)
        assertTrue(UpdateCheck.compare("1.0", "1.0.0") == 0)
        assertTrue(UpdateCheck.compare("0.7.0", "0.7.1") < 0)
        assertTrue(UpdateCheck.compare("abc", "0.0.1") < 0)
    }
}
