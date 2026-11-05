package com.syed.slate.ui.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

/**
 * When the card at the top of the screen leaves the list (nailed out of a সব/Important list, deleted, un-marked),
 * land on the card that followed it, at its top.
 *
 * Without this the list keeps the gone card's index AND its pixel offset, so the reader landed part-way down the next
 * card, or past it on N+2, and had to scroll back. [keys] are the lazy keys of the rows that can disappear. A change
 * that also drops the following card (a new page, another filter) is not a removal and is left alone.
 */
@Composable
fun KeepPlaceOnRemoval(list: LazyListState, keys: List<Any>) {
    val current by rememberUpdatedState(keys.toHashSet())
    // [top key, its index, next key]; plain fields, written by the observer and read before the next layout.
    val anchor = remember { arrayOfNulls<Any>(3) }
    LaunchedEffect(list) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.let { v -> Triple(v.getOrNull(0)?.key, v.getOrNull(0)?.index, v.getOrNull(1)?.key) } }
            .collect { (top, index, next) ->
                if (top != null && top in current) { anchor[0] = top; anchor[1] = index; anchor[2] = next } else anchor.fill(null)
            }
    }
    val now = current
    SideEffect {
        val top = anchor[0] ?: return@SideEffect
        val index = anchor[1] as? Int ?: return@SideEffect
        if (top in now || anchor[2] !in now) return@SideEffect
        // The next card now sits at the gone card's index; requestScrollToItem applies on the coming measure, so the
        // jump never paints.
        list.requestScrollToItem(index, 0)
        anchor.fill(null)
    }
}
