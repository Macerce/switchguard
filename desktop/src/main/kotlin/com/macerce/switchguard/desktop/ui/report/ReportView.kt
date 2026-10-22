package com.macerce.switchguard.desktop.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import com.macerce.switchguard.desktop.ui.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.Finding
import com.macerce.switchguard.core.Kpi
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.ReportSection
import com.macerce.switchguard.core.ReportTable
import com.macerce.switchguard.core.Severity
import com.macerce.switchguard.core.Tone
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.StatusColors
import com.macerce.switchguard.desktop.ui.str
import com.macerce.switchguard.desktop.ui.tileColor
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Rapor penceresi (telefondaki tam ekran `ReportDialog`): ayrı, boyutlandırılabilir bir pencere; başlık, "bu rapor neyi
 * gösterir", açıklamalı göstergeler, bölümler, yapılacaklar, terimler. [onPdf] / [onTelegram] verilirse altta düğmeler.
 * Esc ya da pencereyi kapatmak [onDismiss].
 */
@Composable
fun ReportWindow(doc: ReportDoc, onDismiss: () -> Unit, onPdf: (() -> Unit)? = null, onTelegram: (() -> Unit)? = null) {
    DialogWindow(
        onCloseRequest = onDismiss,
        state = rememberDialogState(size = DpSize(860.dp, 820.dp)),
        title = "SwitchGuard · " + doc.title,
        onPreviewKeyEvent = {
            if (it.type == androidx.compose.ui.input.key.KeyEventType.KeyDown && it.key == androidx.compose.ui.input.key.Key.Escape) { onDismiss(); true } else false
        },
    ) {
        val dark = com.macerce.switchguard.desktop.ui.LocalDarkTheme.current
        androidx.compose.runtime.LaunchedEffect(dark) { com.macerce.switchguard.desktop.platform.TitleBar.forTheme(window, dark) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 16.dp)) {
                    ReportBody(doc)
                }
                if (onPdf != null || onTelegram != null) {
                    androidx.compose.material3.HorizontalDivider()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        if (onTelegram != null) {
                            com.macerce.switchguard.desktop.ui.OutlinedButton(onClick = onTelegram, modifier = Modifier.testTag("report-telegram")) {
                                Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(18.dp))
                                Text(str("report_send_telegram"), Modifier.padding(start = 8.dp))
                            }
                        }
                        if (onPdf != null) {
                            com.macerce.switchguard.desktop.ui.Button(onClick = onPdf, modifier = Modifier.testTag("report-pdf")) {
                                Icon(Icons.Rounded.Description, null, Modifier.size(18.dp))
                                Text(str("report_save_pdf"), Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Farklı kaydet" penceresi; uzantı yoksa [ext] eklenir; vazgeçilirse null. Arayüz iş parçacığından. */
fun chooseSaveTarget(defaultName: String, title: String, ext: String): Path? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE).apply { file = defaultName }
    dialog.isVisible = true
    val p = Paths.get(dialog.directory ?: return null, dialog.file ?: return null)
    return if (p.fileName.toString().endsWith(".$ext", ignoreCase = true)) p else p.resolveSibling("${p.fileName}.$ext")
}

@Composable
fun ReportBody(doc: ReportDoc) {
    val scheme = MaterialTheme.colorScheme
    // Başlık kartı
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(6.dp))
            .background(scheme.primary).padding(20.dp),
    ) {
        Text(doc.title, style = MaterialTheme.typography.headlineSmall, color = scheme.onPrimary, fontWeight = FontWeight.SemiBold)
        Text(doc.subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimary.copy(alpha = 0.85f))
    }
    // Bu rapor neyi gösterir
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(10.dp))
            .background(scheme.primaryContainer.copy(alpha = 0.5f)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            Text(str("report_what_title"), style = MaterialTheme.typography.labelLarge, color = scheme.primary, modifier = Modifier.padding(start = 6.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(doc.intro, style = MaterialTheme.typography.bodyMedium)
    }
    // Göstergeler: iki sütun
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        doc.kpis.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { KpiCard(it, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    doc.sections.forEach { SectionBlock(it) }
    if (doc.actions.isNotEmpty()) {
        SectionTitle(str("report_actions_title"))
        SectionCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                doc.actions.forEachIndexed { i, a ->
                    Row {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = scheme.onPrimary)
                        }
                        Text(a, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
    }
    if (doc.glossary.isNotEmpty()) Glossary(doc.glossary)
    if (doc.footnote.isNotEmpty()) {
        Text(doc.footnote, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
    }
}

@Composable
private fun KpiCard(k: Kpi, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier.height(IntrinsicSize.Min).clip(RoundedCornerShape(10.dp)).background(tileColor())) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(toneColor(k.tone)))
        Column(Modifier.padding(12.dp)) {
            Text(k.label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1)
            Text(k.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            k.trend?.let { Text(it.text, style = MaterialTheme.typography.labelMedium, color = toneColor(it.tone), maxLines = 2) }
            Spacer(Modifier.height(4.dp))
            Text(k.caption, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionBlock(sec: ReportSection) {
    SectionTitle(sec.title)
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sec.explain?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            sec.findings.forEach { FindingRow(it) }
            sec.table?.let { TableBlock(it) }
        }
    }
}

@Composable
fun FindingRow(f: Finding) {
    val (icon, color) = when (f.severity) {
        Severity.ALERT -> Icons.Rounded.ErrorOutline to MaterialTheme.colorScheme.error
        Severity.WARN -> Icons.Rounded.WarningAmber to StatusColors.warning
        Severity.INFO -> Icons.Rounded.Info to MaterialTheme.colorScheme.primary
        Severity.GOOD -> Icons.Rounded.CheckCircle to StatusColors.online
    }
    Row {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Column(Modifier.padding(start = 10.dp)) {
            Text(f.text, style = MaterialTheme.typography.bodyMedium)
            f.action?.let {
                Text(str("report_what_to_do", it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TableBlock(t: ReportTable) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.clip(RoundedCornerShape(6.dp))) {
        TableRow(t.headers, header = true, zebra = false)
        t.rows.forEachIndexed { i, r -> TableRow(r, header = false, zebra = i % 2 == 1) }
    }
    t.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant) }
}

@Composable
private fun TableRow(values: List<String>, header: Boolean, zebra: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(if (zebra) scheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        values.forEachIndexed { i, v ->
            Text(
                v,
                style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                color = if (header) scheme.onSurfaceVariant else scheme.onSurface,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                maxLines = 2,
                modifier = Modifier.weight(if (i == 0) 1.6f else 1f),
            )
        }
    }
}

@Composable
private fun Glossary(items: List<Pair<String, String>>) {
    var open by remember { mutableStateOf(false) }
    SectionCard(Modifier.padding(top = 16.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(str("report_glossary_title"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            if (open) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { (k, v) ->
                        Column {
                            Text(k, style = MaterialTheme.typography.labelLarge)
                            Text(v, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun toneColor(t: Tone): Color = when (t) {
    Tone.GOOD -> StatusColors.online
    Tone.NEUTRAL -> MaterialTheme.colorScheme.primary
    Tone.WARN -> StatusColors.warning
    Tone.BAD -> MaterialTheme.colorScheme.error
}
