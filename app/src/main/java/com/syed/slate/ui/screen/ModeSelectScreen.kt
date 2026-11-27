package com.syed.slate.ui.screen

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.syed.slate.ui.component.BarTitle
import com.syed.slate.ui.component.SlateTopBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.syed.slate.backend.ModuleId
import com.syed.slate.content.ContentState
import com.syed.slate.content.PoolSet
import com.syed.slate.content.QuizPool
import com.syed.slate.content.TopicCatalog
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.theme.LocalPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectScreen(
    vm: SlateViewModel, id: ModuleId, group: String, topic: String,
    onBack: () -> Unit, onQuiz: (PoolSet) -> Unit, onStudy: () -> Unit,
    onSwitchTopic: (String) -> Unit = {}, onStudySub: (String) -> Unit = {},
) {
    val m = vm.module(id)
    val state by m.content.state.collectAsState()
    val flags by m.progress.flags.collectAsState()
    val p = LocalPalette.current
    val content = (state as? ContentState.Ready)?.content
    val name = content?.groups?.firstOrNull { it.key == group }?.topics?.firstOrNull { it.slug == topic }?.name ?: TopicCatalog.prettify(topic)
    val items = remember(content, group, topic) { content?.items(group, topic).orEmpty() }
    val counts = remember(items, flags) { QuizPool.counts(items, flags) }
    var chooser by remember { mutableStateOf(false) }
    var switcher by remember { mutableStateOf(false) }
    val quizzable = counts.getValue(PoolSet.ALL)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle(name, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { com.syed.slate.ui.component.TopicSwitchButton { switcher = true } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ModeCard(Icons.Filled.PlayArrow, "MCQ Mode", "$quizzable questions", quizzable > 0) { chooser = true }
            ModeCard(Icons.AutoMirrored.Filled.MenuBook, "Study Mode", "${items.size} questions", items.isNotEmpty(), onStudy)
        }
    }

    // Categories of this section; for LiveMCQ a sub-topic opens Study on it.
    if (switcher && content != null) {
        val g = content.groups.firstOrNull { it.key == group }
        val subs = if (group == "livemcq") com.syed.slate.content.Subtopics.cards(items.filter { it.isQuizzable && flags[it.uid]?.nailed != true }, content.subtopicsFor(topic)) else emptyList()
        com.syed.slate.ui.component.TopicSwitcherSheet(
            g?.title ?: "Categories", g?.topics.orEmpty().map { com.syed.slate.ui.component.SwitchEntry(it.slug, it.name, it.count) }, topic,
            onTopic = { switcher = false; onSwitchTopic(it) }, onDismiss = { switcher = false },
            subtopics = if (subs.isEmpty()) emptyList() else listOf(com.syed.slate.ui.component.SwitchEntry(null, "সব sub-topic")) + subs.map { (s, n) -> com.syed.slate.ui.component.SwitchEntry(s.slug, s.name, n) },
            currentSub = "",
            onSubtopic = { s -> switcher = false; if (s == null) onStudy() else onStudySub(s) },
        )
    }

    // Always offered, like the web app: সব / Important / Weak / Nailed — disabled options show their zero counts.
    if (chooser) {
        ModalBottomSheet(onDismissRequest = { chooser = false }, containerColor = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("কোন প্রশ্নগুলো থেকে পরীক্ষা দেবে?", style = MaterialTheme.typography.titleMedium)
                PoolOption(Icons.Filled.Checklist, "সব প্রশ্ন", "পুরো টপিক থেকে", counts.getValue(PoolSet.ALL), p.info) { chooser = false; onQuiz(PoolSet.ALL) }
                PoolOption(Icons.Filled.Bookmark, "শুধু Important", "যেগুলো Important করে রেখেছো", counts.getValue(PoolSet.IMPORTANT), p.imp) { chooser = false; onQuiz(PoolSet.IMPORTANT) }
                PoolOption(Icons.Filled.LocalFireDepartment, "শুধু Weak", "Important-এর মধ্যে যেগুলো এখনো পারো না", counts.getValue(PoolSet.WEAK), p.warn) { chooser = false; onQuiz(PoolSet.WEAK) }
                PoolOption(Icons.Filled.Star, "শুধু Nailed It", "যেগুলো আয়ত্তে এসেছে — ঝালিয়ে নাও", counts.getValue(PoolSet.NAILED), p.ok) { chooser = false; onQuiz(PoolSet.NAILED) }
            }
        }
    }
}

/** MCQ / Study entry, in the Written card look: hairline outline, tinted icon coin, name + count, chevron. */
@Composable
private fun ModeCard(icon: ImageVector, title: String, meta: String, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val c = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().alpha(if (enabled) 1f else .5f).clip(MaterialTheme.shapes.large).background(p.surface)
            .border(1.dp, p.outline, MaterialTheme.shapes.large).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(46.dp).clip(MaterialTheme.shapes.medium).background(c.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(24.dp), tint = c)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(meta, style = MaterialTheme.typography.labelMedium, color = p.text3)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.text3)
    }
}

/** One pool in the "which questions" sheet: the same card look; an empty pool is dimmed and shows its 0. */
@Composable
private fun PoolOption(icon: ImageVector, title: String, @Suppress("UNUSED_PARAMETER") sub: String, count: Int, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().alpha(if (count > 0) 1f else .45f).clip(RoundedCornerShape(16.dp)).background(p.surface)
            .border(1.dp, p.outline, RoundedCornerShape(16.dp)).clickable(enabled = count > 0, onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = color)
        }
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
        Text("$count", Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .14f)).padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = color)
    }
}
