package com.syed.slate.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syed.slate.backend.ModuleId
import com.syed.slate.content.ContentState
import com.syed.slate.content.PoolSet
import com.syed.slate.content.QuizDecks
import com.syed.slate.content.QuizPool
import com.syed.slate.content.TopicCatalog
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.ExamSpec
import com.syed.slate.ui.quiz.QuizSession
import com.syed.slate.ui.theme.LocalPalette

private val poolLabel = mapOf(PoolSet.IMPORTANT to "Important", PoolSet.WEAK to "Weak", PoolSet.NAILED to "Nailed")

@Composable
fun QuizScreen(vm: SlateViewModel, id: ModuleId, group: String, topic: String, set: PoolSet, onBack: () -> Unit, onHome: () -> Unit) {
    val m = vm.module(id)
    val state by m.content.state.collectAsState()
    val content = (state as? ContentState.Ready)?.content ?: return
    val name = content.groups.firstOrNull { it.key == group }?.topics?.firstOrNull { it.slug == topic }?.name ?: TopicCatalog.prettify(topic)
    // Built once per quiz and then fixed: deleting a question, a refresh or a flag change must not reshuffle or
    // shorten it. The seed survives recreation, so even a rebuild after process death gives the same order.
    val seed = rememberSaveable(group, topic, set) { kotlin.random.Random.nextLong() }
    val pool = remember(seed) { QuizDecks.get(seed) { QuizPool.build(content.items(group, topic), m.progress.flags.value, set, seed) } }
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        QuizSession(
            services = m, questions = pool, topicName = { null }, pill = name,
            resultTitle = poolLabel[set]?.let { "$it Quiz Complete!" } ?: "Quiz Complete!",
            emptyMessage = "$name-এ এখনো কোনো ${poolLabel[set] ?: ""} প্রশ্ন নেই।",
            onBack = onBack, onHome = onHome,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamConfigScreen(vm: SlateViewModel, id: ModuleId, initialGroup: String?, onBack: () -> Unit, onStart: () -> Unit) {
    val m = vm.module(id)
    val state by m.content.state.collectAsState()
    val flags by m.progress.flags.collectAsState()
    val p = LocalPalette.current
    val content = (state as? ContentState.Ready)?.content
    var groupKey by rememberSaveable { mutableStateOf(initialGroup ?: content?.groups?.firstOrNull()?.key ?: "") }
    var choice by rememberSaveable { mutableStateOf("all") } // all | important | weak | topic:<slug>
    var count by rememberSaveable { mutableIntStateOf(10) }
    // "mix" pools every group at once — the web's "All Topics (Random Mix)".
    val isMix = groupKey == "mix"
    val group = if (isMix) null else content?.groups?.firstOrNull { it.key == groupKey } ?: content?.groups?.firstOrNull()

    // Important / Weak are scoped to the chosen pool: the group, or everything in mix mode.
    val scopeItems = remember(content, group, isMix) {
        if (isMix) content!!.allItems().filter { it.isQuizzable }.toList()
        else group?.topics.orEmpty().flatMap { content!!.items(group!!.key, it.slug) }
    }
    val set = when {
        choice == "important" -> PoolSet.IMPORTANT
        choice == "weak" -> PoolSet.WEAK
        else -> PoolSet.ALL
    }
    val items = remember(content, group, choice, isMix, scopeItems) {
        if (choice.startsWith("topic:") && !isMix) content!!.items(group!!.key, choice.removePrefix("topic:")) else scopeItems
    }
    val pool = remember(items, flags, set) { QuizPool.build(items, flags, set) }
    val counts = remember(scopeItems, flags) { QuizPool.counts(scopeItems, flags) }
    val maxCount = pool.size
    val safeCount = if (maxCount == 0) 0 else count.coerceIn(1, maxCount)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle("Exam Mode", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        if (content == null || (!isMix && group == null)) return@Scaffold
        // Laid out like the web's ExamConfig: the two pickers and the count share one card, the Start button sits
        // under it, full width, so nothing on the screen is left ragged against the edge.
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(p.surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Dropdown(
                    "Subject group",
                    listOf("mix" to "Random Mix · every group") + content.groups.map { it.key to "${it.title} · ${it.count}" },
                    groupKey,
                ) { groupKey = it; choice = "all" }
                Dropdown(
                    "Topic",
                    buildList {
                        add("all" to "All topics (${counts.getValue(PoolSet.ALL)} Q)")
                        add("important" to "Important Questions (${counts.getValue(PoolSet.IMPORTANT)} Q)")
                        add("weak" to "Weak Questions (${counts.getValue(PoolSet.WEAK)} Q)")
                        if (!isMix) group?.topics?.forEach { add("topic:${it.slug}" to "${it.name} (${it.count})") }
                    },
                    choice,
                ) { choice = it }
                QuestionCount(safeCount, maxCount) { count = it }
            }
            Button(
                onClick = {
                    val label = when {
                        isMix -> "Random Mix"
                        choice == "important" -> "Important Questions"
                        choice == "weak" -> "Weak Questions"
                        choice.startsWith("topic:") -> group?.topics?.firstOrNull { it.slug == choice.removePrefix("topic:") }?.name
                        else -> group?.title
                    }
                    vm.exam = ExamSpec(id, pool.take(safeCount), label)
                    onStart()
                },
                Modifier.fillMaxWidth().height(52.dp), enabled = maxCount > 0,
            ) {
                Icon(Icons.Filled.Bolt, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Exam — $safeCount Questions", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * The web's "Number of Questions" field: a label row with the pool size at its end, then a centred row of equal
 * 44 dp steppers (−5 · − · count · + · +5). The count itself is a text field — type 611 instead of tapping +5 a
 * hundred times. It is free text while it has focus (so clearing it does not snap to 1 under your thumb); the parent
 * clamps whatever is typed to 1…[max] everywhere the value is used, and the field re-syncs to that when focus leaves.
 */
@Composable
private fun QuestionCount(value: Int, max: Int, onChange: (Int) -> Unit) {
    val p = LocalPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val focus = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value, focused) { if (!focused) draft = value.toString() }
    // A stepper tap drops the keyboard first, so the field shows the stepped value rather than a stale draft.
    fun step(delta: Int) { focus.clearFocus(); onChange((value + delta).coerceIn(1, max.coerceAtLeast(1))) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("NUMBER OF QUESTIONS", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = p.text3, letterSpacing = 1.sp)
            Text("max $max", style = MaterialTheme.typography.labelMedium, color = primary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            StepChip(value > 1, { step(-5) }) { Text("−5", style = MaterialTheme.typography.labelLarge, color = p.text2) }
            StepChip(value > 1, { step(-1) }) { Icon(Icons.Filled.Remove, "Fewer", Modifier.size(18.dp), tint = p.text2) }
            BasicTextField(
                value = draft,
                onValueChange = { typed ->
                    val digits = typed.filter { it.isDigit() }.take(5)
                    draft = digits
                    digits.toIntOrNull()?.let(onChange)
                },
                modifier = Modifier.width(88.dp).onFocusChanged { focused = it.isFocused },
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = primary, textAlign = TextAlign.Center, fontWeight = FontWeight.ExtraBold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                singleLine = true,
                cursorBrush = SolidColor(primary),
                decorationBox = { inner ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.padding(vertical = 2.dp), contentAlignment = Alignment.Center) { inner() }
                        // The web's focus underline: the only hint that the number is editable, shown while it is.
                        Box(Modifier.width(56.dp).height(2.dp).background(if (focused) primary else Color.Transparent))
                    }
                },
            )
            StepChip(value < max, { step(1) }) { Icon(Icons.Filled.Add, "More", Modifier.size(18.dp), tint = p.text2) }
            StepChip(value < max, { step(5) }) { Text("+5", style = MaterialTheme.typography.labelLarge, color = p.text2) }
        }
    }
}

/** One square exam stepper, the same size whether it holds a glyph or "±5", so the row lines up. */
@Composable
private fun StepChip(enabled: Boolean, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.size(44.dp).alpha(if (enabled) 1f else .35f).clip(MaterialTheme.shapes.medium).background(p.elevated)
            .border(BorderStroke(1.dp, p.outline), MaterialTheme.shapes.medium).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center, content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { open = it }) {
        OutlinedTextField(
            options.firstOrNull { it.first == selected }?.second.orEmpty(), {}, Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
        )
        ExposedDropdownMenu(open, { open = false }) {
            options.forEach { (k, v) -> DropdownMenuItem({ Text(v) }, { onSelect(k); open = false }) }
        }
    }
}

@Composable
fun ExamRunScreen(vm: SlateViewModel, onBack: () -> Unit, onHome: () -> Unit) {
    val spec = vm.exam
    if (spec == null) { onBack(); return }
    val m = vm.module(spec.module)
    val state by m.content.state.collectAsState()
    val content = (state as? ContentState.Ready)?.content
    val names = remember(content) { content?.groups?.flatMap { it.topics }?.associate { it.key to it.name }.orEmpty() }
    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        QuizSession(
            services = m, questions = spec.items, topicName = { names["${it.group}/${it.topic}"] },
            pill = "Exam · ${spec.label ?: ""}", resultTitle = "Exam Complete!", resultLabel = spec.label,
            showTopicTag = true, stoppable = true, onBack = onBack, onHome = onHome,
        )
    }
}
