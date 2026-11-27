package com.syed.slate.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syed.slate.ui.theme.LocalPalette

/** One choice of a [SlateDropdown]: an optional count badge and colour dot, as on the web's category dropdowns. */
class DropOption<K>(val key: K, val label: String, val count: Int? = null, val color: Color? = null)

/*
 * The app's one dropdown look (the user picked it, 2026-11-27): a rounded field with an accent border and an accent
 * chevron ([SlateDropTrigger]), opening a rounded themed list ([SlateMenu]) whose rows ([SlateMenuItem]) are padded
 * pills — the current one tinted in the accent, bold, with a tick. Every picker uses these; never a bare Material
 * DropdownMenu/ExposedDropdownMenu. The web's ExamSelect and the Focus/Data in-page dropdown (assets/web/focus.js) match.
 */
val DropShape = RoundedCornerShape(14.dp)

/** The closed field: current text (or a dimmed placeholder), accent border, accent chevron. */
@Composable
fun SlateDropTrigger(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, placeholder: Boolean = false, enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, minHeight: androidx.compose.ui.unit.Dp = 48.dp,
    borderColor: Color? = null,
) {
    val p = LocalPalette.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier.fillMaxWidth().heightIn(min = minHeight).clip(DropShape).background(p.surface)
            .border(BorderStroke(1.5.dp, borderColor ?: if (enabled) accent.copy(alpha = .85f) else p.outline), DropShape)
            .clickable(enabled = enabled, onClick = onClick).padding(start = 14.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { it(); Spacer(Modifier.width(10.dp)) }
        Text(
            text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (placeholder) FontWeight.Normal else FontWeight.SemiBold, color = if (placeholder) p.text3 else p.text,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        trailing?.let { it(); Spacer(Modifier.width(6.dp)) }
        Icon(Icons.Filled.KeyboardArrowDown, "Choose", Modifier.size(22.dp), tint = if (enabled) accent else p.text3)
    }
}

/** The open list: a rounded, bordered, shadowed card in the theme's surface; scrolls past [maxHeight]. */
@Composable
fun SlateMenu(
    expanded: Boolean, onDismiss: () -> Unit, modifier: Modifier = Modifier, minWidthPx: Int = 0,
    maxHeight: androidx.compose.ui.unit.Dp = 420.dp, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismiss,
        modifier = modifier.widthIn(min = with(LocalDensity.current) { minWidthPx.toDp() }, max = 420.dp).heightIn(max = maxHeight).padding(horizontal = 6.dp),
        offset = androidx.compose.ui.unit.DpOffset(0.dp, 6.dp),
        shape = RoundedCornerShape(18.dp), containerColor = p.surface, tonalElevation = 0.dp, shadowElevation = 12.dp,
        border = BorderStroke(1.dp, p.outline), content = content,
    )
}

/** One row of a [SlateMenu]. */
@Composable
fun SlateMenuItem(
    label: String, selected: Boolean, onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = .13f) else Color.Transparent)
            .clickable(onClick = onClick).heightIn(min = 46.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { it(); Spacer(Modifier.width(10.dp)) }
        Text(
            label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) accent else p.text,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        trailing?.let { Spacer(Modifier.width(10.dp)); it() }
        Box(Modifier.padding(start = 8.dp).size(20.dp), contentAlignment = Alignment.Center) {
            if (selected) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = accent)
        }
    }
}

/** A small caption row inside a [SlateMenu] (e.g. "SET A"). */
@Composable
fun SlateMenuHeader(text: String) = Text(
    text, Modifier.padding(start = 12.dp, top = 10.dp, bottom = 4.dp),
    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = LocalPalette.current.text3,
)

/**
 * The standard single-choice picker (the web's topic / filter dropdown): a full-width field naming the current choice
 * with its count, opening a menu of every choice. Use it instead of a horizontally scrolling chip row.
 */
@Composable
fun <K> SlateDropdown(options: List<DropOption<K>>, selected: K, onSelect: (K) -> Unit, modifier: Modifier = Modifier, prefix: String? = null) {
    var open by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf(0) }
    val current = options.firstOrNull { it.key == selected } ?: options.firstOrNull() ?: return
    Box(modifier.fillMaxWidth().onSizeChanged { width = it.width }) {
        SlateDropTrigger(
            (prefix?.let { "$it " }.orEmpty()) + current.label, { open = true },
            leading = current.color?.let { c -> { Box(Modifier.size(10.dp).clip(CircleShape).background(c)) } },
            trailing = current.count?.let { n -> { CountBadge(n, current.color ?: MaterialTheme.colorScheme.primary) } },
        )
        SlateMenu(open, { open = false }, minWidthPx = width) {
            options.forEach { o ->
                SlateMenuItem(
                    o.label, o.key == selected, { open = false; onSelect(o.key) },
                    leading = o.color?.let { c -> { Box(Modifier.size(9.dp).clip(CircleShape).background(c)) } },
                    trailing = o.count?.let { n -> { CountBadge(n, o.color ?: MaterialTheme.colorScheme.primary) } },
                )
            }
        }
    }
}

@Composable
private fun CountBadge(n: Int, color: Color) {
    Text(
        "$n", Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = .14f)).padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold,
    )
}
