package com.syed.slate.ui.screen

import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.syed.slate.ui.component.SlateLinearLoader
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.syed.slate.ui.component.BarTitle
import com.syed.slate.ui.component.SlateTopBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.syed.slate.BuildConfig
import com.syed.slate.backend.ModuleId
import com.syed.slate.content.ContentState
import com.syed.slate.content.SyncState
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.ThemeMode
import com.syed.slate.ui.component.UpdateCard
import com.syed.slate.ui.component.formatBytes
import com.syed.slate.ui.theme.LocalPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SlateViewModel, activity: Activity, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle("Settings", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The Written look: a spaced heading with a hairline over each card; cards are surface + outline.
            SettingsLabel("Appearance")
            Card {
                SettingRow("Theme") {
                    Segmented(ThemeMode.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, ThemeMode.entries.indexOf(vm.themeMode)) { vm.setTheme(ThemeMode.entries[it]) }
                }
                SettingRow("Hand") {
                    Segmented(listOf("Right", "Left"), if (vm.leftHand) 1 else 0) { vm.chooseLeftHand(it == 1) }
                }
            }
            SettingsLabel("Content & sync")
            ModuleId.entries.forEach { id -> ModuleCard(vm, activity, id) }
            SettingsLabel("Updates")
            UpdateCard(vm, showWhenIdle = true)
        }
    }
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        content()
    }
}

/** One connected pill of options (the current one filled in the accent), instead of loose chips. */
@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val accent = MaterialTheme.colorScheme.primary
    Row(Modifier.clip(CircleShape).background(p.bg).border(1.dp, p.outline, CircleShape).padding(3.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label, Modifier.clip(CircleShape).background(if (on) accent else Color.Transparent).clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelLarge, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                color = if (on) MaterialTheme.colorScheme.onPrimary else p.text2,
            )
        }
    }
}

/** A settings section heading: small spaced capitals and a hairline (the launcher's section labels). */
@Composable
private fun SettingsLabel(text: String) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, start = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = p.text3,
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.4f, androidx.compose.ui.unit.TextUnitType.Sp))
        Box(Modifier.weight(1f).height(1.dp).background(p.outline))
    }
}

@Composable
private fun ModuleCard(vm: SlateViewModel, activity: Activity, id: ModuleId) {
    val m = vm.module(id)
    val state by m.content.state.collectAsState()
    val sync by m.content.sync.collectAsState()
    val last by m.content.lastRefresh.collectAsState()
    val session by m.auth.session.collectAsState()
    val flagsWaiting by m.progress.unsynced.collectAsState()
    val trashWaiting by m.trash.queueOps.collectAsState()
    val hlWaiting by m.highlights.unsynced.collectAsState()
    val waiting = flagsWaiting + trashWaiting.size + hlWaiting
    val online by vm.online.collectAsState()
    val saved by vm.imagesSaved.collectAsState()
    val p = LocalPalette.current
    val ready = (state as? ContentState.Ready)?.content
    var confirmRemove by remember { mutableStateOf(false) }

    Card {
        // Header: module, and one status chip that says whether your changes are safe on the server.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(if (id == ModuleId.GENERAL) Icons.AutoMirrored.Filled.MenuBook else Icons.Filled.Bolt, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Text(id.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            when {
                session == null && waiting > 0 -> StatusChip("$waiting not synced", p.warn)
                waiting > 0 && !online -> StatusChip("$waiting waiting · offline", p.warn)
                waiting > 0 -> StatusChip("Syncing $waiting", p.info)
                session != null -> StatusChip("All synced", p.ok)
            }
        }

        // The offline copy in one quiet line; what the last refresh brought in under it.
        if (ready != null) {
            val total = remember(ready) { m.imageUrls(ready).size }
            Text(
                listOfNotNull("${ready.total} questions", formatBytes(m.content.cacheBytes), if (total > 0) "${minOf(saved, total)}/$total pictures" else null).joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium, color = p.text2,
            )
        } else {
            Text(if (state is ContentState.Empty) "Not downloaded yet." else "Loading…", style = MaterialTheme.typography.bodyMedium, color = p.text3)
        }
        when (val sy = sync) {
            is SyncState.Running -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SlateLinearLoader(Modifier.fillMaxWidth())
                Text(sy.message, style = MaterialTheme.typography.labelMedium, color = p.text3)
            }
            is SyncState.Failed -> Text("Refresh failed · ${sy.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            SyncState.Idle -> last?.let { r ->
                val d = r.delta
                val what = when {
                    r.firstDownload -> "first download"
                    d.isEmpty -> "nothing new"
                    else -> listOfNotNull(
                        d.added.takeIf { it > 0 }?.let { "$it new" },
                        d.updated.takeIf { it > 0 }?.let { "$it edited" },
                        d.removed.takeIf { it > 0 }?.let { "$it removed" },
                    ).joinToString(", ")
                }
                Text("Updated ${formatShortDate(r.at)} · $what", style = MaterialTheme.typography.bodySmall, color = p.text3)
            } ?: if (ready != null) Text("Updated ${formatShortDate(m.content.cachedAt)}", style = MaterialTheme.typography.bodySmall, color = p.text3) else Unit
        }

        // Two quiet actions on one line.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.refreshContent(id) }, enabled = online && sync !is SyncState.Running, contentPadding = PaddingValues(horizontal = 16.dp)) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                Text(if (!online) "Offline" else if (ready != null) "Refresh" else "Download")
            }
            if (ready != null) TextButton(onClick = { confirmRemove = true }) { Text("Remove copy", color = p.text3) }
        }

        // Account: one line — who is signed in, their counts, sign in / out.
        val acct = session
        if (acct == null) {
            if (m.config.googleWebClientId.isBlank()) {
                Text("Google sign-in isn't configured yet (set GOOGLE_WEB_CLIENT_ID_${id.name} in local.properties).", style = MaterialTheme.typography.bodySmall, color = p.warn)
            }
            OutlinedButton(onClick = { vm.signIn(activity, id) }, Modifier.fillMaxWidth(), enabled = online && m.config.googleWebClientId.isNotBlank()) { Text("Sign in with Google") }
        } else {
            val flags by m.progress.flags.collectAsState()
            val totals = remember(flags) { Triple(flags.values.count { it.nailed }, flags.values.count { it.important }, flags.values.count { it.weak }) }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.bg).padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(acct.email ?: "Signed in", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        androidx.compose.ui.text.buildAnnotatedString {
                            fun part(n: Int, label: String, c: Color) {
                                withStyle(androidx.compose.ui.text.SpanStyle(color = c, fontWeight = FontWeight.Bold)) { append("$n") }
                                append(" $label")
                            }
                            part(totals.first, "nailed", p.ok); append("  ·  "); part(totals.second, "important", p.imp); append("  ·  "); part(totals.third, "weak", p.warn)
                        },
                        style = MaterialTheme.typography.labelMedium, color = p.text3,
                    )
                }
                TextButton(onClick = { vm.signOut(id) }) { Text("Sign out", color = p.text3) }
            }
        }
        vm.authError?.takeIf { it.startsWith(id.title) }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }

    if (confirmRemove) AlertDialog(
        onDismissRequest = { confirmRemove = false },
        title = { Text("Remove ${id.title}'s offline copy?") },
        text = { Text("The questions are deleted from this phone until you download them again. Your flags and notes are kept.") },
        confirmButton = { TextButton(onClick = { confirmRemove = false; vm.clearContent(id) }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
    )
}


@Composable
private fun StatusChip(text: String, color: Color) {
    Text(
        text, Modifier.clip(CircleShape).background(color.copy(alpha = .14f)).padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1,
    )
}


/** "Oct 8, 4:51 PM" — the year only when it isn't this year. */
private fun formatShortDate(ms: Long): String {
    if (ms <= 0) return "—"
    val sameYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) ==
        java.util.Calendar.getInstance().apply { timeInMillis = ms }.get(java.util.Calendar.YEAR)
    return java.text.SimpleDateFormat(if (sameYear) "MMM d, h:mm a" else "MMM d yyyy, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(ms))
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(LocalPalette.current.surface)
            .border(1.dp, LocalPalette.current.outline, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}
