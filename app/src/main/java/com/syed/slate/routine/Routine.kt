package com.syed.slate.routine

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** The four study slots of a day, in display order; `id` is the `routine_subjects.slot` value. */
enum class Slot(val id: String, val label: String) {
    MORNING("morning", "Morning"), AFTERNOON("afternoon", "Afternoon"), EVENING("evening", "Evening"), LATE_NIGHT("late_night", "Late night");

    companion object { fun of(id: String) = entries.firstOrNull { it.id == id } ?: MORNING }
}

data class RoutineItem(val id: String, val label: String, val done: Boolean)

/**
 * One subject on one day (`routine_subjects` row, shared with the web apps' /routine). The subject's tick and each
 * item's tick are independent. `day` is the local calendar date `YYYY-MM-DD`.
 */
data class RoutineSubject(
    val id: String,
    val day: String,
    val slot: Slot,
    val title: String,
    val done: Boolean,
    val items: List<RoutineItem>,
    val position: Double,
    val updatedAt: String,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("day", day).put("slot", slot.id).put("title", title).put("done", done)
        .put("items", JSONArray().also { a -> items.forEach { a.put(JSONObject().put("id", it.id).put("label", it.label).put("done", it.done)) } })
        .put("position", position).put("updated_at", updatedAt)

    companion object {
        fun fromJson(o: JSONObject): RoutineSubject {
            val a = o.optJSONArray("items") ?: JSONArray()
            val items = (0 until a.length()).map { i ->
                val it = a.getJSONObject(i)
                RoutineItem(it.optString("id").ifEmpty { newId() }, it.optString("label"), it.optBoolean("done"))
            }
            return RoutineSubject(
                o.getString("id"), o.getString("day"), Slot.of(o.optString("slot")), o.optString("title"), o.optBoolean("done"),
                items, o.optDouble("position", 0.0), o.optString("updated_at"),
            )
        }
    }
}

/**
 * One choice of the add/edit sheet's pick lists (`routine_options`, shared with the web): a topic for the Topic
 * dropdown or a task for the task chips. [position] orders each list. A temporary topic carries [expiresAt] (7 days
 * out); past it, it drops off the list and is deleted on the next load.
 */
data class RoutineOption(
    val id: String, val kind: String, val label: String, val position: Double, val updatedAt: String, val expiresAt: String? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("kind", kind).put("label", label).put("position", position).put("updated_at", updatedAt)
        .put("expires_at", expiresAt ?: JSONObject.NULL)

    private fun expiresMs(): Long? = expiresAt?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
    fun expired(now: Long = System.currentTimeMillis()) = expiresMs()?.let { it <= now } ?: false
    /** Whole days left (0 on the last day); null when permanent. */
    fun daysLeft(now: Long = System.currentTimeMillis()): Int? = expiresMs()?.let { maxOf(0L, (it - now) / 86_400_000L).toInt() }

    companion object {
        const val TOPIC = "topic"
        const val TASK = "task"
        const val TEMP_DAYS = 7L
        fun fromJson(o: JSONObject) = RoutineOption(
            o.getString("id"), o.optString("kind"), o.optString("label"), o.optDouble("position", 0.0), o.optString("updated_at"),
            if (o.isNull("expires_at")) null else o.optString("expires_at").ifEmpty { null },
        )
    }
}

fun newId(): String = UUID.randomUUID().toString()

/** Pure edits on the list of subjects; the repository applies them and queues what changed. */
object RoutineOps {
    val PRESET_ITEMS = listOf("Book", "Online written", "MCQ", "Note", "Self note", "Math", "YouTube")

    fun clean(s: String) = s.trim().replace(Regex("\\s+"), " ")

    val order: Comparator<RoutineSubject> = compareBy<RoutineSubject> { it.position }.thenBy { it.id }

    fun nextPosition(all: Collection<RoutineSubject>, day: String, slot: Slot): Double =
        (all.filter { it.day == day && it.slot == slot }.maxOfOrNull { it.position } ?: -1.0) + 1

    fun items(labels: List<String>) = labels.map(::clean).filter { it.isNotEmpty() }.map { RoutineItem(newId(), it, false) }

    /** The two rows to write when [s] swaps with its neighbour above (dir -1) or below (dir 1) in its slot. */
    fun move(all: Collection<RoutineSubject>, s: RoutineSubject, dir: Int): List<RoutineSubject> {
        val list = all.filter { it.day == s.day && it.slot == s.slot }.sortedWith(order)
        val i = list.indexOfFirst { it.id == s.id }
        val o = list.getOrNull(i + dir) ?: return emptyList()
        return listOf(s.copy(position = o.position), o.copy(position = if (s.position == o.position) o.position - dir else s.position))
    }

    /** A day's plan copied to [to], unticked, after what [to] already holds. */
    fun copy(all: Collection<RoutineSubject>, from: String, to: String, stamp: String): List<RoutineSubject> {
        val next = HashMap<Slot, Double>()
        return all.filter { it.day == from }.sortedWith(order).map { s ->
            val pos = next.getOrPut(s.slot) { nextPosition(all, to, s.slot) }
            next[s.slot] = pos + 1
            RoutineSubject(newId(), to, s.slot, s.title, false, s.items.map { RoutineItem(newId(), it.label, false) }, pos, stamp)
        }
    }

    /** Item labels used before that are not in [known] (the task list), most used first. */
    fun customLabels(all: Collection<RoutineSubject>, known: List<String> = PRESET_ITEMS): List<String> {
        val preset = known.map { it.lowercase() }.toSet()
        val count = LinkedHashMap<String, Pair<String, Int>>()
        all.forEach { s -> s.items.forEach { it ->
            val k = it.label.lowercase()
            if (k !in preset) count[k] = (count[k]?.first ?: it.label) to ((count[k]?.second ?: 0) + 1)
        } }
        return count.values.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }).map { it.first }
    }
}
