package com.syed.slate.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syed.slate.ui.theme.LocalPalette

/** One row of the switcher: a category, or (nested under the current one) a sub-topic. `key` null = "all sub-topics". */
class SwitchEntry(val key: String?, val name: String, val count: Int? = null)

/** The top-bar button that opens [TopicSwitcherSheet] (the web's ⊞ categories button). */
@Composable
fun TopicSwitchButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Filled.GridView, "Categories") }
}

/**
 * The web's CATEGORIES panel: every category of the section the page belongs to, the current one marked, and — for a
 * category with sub-topics (LiveMCQ) — its sub-topics nested under it. Lets a reader move sideways without going back
 * to the list. Picking the current category with no sub-topic change just closes the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSwitcherSheet(
    title: String,
    topics: List<SwitchEntry>,
    current: String,
    onTopic: (String) -> Unit,
    onDismiss: () -> Unit,
    subtopics: List<SwitchEntry> = emptyList(),
    currentSub: String? = null,
    onSubtopic: (String?) -> Unit = {},
) {
    val p = LocalPalette.current
    val primary = MaterialTheme.colorScheme.primary
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = p.surface) {
        Text(title.uppercase(), Modifier.padding(start = 20.dp, bottom = 8.dp), style = MaterialTheme.typography.labelLarge, color = p.text3)
        LazyColumn(Modifier.fillMaxWidth().navigationBarsPadding(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            items(topics, key = { it.key ?: "" }) { t ->
                val isCurrent = t.key == current
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(MaterialTheme.shapes.medium)
                            .then(if (isCurrent) Modifier.background(primary.copy(alpha = .08f)).border(BorderStroke(1.dp, primary.copy(alpha = .45f)), MaterialTheme.shapes.medium) else Modifier)
                            .clickable { if (isCurrent && subtopics.isEmpty()) onDismiss() else if (!isCurrent) onTopic(t.key!!) else onSubtopic(null) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(t.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = if (isCurrent) primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (isCurrent) Text("CURRENT", Modifier.clip(MaterialTheme.shapes.small).border(1.dp, primary.copy(alpha = .5f), MaterialTheme.shapes.small)
                            .padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = primary, fontWeight = FontWeight.Bold)
                        else t.count?.let { Text("$it", style = MaterialTheme.typography.labelLarge, color = p.text3) }
                    }
                    // Sub-topics of the current category, on a guide line like the web's.
                    if (isCurrent && subtopics.isNotEmpty()) Row(Modifier.padding(start = 22.dp, top = 4.dp, bottom = 6.dp)) {
                        Box(Modifier.width(2.dp).height((subtopics.size * 46).dp).background(p.outline))
                        Column(Modifier.padding(start = 10.dp)) {
                            subtopics.forEach { s ->
                                val on = s.key == currentSub
                                Row(
                                    Modifier.fillMaxWidth().height(46.dp).clip(MaterialTheme.shapes.small)
                                        .then(if (on) Modifier.background(primary.copy(alpha = .10f)) else Modifier)
                                        .clickable { onSubtopic(s.key) }.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(s.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = if (on) primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    s.count?.let { Text("$it", style = MaterialTheme.typography.labelLarge, color = primary) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
