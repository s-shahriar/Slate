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

import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

/** General » Written » GK, from the General project's `content_blobs` (kind `written`, key `gk`; general-quiz scripts/sync-gk.mjs):
 *  one page of question-answer blocks per category, read like Written » Focus (same stars, filter and highlights). */
class GkCat(val id: String, val n: Int, val title: String, val icon: String, val color: Int, val count: Int, val html: String)

/** A Q&A section of Written: blob written/<key> (general-quiz scripts/sync-gk.mjs <key>), like the web's QA_SECTIONS. */
enum class QaSection(val key: String, val title: String, val label: String, val unit: String) {
    GK("gk", "GK", "লিখিত সাধারণ জ্ঞান", "প্রশ্ন"),
    FINANCE("finance", "Financial", "ফিন্যান্স ও ব্যাংকিং", "আইটেম");
    companion object { fun of(key: String) = entries.firstOrNull { it.key == key } ?: GK }
}

object GkIndex {
    fun cats(blobs: BlobRepository, sec: QaSection = QaSection.GK): List<GkCat> {
        val p = blobs.payload("written", sec.key) ?: return emptyList()
        val a = p.getJSONArray("categories")
        return List(a.length()) { i -> a.getJSONObject(i).let { o -> GkCat(o.getString("id"), o.getInt("n"), o.getString("title"), o.optString("icon"), o.optInt("color", 1), o.getInt("count"), o.getString("html")) } }
    }
}

/** The web's lucide icon names (WrittenGK.jsx ICONS) mapped to Material icons. */
private val GK_ICON: Map<String, ImageVector> = mapOf(
    "TrendingUp" to Icons.Filled.TrendingUp,
    "Landmark" to Icons.Filled.AccountBalance,
    "Globe" to Icons.Filled.Public,
    "Map" to Icons.Filled.Map,
    "Trophy" to Icons.Filled.EmojiEvents,
    "FlaskConical" to Icons.Filled.Science,
    "ScrollText" to Icons.Filled.HistoryEdu,
    "Leaf" to Icons.Filled.Eco,
    "Award" to Icons.Filled.MilitaryTech,
    "Coins" to Icons.Filled.Payments,
    "Building2" to Icons.Filled.LocationCity,
    "Shield" to Icons.Filled.Security,
    "Palette" to Icons.Filled.Palette,
    "Sparkles" to Icons.Filled.AutoAwesome,
    "Flag" to Icons.Filled.Flag,
    "TreePalm" to Icons.Filled.BeachAccess,
    "SunMedium" to Icons.Filled.WbSunny,
    "Waves" to Icons.Filled.Water,
    "Mountain" to Icons.Filled.Terrain,
    "Earth" to Icons.Filled.Language,
    "HeartHandshake" to Icons.Filled.Handshake,
    "Ship" to Icons.Filled.DirectionsBoat,
    "Network" to Icons.Filled.Hub,
    "CloudSun" to Icons.Filled.Cloud,
    "Star" to Icons.Filled.Star,
)

private fun bnGk(n: Int) = n.toString().map { if (it.isDigit()) "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")

@Composable
private fun gkGround(): Color = if (LocalPalette.current.isDark) MaterialTheme.colorScheme.background else Color.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GkHomeScreen(vm: SlateViewModel, sec: QaSection, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val blobs = vm.module(ModuleId.GENERAL).blobs
    val ver by blobs.version.collectAsState()
    val cats = remember(ver) { GkIndex.cats(blobs, sec) }
    val p = LocalPalette.current
    val ground = gkGround()
    Scaffold(
        containerColor = ground,
        topBar = {
            SlateTopBar(
                title = { BarTitle(sec.title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ground),
            )
        },
    ) { pad ->
        if (cats.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(pad).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("এই পেজটি এখনো ডাউনলোড হয়নি।", style = MaterialTheme.typography.titleMedium)
                Text("একবার ইন্টারনেটে যুক্ত হয়ে অ্যাপটি খুলুন, তারপর অফলাইনেও কাজ করবে।", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "label") { Text("${sec.label} — ${bnGk(cats.sumOf { it.count })}টি ${sec.unit}, ${bnGk(cats.size)}টি বিষয়", style = MaterialTheme.typography.labelLarge, color = p.text3, modifier = Modifier.padding(bottom = 4.dp)) }
            items(cats, key = { it.id }) { c ->
                val col = TopicColors.of(c.color, p.isDark)
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(p.surface)
                        .border(1.dp, p.outline, MaterialTheme.shapes.medium).clickable { onOpen(c.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(44.dp).clip(MaterialTheme.shapes.small).background(col.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                        Icon(GK_ICON[c.icon] ?: Icons.Filled.MenuBook, null, Modifier.size(22.dp), tint = col)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(c.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${bnGk(c.count)}টি ${sec.unit}", style = MaterialTheme.typography.labelMedium, color = p.text3)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
                }
            }
        }
    }
}

private fun escGk(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GkCategoryScreen(vm: SlateViewModel, sec: QaSection, id: String, onBack: () -> Unit, onSwitch: (String) -> Unit) {
    val m = vm.module(ModuleId.GENERAL)
    val blobs = m.blobs
    val ver by blobs.version.collectAsState()
    val cats = remember(ver) { GkIndex.cats(blobs, sec) }
    val cat = cats.firstOrNull { it.id == id } ?: cats.firstOrNull()
    val p = LocalPalette.current
    val dark = p.isDark
    val ground = gkGround()
    var switcher by remember { mutableStateOf(false) }
    var importantOnly by remember { mutableStateOf(false) }
    // Eye (cover mode): answers blurred until a question is tapped, like the Math page.
    var cover by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val flags by m.progress.flags.collectAsState()
    // GK blocks share Focus's "f<hash>" uids (lib/focusMarks.js on the web, focus.js here).
    val important = remember(flags) { flags.filter { it.key.startsWith("f") && it.value.important }.keys }
    val nailed = remember(flags) { flags.filter { it.key.startsWith("f") && it.value.nailed }.keys }

    Scaffold(
        containerColor = ground,
        topBar = {
            SlateTopBar(
                title = { BarTitle(cat?.title ?: sec.title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (cat != null) {
                        IconButton(onClick = { cover = !cover }) {
                            Icon(if (cover) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, if (cover) "Show answers" else "Cover answers", tint = if (cover) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current)
                        }
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
        if (cat == null) return@Scaffold
        val body = remember(cat) {
            buildString {
                append("""<div style="--fc:var(--topic-${cat.color})" data-gid="${cat.id}"><header class="fc-head" id="fc-top"><h1 class="fc-title">${escGk(cat.title)}</h1>""")
                append("""<p class="fc-topics">${sec.label} · ${bnGk(cat.count)}টি ${sec.unit}</p></header><article class="fc-content">""")
                append(cat.html).append("</article></div>")
            }
        }
        val ctl = com.syed.slate.ui.highlight.LocalHighlights.current
        val saved by (ctl?.repo?.byUid ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap<String, List<com.syed.slate.highlight.Highlight>>()) }).collectAsState()
        val onPage = remember(saved) { saved.filterKeys { it.startsWith("f") } }
        val owner = remember { Any() }
        var clearNonce by remember { mutableIntStateOf(0) }
        Box(Modifier.fillMaxSize().padding(pad)) {
            FormulaPage(
                kind = PageKind.FOCUS, body = body, palette = p, dark = dark, cover = cover,
                importantOnly = importantOnly, important = important,
                onToggleImportant = { uid -> m.progress.update(uid, FlagRules::toggleImportant) },
                nailed = nailed,
                onToggleNailed = { uid -> m.progress.update(uid, FlagRules::toggleNailed) },
                highlights = onPage, clearSelectionNonce = clearNonce,
                onSelection = { uid, anchors ->
                    ctl?.target = if (anchors.isEmpty()) ctl?.target?.takeIf { it.owner !== owner }
                    else com.syed.slate.ui.highlight.HlTarget.Add(owner, uid, anchors, anchors.joinToString(" ") { it.quote }) { clearNonce++ }
                },
                onMark = { _, ids, color -> ctl?.target = com.syed.slate.ui.highlight.HlTarget.Edit(owner, ids, color) { } },
            )
            if (importantOnly && cats.isNotEmpty()) {
                // Nothing starred on this page yet: say so instead of a blank page.
                val any = remember(important, cat) { important.isNotEmpty() }
                if (!any) Column(Modifier.fillMaxSize().background(ground).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("এখনো কিছু Important হিসেবে সেভ করা হয়নি।", style = MaterialTheme.typography.titleMedium)
                    Text("যেকোনো প্রশ্নের কোণার ⭐ বাটনে চাপ দিয়ে Important করো।", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = p.text3)
                }
            }
        }
        if (switcher) TopicSwitcherSheet(
            title = sec.title,
            topics = cats.map { SwitchEntry(it.id, it.title) },
            current = cat.id,
            onTopic = { switcher = false; onSwitch(it) },
            onDismiss = { switcher = false },
            subtopics = emptyList(),
            currentSub = null,
            onSubtopic = { },
        )
    }
}
