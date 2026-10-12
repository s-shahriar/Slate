package com.syed.slate.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syed.slate.ModuleServices
import com.syed.slate.backend.ModuleId
import com.syed.slate.content.Item
import com.syed.slate.content.QuizPool
import com.syed.slate.content.correctAnswer
import com.syed.slate.content.explanation
import com.syed.slate.content.optionList
import com.syed.slate.progress.Flag
import com.syed.slate.ui.component.HandMirror
import com.syed.slate.ui.component.LocalLeftHand
import com.syed.slate.ui.component.LocalToggleHand
import com.syed.slate.ui.theme.LocalPalette
import kotlinx.coroutines.delay

/**
 * One question at a time with instant feedback, used by both Quiz and Exam. The list is fixed when the session
 * starts, so marking or un-marking questions part-way through never reshuffles or shortens it.
 *
 * Prev / Next live in a bar pinned to the bottom edge (thumb reach, one-handed; the hand mirror flips it). Prev
 * steps back through the questions already answered as a read-only review: the answer given stays, but the
 * flag bar (Nail / Important / Weak / Note / Delete) and highlights still work. Next lights up once the current
 * question is answered.
 */
@Composable
fun QuizSession(
    services: ModuleServices,
    questions: List<Item>,
    topicName: (Item) -> String?,
    pill: String,
    resultTitle: String,
    resultLabel: String? = null,
    showTopicTag: Boolean = false,
    stoppable: Boolean = false,
    emptyMessage: String = "No questions here yet.",
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    val p = LocalPalette.current
    val module = services.id
    val flags by services.progress.flags.collectAsState()
    var idx by rememberSaveable { mutableIntStateOf(0) }
    // The option picked for each question (row id → letter). An answer never changes once given: going back shows
    // it again read-only. Score and progress are counted from it, so deleting a question drops its point by itself.
    var answers by rememberSaveable { mutableStateOf(emptyMap<String, String>()) }
    var done by rememberSaveable { mutableStateOf(false) }
    var attempt by rememberSaveable { mutableIntStateOf(0) } // bumps on retry so the list is reshuffled
    // A retry reshuffles from a saved seed, so recreating the screen mid-retry gives back the same order.
    val retrySeed = rememberSaveable { kotlin.random.Random.nextLong() }
    val ordered = remember(questions, attempt) { if (attempt == 0) questions else QuizPool.order(questions, retrySeed + attempt) }
    // Questions deleted during this run (row ids). They leave the deck for good, so the total shrinks with them.
    var removed by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val deck = remember(ordered, removed) { if (removed.isEmpty()) ordered else removed.toHashSet().let { r -> ordered.filter { it.id !in r } } }
    val answered = deck.count { it.id in answers }
    val score = deck.count { answers[it.id] == it.correctAnswer }

    Column(Modifier.fillMaxSize()) {
        HandMirror { Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text(pill, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (stoppable && !done && deck.isNotEmpty()) StopButton { done = true }
            LocalToggleHand.current?.let { toggle ->
                IconButton(onClick = toggle) {
                    Icon(
                        Icons.Filled.PanTool,
                        if (LocalLeftHand.current) "Left-hand layout: switch to right" else "Right-hand layout: switch to left",
                        tint = if (LocalLeftHand.current) MaterialTheme.colorScheme.primary else p.text3,
                    )
                }
            }
        } }

        if (deck.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("ফিরে যাও") }
            }
            return@Column
        }
        if (done || idx >= deck.size) {
            // A stopped exam is scored over what was answered; a finished run over the whole deck.
            ScoreRingScreen(score, if (stoppable) answered else deck.size, resultTitle, resultLabel,
                onRetry = { idx = 0; answers = emptyMap(); done = false; attempt++ }, onHome = onHome)
            return@Column
        }

        val q = deck[idx]
        val uid = q.uid
        val selected = answers[q.id]
        val revealed = selected != null
        // The question you'd be answering next: anything before it is a review.
        val live = deck.indexOfFirst { it.id !in answers }.let { if (it < 0) deck.size - 1 else it }
        val reviewing = revealed && idx < live
        val progress = answered / deck.size.toFloat()
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Question ${idx + 1} of ${deck.size}", style = MaterialTheme.typography.labelLarge)
                if (reviewing) {
                    Text("Review", Modifier.padding(start = 8.dp).clip(MaterialTheme.shapes.small).background(p.elevated).padding(horizontal = 8.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelMedium, color = p.text2)
                }
                Box(Modifier.weight(1f))
                Text("${Math.round(progress * 100)}%", style = MaterialTheme.typography.labelLarge, color = p.text3)
            }
            LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth(), trackColor = p.outline)
        }
        // Every question (forward or back) opens at the top: Next is tapped from the end of the explanation, so a
        // long question would otherwise come in part-way down.
        val scroll = rememberScrollState()
        LaunchedEffect(idx) { scroll.scrollTo(0) }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showTopicTag) topicName(q)?.let {
                Text(it, Modifier.clip(MaterialTheme.shapes.small).background(p.primaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium, color = p.onPrimaryContainer)
            }
            QuestionBody(module, q)
            val correct = q.correctAnswer
            q.optionList().forEach { (letter, text) ->
                val state = when {
                    !revealed -> OptState.IDLE
                    letter == correct -> OptState.CORRECT
                    letter == selected -> OptState.WRONG
                    else -> OptState.DIM
                }
                OptionRow(module, letter, text, state, enabled = !revealed) { answers = answers + (q.id to letter) }
            }
            if (revealed) {
                if (uid != null) FlagBar(flags[uid] ?: Flag(), uid, services.progress, labels = true, itemId = q.id, onDeleted = {
                    // Deleting mid-run takes the question out: its point (if any) goes with it, and the next question
                    // slides into this position. Nothing is skipped, the result counts only what is left, and once
                    // nothing unanswered remains the result shows.
                    val rest = deck.filter { it.id != q.id }
                    removed = removed + q.id
                    if (rest.isEmpty()) done = true
                    else {
                        if (idx >= rest.size) idx = rest.size - 1
                        if (rest.all { it.id in answers }) done = true
                    }
                })
                q.explanation?.let { ExplanationBox(module, it, selected == correct, uid = uid) }
            }
        }
        HorizontalDivider(color = p.outline)
        HandMirror {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { if (idx > 0) idx-- }, enabled = idx > 0,
                    modifier = Modifier.width(64.dp).height(50.dp), contentPadding = PaddingValues(0.dp),
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "আগের প্রশ্ন") }
                Button(
                    onClick = { if (idx + 1 >= deck.size) done = true else idx++ }, enabled = revealed,
                    modifier = Modifier.weight(1f).height(50.dp),
                ) {
                    Text(if (idx + 1 >= deck.size) "ফলাফল দেখুন" else "পরবর্তী প্রশ্ন")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

/** Two taps to stop, so a stray touch cannot end an exam: the first arms it for three seconds. */
@Composable
private fun StopButton(onStop: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(3000); armed = false } }
    OutlinedButton(onClick = { if (armed) onStop() else armed = true }, Modifier.padding(end = 8.dp)) {
        Text(if (armed) "Sure?" else "Stop Exam")
    }
}
