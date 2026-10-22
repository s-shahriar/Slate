package com.syed.slate.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syed.slate.backend.ModuleId
import com.syed.slate.routine.RoutineItem
import com.syed.slate.routine.RoutineOps
import com.syed.slate.routine.RoutineSubject
import com.syed.slate.routine.Slot
import com.syed.slate.ui.SlateViewModel
import com.syed.slate.ui.component.BarTitle
import com.syed.slate.ui.component.Notice
import com.syed.slate.ui.component.Notices
import com.syed.slate.ui.component.SlateTopBar
import com.syed.slate.ui.component.LocalDarkTheme
import com.syed.slate.ui.theme.LocalPalette
import com.syed.slate.ui.theme.Palette
import com.syed.slate.ui.theme.TopicColors
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private fun slotIcon(s: Slot): ImageVector = when (s) {
    Slot.MORNING -> Icons.Outlined.WbTwilight
    Slot.AFTERNOON -> Icons.Filled.WbSunny
    Slot.EVENING -> Icons.Filled.WbTwilight
    Slot.LATE_NIGHT -> Icons.Filled.Bedtime
}

// Same hues as the web's .rt-slot--* (topic 3 / 5 / 7 / 9).
private fun slotColor(s: Slot, dark: Boolean): Color = TopicColors.of(3 + 2 * s.ordinal, dark)

private fun LocalDate.short() = "${dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}, $dayOfMonth ${month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
private fun LocalDate.long() = "${dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}, $dayOfMonth ${month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"

private class Progress(val subjects: Int, val done: Int, val items: Int, val itemsDone: Int)
private fun progressOf(list: List<RoutineSubject>) =
    Progress(list.size, list.count { it.done }, list.sumOf { it.items.size }, list.sumOf { s -> s.items.count { it.done } })

/** A slot whose add form is open, or a subject being edited. */
private sealed interface Editing { data class Add(val slot: Slot) : Editing; data class Edit(val id: String) : Editing }

/**
 * Study routine, the web's /routine: a date-wise plan in four slots; each subject has its own tick and its own
 * tickable tasks (Book, MCQ…). Add / edit / delete / reorder inline, copy another day's plan in one tap.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutineScreen(vm: SlateViewModel, id: ModuleId, onBack: () -> Unit) {
    val m = vm.module(id)
    val repo = m.routine
    val all by repo.subjects.collectAsState()
    val pendingCount by repo.pendingCount.collectAsState()
    val failure by repo.failure.collectAsState()
    val session by m.auth.session.collectAsState()
    val online by vm.online.collectAsState()
    val p = LocalPalette.current
    val dark = LocalDarkTheme.current
    LaunchedEffect(session?.userId, online) { if (online) repo.refresh() }

    val today = LocalDate.now()
    var dayText by rememberSaveable { mutableStateOf(today.toString()) }
    val day = LocalDate.parse(dayText)
    var editing by remember { mutableStateOf<Editing?>(null) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) } // null | "jump" | "copy"
    fun go(d: LocalDate) { dayText = d.toString(); editing = null }

    val byDay = remember(all) { all.values.groupBy { it.day } }
    val subjects = byDay[dayText].orEmpty()
    val prog = progressOf(subjects)
    val suggestions = remember(all) { RoutineOps.PRESET_ITEMS + RoutineOps.customLabels(all.values) }
    val lastPlanned = byDay.keys.filter { it < dayText }.maxOrNull()

    fun copyFrom(from: String) {
        val n = repo.copyDay(from, dayText)
        picker = null
        val label = LocalDate.parse(from).short()
        Notices.post(Notice(if (n > 0) "$n subject${if (n > 1) "s" else ""} copied from $label" else "$label has no plan", Notice.Kind.OK))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SlateTopBar(
                title = { BarTitle("Routine") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "date") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = { go(day.minusDays(1)) }) { Icon(Icons.Filled.ChevronLeft, "Previous day") }
                    Surface(
                        onClick = { picker = if (picker == "jump") null else "jump" }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        shape = RoundedCornerShape(14.dp), color = p.surface, border = BorderStroke(1.dp, p.outline),
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp), tint = p.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(day.long(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (day == today) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "TODAY", Modifier.clip(RoundedCornerShape(50)).background(p.primaryContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall, color = p.onPrimaryContainer, fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                    IconButton(onClick = { go(day.plusDays(1)) }) { Icon(Icons.Filled.ChevronRight, "Next day") }
                }
            }

            picker?.let { mode ->
                item(key = "picker") {
                    MonthPicker(
                        value = day, planned = byDay.keys, title = if (mode == "copy") "Copy plan from…" else null,
                        enabled = { d -> mode != "copy" || (d != day && byDay.containsKey(d.toString())) },
                        onPick = { d -> if (mode == "copy") copyFrom(d.toString()) else { go(d); picker = null } },
                        onClose = { picker = null },
                    )
                }
            }

            item(key = "week") { WeekStrip(day, today, byDay) { go(it) } }

            if (session == null || pendingCount > 0 || failure != null) item(key = "sync") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CloudOff, null, Modifier.size(15.dp), tint = p.warn)
                    Text(
                        when {
                            session == null -> "Not signed in — the plan stays on this phone until you sign in (Settings)"
                            !online && pendingCount > 0 -> "Offline — $pendingCount change${if (pendingCount == 1) "" else "s"} saved here, will sync"
                            pendingCount > 0 -> "Syncing $pendingCount change${if (pendingCount == 1) "" else "s"}…"
                            else -> "Couldn't reach the server: $failure"
                        },
                        style = MaterialTheme.typography.labelMedium, color = p.warn,
                    )
                }
            }

            if (prog.subjects > 0) item(key = "summary") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${prog.done}/${prog.subjects} subjects" + if (prog.items > 0) " · ${prog.itemsDone}/${prog.items} tasks" else "",
                        style = MaterialTheme.typography.bodyMedium, color = p.text2, fontWeight = FontWeight.SemiBold,
                    )
                    LinearProgressIndicator(
                        progress = { if (prog.items > 0) prog.itemsDone / prog.items.toFloat() else prog.done / prog.subjects.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)),
                        color = p.ok, trackColor = p.elevated, drawStopIndicator = {},
                    )
                }
            } else item(key = "blank") {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, p.outline, RoundedCornerShape(16.dp)).padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("No plan for ${day.short()} yet.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Add a subject under a slot below" + if (lastPlanned != null) ", or start from an earlier day." else ".",
                        style = MaterialTheme.typography.bodySmall, color = p.text3,
                    )
                    if (lastPlanned != null) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RtButton("Copy ${LocalDate.parse(lastPlanned).short()}", solid = true, icon = Icons.Filled.ContentCopy) { copyFrom(lastPlanned) }
                        RtButton("Other day…") { picker = "copy" }
                    }
                }
            }

            var number = 0
            Slot.entries.forEach { slot ->
                val color = slotColor(slot, dark)
                val list = subjects.filter { it.slot == slot }.sortedWith(RoutineOps.order)
                item(key = "slot-${slot.id}") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                            Icon(slotIcon(slot), null, Modifier.size(19.dp), tint = color)
                        }
                        Text(slot.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        if (list.isNotEmpty()) Text("${list.count { it.done }}/${list.size}", style = MaterialTheme.typography.labelMedium, color = p.text3)
                        Spacer(Modifier.weight(1f))
                        Surface(
                            onClick = { editing = if (editing == Editing.Add(slot)) null else Editing.Add(slot) },
                            shape = RoundedCornerShape(10.dp), color = color.copy(alpha = .10f), border = BorderStroke(1.dp, color.copy(alpha = .3f)),
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, null, Modifier.size(17.dp), tint = color)
                                Spacer(Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                list.forEachIndexed { i, s ->
                    number++
                    val n = number
                    item(key = s.id) {
                        if (editing == Editing.Edit(s.id)) SubjectForm(
                            initial = s, color = color, suggestions = suggestions,
                            canUp = i > 0, canDown = i < list.lastIndex,
                            onMove = { dir -> repo.move(s.id, dir) },
                            onDelete = { repo.delete(s.id); editing = null },
                            onSave = { title, sl, items -> repo.edit(s.id, title, sl, items); editing = null },
                            onCancel = { editing = null },
                        ) else SubjectCard(n, s, color, suggestions, repo::toggle, repo::toggleItem, repo::addItem) { editing = Editing.Edit(s.id) }
                    }
                }
                if (editing == Editing.Add(slot)) item(key = "add-${slot.id}") {
                    SubjectForm(
                        initial = RoutineSubject("", dayText, slot, "", false, emptyList(), 0.0, ""), color = color, suggestions = suggestions,
                        onSave = { title, sl, items -> repo.add(dayText, sl, title, items.map { it.label }); editing = null },
                        onCancel = { editing = null },
                    )
                }
            }

            if (prog.subjects > 0) item(key = "copy") {
                Surface(
                    onClick = { picker = "copy" }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(14.dp), color = Color.Transparent, border = BorderStroke(1.dp, p.outline),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp), tint = p.text3)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy another day's plan into ${day.short()}", style = MaterialTheme.typography.labelLarge, color = p.text3)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekStrip(day: LocalDate, today: LocalDate, byDay: Map<String, List<RoutineSubject>>, onPick: (LocalDate) -> Unit) {
    val p = LocalPalette.current
    // Saturday-first, the local study week.
    val start = day.minusDays(((day.dayOfWeek.value + 1) % 7).toLong())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        (0 until 7).forEach { i ->
            val d = start.plusDays(i.toLong())
            val prog = progressOf(byDay[d.toString()].orEmpty())
            val active = d == day
            val ink = if (active) p.onPrimary else p.text
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(if (active) p.primary else p.surface)
                    .border(1.dp, if (active) p.primary else p.outline, RoundedCornerShape(13.dp)).clickable { onPick(d) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase(), style = MaterialTheme.typography.labelSmall, color = if (active) p.onPrimary else p.text3)
                Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (!active && d == today) p.primary else ink)
                Box(Modifier.width(22.dp).height(4.dp).clip(RoundedCornerShape(4.dp)).background(
                    when {
                        prog.subjects == 0 -> Color.Transparent
                        prog.done == prog.subjects -> if (active) p.onPrimary else p.ok
                        else -> ink.copy(alpha = .22f)
                    },
                )) {
                    if (prog.subjects > 0 && prog.done < prog.subjects) Box(
                        Modifier.fillMaxWidth(prog.done / prog.subjects.toFloat()).height(4.dp).background(if (active) p.onPrimary else p.ok),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthPicker(value: LocalDate, planned: Set<String>, title: String?, enabled: (LocalDate) -> Boolean, onPick: (LocalDate) -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    var month by remember { mutableStateOf(YearMonth.from(value)) }
    val today = LocalDate.now()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface).border(1.dp, p.outline, RoundedCornerShape(16.dp)).padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.Filled.ChevronLeft, "Previous month") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                title?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = p.primary, fontWeight = FontWeight.Bold) }
                Text("${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.Filled.ChevronRight, "Next month") }
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close") }
        }
        val lead = month.atDay(1).dayOfWeek.value % 7 // Sunday-first grid
        val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        Row(Modifier.fillMaxWidth()) {
            listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY).forEach {
                Text(it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(2), Modifier.weight(1f).padding(vertical = 4.dp), style = MaterialTheme.typography.labelSmall,
                    color = p.text3, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { i ->
                    val d = week.getOrNull(i)
                    Box(Modifier.weight(1f).height(42.dp).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (d != null) {
                            val on = enabled(d)
                            val active = d == value
                            Box(
                                Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(if (active) p.primary else Color.Transparent)
                                    .border(1.dp, if (d == today && !active) p.primary else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable(enabled = on) { onPick(d) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${d.dayOfMonth}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                                    color = (if (active) p.onPrimary else p.text).copy(alpha = if (on) 1f else .28f))
                                if (d.toString() in planned) Box(
                                    Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(5.dp).clip(RoundedCornerShape(50))
                                        .background(if (active) p.onPrimary else p.primary),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckBox(on: Boolean, size: Int, p: Palette, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(if (on) p.ok else Color.Transparent)
            .border(2.dp, if (on) p.ok else p.text3.copy(alpha = .6f), RoundedCornerShape((size / 3).dp)),
        contentAlignment = Alignment.Center,
    ) { if (on) Icon(Icons.Filled.Check, null, Modifier.size((size * .65f).dp), tint = p.surface) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectCard(
    n: Int, s: RoutineSubject, color: Color, suggestions: List<String>,
    onToggle: (String) -> Unit, onToggleItem: (String, String) -> Unit, onAddItem: (String, String) -> Unit, onEdit: () -> Unit,
) {
    val p = LocalPalette.current
    var adding by rememberSaveable(s.id) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface)
            // The slot-colour rail on the left edge.
            .drawBehind { drawRect(color, size = Size(3.dp.toPx(), size.height)) }
            .border(1.dp, p.outline, RoundedCornerShape(16.dp)),
    ) {
        Column(Modifier.weight(1f).padding(start = 13.dp, end = 4.dp, top = 6.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().clickable { onToggle(s.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                CheckBox(s.done, 28, p)
                Spacer(Modifier.width(10.dp))
                Text("$n", style = MaterialTheme.typography.labelLarge, color = p.text3, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(10.dp))
                Text(
                    s.title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = if (s.done) p.text3 else p.text, textDecoration = if (s.done) TextDecoration.LineThrough else null,
                )
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Edit", Modifier.size(18.dp), tint = p.text3) }
            }
            FlowRow(Modifier.padding(top = 6.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                s.items.forEach { it ->
                    Row(
                        Modifier.heightIn(min = 38.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (it.done) p.ok.copy(alpha = .14f) else p.elevated)
                            .border(1.dp, if (it.done) p.ok.copy(alpha = .35f) else p.outline, RoundedCornerShape(10.dp))
                            .clickable { onToggleItem(s.id, it.id) }.padding(start = 8.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckBox(it.done, 18, p)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            it.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                            color = if (it.done) p.text3 else p.text, textDecoration = if (it.done) TextDecoration.LineThrough else null,
                        )
                    }
                }
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, p.outline, RoundedCornerShape(10.dp)).clickable { adding = !adding },
                    contentAlignment = Alignment.Center,
                ) { Icon(if (adding) Icons.Filled.Close else Icons.Filled.Add, "Add a task", Modifier.size(17.dp), tint = p.text3) }
            }
            if (adding) {
                val present = s.items.map { it.label.lowercase() }.toSet()
                ItemPicker(suggestions.filter { it.lowercase() !in present }, Modifier.padding(top = 10.dp, end = 8.dp)) { onAddItem(s.id, it) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemPicker(suggestions: List<String>, modifier: Modifier = Modifier, onAdd: (String) -> Unit) {
    val p = LocalPalette.current
    var text by rememberSaveable { mutableStateOf("") }
    fun submit() { if (text.isNotBlank()) { onAdd(text); text = "" } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        if (suggestions.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            suggestions.forEach { l ->
                Row(
                    Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(50)).border(1.dp, p.text3.copy(alpha = .45f), RoundedCornerShape(50))
                        .clickable { onAdd(l) }.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(14.dp), tint = p.text2)
                    Spacer(Modifier.width(4.dp))
                    Text(l, style = MaterialTheme.typography.labelLarge, color = p.text2)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                text, { text = it }, Modifier.weight(1f), placeholder = { Text("Other task…") }, singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            RtButton("Add", enabled = text.isNotBlank()) { submit() }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectForm(
    initial: RoutineSubject, color: Color, suggestions: List<String>,
    onSave: (String, Slot, List<RoutineItem>) -> Unit, onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null, onMove: (Int) -> Unit = {}, canUp: Boolean = false, canDown: Boolean = false,
) {
    val p = LocalPalette.current
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var slot by remember(initial.id) { mutableStateOf(initial.slot) }
    val items = remember(initial.id) { mutableStateListOf<RoutineItem>().apply { addAll(initial.items) } }
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(3000); armed = false } }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val present = items.map { it.label.lowercase() }.toSet()
    fun addLabel(l: String) { val c = RoutineOps.clean(l); if (c.isNotEmpty() && c.lowercase() !in present) items.add(RoutineItem(com.syed.slate.routine.newId(), c, false)) }
    fun save() { if (title.isNotBlank()) onSave(title, slot, items.toList()) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface).border(1.dp, color.copy(alpha = .5f), RoundedCornerShape(16.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            title, { title = it }, Modifier.fillMaxWidth().focusRequester(focus), placeholder = { Text("Subject, e.g. Cloud Computing") },
            singleLine = true, shape = RoundedCornerShape(12.dp), textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
        Slot.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { s ->
                    val on = s == slot
                    Row(
                        Modifier.weight(1f).heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp)).background(if (on) p.primary else p.elevated)
                            .border(1.dp, if (on) p.primary else p.outline, RoundedCornerShape(10.dp)).clickable { slot = s },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(slotIcon(s), null, Modifier.size(15.dp), tint = if (on) p.onPrimary else p.text2)
                        Spacer(Modifier.width(6.dp))
                        Text(s.label, style = MaterialTheme.typography.labelLarge, color = if (on) p.onPrimary else p.text2, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text("TASKS", style = MaterialTheme.typography.labelSmall, color = p.text3, fontWeight = FontWeight.ExtraBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            if (items.isEmpty()) Text("Tap below to add Book, MCQ…", style = MaterialTheme.typography.bodySmall, color = p.text3)
            items.toList().forEach { it ->
                Row(
                    Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(50)).background(if (it.done) p.ok.copy(alpha = .16f) else p.primaryContainer)
                        .padding(start = 12.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (it.done) { Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = p.ok); Spacer(Modifier.width(4.dp)) }
                    Text(it.label, style = MaterialTheme.typography.labelLarge, color = if (it.done) p.text else p.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { items.remove(it) }, Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, "Remove ${it.label}", Modifier.size(15.dp), tint = if (it.done) p.text2 else p.onPrimaryContainer)
                    }
                }
            }
        }
        ItemPicker(suggestions.filter { it.lowercase() !in present }) { addLabel(it) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onDelete != null) {
                IconButton(onClick = { onMove(-1) }, enabled = canUp) { Icon(Icons.Filled.ArrowUpward, "Move up") }
                IconButton(onClick = { onMove(1) }, enabled = canDown) { Icon(Icons.Filled.ArrowDownward, "Move down") }
                Box(
                    Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(12.dp)).background(if (armed) p.bad else Color.Transparent)
                        .border(1.dp, p.bad.copy(alpha = if (armed) 1f else .4f), RoundedCornerShape(12.dp))
                        .clickable { if (armed) onDelete() else armed = true }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DeleteOutline, null, Modifier.size(17.dp), tint = if (armed) p.surface else p.bad)
                        Spacer(Modifier.width(4.dp))
                        Text(if (armed) "Tap again" else "Delete", style = MaterialTheme.typography.labelLarge, color = if (armed) p.surface else p.bad, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            RtButton("Cancel") { onCancel() }
            RtButton(if (onDelete != null) "Save" else "Add", solid = true, enabled = title.isNotBlank()) { save() }
        }
    }
}

@Composable
private fun RtButton(text: String, solid: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null, onClick: () -> Unit) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        color = if (solid) p.primary else p.elevated, border = if (solid) null else BorderStroke(1.dp, p.outline),
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            val ink = (if (solid) p.onPrimary else p.text).copy(alpha = if (enabled) 1f else .45f)
            icon?.let { Icon(it, null, Modifier.size(16.dp), tint = ink); Spacer(Modifier.width(6.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, color = ink, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
