package com.syed.slate.ui.screen

import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import com.syed.slate.ui.theme.isDark
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.syed.slate.SlateApp
import com.syed.slate.backend.ModuleId
import com.syed.slate.ui.component.HandMirror
import com.syed.slate.ui.component.LocalOpenSyncQueue
import com.syed.slate.ui.screen.AdminScreen
import com.syed.slate.ui.screen.LivemcqAdmin
import com.syed.slate.content.ContentState
import com.syed.slate.content.Item
import com.syed.slate.content.LongForm
import com.syed.slate.content.ModuleContent
import com.syed.slate.content.PoolSet
import com.syed.slate.content.QuizPool
import com.syed.slate.content.SearchText
import com.syed.slate.content.SyncState
import com.syed.slate.content.TopicCatalog
import com.syed.slate.progress.Flag
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.component.Dot
import com.syed.slate.ui.component.Pager
import com.syed.slate.ui.component.SyncStatusStrip
import com.syed.slate.ui.rich.HtmlParser
import com.syed.slate.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModuleNav(
    val onBack: () -> Unit,
    val onTopic: (group: String, topic: String) -> Unit,
    val onExam: (group: String) -> Unit,
    val onSaved: (kind: PoolSet, group: String) -> Unit,
    val onSearchHit: (Item) -> Unit,
    val onWritten: () -> Unit,
    val onBin: () -> Unit,
    val onPractice: (String) -> Unit,
    val onPracticeImportant: () -> Unit,
    val onMath: () -> Unit,
    val onEquation: (String) -> Unit,
    val onEquationImportant: () -> Unit = {},
    val onSql: (String) -> Unit = {},
    val onAdmin: () -> Unit = {},
    val onRoutine: () -> Unit = {},
    val onFocus: () -> Unit = {},
    val onWrittenSaved: (Boolean) -> Unit = {},
    val onEnglish: () -> Unit = {},
)

/** Section keys that are not database groups. */
private const val PRACTICE = "practice"
private const val EQUATION = "equation"
private const val SQL = "sql"
private const val WRITTEN = "written"
private const val UTILITY = "utility"
private const val ROUTINE = "routine"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleScreen(vm: SlateViewModel, id: ModuleId, nav: ModuleNav) {
    val m = vm.module(id)
    val state by m.content.state.collectAsState()
    val sync by m.content.sync.collectAsState()
    val online by vm.online.collectAsState()
    val flags by m.progress.flags.collectAsState()
    // Written's own Nailed / Important counts come from the Written block index (blob written/index).
    val wver by m.blobs.version.collectAsState()
    val wunits = remember(wver, id) { if (id == ModuleId.GENERAL) WrittenIndex.units(m.blobs) else emptyList() }
    val session by m.auth.session.collectAsState()
    LaunchedEffect(id) { vm.openModule(id) }
    // null = the sub-module launcher (Magpie-style cards); otherwise the section's own page.
    var selected by rememberSaveable(id) { mutableStateOf<String?>(null) }
    // Written » GK / Financial are sections of their own (modules wgk / wfin) opened from the Written page; Back returns there.
    fun up() { selected = if (selected == "wgk" || selected == "wfin") WRITTEN else null }
    androidx.activity.compose.BackHandler(enabled = selected != null) { up() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle(id.title, style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = { IconButton(onClick = { if (selected != null) up() else nav.onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = nav.onBin) { Icon(Icons.Filled.DeleteOutline, "Recycle bin") }
                    IconButton(onClick = { vm.refreshContent(id) }, enabled = online && sync !is SyncState.Running) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (!online) OfflineBanner(state, m.content.cachedAt)
            val openQueue = LocalOpenSyncQueue.current
            SyncStatusStrip(m, online, onOpen = { openQueue?.invoke() })
            if (sync is SyncState.Running) {
                com.syed.slate.ui.component.SlateLinearLoader(Modifier.fillMaxWidth())
                Text((sync as SyncState.Running).message, Modifier.padding(horizontal = 20.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
            }
            (sync as? SyncState.Failed)?.let {
                Text("Couldn't update: ${it.message}", Modifier.padding(horizontal = 20.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            when (val s = state) {
                is ContentState.Ready -> {
                    val sel = selected
                    if (sel == null) SubModuleLauncher(id, s.content) { key ->
                        // General's Written is its own page; every other card opens in place.
                        if (key == ROUTINE) nav.onRoutine() else selected = key
                    } else ModuleBody(id, s.content, flags, nav, sel, owner = LivemcqAdmin.isOwner(session?.userId), wunits = wunits, onSelect = { selected = it })
                }
                ContentState.Empty -> if (sync !is SyncState.Running) EmptyState(online) { vm.refreshContent(id) }
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { com.syed.slate.ui.component.SlateLoader(label = (sync as SyncState.Running).message) }
                ContentState.NotLoaded -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { com.syed.slate.ui.component.SlateLoader() }
            }
        }
    }
}

@Composable
private fun OfflineBanner(state: ContentState, cachedAt: Long) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().background(p.warn.copy(alpha = .16f)).padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.WifiOff, null, Modifier.size(16.dp), tint = p.warn)
        // One line: the date of the offline copy is enough (the full timestamp is in Settings).
        Text(
            if (state is ContentState.Ready) "Offline · saved copy from ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(cachedAt))}" else "Offline",
            style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(online: Boolean, onDownload: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Nothing downloaded yet", style = MaterialTheme.typography.titleLarge)
        Text(
            if (online) "Download this module once and it works without a connection." else "Connect to the internet to download this module for offline use.",
            Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onDownload, enabled = online) { Text("Download") }
    }
}

private const val SEARCH_PAGE = 8

/**
 * The web app's shape, translated: General is three submodules (General / Vocabulary / Utility) behind a tab row;
 * each part then has a section dropdown (current section named on the trigger, all sections with counts in the menu)
 * and a vertical topic grid. ICT has the one dropdown: MCQ / Written / Extra / Viva / Equation / Practice.
 */
@Composable
private fun ModuleBody(id: ModuleId, content: ModuleContent, flags: Map<String, Flag>, nav: ModuleNav, selected: String, owner: Boolean = false, wunits: List<WrittenUnit> = emptyList(), onSelect: (String) -> Unit = {}) {
    val p = LocalPalette.current
    val groupKeys = content.groups.map { it.key }
    val section = selected
    val inVocab = id == ModuleId.GENERAL && section == "vocab"
    val inUtility = id == ModuleId.GENERAL && section == UTILITY
    val inWritten = id == ModuleId.GENERAL && section == WRITTEN
    val inPractice = id == ModuleId.ICT && section == PRACTICE
    val inEquation = id == ModuleId.ICT && section == EQUATION
    val inSql = id == ModuleId.ICT && section == SQL
    val app = LocalContext.current.applicationContext as SlateApp
    val blobVer by app.module(ModuleId.ICT).blobs.version.collectAsState()
    val equationTopics = remember(blobVer) { FormulaIndex.equationTopics(app.module(ModuleId.ICT).blobs).map { it.first } }
    val sqlTopics = remember(blobVer) { SqlLabIndex.topics(app.module(ModuleId.ICT).blobs) }
    // SQL Lab Important: the web's flag on uidFor('sql', '<topic> / <lesson id>'), the uid its note and preferred version use.
    val sqlImportant = remember(sqlTopics, flags) {
        sqlTopics.flatMap { t -> t.lessons.mapNotNull { l -> com.syed.slate.core.Uid.ict("sql", "${t.id} / ${l.id}")?.takeIf { flags[it]?.important == true }?.let { l.id } } }.toSet()
    }
    var sqlImportantOnly by remember { mutableStateOf(false) }
    val practiceCats = remember(blobVer) { app.practice }
    val practiceFlags = remember(flags) { flags.filter { it.key.startsWith("practice__") } }
    val group = content.groups.firstOrNull { it.key == section }
    val groupItems = remember(content, group) { group?.topics?.flatMap { content.items(group.key, it.slug) }.orEmpty().filter { it.isQuizzable || LongForm.isLongForm(it) } }
    val counts = remember(groupItems, flags) { QuizPool.counts(groupItems, flags, includeLongForm = true) }
    val topicNames = remember(group) { group?.topics?.associate { it.slug to it.name }.orEmpty() }
    val quizzableGroup = group != null && !inUtility && !inPractice && !inEquation && !inSql

    var query by rememberSaveable(selected) { mutableStateOf("") }
    var debounced by rememberSaveable(selected) { mutableStateOf("") }
    var page by rememberSaveable(selected) { mutableIntStateOf(0) }
    LaunchedEffect(query) { delay(250); debounced = query; page = 0 }
    // Normalised once per group, off the main thread: a few thousand questions is real work.
    val haystacks by produceState<Map<String, String>>(emptyMap(), groupItems) {
        value = withContext(Dispatchers.Default) { groupItems.associate { it.id to SearchText.haystack(it) } }
    }
    val tokens = SearchText.tokens(debounced)
    val hits = remember(tokens, haystacks, groupItems) {
        if (tokens.isEmpty()) emptyList() else groupItems.filter { SearchText.matches(haystacks[it.id].orEmpty(), tokens) }
    }

    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LazyColumn(state = list, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // The section's hero: the sub-module card's icon coin, its name and size.
        if (!inUtility && !inWritten) item(key = "section-hero") {
            // Every section opens with the Written-style title block: a rounded-square icon and the bold name.
            SectionHero(sectionIcon(id, section), subModuleTitle(id, section))
        }
        if (inWritten) {
            // Written's own Nailed / Important (Focus, GK, Financial blocks), apart from the MCQ modules'.
            item(key = "written-hero") { SectionHero(Icons.Filled.EditNote, "Written") }
            item(key = "written-actions") {
                // Focus blocks (index) + the GK / Financial questions.
                val wq = remember(content) { content.allItems().filter { it.group == "wgk" || it.group == "wfin" }.mapNotNull { it.uid }.toList() }
                fun n(pick: (com.syed.slate.progress.Flag) -> Boolean) = wunits.count { flags[it.uid]?.let(pick) == true } + wq.count { flags[it]?.let(pick) == true }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SavedTile(Icons.Filled.Star, "Nailed It", n { it.nailed }, p.ok, Modifier.weight(1f)) { nav.onWrittenSaved(true) }
                    SavedTile(Icons.Filled.Bookmark, "Important", n { it.important }, p.imp, Modifier.weight(1f)) { nav.onWrittenSaved(false) }
                }
            }
            item(key = "written-label") {
                Text("বিষয় বেছে নাও", Modifier.padding(top = 6.dp, start = 2.dp), style = MaterialTheme.typography.labelLarge, color = p.text3)
            }
            item(key = "written-focus") {
                WrittenEntry(Icons.Filled.EditNote, 2, "Focus", "৬ গ্রুপ · ভূমিকা, উক্তি, উপসংহার", nav.onFocus)
            }
            item(key = "written-gk") {
                val n = content.groups.firstOrNull { it.key == "wgk" }
                WrittenEntry(Icons.Filled.Public, 6, "GK", n?.let { "${bnN(it.count)}টি প্রশ্নোত্তর · ${bnN(it.topics.size)}টি বিষয়" } ?: "লিখিত সাধারণ জ্ঞান") { onSelect("wgk") }
            }
            item(key = "written-finance") {
                val n = content.groups.firstOrNull { it.key == "wfin" }
                WrittenEntry(Icons.Filled.AccountBalance, 3, "Financial", n?.let { "পূর্ণরূপ ও সংখ্যা-শিট · ${bnN(it.count)}টি" } ?: "ফিন্যান্স ও ব্যাংকিং") { onSelect("wfin") }
            }
        } else if (inUtility) {
            // Same look as the Written page: a title block, then theme cards (icon coin, name, chevron).
            item(key = "utility-hero") { SectionHero(sectionIcon(id, UTILITY), "Utility") }
            item(key = "tool-math") { WrittenEntry(Icons.Filled.Calculate, 4, "গণিত সূত্র", "", nav.onMath) }
            item(key = "tool-english") { WrittenEntry(Icons.Filled.Translate, 9, "English Grammar", "", nav.onEnglish) }
        } else if (inPractice) {
            item(key = "practice-actions") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val saved = practiceFlags.values.count { it.important }
                    val weak = practiceFlags.values.count { it.important && it.weak }
                    SavedTile(Icons.Filled.Bookmark, "Important", saved, p.imp, Modifier.weight(1f)) { nav.onPracticeImportant() }
                    // Weak drills live inside the Important list (marked there), so this tile opens it too.
                    SavedTile(Icons.Filled.LocalFireDepartment, "Weak", weak, p.warn, Modifier.weight(1f)) { nav.onPracticeImportant() }
                }
            }
            items(practiceCats, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(p.surface).border(1.dp, p.outline, MaterialTheme.shapes.large).clickable { nav.onPractice(c.id) }.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${c.name} Practice", style = MaterialTheme.typography.titleMedium)
                        Text("${c.topics.size} topics · ${c.topics.sumOf { t -> t.practice.size }} drills", style = MaterialTheme.typography.labelMedium, color = p.text3)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
                }
            }
        } else if (inSql) {
            // The web's lesson rail: each section's "how it runs" intro (book icon, no number), then its numbered queries.
            item(key = "sql-imp-only") {
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .background(if (sqlImportantOnly) p.imp.copy(alpha = .12f) else p.surface)
                        .clickable { sqlImportantOnly = !sqlImportantOnly }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(if (sqlImportantOnly) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, null, Modifier.size(20.dp), tint = p.imp)
                    Text(if (sqlImportantOnly) "শুধু Important দেখাচ্ছে" else "শুধু Important", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Text("${sqlImportant.size}", style = MaterialTheme.typography.labelLarge, color = p.imp)
                }
            }
            if (sqlImportantOnly && sqlImportant.isEmpty()) item(key = "sql-imp-empty") {
                Text("এখনো কোনো lesson Important করা হয়নি — lesson খুলে উপরের 🔖 চাপো।", Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium, color = p.text3)
            }
            sqlTopics.forEach { t ->
                t.sections.forEach { sec ->
                    val shown = t.lessons.filter { it.section == sec.id && (!sqlImportantOnly || it.id in sqlImportant) }
                    if (shown.isEmpty()) return@forEach
                    item(key = "sql-${t.id}-${sec.id}") {
                        Text(sec.title, Modifier.padding(top = 8.dp, start = 4.dp), style = MaterialTheme.typography.labelLarge, color = sqlLevelColor(sec.level))
                    }
                    items(shown, key = { "sql-${t.id}-${it.id}" }) { l ->
                        Row(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(p.surface).border(1.dp, p.outline, MaterialTheme.shapes.large).clickable { nav.onSql(l.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            if (l.intro) Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(20.dp), tint = p.text3)
                            else Text("%02d".format(l.n), style = MaterialTheme.typography.labelLarge, color = sqlLevelColor(sec.level))
                            Column(Modifier.weight(1f)) {
                                Text(l.title, style = MaterialTheme.typography.titleMedium, color = if (l.intro) p.text2 else MaterialTheme.colorScheme.onSurface)
                                Text(if (l.versions > 1) "${l.concept} · ${l.versions} versions" else l.concept, style = MaterialTheme.typography.labelMedium, color = p.text3)
                            }
                            if (l.id in sqlImportant) Icon(Icons.Filled.Bookmark, "Important", Modifier.size(18.dp), tint = p.imp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
                        }
                    }
                }
            }
        } else if (inEquation) {
            // Every starred group across the topics, like the web's Important card on the Equation tab.
            item(key = "equation-actions") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val saved = flags.count { it.key.startsWith("equation:") && it.value.important }
                    SavedTile(Icons.Filled.Bookmark, "Important", saved, p.imp, Modifier.weight(1f)) { nav.onEquationImportant() }
                }
            }
            items(equationTopics, key = { it.id }) { t ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(p.surface).border(1.dp, p.outline, MaterialTheme.shapes.large).clickable { nav.onEquation(t.id) }.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(TopicCatalog.ictTopicNames[t.id] ?: t.id, style = MaterialTheme.typography.titleMedium)
                        Text("${t.groups.size} groups · ${t.equations} equations", style = MaterialTheme.typography.labelMedium, color = p.text3)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
                }
            }
        } else {
            // Quiz sections (including Vocabulary): quick actions, search, then the topic grid.
            if (quizzableGroup) item(key = "actions") {
                HandMirror {  // like the web's .study-card-actions: the order flips with the hand
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Exams draw multiple-choice questions only; Written/Extra/Viva have none.
                        if (!LongForm.isLongForm(group!!.key)) SavedTile(Icons.Filled.Timer, "Exam", null, MaterialTheme.colorScheme.primary, Modifier.weight(1f), value = "Start") { nav.onExam(group.key) }
                        SavedTile(Icons.Filled.Star, "Nailed It", counts.getValue(PoolSet.NAILED), p.ok, Modifier.weight(1f)) { nav.onSaved(PoolSet.NAILED, group.key) }
                        SavedTile(Icons.Filled.Bookmark, "Important", counts.getValue(PoolSet.IMPORTANT), p.imp, Modifier.weight(1f)) { nav.onSaved(PoolSet.IMPORTANT, group.key) }
                    }
                }
            }
            item(key = "search") { com.syed.slate.ui.component.SlateSearchField(query, { query = it }, "Search ${group?.title ?: ""}") }
            if (tokens.isNotEmpty()) {
                item(key = "hits") { Text("${hits.size} results", style = MaterialTheme.typography.labelMedium, color = p.text3) }
                val pages = maxOf(1, (hits.size + SEARCH_PAGE - 1) / SEARCH_PAGE)
                items(hits.drop(page.coerceAtMost(pages - 1) * SEARCH_PAGE).take(SEARCH_PAGE), key = { it.id }) { hit ->
                    Column(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(p.surface).border(1.dp, p.outline, MaterialTheme.shapes.large).clickable { nav.onSearchHit(hit) }.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(topicNames[hit.topic] ?: hit.topic, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        val text = if (id == ModuleId.ICT) hit.question.lineSequence().first() else HtmlParser.plainText(hit.question)
                        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                item(key = "pager") { Pager(page.coerceAtMost(pages - 1), pages) { page = it; scope.launch { list.scrollToItem(2) } } }
            } else {
                // LiveMCQ Admin, for the owner only: import / classify / manage the section's questions.
                if (id == ModuleId.GENERAL && section == "livemcq" && owner) {
                    item(key = "admin") {
                        // The Written entry look (icon coin, name, chevron), not a filled block.
                        WrittenEntry(Icons.Filled.AdminPanelSettings, 1, "LiveMCQ Admin", "", nav.onAdmin)
                    }
                }
                items(group?.topics.orEmpty(), key = { it.key }) { t ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(p.surface).border(1.dp, p.outline, MaterialTheme.shapes.large)
                            .clickable { nav.onTopic(t.group, t.slug) }.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(t.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text("${t.count}", style = MaterialTheme.typography.labelLarge, color = p.text3)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
                    }
                }
            }
        }
    }
}

private fun sectionIcon(id: ModuleId, key: String): ImageVector = when (key) {
    "bangla", "english" -> Icons.Filled.Translate
    "sahitya" -> Icons.AutoMirrored.Filled.MenuBook
    "gk" -> Icons.Filled.Public
    "livemcq", "mcq" -> Icons.Filled.Psychology
    "extra" -> Icons.Filled.AutoAwesome
    "viva" -> Icons.Filled.Mic
    "practice" -> Icons.Filled.Code
    "vocab" -> Icons.Filled.Spellcheck
    UTILITY -> Icons.Filled.Build
    EQUATION -> Icons.Filled.Functions
    PRACTICE -> Icons.Filled.Code
    SQL -> Icons.Filled.Storage
    ROUTINE -> Icons.Filled.Checklist
    else -> Icons.Filled.EditNote // written
}

private fun subModuleTitle(id: ModuleId, key: String): String = when (key) {
    "bangla" -> "বাংলা ব্যাকরণ"; "english" -> "English Grammar"; "sahitya" -> "বাংলা সাহিত্য"
    "gk" -> "সাধারণ জ্ঞান"; "livemcq" -> "LiveMCQ"; WRITTEN -> "Written"; "vocab" -> "Vocabulary"; UTILITY -> "Utility"
    "wgk" -> "Written · GK"; "wfin" -> "Written · Financial"
    EQUATION -> "Equation"; PRACTICE -> "Practice"; SQL -> "SQL Lab"; ROUTINE -> "Routine"
    else -> TopicCatalog.groupTitle(key)
}

private fun subModuleTagline(id: ModuleId, key: String): String? = when (key) {
    "bangla" -> "ধ্বনি, শব্দ, বাক্য ও অলংকার"; "english" -> "Grammar, usage & idioms"; "sahitya" -> "কবি, গ্রন্থ ও সাহিত্যের ইতিহাস"
    "gk" -> "Bangladesh, world & current affairs"; "livemcq" -> "Live-exam favourites by topic"; WRITTEN -> "Notes & written answers"
    "vocab" -> "Words, meanings & usage"; UTILITY -> "Formulas & English grammar"
    "mcq" -> "Objective questions by topic"; "written" -> "Long answers & explanations"; "extra" -> "Extra questions & notes"
    "viva" -> "Oral questions & answers"; EQUATION -> "Cover & recall formulas"; PRACTICE -> "Linux & SQL command drills"; SQL -> "Subqueries step by step"
    else -> null
}

/**
 * The module launcher. General follows the web's top nav: the four quiz subjects as a 2×2 grid under "General", then
 * LiveMCQ, Vocabulary, Written, Utility and Routine each as their own labelled section (one wide card). ICT keeps the
 * plain grid of cards (icon coin, name, count).
 */
@Composable
private fun SubModuleLauncher(id: ModuleId, content: ModuleContent, onOpen: (String) -> Unit) {
    val groupKeys = content.groups.map { it.key }
    val count = { key: String -> content.groups.firstOrNull { it.key == key }?.count }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (id == ModuleId.GENERAL) {
            LauncherLabel("General")
            CardGrid(id, listOf("bangla", "english", "sahitya", "gk").filter { it in groupKeys }, count, onOpen)
            listOf("livemcq" to "LiveMCQ", "vocab" to "Vocabulary", WRITTEN to "Written", UTILITY to "Utility", ROUTINE to "Routine")
                .filter { (k, _) -> k !in listOf("livemcq", "vocab") || k in groupKeys }
                .forEach { (key, label) ->
                    LauncherLabel(label, top = 10.dp)
                    WideCard(id, key, count(key), onOpen)
                }
        } else {
            CardGrid(id, listOf("mcq", "written", "extra", "viva").filter { it in groupKeys } + listOf(EQUATION, PRACTICE, SQL, ROUTINE), count, onOpen)
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** A section's title block (the Written look, used by every section): rounded-square accent icon + bold name. */
@Composable
private fun SectionHero(icon: ImageVector, title: String) {
    Row(Modifier.padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onPrimary)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

/** A launcher section heading: small spaced capitals with a hairline, like the web's section labels. */
@Composable
private fun LauncherLabel(text: String, top: androidx.compose.ui.unit.Dp = 0.dp) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = top, start = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = p.text3, letterSpacing = androidx.compose.ui.unit.TextUnit(1.4f, androidx.compose.ui.unit.TextUnitType.Sp))
        Box(Modifier.weight(1f).height(1.dp).background(p.outline))
    }
}

@Composable
private fun CardGrid(id: ModuleId, keys: List<String>, count: (String) -> Int?, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    keys.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            row.forEach { key ->
                Surface(
                    onClick = { onOpen(key) }, modifier = Modifier.weight(1f).height(156.dp),
                    shape = RoundedCornerShape(28.dp), color = p.surface, border = BorderStroke(1.dp, p.outline),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Icon(sectionIcon(id, key), null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(subModuleTitle(id, key), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        count(key)?.let { Text("$it questions", style = MaterialTheme.typography.labelMedium, color = p.text3, maxLines = 1) }
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** One full-width section card: icon coin, name, question count, chevron. */
@Composable
private fun WideCard(id: ModuleId, key: String, count: Int?, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    Surface(onClick = { onOpen(key) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = p.surface, border = BorderStroke(1.dp, p.outline)) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(sectionIcon(id, key), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text(subModuleTitle(id, key), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                count?.let { Text("$it questions", style = MaterialTheme.typography.labelMedium, color = p.text3) }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
        }
    }
}


private fun bnN(n: Int) = n.toString().map { if (it.isDigit()) "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")

/** Written's Nailed / Important tile: tinted in its colour, icon coin, big count, "saved · দেখো ›" footer (the web's action cards). */
@Composable
private fun SavedTile(icon: ImageVector, label: String, count: Int?, color: Color, modifier: Modifier, value: String? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(
        // Theme surface and outline like the entry cards below; only the icon carries the Nailed / Important colour.
        modifier.clip(RoundedCornerShape(20.dp)).background(p.surface)
            .border(1.dp, p.outline, RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(color.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
        }
        Column {
            // A word ("Start") is set smaller than a count, in a box of the count's height so the labels stay level.
            Box(Modifier.height(with(androidx.compose.ui.platform.LocalDensity.current) { MaterialTheme.typography.headlineMedium.lineHeight.toDp() }), contentAlignment = Alignment.CenterStart) {
                Text(value ?: count?.toString().orEmpty(),
                    style = if (value != null) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = if (value != null) color else androidx.compose.ui.graphics.Color.Unspecified, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = p.text3)
            }
        }
    }
}

/** One entry of General » Written: a coloured icon coin, the name, a one-line summary and a chevron (Focus's group-list style). */
@Composable
private fun WrittenEntry(icon: ImageVector, topic: Int, title: String, sub: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = com.syed.slate.ui.theme.TopicColors.of(topic, p.isDark)
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(p.surface)
            .border(1.dp, p.outline, MaterialTheme.shapes.large).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(46.dp).clip(MaterialTheme.shapes.medium).background(c.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(24.dp), tint = c)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
    }
}

