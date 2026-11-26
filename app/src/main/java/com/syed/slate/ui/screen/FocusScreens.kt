package com.syed.slate.ui.screen

import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Storage
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syed.slate.backend.ModuleId
import com.syed.slate.progress.FlagRules
import com.syed.slate.content.BlobRepository
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.component.BarTitle
import com.syed.slate.ui.component.SlateTopBar
import com.syed.slate.ui.component.SwitchEntry
import com.syed.slate.ui.component.TopicSwitchButton
import com.syed.slate.ui.component.TopicSwitcherSheet
import com.syed.slate.ui.theme.LocalPalette
import com.syed.slate.ui.theme.TopicColors
import com.syed.slate.ui.theme.isDark
import com.syed.slate.ui.web.FormulaPage
import com.syed.slate.ui.web.PageKind

/** One tab of Written » Focus: the universal template ("u") or a group ("g1".."g6"). */
class FocusTab(val id: String, val n: Int, val title: String, val topics: String, val html: String, val sections: List<Pair<String, String>>, val topic: Boolean = false) {
    val full get() = if (n == 0 || topic) title else "গ্রুপ ${bn(n)}: $title"
}

private fun bn(n: Int) = n.toString().map { if (it.isDigit()) "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")

/** The same topic colour per group as the web (WrittenFocus.jsx GROUP_COLOR); 0 = the universal tab. */
private val GROUP_TOPIC = intArrayOf(0, 1, 4, 2, 11, 5, 8)
private val GROUP_ICON: List<ImageVector> = listOf(Icons.Filled.Dashboard, Icons.Filled.AccountBalance, Icons.Filled.PhoneAndroid, Icons.Filled.TrendingUp, Icons.Filled.LocalFireDepartment, Icons.Filled.Eco, Icons.Filled.Groups)

/** General » Written » Focus, from the General project's `content_blobs` (kind `written`, key `focus`). */
object FocusIndex {
    fun tabs(blobs: BlobRepository): List<FocusTab> {
        val p = blobs.payload("written", "focus") ?: return emptyList()
        fun secs(o: org.json.JSONObject) = o.getJSONArray("sections").let { a -> List(a.length()) { a.getJSONObject(it).let { s -> s.getString("id") to s.getString("label") } } }
        val u = p.getJSONObject("universal")
        val g = p.getJSONArray("groups")
        val q = p.optJSONObject("quotes")
        return listOf(FocusTab("u", 0, u.getString("title"), "", u.getString("html"), secs(u))) +
            (if (q != null) listOf(FocusTab("q", 0, q.getString("title"), "", q.getString("html"), secs(q))) else emptyList()) +
            List(g.length()) { i -> g.getJSONObject(i).let { o -> FocusTab(o.getString("id"), o.getInt("n"), o.getString("title"), o.getString("topics"), o.getString("html"), secs(o)) } }
    }

    /** A topic page under group [group] (content/focus/topics on the web), shown like a group but titled by the topic. */
    fun topic(blobs: BlobRepository, group: String, id: String): FocusTab? {
        val p = blobs.payload("written", "focus") ?: return null
        val a = p.optJSONArray("topics") ?: return null
        val g = tabs(blobs).firstOrNull { it.id == group } ?: return null
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (o.getString("id") == id && o.getString("group") == group) {
                val secs = o.getJSONArray("sections").let { x -> List(x.length()) { x.getJSONObject(it).let { s -> s.getString("id") to s.getString("label") } } }
                return FocusTab("$group/$id", g.n, o.getString("title"), g.full, o.getString("html"), secs, topic = true)
            }
        }
        return null
    }
}

private fun groupColor(n: Int, dark: Boolean, accent: Color): Color = if (n == 0) accent else TopicColors.of(GROUP_TOPIC[n], dark)

/** The module's own ground in both modes (the same warm beige as the Written hub in light mode). */
@Composable
private fun focusGround(): Color = MaterialTheme.colorScheme.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHomeScreen(vm: SlateViewModel, onBack: () -> Unit, onOpen: (String) -> Unit, onData: () -> Unit) {
    val blobs = vm.module(ModuleId.GENERAL).blobs
    val ver by blobs.version.collectAsState()
    val tabs = remember(ver) { FocusIndex.tabs(blobs) }
    val p = LocalPalette.current
    val dark = p.isDark
    val ground = focusGround()
    Scaffold(
        containerColor = ground,
        topBar = {
            SlateTopBar(
                title = { BarTitle("Focus", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ground),
            )
        },
    ) { pad ->
        if (tabs.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(pad).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("এই পেজটি এখনো ডাউনলোড হয়নি।", style = MaterialTheme.typography.titleMedium)
                Text("একবার ইন্টারনেটে যুক্ত হয়ে অ্যাপটি খুলুন, তারপর অফলাইনেও কাজ করবে।", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Two parts like the web: the pages every group shares (framework, quotes, Data), then the six groups.
            item(key = "label-common") { FocusLabel("সব গ্রুপের জন্য") }
            items(tabs.filter { it.n == 0 }, key = { it.id }) { t ->
                FocusEntry(if (t.id == "q") Icons.Filled.FormatQuote else GROUP_ICON[0],
                    if (t.id == "q") TopicColors.of(6, dark) else MaterialTheme.colorScheme.primary, t.title, null) { onOpen(t.id) }
            }
            item(key = "data") { FocusEntry(Icons.Filled.Storage, TopicColors.of(8, dark), "ডাটা", null, onData) }
            item(key = "label-groups") {
                Column(Modifier.padding(top = 18.dp)) {
                    androidx.compose.material3.HorizontalDivider(color = p.outline)
                    Spacer(Modifier.height(14.dp))
                    FocusLabel("গ্রুপ")
                }
            }
            items(tabs.filter { it.n > 0 }, key = { it.id }) { t ->
                FocusEntry(GROUP_ICON[t.n], groupColor(t.n, dark, MaterialTheme.colorScheme.primary), t.title, "${bn(t.topics.split('·').size)}টি টপিক") { onOpen(t.id) }
            }
        }
    }
}

@Composable
private fun FocusLabel(text: String) = Text(text, style = MaterialTheme.typography.labelLarge, color = LocalPalette.current.text3, modifier = Modifier.padding(bottom = 4.dp))

@Composable
private fun FocusEntry(icon: ImageVector, c: Color, title: String, sub: String?, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(p.surface)
            .border(1.dp, p.outline, MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(c.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = c)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelMedium, color = p.text3)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
    }
}

/** A page's section jumps as one dropdown (a phone can't fit a dozen chips); the native picker scrolls via controller.js. */
internal fun jumpSelect(sections: List<Pair<String, String>>): String = buildString {
    if (sections.isEmpty()) return@buildString
    append("""<select class="fc-jump-sel" aria-label="অংশে যাও" onchange="if(this.value)scrollToId(this.value);this.selectedIndex=0"><option value="">অংশ বেছে নাও…</option>""")
    sections.forEach { (sid, label) -> append("""<option value="$sid">${esc(label)}</option>""") }
    append("</select>")
}

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusGroupScreen(vm: SlateViewModel, id: String, onBack: () -> Unit, onSwitch: (String) -> Unit, topicId: String? = null, onOpenTopic: (String) -> Unit = {}) {
    val blobs = vm.module(ModuleId.GENERAL).blobs
    val ver by blobs.version.collectAsState()
    val tabs = remember(ver) { FocusIndex.tabs(blobs) }
    val group = tabs.firstOrNull { it.id == id } ?: tabs.firstOrNull()
    val tab = if (topicId != null) remember(ver, id, topicId) { FocusIndex.topic(blobs, id, topicId) } ?: group else group
    val p = LocalPalette.current
    val dark = p.isDark
    val ground = focusGround()
    var switcher by remember { mutableStateOf(false) }
    var importantOnly by remember { mutableStateOf(false) }
    val m = vm.module(ModuleId.GENERAL)
    val flags by m.progress.flags.collectAsState()
    // Focus blocks are keyed "f<hash>" (lib/focusMarks.js on the web, focus.js here), apart from questions and math.
    val important = remember(flags) { flags.filter { it.key.startsWith("f") && it.value.important }.keys }
    var target by remember { mutableStateOf<String?>(null) }
    var nonce by remember { mutableIntStateOf(0) }
    fun jump(to: String) { target = to; nonce++ }

    Scaffold(
        containerColor = ground,
        topBar = {
            SlateTopBar(
                title = { BarTitle(tab?.let { if (it.n == 0 || it.topic) it.title else "গ্রুপ ${bn(it.n)}" } ?: "Focus", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (tab != null) {
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
        if (tab == null) return@Scaffold
        val topicVar = if (tab.n == 0) "var(--accent)" else "var(--topic-${GROUP_TOPIC[tab.n]})"
        val body = remember(tab) {
            buildString {
                append("""<div style="--fc:$topicVar" data-gid="${tab.id}"><header class="fc-head" id="fc-top"><h1 class="fc-title">${esc(tab.full)}</h1>""")
                if (tab.topics.isNotEmpty()) append("""<p class="fc-topics">${esc(tab.topics)}</p>""")
                append("""<div class="fc-jumps">""")
                append(jumpSelect(tab.sections))
                append("""</div></header><article class="fc-content">""").append(tab.html).append("</article></div>")
            }
        }
        val ctl = com.syed.slate.ui.highlight.LocalHighlights.current
        val saved by (ctl?.repo?.byUid ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap<String, List<com.syed.slate.highlight.Highlight>>()) }).collectAsState()
        val onPage = remember(saved) { saved.filterKeys { it.startsWith("f") } }
        val owner = remember { Any() }
        var clearNonce by remember { mutableIntStateOf(0) }
        Box(Modifier.fillMaxSize().padding(pad)) {
            FormulaPage(
                kind = PageKind.FOCUS, body = body, palette = p, dark = dark, cover = false,
                importantOnly = importantOnly, important = important,
                scrollTo = target, scrollNonce = nonce,
                onToggleImportant = { uid -> m.progress.update(uid, FlagRules::toggleImportant) },
                // focus.js reports a tap on a topic link as "topic:<group>/<id>".
                onSection = { s -> if (s.startsWith("topic:")) onOpenTopic(s.removePrefix("topic:")) },
                highlights = onPage, clearSelectionNonce = clearNonce,
                onSelection = { uid, anchors ->
                    ctl?.target = if (anchors.isEmpty()) ctl?.target?.takeIf { it.owner !== owner }
                    else com.syed.slate.ui.highlight.HlTarget.Add(owner, uid, anchors, anchors.joinToString(" ") { it.quote }) { clearNonce++ }
                },
                onMark = { _, ids, color -> ctl?.target = com.syed.slate.ui.highlight.HlTarget.Edit(owner, ids, color) { } },
            )
            if (importantOnly && important.isEmpty()) Column(Modifier.fillMaxSize().background(ground).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("এখনো কিছু Important হিসেবে সেভ করা হয়নি।", style = MaterialTheme.typography.titleMedium)
                Text("যেকোনো বক্সের কোণার ⭐ বাটনে চাপ দিয়ে Important করো।", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = p.text3)
            }
        }
        if (switcher) TopicSwitcherSheet(
            title = "Focus",
            topics = tabs.map { SwitchEntry(it.id, it.title) },
            current = group?.id ?: tab.id,
            onTopic = { switcher = false; onSwitch(it) },
            onDismiss = { switcher = false },
            subtopics = tab.sections.map { (sid, label) -> SwitchEntry(sid, label) },
            currentSub = null,
            onSubtopic = { sid -> switcher = false; jump(sid ?: "fc-top") },
        )
    }
}
