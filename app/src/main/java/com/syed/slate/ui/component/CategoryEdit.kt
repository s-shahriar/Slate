package com.syed.slate.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syed.slate.ModuleServices
import com.syed.slate.content.ContentState
import com.syed.slate.content.Item
import com.syed.slate.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ICT, owner only: opens the category picker for an MCQ or Written question (the web's ⋯ › Category). Null when the
 * signed-in account is not the owner, or outside ICT, so the cards simply show no such action.
 */
val LocalCategoryEdit = compositionLocalOf<((Item) -> Unit)?> { null }

/** The groups whose questions can change category (admin_set_category accepts these). */
fun Item.categoryEditable() = group == "mcq" || group == "written"

private const val OWNER_EMAIL = "ksnkkc@gmail.com"

/** Wraps an ICT page: provides [LocalCategoryEdit] for the owner and shows the picker sheet. */
@Composable
fun CategoryEditHost(m: ModuleServices, content: @Composable () -> Unit) {
    val session by m.auth.session.collectAsState()
    var editing by remember { mutableStateOf<Item?>(null) }
    val open: ((Item) -> Unit)? = if (session?.email == OWNER_EMAIL) { item -> editing = item } else null
    CompositionLocalProvider(LocalCategoryEdit provides open) { content() }
    editing?.let { CategorySheet(m, it) { editing = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(m: ModuleServices, item: Item, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var cats by remember { mutableStateOf<List<Pair<String, String>>?>(null) }
    var target by remember { mutableStateOf(item.topic) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    // Only the categories of the card's own section, as its topic list shows them: Written topics for a Written card,
    // MCQ topics (`mcq = true`, even when empty) for an MCQ. The current one always stays listed.
    val ownTopics = (m.content.state.value as? ContentState.Ready)?.content?.groups
        ?.firstOrNull { it.key == item.group }?.topics?.map { it.slug }?.toSet().orEmpty()
    LaunchedEffect(Unit) {
        runCatching {
            m.db.selectAll("categories", "select=slug,name,mcq&order=sort_order,slug").filter {
                val slug = it.getString("slug")
                slug == item.topic || slug in ownTopics || (item.group == "mcq" && it.optBoolean("mcq"))
            }.map { it.getString("slug") to it.getString("name") }
        }
            .onSuccess { cats = it }.onFailure { error = "Couldn't load the categories (needs a connection)." }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = p.surface) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Category", style = MaterialTheme.typography.titleLarge)
            Text(item.question.lineSequence().firstOrNull().orEmpty(), style = MaterialTheme.typography.bodySmall, color = p.text3, maxLines = 3, overflow = TextOverflow.Ellipsis)
            val list = cats
            if (list != null) SlateDropdown(list.map { (slug, name) -> DropOption(slug, if (slug == item.topic) "$name (এখন)" else name) }, target, { target = it }, prefix = "Move to")
            else if (error == null) SlateLoaderInline(22.dp)
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.bad) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Spacer(Modifier.padding(4.dp))
                Button(
                    enabled = !busy && cats != null && target != item.topic,
                    onClick = {
                        busy = true; error = null
                        scope.launch {
                            runCatching { m.db.rpc("admin_set_category", JSONObject().put("p_id", item.id).put("p_slug", target)) }
                                .onSuccess {
                                    val name = cats?.firstOrNull { it.first == target }?.second ?: target
                                    // The delta refresh picks the row up at its new place (its updated_at moved); closing first
                                    // would cancel it with this sheet's scope.
                                    m.content.refresh()
                                    Notices.post(Notice("Moved to $name", Notice.Kind.OK))
                                    onDismiss()
                                }
                                .onFailure { busy = false; error = it.message ?: "Couldn't move it" }
                        }
                    },
                ) { Text(if (busy) "Moving…" else "Move") }
            }
            Box(Modifier.padding(2.dp))
        }
    }
}
