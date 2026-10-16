package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceKind
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/** Ortak bileşenlerin görsel örneği (açık + koyu); `build/snapshots/components-*.png`. */
class ComponentsSnapshotTest {
    @Composable
    private fun Gallery() {
        Column(Modifier.padding(vertical = 8.dp)) {
            PageHeader("Ayarlar")
            SectionTitle("Bağlantılar")
            SectionCard {
                MenuRow(Icons.Rounded.Hub, Hue.TEAL, "Hesaplar", onClick = {}, summary = "eWeLink · Tuya")
                RowDivider(68.dp)
                MenuRow(Icons.Rounded.NotificationsActive, Hue.RED, "Alarm", onClick = {}, warning = "Ses kapalı")
                RowDivider(68.dp)
                MenuRow(Icons.Rounded.Bedtime, Hue.INDIGO, "Sessiz saatler", onClick = {}, summary = "23:00 – 07:00")
            }
            SectionTitle("Satırlar")
            SectionCard {
                InfoRow(Icons.Rounded.Schedule, "Başlangıç", "23:00")
                RowDivider()
                InfoRow(Icons.Rounded.Bedtime, "Sessiz saatler") { Switch(checked = true, onCheckedChange = {}) }
            }
            Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PowerButton(on = true, busy = false, enabled = true, onToggle = {})
                PowerButton(on = false, busy = false, enabled = true, onToggle = {})
                PowerButton(on = true, busy = true, enabled = true, onToggle = {})
                PowerButton(on = false, busy = false, enabled = false, onToggle = {})
                Box(Modifier.clip(RoundedCornerShape(16.dp)).background(Brand.active(LocalDarkTheme.current)).padding(8.dp)) {
                    PowerButton(on = true, busy = false, enabled = true, onToggle = {}, onBrand = true)
                }
                DeviceKind.entries.forEach { IconBadge(it.icon(), tint = Brand.deep, background = raisedOnTile()) }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(60.dp).clip(RoundedCornerShape(24.dp)).background(Brand.guard(LocalDarkTheme.current)))
        }
    }

    @Test
    fun gallery() {
        for (dark in listOf(false, true)) {
            val out = Snapshots.render("components-" + if (dark) "dark" else "light", width = 900, height = 720, dark = dark) { Gallery() }
            assertTrue(Files.size(out) > 1000)
        }
    }
}
