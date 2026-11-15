package com.syed.slate.ui.screen

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
class FocusTab(val id: String, val n: Int, val title: String, val topics: String, val html: String, val sections: List<Pair<String, String>>) {
    val full get() = if (n == 0) title else "গ্রুপ ${bn(n)}: $title"
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
        return listOf(FocusTab("u", 0, u.getString("title"), "", u.getString("html"), secs(u))) +
            List(g.length()) { i -> g.getJSONObject(i).let { o -> FocusTab(o.getString("id"), o.getInt("n"), o.getString("title"), o.getString("topics"), o.getString("html"), secs(o)) } }
    }
}

private fun groupColor(n: Int, dark: Boolean, accent: Color): Color = if (n == 0) accent else TopicColors.of(GROUP_TOPIC[n], dark)

/** White reading ground in light mode, like the printed PDF; dark mode keeps the palette. */
@Composable
private fun focusGround(): Color = if (LocalPalette.current.isDark) MaterialTheme.colorScheme.background else Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusHomeScreen(vm: SlateViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
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
            item(key = "label") { Text("ফোকাস রাইটিং — গ্রুপ বেছে নাও", style = MaterialTheme.typography.labelLarge, color = p.text3, modifier = Modifier.padding(bottom = 4.dp)) }
            items(tabs, key = { it.id }) { t ->
                val c = groupColor(t.n, dark, MaterialTheme.colorScheme.primary)
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(p.surface)
                        .border(1.dp, p.outline, MaterialTheme.shapes.medium).clickable { onOpen(t.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(c.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                        Icon(GROUP_ICON[t.n], null, Modifier.size(22.dp), tint = c)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (t.n == 0) "ভূমিকা, উক্তি ও উপসংহারের ছাঁচ" else "${bn(t.topics.split('·').size)}টি টপিক",
                            style = MaterialTheme.typography.labelMedium, color = p.text3,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
                }
            }
        }
    }
}

private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusGroupScreen(vm: SlateViewModel, id: String, onBack: () -> Unit, onSwitch: (String) -> Unit) {
    val blobs = vm.module(ModuleId.GENERAL).blobs
    val ver by blobs.version.collectAsState()
    val tabs = remember(ver) { FocusIndex.tabs(blobs) }
    val tab = tabs.firstOrNull { it.id == id } ?: tabs.firstOrNull()
    val p = LocalPalette.current
    val dark = p.isDark
    val ground = focusGround()
    var switcher by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<String?>(null) }
    var nonce by remember { mutableIntStateOf(0) }
    fun jump(to: String) { target = to; nonce++ }

    Scaffold(
        containerColor = ground,
        topBar = {
            SlateTopBar(
                title = { BarTitle(tab?.let { if (it.n == 0) it.title else "গ্রুপ ${bn(it.n)}" } ?: "Focus", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { if (tab != null) TopicSwitchButton { switcher = true } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ground),
            )
        },
    ) { pad ->
        if (tab == null) return@Scaffold
        val topicVar = if (tab.n == 0) "var(--accent)" else "var(--topic-${GROUP_TOPIC[tab.n]})"
        val body = remember(tab) {
            buildString {
                append("""<div style="--fc:$topicVar"><header class="fc-head" id="fc-top"><h1 class="fc-title">${esc(tab.full)}</h1>""")
                if (tab.topics.isNotEmpty()) append("""<p class="fc-topics">${esc(tab.topics)}</p>""")
                else append("""<p class="fc-topics">যেকোনো ফোকাসের ভূমিকা, উক্তি ও উপসংহারের সার্বজনীন ছাঁচ</p>""")
                append("""<div class="fc-jumps">""")
                tab.sections.forEach { (sid, label) -> append("""<button class="fc-jump" onclick="scrollToId('$sid')">${esc(label)}</button>""") }
                append("""</div></header><article class="fc-content">""").append(tab.html).append("</article></div>")
            }
        }
        Box(Modifier.fillMaxSize().padding(pad)) {
            FormulaPage(
                kind = PageKind.FOCUS, body = body, palette = p, dark = dark, cover = false,
                scrollTo = target, scrollNonce = nonce,
            )
        }
        if (switcher) TopicSwitcherSheet(
            title = "Focus",
            topics = tabs.map { SwitchEntry(it.id, it.title) },
            current = tab.id,
            onTopic = { switcher = false; onSwitch(it) },
            onDismiss = { switcher = false },
            subtopics = tab.sections.map { (sid, label) -> SwitchEntry(sid, label) },
            currentSub = null,
            onSubtopic = { sid -> switcher = false; jump(sid ?: "fc-top") },
        )
    }
}
