package com.syed.slate.ui.screen

import com.syed.slate.ui.component.SlateDropdown
import com.syed.slate.ui.component.DropOption
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.syed.slate.ui.component.BarTitle
import com.syed.slate.ui.component.SlateTopBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.syed.slate.backend.ModuleId
import com.syed.slate.content.TopicCatalog
import com.syed.slate.progress.FlagRules
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.theme.LocalPalette
import com.syed.slate.ui.theme.TopicColors
import com.syed.slate.ui.theme.isDark
import com.syed.slate.ui.web.FormulaPage
import com.syed.slate.ui.web.PageKind
import org.json.JSONArray
import com.syed.slate.content.BlobRepository
import org.json.JSONObject

class MathSection(val id: String, val label: String, val icon: String, val color: String)
class EquationGroup(val id: String, val title: String)
class EquationTopic(val id: String, val groups: List<EquationGroup>, val equations: Int)

/** The pre-rendered pages and their small indexes, read from the cached `content_blobs` rows (kind `web`). */
object FormulaIndex {
    fun mathPage(blobs: BlobRepository): JSONObject? = blobs.payload("web", "math")

    fun mathSections(page: JSONObject): List<MathSection> =
        page.getJSONArray("sections").let { a ->
            List(a.length()) { a.getJSONObject(it).let { o -> MathSection(o.getString("id"), o.getString("label"), o.optString("icon"), o.optString("color")) } }
        }

    fun equationTopics(blobs: BlobRepository): List<Pair<EquationTopic, String>> =
        blobs.all("web").filter { it.first.startsWith("equation_") }.map { (_, o) ->
            val g = o.getJSONArray("groups")
            EquationTopic(o.getString("id"), List(g.length()) { j -> EquationGroup(g.getJSONObject(j).getString("id"), g.getJSONObject(j).getString("title")) }, o.getInt("equations")) to o.getString("html")
        }
}

/** Shown when a page's rows have not been downloaded yet (first launch without a connection). */
@Composable
private fun NotDownloaded(title: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("এই পেজটি এখনো ডাউনলোড হয়নি।", style = MaterialTheme.typography.titleMedium)
            Text("একবার ইন্টারনেটে যুক্ত হয়ে অ্যাপটি খুলুন, তারপর অফলাইনেও কাজ করবে।", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CoverBanner() {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().background(p.imp.copy(alpha = .10f)).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.VisibilityOff, null, tint = p.imp)
        Text("সূত্র ঢাকা আছে — মনে করার চেষ্টা করে, তারপর ট্যাপ করে মিলিয়ে নিন।", style = MaterialTheme.typography.labelMedium)
    }
}

/** General » Utility » গণিত সূত্র সংকলন: 19 sections of formulas, with cover-and-recall and per-card Important stars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathFormulasScreen(vm: SlateViewModel, onBack: () -> Unit) {
    val blobs = vm.module(ModuleId.GENERAL).blobs
    val ver by blobs.version.collectAsState()
    val page = remember(ver) { FormulaIndex.mathPage(blobs) }
    if (page == null) NotDownloaded("গণিত সূত্র সংকলন", onBack) else MathFormulasContent(vm, page, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MathFormulasContent(vm: SlateViewModel, page: JSONObject, onBack: () -> Unit) {
    val m = vm.module(ModuleId.GENERAL)
    val flags by m.progress.flags.collectAsState()
    val p = LocalPalette.current
    val sections = remember(page) { FormulaIndex.mathSections(page) }
    var cover by remember { mutableStateOf(vm.boolPref("mf-cover")) }
    var importantOnly by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(sections.first().id) }
    var target by remember { mutableStateOf<String?>(null) }
    var nonce by remember { mutableIntStateOf(0) }
    // Math cards are keyed "m<hash>"; question uids start with "q", so the prefix keeps the two apart.
    val important = remember(flags) { flags.filter { it.key.startsWith("m") && it.value.important }.keys }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle("গণিত সূত্র সংকলন", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { cover = !cover; vm.setBoolPref("mf-cover", cover) }) {
                        Icon(if (cover) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, if (cover) "Show formulas" else "Cover formulas")
                    }
                    IconButton(onClick = { importantOnly = !importantOnly }) {
                        Icon(if (importantOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Important only", tint = if (importantOnly) p.imp else LocalContentColorFallback())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            // Jump to a section; follows the section scrolled into view.
            if (!importantOnly && sections.isNotEmpty()) SlateDropdown(
                sections.map { s -> DropOption(s.id, "${s.icon} ${s.label}", color = TopicColors.parse(s.color, p.isDark, p.primary)) },
                active, { target = it; active = it; nonce++ },
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (cover) CoverBanner()
            Box(Modifier.fillMaxSize()) {
                FormulaPage(
                    PageKind.MATH, page.getString("html"), p, p.isDark, cover,
                    importantOnly = importantOnly, important = important, scrollTo = target, scrollNonce = nonce,
                    onToggleImportant = { uid -> m.progress.update(uid, FlagRules::toggleImportant) },
                    onSection = { active = it },
                )
                if (importantOnly && important.isEmpty()) Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("এখনো কোনো কার্ড Important হিসেবে সেভ করা হয়নি।", style = MaterialTheme.typography.titleMedium)
                    Text("যেকোনো কার্ডের কোণায় ⭐ বাটনে ক্লিক করে Important করুন।", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = p.text3)
                }
            }
        }
    }
}

@Composable
private fun LocalContentColorFallback(): Color = androidx.compose.material3.LocalContentColor.current

/** ICT » Equation: formula sheets for one topic, each group a diagram plus its equations, with cover-and-recall and per-group Important stars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquationScreen(vm: SlateViewModel, topicId: String, onBack: () -> Unit) {
    val blobs = vm.module(ModuleId.ICT).blobs
    val ver by blobs.version.collectAsState()
    val found = remember(ver, topicId) { FormulaIndex.equationTopics(blobs).firstOrNull { it.first.id == topicId } }
    if (found == null) NotDownloaded("Equations", onBack) else EquationContent(vm, found.first, found.second, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquationContent(vm: SlateViewModel, topic: EquationTopic, html: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    val m = vm.module(ModuleId.ICT)
    val flags by m.progress.flags.collectAsState()
    var cover by remember { mutableStateOf(vm.boolPref("ict-eq-cover")) }
    var importantOnly by remember { mutableStateOf(false) }
    // Each group carries its uid (uidFor('equation', '<topic>/<group id>'), the same key the web stars).
    val groupUids = remember(html) { Regex("data-uid=\"([^\"]+)\"").findAll(html).map { it.groupValues[1] }.toSet() }
    val important = remember(flags, groupUids) { flags.filter { it.key in groupUids && it.value.important }.keys }
    var active by remember { mutableStateOf("eq-${topic.groups.first().id}") }
    var target by remember { mutableStateOf<String?>(null) }
    var nonce by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle("${TopicCatalog.ictTopicNames[topic.id] ?: topic.id} — Equations", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { cover = !cover; vm.setBoolPref("ict-eq-cover", cover) }) {
                        Icon(if (cover) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, if (cover) "Show equations" else "Cover equations")
                    }
                    IconButton(onClick = { importantOnly = !importantOnly }) {
                        Icon(if (importantOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Important only", tint = if (importantOnly) p.imp else LocalContentColorFallback())
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (!importantOnly && topic.groups.size > 1) SlateDropdown(
                topic.groups.map { g -> DropOption("eq-${g.id}", g.title) },
                active, { target = it; active = it; nonce++ },
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (cover) CoverBanner()
            val ctl = com.syed.slate.ui.highlight.LocalHighlights.current
            val saved by (ctl?.repo?.byUid ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap<String, List<com.syed.slate.highlight.Highlight>>()) }).collectAsState()
            val onPage = remember(saved) { saved.filterKeys { it.startsWith("equation:") } }
            val owner = remember { Any() }
            var clearNonce by remember { mutableIntStateOf(0) }
            Box(Modifier.fillMaxSize()) {
                FormulaPage(
                    PageKind.EQUATION, html, p, p.isDark, cover,
                    importantOnly = importantOnly, important = important,
                    onToggleImportant = { uid -> m.progress.update(uid, FlagRules::toggleImportant) },
                    scrollTo = target, scrollNonce = nonce, onSection = { active = it },
                    highlights = onPage, clearSelectionNonce = clearNonce,
                    onSelection = { uid, anchors ->
                        ctl?.target = if (anchors.isEmpty()) ctl?.target?.takeIf { it.owner !== owner }
                        else com.syed.slate.ui.highlight.HlTarget.Add(owner, uid, anchors, anchors.joinToString(" ") { it.quote }) { clearNonce++ }
                    },
                    onMark = { _, ids, color -> ctl?.target = com.syed.slate.ui.highlight.HlTarget.Edit(owner, ids, color) { } },
                )
                if (importantOnly && important.isEmpty()) Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("এই topic-এ এখনো কোনো group Important হিসেবে সেভ করা হয়নি।", style = MaterialTheme.typography.titleMedium)
                    Text("যেকোনো group-এর শিরোনামের ডান পাশে ⭐ বাটনে ক্লিক করে Important করুন।", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = p.text3)
                }
            }
        }
    }
}

class SqlSection(val id: String, val title: String, val level: String)
/** One SQL Lab lesson: a numbered query ([n] 1..), or an unnumbered "how it runs" intro ([n] null). */
class SqlLesson(val id: String, val intro: Boolean, val section: String, val title: String, val concept: String, val n: Int?, val versions: Int)
class SqlTopic(val id: String, val name: String, val sections: List<SqlSection>, val lessons: List<SqlLesson>, val html: JSONObject) {
    val queries get() = lessons.count { !it.intro }
}

/** ICT » SQL Lab topics, from the rendered `web/sql_<topic>` rows the prerender tool publishes. */
object SqlLabIndex {
    fun topics(blobs: BlobRepository): List<SqlTopic> =
        blobs.all("web").filter { it.first.startsWith("sql_") }.map { (_, o) ->
            val s = o.getJSONArray("sections")
            val l = o.getJSONArray("lessons")
            SqlTopic(
                o.getString("id"), o.getString("name"),
                List(s.length()) { i -> s.getJSONObject(i).let { x -> SqlSection(x.getString("id"), x.getString("title"), x.optString("level")) } },
                List(l.length()) { i ->
                    l.getJSONObject(i).let { x ->
                        SqlLesson(x.getString("id"), x.optString("kind") == "intro", x.optString("section"), x.getString("title"),
                            x.optString("concept"), if (x.isNull("n")) null else x.getInt("n"), x.optInt("versions", 1))
                    }
                },
                o.getJSONObject("html"),
            )
        }
}

/** Colour of a SQL Lab section's level (the web's easy / medium / hard). */
@Composable
fun sqlLevelColor(level: String): Color = LocalPalette.current.let { p -> when (level) { "easy" -> p.ok; "hard" -> p.bad; else -> p.warn } }

/** ICT » SQL Lab » one lesson: every step, the output and the traps; a lesson solved two ways has a V1 / V2 switch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SqlLessonScreen(vm: SlateViewModel, lessonId: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val blobs = vm.module(ModuleId.ICT).blobs
    val ver by blobs.version.collectAsState()
    val topic = remember(ver, lessonId) { SqlLabIndex.topics(blobs).firstOrNull { t -> t.lessons.any { it.id == lessonId } } }
    if (topic == null) { NotDownloaded("SQL Lab", onBack); return }
    val p = LocalPalette.current
    val i = topic.lessons.indexOfFirst { it.id == lessonId }
    val lesson = topic.lessons[i]
    // Same uid the web keys this lesson's note and preferred version by: uidFor('sql', '<topic> / <lesson id>').
    val progress = vm.module(ModuleId.ICT).progress
    val flags by progress.flags.collectAsState()
    val uid = remember(topic.id, lesson.id) { com.syed.slate.core.Uid.ict("sql", "${topic.id} / ${lesson.id}") }
    val preferred = uid?.let { flags[it]?.preferred }?.takeIf { it < lesson.versions }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle(if (lesson.intro) "কীভাবে চলে" else "Query %02d / %d".format(lesson.n, topic.queries), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { onOpen(topic.lessons[i - 1].id) }, enabled = i > 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous lesson") }
                    IconButton(onClick = { onOpen(topic.lessons[i + 1].id) }, enabled = i < topic.lessons.size - 1) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next lesson") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            // Highlights: the web's user_highlights rows and block keys (`question`, `s2.note0`, `v2.trap1`…) on this lesson's uid.
            val ctl = com.syed.slate.ui.highlight.LocalHighlights.current
            val saved by (ctl?.repo?.byUid ?: remember { kotlinx.coroutines.flow.MutableStateFlow(emptyMap<String, List<com.syed.slate.highlight.Highlight>>()) }).collectAsState()
            val onPage = remember(saved, uid) { saved.filterKeys { it == uid } }
            val owner = remember { Any() }
            var clearNonce by remember { mutableIntStateOf(0) }
            FormulaPage(PageKind.SQL, topic.html.getString(lesson.id), p, p.isDark, cover = false,
                preferred = preferred,
                onPrefer = { v -> if (uid != null) progress.update(uid) { it.copy(preferred = v) } },
                highlights = onPage, clearSelectionNonce = clearNonce,
                onSelection = { u, anchors ->
                    ctl?.target = if (anchors.isEmpty()) ctl?.target?.takeIf { it.owner !== owner }
                    else com.syed.slate.ui.highlight.HlTarget.Add(owner, u, anchors, anchors.joinToString(" ") { it.quote }) { clearNonce++ }
                },
                onMark = { _, ids, color -> ctl?.target = com.syed.slate.ui.highlight.HlTarget.Edit(owner, ids, color) { } })
        }
    }
}
