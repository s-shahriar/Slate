package com.syed.slate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.syed.slate.backend.ModuleId
import com.syed.slate.progress.FlagRules
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.component.BarTitle
import com.syed.slate.ui.component.SlateTopBar
import com.syed.slate.ui.component.SwitchEntry
import com.syed.slate.ui.component.TopicSwitchButton
import com.syed.slate.ui.component.TopicSwitcherSheet
import com.syed.slate.ui.theme.LocalPalette
import com.syed.slate.ui.theme.isDark
import com.syed.slate.ui.web.FormulaPage
import com.syed.slate.ui.web.PageKind

private fun escD(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/** General » Written » Data: the datasets page from content_blobs written/data (general-quiz content/data/data.json →
 *  scripts/sync-data-page.mjs), shown like a Focus page — every dataset box starrable (uids 'f…' on gid "data", the
 *  same as the web), Important-only filter, highlights, and a section jump list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrittenDataPage(vm: SlateViewModel, onBack: () -> Unit) {
    val m = vm.module(ModuleId.GENERAL)
    val ver by m.blobs.version.collectAsState()
    val page = remember(ver) { m.blobs.payload("written", "data") }
    val p = LocalPalette.current
    val ground = if (p.isDark) MaterialTheme.colorScheme.background else Color.White
    var importantOnly by remember { mutableStateOf(false) }
    var switcher by remember { mutableStateOf(false) }
    // Dataset picker (top-bar switcher): null = all datasets (default), otherwise only that one.
    var only by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val flags by m.progress.flags.collectAsState()
    val important = remember(flags) { flags.filter { it.key.startsWith("f") && it.value.important }.keys }
    val sections = remember(page) {
        page?.optJSONArray("sections")?.let { a -> List(a.length()) { a.getJSONObject(it).let { s -> s.getString("id") to s.getString("label") } } }.orEmpty()
    }

    Scaffold(
        containerColor = ground,
        topBar = {
            SlateTopBar(
                title = { BarTitle("Data", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (page != null) {
                        IconButton(onClick = { importantOnly = !importantOnly }) {
                            Icon(if (importantOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Important only", tint = if (importantOnly) p.imp else androidx.compose.material3.LocalContentColor.current)
                        }
                        TopicSwitchButton { switcher = true }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ground),
            )
        },
    ) { pad ->
        if (page == null) {
            Column(Modifier.fillMaxSize().padding(pad).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("এই পেজটি এখনো ডাউনলোড হয়নি।", style = MaterialTheme.typography.titleMedium)
                Text("একবার ইন্টারনেটে যুক্ত হয়ে অ্যাপটি খুলুন, তারপর অফলাইনেও কাজ করবে।", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }
        val body = remember(page, only) {
            buildString {
                append("""<div style="--fc:var(--accent)" data-gid="data"><header class="fc-head" id="fc-top"><h1 class="fc-title">${escD(page.optString("title"))}</h1>""")
                append("""<p class="fc-topics">${escD(page.optString("subtitle"))}</p></header><article class="fc-content">""")
                // One line per dataset box (scripts/sync-data-page.mjs), so a pick keeps just that line.
                val html = page.optString("html")
                append(if (only == null) html else html.lines().filter { it.contains("id=\"$only\"") }.joinToString("\n")).append("</article></div>")
            }
        }
        val ctl = com.syed.slate.ui.highlight.LocalHighlights.current
        val saved by (ctl?.repo?.byUid ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap<String, List<com.syed.slate.highlight.Highlight>>()) }).collectAsState()
        val onPage = remember(saved) { saved.filterKeys { it.startsWith("f") } }
        val owner = remember { Any() }
        var clearNonce by remember { mutableIntStateOf(0) }
        Box(Modifier.fillMaxSize().padding(pad)) {
            FormulaPage(
                kind = PageKind.FOCUS, body = body, palette = p, dark = p.isDark, cover = false,
                importantOnly = importantOnly, important = important,
                onToggleImportant = { uid -> m.progress.update(uid, FlagRules::toggleImportant) },
                highlights = onPage, clearSelectionNonce = clearNonce,
                onSelection = { uid, anchors ->
                    ctl?.target = if (anchors.isEmpty()) ctl?.target?.takeIf { it.owner !== owner }
                    else com.syed.slate.ui.highlight.HlTarget.Add(owner, uid, anchors, anchors.joinToString(" ") { it.quote }) { clearNonce++ }
                },
                onMark = { _, ids, color -> ctl?.target = com.syed.slate.ui.highlight.HlTarget.Edit(owner, ids, color) { } },
            )
        }
        if (switcher) TopicSwitcherSheet(
            title = "Data",
            topics = listOf(SwitchEntry("data", "সব ডেটা (${sections.size}টি)")),
            current = "data",
            onTopic = { switcher = false },
            onDismiss = { switcher = false },
            subtopics = sections.map { (id, label) -> SwitchEntry(id, label) },
            currentSub = only,
            onSubtopic = { sid -> switcher = false; only = sid },
        )
    }
}
