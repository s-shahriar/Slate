package com.syed.slate

import com.syed.slate.progress.Flag
import com.syed.slate.progress.FlagRules
import com.syed.slate.progress.PendingQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlagsTest {
    @Test fun weakImpliesImportant() {
        val f = FlagRules.toggleWeak(Flag())
        assertTrue(f.weak && f.important)
    }

    @Test fun unmarkingImportantClearsWeak() {
        val f = FlagRules.toggleImportant(FlagRules.toggleWeak(Flag()))
        assertFalse(f.important); assertFalse(f.weak)
    }

    @Test fun nailingClearsWeakButKeepsImportant() {
        val f = FlagRules.toggleNailed(FlagRules.toggleWeak(Flag()))
        assertTrue(f.nailed && f.important); assertFalse(f.weak)
    }

    @Test fun emptyNoteBecomesNull() {
        assertEquals(null, FlagRules.setNote(Flag(note = "x"), "   ").note)
    }

    @Test fun diffOnlyHasChangedColumns() {
        val old = Flag(important = true)
        val patch = FlagRules.diff(old, FlagRules.toggleNailed(old))
        assertEquals(mapOf<String, Any?>("nailed" to true), patch)
        assertEquals(FlagRules.toggleNailed(old), FlagRules.apply(old, patch))
    }

    @Test fun laterEditsWinPerColumn() {
        val q = PendingQueue()
        q.add("u1", mapOf("nailed" to true))
        q.add("u1", mapOf("nailed" to false, "note" to "hi"))
        assertEquals(mapOf<String, Any?>("nailed" to false, "note" to "hi"), q.patchFor("u1"))
    }

    @Test fun sentEntriesAreDroppedUnlessEditedMeanwhile() {
        val q = PendingQueue()
        q.add("a", mapOf("nailed" to true)); q.add("b", mapOf("important" to true))
        val sent = q.snapshot()
        q.add("b", mapOf("important" to false))
        q.remove(sent)
        assertEquals(null, q.patchFor("a"))
        assertEquals(mapOf<String, Any?>("important" to false), q.patchFor("b"))
    }

    @Test fun batchesGroupRowsByColumnSet() {
        val b = PendingQueue.batches(
            mapOf(
                "a" to mapOf("nailed" to true),
                "b" to mapOf("nailed" to false),
                "c" to mapOf("important" to true, "weak" to true),
            ),
            "user-1",
        )
        assertEquals(2, b.size)
        assertEquals(listOf(2, 1), b.map { it.length() }.sortedDescending())
    }

    @Test fun preferredVersionRoundTrips() {
        val set = FlagRules.diff(Flag(), Flag(preferred = 1))
        assertEquals(mapOf("preferred" to 1), set)
        val applied = FlagRules.apply(Flag(important = true), set)
        assertEquals(1, applied.preferred)
        assertTrue(applied.important)
        // Clearing sends an explicit null and leaves the row blank again.
        val clear = FlagRules.diff(Flag(preferred = 1), Flag())
        assertTrue(clear.containsKey("preferred") && clear["preferred"] == null)
        assertTrue(FlagRules.apply(Flag(preferred = 1), clear).isBlank)
        assertFalse(Flag(preferred = 0).isBlank)
        // The upload row carries the column (a JSON number survives the queue file round trip as Int).
        val row = PendingQueue.batches(mapOf("sql:x" to set), "u")[0].getJSONObject(0)
        assertEquals(1, row.getInt("preferred"))
        val back = PendingQueue.fromJson(PendingQueue.toJson(mapOf("sql:x" to set))).snapshot()["sql:x"]!!
        assertEquals(1, FlagRules.apply(Flag(), back).preferred)
    }
}
