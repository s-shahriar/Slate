package com.syed.slate

import com.syed.slate.routine.RoutineItem
import com.syed.slate.routine.RoutineOps
import com.syed.slate.routine.RoutineSubject
import com.syed.slate.routine.Slot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTest {
    private fun s(id: String, day: String, slot: Slot, pos: Double, items: List<String> = emptyList(), done: Boolean = false) =
        RoutineSubject(id, day, slot, "T$id", done, items.mapIndexed { i, l -> RoutineItem("$id-$i", l, done) }, pos, "")

    @Test fun jsonRoundTrip() {
        val a = s("a", "2026-10-23", Slot.LATE_NIGHT, 2.0, listOf("Book", "MCQ"), done = true)
        assertEquals(a, RoutineSubject.fromJson(JSONObject(a.toJson().toString())))
    }

    @Test fun webRowParses() {
        val row = JSONObject("""{"id":"x","day":"2026-10-23","slot":"evening","title":"OS","done":false,"items":[{"id":"i","label":"Book","done":true}],"position":0,"updated_at":"2026-10-22T10:00:00Z"}""")
        val r = RoutineSubject.fromJson(row)
        assertEquals(Slot.EVENING, r.slot)
        assertTrue(r.items.single().done)
    }

    @Test fun moveSwapsWithNeighbour() {
        val all = listOf(s("a", "d", Slot.MORNING, 0.0), s("b", "d", Slot.MORNING, 1.0), s("c", "d", Slot.EVENING, 0.0))
        val out = RoutineOps.move(all, all[1], -1).associateBy { it.id }
        assertEquals(0.0, out.getValue("b").position, 0.0)
        assertEquals(1.0, out.getValue("a").position, 0.0)
        assertTrue(RoutineOps.move(all, all[0], -1).isEmpty())
    }

    @Test fun moveSeparatesEqualPositions() {
        val all = listOf(s("a", "d", Slot.MORNING, 0.0), s("b", "d", Slot.MORNING, 0.0))
        val out = RoutineOps.move(all, all[1], -1)
        val sorted = (out + all.filter { o -> out.none { it.id == o.id } }).sortedWith(RoutineOps.order).map { it.id }
        assertEquals(listOf("b", "a"), sorted)
    }

    @Test fun copyIsUntickedAndAppends() {
        val all = listOf(
            s("a", "d1", Slot.MORNING, 0.0, listOf("Book"), done = true), s("b", "d1", Slot.MORNING, 1.0),
            s("c", "d2", Slot.MORNING, 4.0),
        )
        val out = RoutineOps.copy(all, "d1", "d2", "")
        assertEquals(listOf(5.0, 6.0), out.map { it.position })
        assertTrue(out.all { !it.done && it.items.none { i -> i.done } && it.day == "d2" })
        assertTrue(out.none { o -> all.any { it.id == o.id } })
    }

    @Test fun customLabelsSkipPresets() {
        val all = listOf(s("a", "d", Slot.MORNING, 0.0, listOf("Book", "Slides", "slides", "Viva")))
        assertEquals(listOf("Slides", "Viva"), RoutineOps.customLabels(all))
    }
}
