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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.syed.slate.ui.component.LocalLeftHand
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
import com.syed.slate.routine.RoutineOption
import com.syed.slate.routine.RoutineRepository
import com.syed.slate.ui.component.DropOption
import com.syed.slate.ui.component.SlateDropdown
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

/** The open sheet: a new subject for a slot, or an existing one being edited. */
private sealed interface Sheet { data class Add(val slot: Slot) : Sheet; data class Edit(val id: String) : Sheet }

/** The slot a new subject lands in: the time of day for today, Morning otherwise (the web's slotNow). */
private fun slotNow(): Slot = when (java.time.LocalTime.now().hour) {
    in 5..11 -> Slot.MORNING; in 12..16 -> Slot.AFTERNOON; in 17..20 -> Slot.EVENING; else -> Slot.LATE_NIGHT
}

/**
 * Study routine, the web's /routine: a date-wise plan in four slots; each subject has its own tick and its own
 * tickable tasks (Book, MCQ…). The list only ticks; tapping a subject's name opens the edit sheet, the pinned Add
 * button the add sheet. Edits and deletes ask once more at the foot of the sheet.
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
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var picker by rememberSaveable { mutableStateOf<String?>(null) } // null | "jump" | "copy"
    fun go(d: LocalDate) { dayText = d.toString(); sheet = null }

    val byDay = remember(all) { all.values.groupBy { it.day } }
    val subjects = byDay[dayText].orEmpty()
    val prog = progressOf(subjects)
    val options by repo.options.collectAsState()
    val topics = remember(options) { options.filter { it.kind == RoutineOption.TOPIC } }
    val tasks = remember(options) { options.filter { it.kind == RoutineOption.TASK } }
    val suggestions = remember(all, tasks) {
        val known = tasks.map { it.label }.ifEmpty { RoutineOps.PRESET_ITEMS }
        known + RoutineOps.customLabels(all.values, known)
    }
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
        // Pinned bottom corner, within thumb reach; follows the hand toggle.
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { sheet = Sheet.Add(if (day == today) slotNow() else Slot.MORNING) },
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add", fontWeight = FontWeight.ExtraBold) },
                containerColor = p.primary, contentColor = p.onPrimary, shape = RoundedCornerShape(18.dp),
            )
        },
        floatingActionButtonPosition = if (LocalLeftHand.current) FabPosition.Start else FabPosition.End,
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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

            if (prog.subjects == 0) item(key = "blank") {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nothing planned for ${day.short()}.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tap Add to plan a subject" + if (lastPlanned != null) ", or start from an earlier day." else ".",
                        style = MaterialTheme.typography.bodySmall, color = p.text3,
                    )
                    if (lastPlanned != null) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RtButton("Copy ${LocalDate.parse(lastPlanned).short()}", solid = true, icon = Icons.Filled.ContentCopy) { copyFrom(lastPlanned) }
                        RtButton("Other day…") { picker = "copy" }
                    }
                }
            } else {
                item(key = "summary") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${prog.done}/${prog.subjects} subjects" + if (prog.items > 0) "   ·   ${prog.itemsDone}/${prog.items} tasks" else "",
                            style = MaterialTheme.typography.bodyMedium, color = p.text2, fontWeight = FontWeight.SemiBold,
                        )
                        LinearProgressIndicator(
                            progress = { if (prog.items > 0) prog.itemsDone / prog.items.toFloat() else prog.done / prog.subjects.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)),
                            color = p.ok, trackColor = p.elevated, drawStopIndicator = {},
                        )
                    }
                }
                var number = 0
                Slot.entries.forEach { slot ->
                    val list = subjects.filter { it.slot == slot }.sortedWith(RoutineOps.order)
                    if (list.isEmpty()) return@forEach
                    val first = number + 1
                    number += list.size
                    item(key = "slot-${slot.id}") {
                        Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(slotIcon(slot), null, Modifier.size(17.dp), tint = slotColor(slot, dark))
                                Text(slot.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = p.text2, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.weight(1f))
                                Text("${list.count { it.done }}/${list.size}", style = MaterialTheme.typography.labelMedium, color = p.text3)
                            }
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.surface).border(1.dp, p.outline, RoundedCornerShape(16.dp))) {
                                list.forEachIndexed { i, s ->
                                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(p.outline))
                                    SubjectRow(first + i, s, repo::toggle, repo::toggleItem) { sheet = Sheet.Edit(s.id) }
                                }
                            }
                        }
                    }
                }
                item(key = "copy") {
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).clickable { picker = "copy" }.padding(horizontal = 6.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(15.dp), tint = p.text3)
                        Spacer(Modifier.width(8.dp))
                        Text("Copy another day's plan into this day", style = MaterialTheme.typography.labelLarge, color = p.text3)
                    }
                }
            }
        }
    }

    when (val s = sheet) {
        null -> Unit
        is Sheet.Add -> SubjectSheet(
            dayLabel = day.short(), initial = RoutineSubject("", dayText, s.slot, "", false, emptyList(), 0.0, ""), suggestions = suggestions,
            topics = topics, tasks = tasks, repo = repo,
            onSave = { title, sl, items -> repo.add(dayText, sl, title, items.map { it.label }); sheet = null },
            onClose = { sheet = null },
        )
        is Sheet.Edit -> {
            val subject = all[s.id]
            // Deleted elsewhere (another device synced in) while the sheet was open.
            if (subject == null) LaunchedEffect(s.id) { sheet = null }
            else {
                val list = subjects.filter { it.slot == subject.slot }.sortedWith(RoutineOps.order)
                val i = list.indexOfFirst { it.id == subject.id }
                SubjectSheet(
                    dayLabel = day.short(), initial = subject, suggestions = suggestions,
                    topics = topics, tasks = tasks, repo = repo,
                    canUp = i > 0, canDown = i in 0 until list.lastIndex, onMove = { dir -> repo.move(subject.id, dir) },
                    onDelete = { repo.delete(subject.id); sheet = null; Notices.post(Notice("Deleted “${subject.title}”", Notice.Kind.OK)) },
                    onSave = { title, sl, items -> repo.edit(subject.id, title, sl, items); sheet = null },
                    onClose = { sheet = null },
                )
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
private fun CheckBox(on: Boolean, size: Int, p: Palette, modifier: Modifier = Modifier, round: Boolean = false) {
    val shape = if (round) RoundedCornerShape(50) else RoundedCornerShape((size / 3).dp)
    Box(
        modifier.size(size.dp).clip(shape).background(if (on) p.ok else Color.Transparent)
            .border(if (size < 20) 1.6.dp else 2.dp, if (on) p.ok else p.text3.copy(alpha = .7f), shape),
        contentAlignment = Alignment.Center,
    ) { if (on) Icon(Icons.Filled.Check, null, Modifier.size((size * .65f).dp), tint = p.surface) }
}

/** One subject in its slot's list: the round tick, the name (opens the edit sheet), then its tasks as tickable pills. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectRow(n: Int, s: RoutineSubject, onToggle: (String) -> Unit, onToggleItem: (String, String) -> Unit, onOpen: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(50)).clickable { onToggle(s.id) }, contentAlignment = Alignment.Center) {
            CheckBox(s.done, 26, p, round = true)
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = p.text3)) { append("$n. ") }
                    append(s.title)
                },
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onOpen).padding(top = 12.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = if (s.done) p.text3 else p.text, textDecoration = if (s.done) TextDecoration.LineThrough else null,
            )
            if (s.items.isNotEmpty()) FlowRow(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                s.items.forEach { it ->
                    Row(
                        Modifier.heightIn(min = 34.dp).clip(RoundedCornerShape(50))
                            .background(if (it.done) p.ok.copy(alpha = .14f) else p.elevated)
                            .clickable { onToggleItem(s.id, it.id) }.padding(start = 8.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckBox(it.done, 16, p, round = true)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            it.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                            color = if (it.done) p.text3 else p.text2, textDecoration = if (it.done) TextDecoration.LineThrough else null,
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemPicker(suggestions: List<String>, modifier: Modifier = Modifier, onTyped: (String) -> Unit, onAdd: (String) -> Unit) {
    val p = LocalPalette.current
    var text by rememberSaveable { mutableStateOf("") }
    fun submit() { if (text.isNotBlank()) { onTyped(text); text = "" } }
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

/** Add / edit as a bottom sheet. An edit (when something changed) and a delete each ask once more in a quiet strip at the foot. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SubjectSheet(
    dayLabel: String, initial: RoutineSubject, suggestions: List<String>,
    topics: List<RoutineOption>, tasks: List<RoutineOption>, repo: RoutineRepository,
    onSave: (String, Slot, List<RoutineItem>) -> Unit, onClose: () -> Unit,
    onDelete: (() -> Unit)? = null, onMove: (Int) -> Unit = {}, canUp: Boolean = false, canDown: Boolean = false,
) {
    val p = LocalPalette.current
    val isEdit = onDelete != null
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var slot by remember(initial.id) { mutableStateOf(initial.slot) }
    val items = remember(initial.id) { mutableStateListOf<RoutineItem>().apply { addAll(initial.items) } }
    var confirm by remember { mutableStateOf<String?>(null) } // null | "save" | "delete"
    var newTopic by remember { mutableStateOf<String?>(null) } // null | text of a topic being added
    var manage by remember { mutableStateOf<String?>(null) }   // null | RoutineOption.TOPIC | TASK
    val focus = remember { FocusRequester() }
    if (newTopic != null) LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
    val present = items.map { it.label.lowercase() }.toSet()
    fun addLabel(l: String) { val c = RoutineOps.clean(l); if (c.isNotEmpty() && c.lowercase() !in present) { items.add(RoutineItem(com.syed.slate.routine.newId(), c, false)); confirm = null } }
    // The Topic dropdown: the saved topics, the current title when it is not one of them (an older plan), and
    // "+ New topic…" at the foot.
    val known = topics.firstOrNull { it.label.lowercase() == RoutineOps.clean(title).lowercase() }
    val topicValue = known?.label ?: RoutineOps.clean(title)
    val topicOptions = buildList {
        if (topicValue.isEmpty()) add(DropOption("", "Choose a topic…"))
        topics.forEach { add(DropOption(it.label, it.label)) }
        if (topicValue.isNotEmpty() && known == null) add(DropOption(topicValue, "$topicValue (not in the list)"))
        add(DropOption(NEW_TOPIC, "+ New topic…"))
    }
    fun saveNewTopic() { repo.addOption(RoutineOption.TOPIC, newTopic.orEmpty())?.let { title = it.label; newTopic = null; confirm = null } }
    val dirty = RoutineOps.clean(title) != initial.title || slot != initial.slot || items.map { it.label } != initial.items.map { it.label }
    fun save() {
        if (title.isBlank()) return
        when {
            !isEdit || confirm == "save" -> onSave(title, slot, items.toList())
            !dirty -> onClose()
            else -> confirm = "save"
        }
    }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = p.surface) {
        val m = manage
        if (m != null) Column(Modifier.fillMaxWidth().imePadding()) {
            OptionManager(m, if (m == RoutineOption.TOPIC) topics else tasks, repo, Modifier.weight(1f, fill = false))
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.outline))
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
                RtButton("Done", solid = true) { manage = null }
            }
        } else Column(Modifier.fillMaxWidth().imePadding()) {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(if (isEdit) "Edit subject" else "New subject", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(dayLabel, style = MaterialTheme.typography.labelMedium, color = p.text3)
                }
                Text("TOPIC", style = MaterialTheme.typography.labelSmall, color = p.text3, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SlateDropdown(
                        topicOptions, if (newTopic != null) NEW_TOPIC else topicValue,
                        { v -> if (v == NEW_TOPIC) newTopic = "" else { title = v; newTopic = null; confirm = null } },
                        Modifier.weight(1f),
                    )
                    IconButton(onClick = { manage = RoutineOption.TOPIC }) { Icon(Icons.Filled.Tune, "Edit the topic list", tint = p.text2) }
                }
                newTopic?.let { nt ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            nt, { newTopic = it }, Modifier.weight(1f).focusRequester(focus), placeholder = { Text("New topic, e.g. Compiler Design") },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { saveNewTopic() }),
                        )
                        RtButton("Add", solid = true, enabled = nt.isNotBlank()) { saveNewTopic() }
                        IconButton(onClick = { newTopic = null }, Modifier.size(40.dp)) { Icon(Icons.Filled.Close, "Cancel", tint = p.text2) }
                    }
                }
                Slot.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { s ->
                            val on = s == slot
                            Row(
                                Modifier.weight(1f).heightIn(min = 42.dp).clip(RoundedCornerShape(11.dp)).background(if (on) p.primary else p.elevated)
                                    .clickable { slot = s; confirm = null },
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(slotIcon(s), null, Modifier.size(16.dp), tint = if (on) p.onPrimary else p.text2)
                                Spacer(Modifier.width(6.dp))
                                Text(s.label, style = MaterialTheme.typography.labelLarge, color = if (on) p.onPrimary else p.text2, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("TASKS", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = p.text3, fontWeight = FontWeight.ExtraBold)
                    Row(
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { manage = RoutineOption.TASK }.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Tune, null, Modifier.size(14.dp), tint = p.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Edit list", style = MaterialTheme.typography.labelMedium, color = p.primary, fontWeight = FontWeight.Bold)
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (items.isEmpty()) Text("None yet — pick from below.", style = MaterialTheme.typography.bodySmall, color = p.text3)
                    items.toList().forEach { it ->
                        Row(
                            Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(50)).background((if (it.done) p.ok else p.primary).copy(alpha = .16f))
                                .border(1.dp, (if (it.done) p.ok else p.primary).copy(alpha = .38f), RoundedCornerShape(50)).padding(start = 12.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (it.done) { Icon(Icons.Filled.Check, null, Modifier.size(14.dp), tint = p.ok); Spacer(Modifier.width(4.dp)) }
                            Text(it.label, style = MaterialTheme.typography.labelLarge, color = p.text, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { items.remove(it); confirm = null }, Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, "Remove ${it.label}", Modifier.size(15.dp), tint = p.text2)
                            }
                        }
                    }
                }
                ItemPicker(
                    suggestions.filter { it.lowercase() !in present },
                    // A task typed in "Other task…" joins the saved task list too.
                    onTyped = { l -> repo.addOption(RoutineOption.TASK, l)?.let { addLabel(it.label) } },
                ) { addLabel(it) }
                Spacer(Modifier.height(4.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.outline))
            val c = confirm
            Row(
                Modifier.fillMaxWidth()
                    .background(when (c) { "delete" -> p.bad.copy(alpha = .10f); "save" -> p.primary.copy(alpha = .10f); else -> Color.Transparent })
                    .navigationBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (c != null) {
                    Text(
                        if (c == "delete") "Delete “${initial.title}”?" else "Save these changes?", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    RtButton("No", ghost = true) { confirm = null }
                    if (c == "delete") RtButton("Delete", danger = true) { onDelete?.invoke() }
                    else RtButton("Yes, save", solid = true) { save() }
                } else {
                    if (isEdit) {
                        IconButton(onClick = { confirm = "delete" }) { Icon(Icons.Filled.DeleteOutline, "Delete", tint = p.bad) }
                        IconButton(onClick = { onMove(-1) }, enabled = canUp) { Icon(Icons.Filled.ArrowUpward, "Move up") }
                        IconButton(onClick = { onMove(1) }, enabled = canDown) { Icon(Icons.Filled.ArrowDownward, "Move down") }
                    }
                    Spacer(Modifier.weight(1f))
                    RtButton("Cancel", ghost = true) { onClose() }
                    RtButton(if (isEdit) "Save" else "Add", solid = true, enabled = title.isNotBlank()) { save() }
                }
            }
        }
    }
}


private const val NEW_TOPIC = "\u0000new"

/** The saved Topic / Task list, edited inside the sheet: rename (plans that use the old name follow), reorder, delete (plans keep their text), add. */
@Composable
private fun OptionManager(kind: String, list: List<RoutineOption>, repo: RoutineRepository, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    var editing by remember { mutableStateOf<Pair<String, String>?>(null) } // id to text
    var deleting by remember { mutableStateOf<String?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun saveRename() {
        val (id, t) = editing ?: return
        if (t.isBlank()) return
        if (repo.renameOption(id, t)) { editing = null; error = null } else error = "“${t.trim()}” is already in the list."
    }
    fun add() {
        if (text.isBlank()) return
        if (list.any { it.label.lowercase() == RoutineOps.clean(text).lowercase() }) { error = "“${text.trim()}” is already in the list."; return }
        repo.addOption(kind, text); text = ""; error = null
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (kind == RoutineOption.TOPIC) "Topics" else "Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("Renaming also renames it in plans that use it. Deleting only removes it from this list.", style = MaterialTheme.typography.bodySmall, color = p.text3)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, p.outline, RoundedCornerShape(14.dp))) {
            if (list.isEmpty()) Text("Nothing yet — add one below.", Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = p.text3)
            list.forEachIndexed { i, o ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(p.outline))
                val e = editing
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).background(if (deleting == o.id) p.bad.copy(alpha = .10f) else Color.Transparent)
                        .padding(start = 6.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        e?.first == o.id -> {
                            OutlinedTextField(
                                e.second, { editing = o.id to it; error = null }, Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { saveRename() }),
                            )
                            IconButton(onClick = { saveRename() }) { Icon(Icons.Filled.Check, "Save name", tint = p.primary) }
                            IconButton(onClick = { editing = null; error = null }) { Icon(Icons.Filled.Close, "Cancel", tint = p.text2) }
                        }
                        deleting == o.id -> {
                            Text("Delete “${o.label}”?", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            RtButton("No", ghost = true) { deleting = null }
                            Spacer(Modifier.width(4.dp))
                            RtButton("Delete", danger = true) { repo.deleteOption(o.id); deleting = null }
                        }
                        else -> {
                            Text(
                                o.label, Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable { editing = o.id to o.label; deleting = null; error = null }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(onClick = { editing = o.id to o.label; deleting = null; error = null }, Modifier.size(40.dp)) { Icon(Icons.Filled.Edit, "Rename ${o.label}", Modifier.size(18.dp), tint = p.text2) }
                            IconButton(onClick = { repo.moveOption(o.id, -1) }, Modifier.size(40.dp), enabled = i > 0) { Icon(Icons.Filled.ArrowUpward, "Move up", Modifier.size(18.dp)) }
                            IconButton(onClick = { repo.moveOption(o.id, 1) }, Modifier.size(40.dp), enabled = i < list.lastIndex) { Icon(Icons.Filled.ArrowDownward, "Move down", Modifier.size(18.dp)) }
                            IconButton(onClick = { deleting = o.id; editing = null }, Modifier.size(40.dp)) { Icon(Icons.Filled.DeleteOutline, "Delete ${o.label}", Modifier.size(19.dp), tint = p.bad) }
                        }
                    }
                }
            }
        }
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.bad, fontWeight = FontWeight.SemiBold) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                text, { text = it; error = null }, Modifier.weight(1f), placeholder = { Text(if (kind == RoutineOption.TOPIC) "New topic…" else "New task…") },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
            )
            RtButton("Add", enabled = text.isNotBlank()) { add() }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun RtButton(
    text: String, solid: Boolean = false, enabled: Boolean = true, icon: ImageVector? = null,
    ghost: Boolean = false, danger: Boolean = false, onClick: () -> Unit,
) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        color = when { danger -> p.bad; solid -> p.primary; ghost -> Color.Transparent; else -> p.elevated },
        border = if (solid || ghost || danger) null else BorderStroke(1.dp, p.outline),
        modifier = Modifier.heightIn(min = 40.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            val ink = (when { danger -> p.surface; solid -> p.onPrimary; ghost -> p.text2; else -> p.text }).copy(alpha = if (enabled) 1f else .45f)
            icon?.let { Icon(it, null, Modifier.size(16.dp), tint = ink); Spacer(Modifier.width(6.dp)) }
            Text(text, style = MaterialTheme.typography.labelLarge, color = ink, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
