package com.macerce.switchguard.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Finding
import com.macerce.switchguard.core.Kpi
import com.macerce.switchguard.core.ReportDoc
import com.macerce.switchguard.core.ReportSection
import com.macerce.switchguard.core.ReportTable
import com.macerce.switchguard.core.Severity
import com.macerce.switchguard.core.Tone
import com.macerce.switchguard.ui.theme.StatusColors
import com.macerce.switchguard.ui.theme.tileColor

/**
 * Rapor ekranı (tam ekran pencere): başlık, "bu rapor neyi gösterir", açıklamalı göstergeler, bölümler, yapılacaklar,
 * terimler. [onPdf] / [onTelegram] verilirse altta düğmeleri çıkar.
 */
@Composable
fun ReportDialog(doc: ReportDoc, onDismiss: () -> Unit, onPdf: (() -> Unit)? = null, onTelegram: (() -> Unit)? = null) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                AppTopBar(doc.title, navigationIcon = { BackButton(onDismiss) }, small = true)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
                    ReportBody(doc)
                    if (onPdf != null || onTelegram != null) {
                        FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onPdf != null && !LocalIsTv.current) {
                                FilledTonalButton(onClick = onPdf, modifier = Modifier.tvFocusRing()) {
                                    Icon(Icons.Rounded.Description, null, Modifier.size(18.dp))
                                    Text(stringResource(R.string.report_save_pdf), Modifier.padding(start = 8.dp))
                                }
                            }
                            if (onTelegram != null) {
                                FilledTonalButton(onClick = onTelegram, modifier = Modifier.tvFocusRing()) {
                                    Icon(Icons.AutoMirrored.Rounded.Send, null, Modifier.size(18.dp))
                                    Text(stringResource(R.string.report_send_telegram), Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportBody(doc: ReportDoc) {
    val scheme = MaterialTheme.colorScheme
    // Başlık kartı
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(24.dp))
            .background(scheme.primary).padding(20.dp),
    ) {
        Text(doc.title, style = MaterialTheme.typography.headlineSmall, color = scheme.onPrimary, fontWeight = FontWeight.SemiBold)
        Text(doc.subtitle, style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimary.copy(alpha = 0.85f))
    }
    // Bu rapor neyi gösterir
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(20.dp))
            .background(scheme.primaryContainer.copy(alpha = 0.5f)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Info, null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.report_what_title), style = MaterialTheme.typography.labelLarge, color = scheme.primary, modifier = Modifier.padding(start = 6.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(doc.intro, style = MaterialTheme.typography.bodyMedium)
    }
    // Göstergeler: iki sütun
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        doc.kpis.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { KpiCard(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    doc.sections.forEach { SectionBlock(it) }
    if (doc.actions.isNotEmpty()) {
        SectionTitle(stringResource(R.string.report_actions_title))
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
    Row(modifier.height(IntrinsicSize.Min).clip(RoundedCornerShape(20.dp)).background(tileColor())) {
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
private fun SectionBlock(s: ReportSection) {
    SectionTitle(s.title)
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            s.explain?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            s.findings.forEach { FindingRow(it) }
            s.table?.let { TableBlock(it) }
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
                Text(stringResource(R.string.report_what_to_do, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TableBlock(t: ReportTable) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.clip(RoundedCornerShape(12.dp))) {
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
                Modifier.fillMaxWidth().tvFocusRing().clickable { open = !open }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.report_glossary_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
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
