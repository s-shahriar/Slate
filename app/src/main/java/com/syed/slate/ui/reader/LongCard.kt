package com.syed.slate.ui.reader

import com.syed.slate.ui.component.categoryEditable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.syed.slate.content.Item
import com.syed.slate.content.answer
import com.syed.slate.content.headerCode
import com.syed.slate.content.headerCodeLang
import com.syed.slate.content.verdict
import com.syed.slate.progress.Flag
import com.syed.slate.progress.ProgressRepository
import com.syed.slate.ui.quiz.FlagBar
import com.syed.slate.ui.theme.LocalPalette

/**
 * One Written / Extra / Viva card. Collapsed it shows the question (plus a verdict chip and the listing it is about);
 * opened it shows the whole answer. The flag buttons stay within reach either way.
 */
@Composable
fun LongCard(
    item: Item,
    number: Int?,
    open: Boolean,
    flag: Flag,
    progress: ProgressRepository,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    topicLabel: String? = null,
    focused: Boolean = false,
    /** Short-answer cards (Written » GK / Financial): the answer is covered until tapped (the reader's eye button). */
    cover: Boolean = false,
) {
    // A short-answer card (General's Written » GK / Financial) shows its answer under the question like the web —
    // no fold; the eye covers it instead.
    val short = item.group in com.syed.slate.content.LongForm.shortAnswer
    var revealed by androidx.compose.runtime.saveable.rememberSaveable(cover, item.id) { androidx.compose.runtime.mutableStateOf(false) }
    val p = LocalPalette.current
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(p.surface)
            .border(if (focused) 2.dp else 0.dp, if (focused) primary else p.surface, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        topicLabel?.let {
            Text(it, Modifier.clip(MaterialTheme.shapes.small).background(p.primaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium, color = p.onPrimaryContainer)
        }
        Row(Modifier.fillMaxWidth().then(if (short) Modifier else Modifier.clickable(onClick = onToggle)), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            number?.let { Text("Q$it", style = MaterialTheme.typography.labelLarge, color = primary, modifier = Modifier.padding(top = 3.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Collapsed, the question is plain text so a tap anywhere on it opens the card; open, it can be highlighted,
                // and a plain tap on it still folds the card (long-press selects as before).
                QuestionTextBlocks(item.question, uid = item.uid, editable = open || short, onTap = if (short) ({}) else onToggle)
                item.verdict?.let {
                    Text(it, Modifier.clip(MaterialTheme.shapes.small).background(p.primaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge, color = p.onPrimaryContainer)
                }
            }
            if (!short) Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, if (open) "Collapse" else "Expand", tint = p.text3)
        }
        // The listing the card is about, visible while collapsed.
        item.headerCode?.let { CodeBlock(it, item.headerCodeLang, uid = item.uid, block = "headerCode") }
        if (short) item.answer?.let { a ->
            if (cover && !revealed) Box(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(p.primaryContainer.copy(alpha = .55f))
                    .border(1.dp, primary.copy(alpha = .25f), MaterialTheme.shapes.medium).clickable { revealed = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) { Text("উত্তর দেখতে চাপো", style = MaterialTheme.typography.bodyMedium, color = p.text3) }
            else WrittenBody(a, uid = item.uid)
        }
        else item.answer?.let { a ->
            androidx.compose.animation.AnimatedVisibility(open, enter = com.syed.slate.ui.component.Motion.expand, exit = com.syed.slate.ui.component.Motion.collapse) {
                WrittenBody(a, uid = item.uid)
            }
        }
        item.uid?.let { LongFlagBar(item, flag, it, progress) }
    }
}

/** A long card's flag bar; Written cards get the owner's Category action (CategoryEdit). Also used by the pinned bar. */
@Composable
fun LongFlagBar(item: Item, flag: Flag, uid: String, progress: ProgressRepository) {
    val catEdit = com.syed.slate.ui.component.LocalCategoryEdit.current?.takeIf { item.categoryEditable() }
    FlagBar(flag, uid, progress, itemId = item.id, onTopicEdit = catEdit?.let { edit -> { edit(item) } }, topicLabel = "Category")
}
