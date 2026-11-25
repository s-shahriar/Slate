package com.syed.slate.ui.screen

import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syed.slate.backend.ModuleId
import com.syed.slate.content.BlobRepository
import com.syed.slate.progress.FlagRules
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.component.BarTitle
import com.syed.slate.ui.component.SlateTopBar
import com.syed.slate.ui.theme.LocalPalette
import com.syed.slate.ui.theme.TopicColors
import com.syed.slate.ui.theme.isDark

/** One saved-able thing of Written: a Focus block (content_blobs written/index, general-quiz
 *  scripts/build-written-index.mjs — the same uids the page's stars carry), or a GK / Financial question
 *  (modules wgk / wfin, sec = its group, page = its topic slug). */
class WrittenUnit(val uid: String, val sec: String, val page: String, val title: String, val color: Int, val kind: String, val q: String, val a: String, val text: String) {
    /** This app's route to the page (Focus page, or the question's topic in the reader). */
    val pageRoute get() = if (sec == "focus") "focus/$page" else "read/${ModuleId.GENERAL.key}/$sec/$page"
    /** … and to this very block / question. */
    val route get() = if (sec == "focus") "focus/$page" else "read/${ModuleId.GENERAL.key}/$sec/$page?focus=${android.net.Uri.encode(uid)}"
}

object WrittenIndex {
    fun units(blobs: BlobRepository): List<WrittenUnit> {
        val a = blobs.payload("written", "index")?.optJSONArray("units") ?: return emptyList()
        return List(a.length()) { i ->
            a.getJSONObject(i).let { o ->
                WrittenUnit(o.getString("uid"), o.getString("sec"), o.getString("page"), o.getString("title"), o.optInt("color"),
                    o.optString("kind"), o.optString("q"), o.optString("a"), o.optString("text"))
            }
        }
    }
}

private val SECS = listOf("focus" to "Focus", "wgk" to "GK", "wfin" to "Financial")
private fun bnW(n: Int) = n.toString().map { if (it.isDigit()) "০১২৩৪৫৬৭৮৯"[it - '0'] else it }.joinToString("")

/** General » Written » Important / Nailed: Written's own saved blocks, apart from the MCQ modules' lists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrittenSavedScreen(vm: SlateViewModel, nailedMode: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val m = vm.module(ModuleId.GENERAL)
    val ver by m.blobs.version.collectAsState()
    val state by m.content.state.collectAsState()
    val all = remember(ver, state) {
        val content = (state as? com.syed.slate.content.ContentState.Ready)?.content
        val names = content?.groups?.flatMap { g -> g.topics.map { (g.key + "/" + it.slug) to it.name } }?.toMap().orEmpty()
        val qs = content?.allItems()?.filter { it.uid != null && (it.group == "wgk" || it.group == "wfin") }?.map { item ->
            WrittenUnit(item.uid!!, item.group, item.topic, names["${item.group}/${item.topic}"] ?: item.topic, 0, "qa",
                item.question, item.data.optString("correct_answer_text"), "")
        }?.toList().orEmpty()
        WrittenIndex.units(m.blobs).filter { it.sec == "focus" } + qs
    }
    val flags by m.progress.flags.collectAsState()
    val p = LocalPalette.current
    fun saved(uid: String) = flags[uid]?.let { if (nailedMode) it.nailed else it.important } == true
    // Kept on screen until leaving, so un-marking a block doesn't make it vanish mid-read.
    val frozen = remember(all) { all.filter { saved(it.uid) }.map { it.uid }.toSet() }
    val shown = all.filter { it.uid in frozen || saved(it.uid) }
    var sec by remember { mutableStateOf("all") }
    // Eye (cover mode): answers covered; a tap shows one, another tap covers it again.
    var cover by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var shownAns by remember(cover) { mutableStateOf(setOf<String>()) }
    val list = shown.filter { sec == "all" || it.sec == sec }
    val groups = list.groupBy { it.pageRoute }.toList()

    Scaffold(
        topBar = {
            SlateTopBar(
                title = { BarTitle(if (nailedMode) "Written · Nailed" else "Written · Important", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { cover = !cover }) {
                        Icon(if (cover) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            if (cover) "Show answers" else "Cover answers",
                            tint = if (cover) MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "chips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("all" to "সব") + SECS, key = { it.first }) { (k, label) ->
                        val n = shown.count { k == "all" || it.sec == k }
                        FilterChip(selected = sec == k, onClick = { sec = k }, label = { Text("$label · ${bnW(n)}") })
                    }
                }
            }
            if (groups.isEmpty()) item(key = "empty") {
                Text(
                    if (nailedMode) "এখনো কিছু Nailed করা হয়নি — GK বা Financial-এর যেকোনো প্রশ্নের কোণের ✓ চাপো।"
                    else "এখনো কিছু Important করা হয়নি — Written-এর যেকোনো বক্সের কোণের ★ চাপো।",
                    Modifier.padding(vertical = 32.dp), style = MaterialTheme.typography.bodyLarge, color = p.text3,
                )
            }
            groups.forEach { (route, units) ->
                val first = units.first()
                item(key = "h-$route") {
                    val col = if (first.color == 0) MaterialTheme.colorScheme.primary else TopicColors.of(first.color, p.isDark)
                    Row(Modifier.fillMaxWidth().clickable { onOpen(route) }.padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(SECS.firstOrNull { it.first == first.sec }?.second.orEmpty(), Modifier.clip(MaterialTheme.shapes.small).background(col.copy(alpha = .14f)).padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = col)
                        Text(first.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = col, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open page", tint = col)
                    }
                }
                items(units, key = { it.uid }) { u ->
                    val f = flags[u.uid]
                    val imp = f?.important == true
                    val done = f?.nailed == true
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(p.surface).border(1.dp, p.outline, MaterialTheme.shapes.medium)
                            .clickable { onOpen(u.route) }.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f).padding(top = 4.dp)) {
                            if (u.kind == "qa") {
                                Text(u.q, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                val hidden = cover && u.uid !in shownAns
                                Text(
                                    if (hidden) "উত্তর দেখতে চাপো" else u.a,
                                    Modifier.padding(top = 6.dp).fillMaxWidth().clip(MaterialTheme.shapes.small)
                                        .background(p.primaryContainer.copy(alpha = .45f))
                                        .then(if (cover) Modifier.clickable { shownAns = if (u.uid in shownAns) shownAns - u.uid else shownAns + u.uid } else Modifier)
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium, color = if (hidden) p.text3 else p.text2,
                                )
                            } else {
                                Text(u.text, style = MaterialTheme.typography.bodyMedium, maxLines = 8, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Column {
                            IconButton(onClick = { m.progress.update(u.uid, FlagRules::toggleImportant) }) {
                                Icon(if (imp) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Important", tint = if (imp) p.imp else p.text3)
                            }
                            if (u.kind == "qa") IconButton(onClick = { m.progress.update(u.uid, FlagRules::toggleNailed) }) {
                                Icon(Icons.Filled.Check, "Nailed", Modifier.size(22.dp), tint = if (done) p.ok else p.text3)
                            }
                        }
                    }
                }
            }
        }
    }
}
