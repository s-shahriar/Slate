package com.syed.slate.routine

import android.content.Context
import com.syed.slate.backend.ModuleId
import com.syed.slate.backend.Postgrest
import com.syed.slate.backend.SupabaseAuth
import com.syed.slate.core.writeAtomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * The study routine of one module (`routine_subjects`, same rows as the web's /routine). Local first: every edit
 * lands in [subjects] and the cache file at once and is queued per subject id (last write wins); the flusher sends
 * upserts as one bulk call and deletes as one `id=in.(…)` call, with the usual 4/12/30/60/300 s backoff. Works
 * signed out too; the queue waits for a session.
 */
class RoutineRepository(
    context: Context,
    module: ModuleId,
    private val auth: SupabaseAuth,
    private val db: Postgrest,
    private val scope: CoroutineScope,
) {
    private class Pending(val delete: Boolean, val row: RoutineSubject?, val at: Long)

    private val cacheFile = File(context.filesDir, "routine_${module.key}.json")
    private val queueFile = File(context.filesDir, "routine_queue_${module.key}.json")
    private val lock = Any()
    private val rows = LinkedHashMap<String, RoutineSubject>()
    private val pending = LinkedHashMap<String, Pending>()

    private val _subjects = MutableStateFlow<Map<String, RoutineSubject>>(emptyMap())
    val subjects: StateFlow<Map<String, RoutineSubject>> = _subjects
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount
    private val _failure = MutableStateFlow<String?>(null)
    /** The last refresh or flush failure, cleared on the next success. */
    val failure: StateFlow<String?> = _failure
    private var job: Job? = null

    init {
        runCatching { JSONArray(cacheFile.readText()).let { a -> for (i in 0 until a.length()) RoutineSubject.fromJson(a.getJSONObject(i)).let { rows[it.id] = it } } }
        runCatching {
            val o = JSONObject(queueFile.readText())
            o.keys().forEach { id ->
                val p = o.getJSONObject(id)
                pending[id] = Pending(p.optBoolean("delete"), p.optJSONObject("row")?.let(RoutineSubject::fromJson), p.optLong("at"))
            }
        }
        publish()
        if (pending.isNotEmpty()) kick()
    }

    private fun publish() {
        _subjects.value = LinkedHashMap(rows)
        _pendingCount.value = pending.size
    }

    private fun persist() {
        cacheFile.writeAtomic(JSONArray().also { a -> rows.values.forEach { a.put(it.toJson()) } }.toString())
        queueFile.writeAtomic(JSONObject().also { o ->
            pending.forEach { (id, p) -> o.put(id, JSONObject().put("delete", p.delete).put("at", p.at).apply { p.row?.let { put("row", it.toJson()) } }) }
        }.toString())
        publish()
    }

    private fun stamp() = Instant.now().toString()

    private fun queue(changed: List<RoutineSubject>, deleted: List<String> = emptyList()) {
        if (changed.isEmpty() && deleted.isEmpty()) return
        synchronized(lock) {
            val now = System.currentTimeMillis()
            changed.forEach { val r = it.copy(updatedAt = stamp()); rows[r.id] = r; pending[r.id] = Pending(false, r, now) }
            deleted.forEach { rows.remove(it); pending[it] = Pending(true, null, now) }
            persist()
        }
        kick()
    }

    private fun get(id: String) = synchronized(lock) { rows[id] }
    private fun all() = synchronized(lock) { rows.values.toList() }

    // ── edits ───────────────────────────────────────────────────────────

    fun add(day: String, slot: Slot, title: String, labels: List<String>) = queue(listOf(
        RoutineSubject(newId(), day, slot, RoutineOps.clean(title), false, RoutineOps.items(labels), RoutineOps.nextPosition(all(), day, slot), ""),
    ))

    /** [items] is the edited list; kept items keep their id and tick. */
    fun edit(id: String, title: String, slot: Slot, items: List<RoutineItem>) {
        val s = get(id) ?: return
        val moved = slot != s.slot
        queue(listOf(s.copy(
            title = RoutineOps.clean(title), slot = slot,
            position = if (moved) RoutineOps.nextPosition(all(), s.day, slot) else s.position,
            items = items.map { it.copy(label = RoutineOps.clean(it.label)) }.filter { it.label.isNotEmpty() },
        )))
    }

    fun toggle(id: String) { get(id)?.let { queue(listOf(it.copy(done = !it.done))) } }

    fun toggleItem(id: String, itemId: String) {
        get(id)?.let { s -> queue(listOf(s.copy(items = s.items.map { if (it.id == itemId) it.copy(done = !it.done) else it }))) }
    }

    fun addItem(id: String, label: String) {
        val l = RoutineOps.clean(label)
        if (l.isNotEmpty()) get(id)?.let { s -> queue(listOf(s.copy(items = s.items + RoutineItem(newId(), l, false)))) }
    }

    fun delete(id: String) { if (get(id) != null) queue(emptyList(), listOf(id)) }

    fun move(id: String, dir: Int) { get(id)?.let { queue(RoutineOps.move(all(), it, dir)) } }

    /** Copies [from]'s plan into [to]; returns how many subjects were copied. */
    fun copyDay(from: String, to: String): Int = RoutineOps.copy(all(), from, to, "").also { queue(it) }.size

    // ── sync ────────────────────────────────────────────────────────────

    /** Pulls the server copy (signed in only); edits still queued stay on top of it. */
    suspend fun refresh() {
        if (auth.session.value == null) return
        runCatching { db.selectAll("routine_subjects", "select=id,day,slot,title,done,items,position,updated_at&order=id") }
            .onSuccess { list ->
                synchronized(lock) {
                    rows.clear()
                    list.forEach { RoutineSubject.fromJson(it).let { r -> rows[r.id] = r } }
                    pending.forEach { (id, p) -> if (p.delete) rows.remove(id) else p.row?.let { rows[id] = it } }
                    persist()
                }
                _failure.value = null
            }
            .onFailure { _failure.value = it.message ?: "Couldn't load the routine" }
        kick()
    }

    fun retryNow() { job?.cancel(); job = null; kick() }

    fun kick() {
        if (job?.isActive == true) return
        job = scope.launch {
            delay(400)
            val backoff = longArrayOf(4_000, 12_000, 30_000, 60_000, 300_000)
            var attempt = 0
            while (true) {
                val batch = synchronized(lock) { LinkedHashMap(pending) }
                if (batch.isEmpty() || auth.session.value == null) return@launch
                val ups = batch.values.mapNotNull { if (it.delete) null else it.row }
                val dels = batch.filterValues { it.delete }.keys
                val failure = runCatching {
                    if (ups.isNotEmpty()) db.upsert("routine_subjects", JSONArray().also { a -> ups.forEach { a.put(it.toJson()) } }, "id")
                    if (dels.isNotEmpty()) db.delete("routine_subjects", "id=in.(${dels.joinToString(",")})")
                }.exceptionOrNull()
                if (failure == null) {
                    _failure.value = null
                    attempt = 0
                    // Only what was sent; an edit made while the call was in flight stays queued.
                    synchronized(lock) { batch.forEach { (id, p) -> if (pending[id] === p) pending.remove(id) }; persist() }
                } else {
                    _failure.value = failure.message ?: "Couldn't reach the server"
                    delay(backoff[minOf(attempt++, backoff.lastIndex)])
                }
            }
        }
    }
}
