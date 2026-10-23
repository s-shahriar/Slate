package com.syed.slate.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

/**
 * The standard single-choice picker (the web's topic / filter dropdown): a full-width field naming the current choice
 * with its count, opening a menu of every choice. Use it instead of a horizontally scrolling chip row.
 */
@Composable
fun <K> SlateDropdown(options: List<DropOption<K>>, selected: K, onSelect: (K) -> Unit, modifier: Modifier = Modifier, prefix: String? = null) {
    val p = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    var width by remember { mutableStateOf(0) }
    val current = options.firstOrNull { it.key == selected } ?: options.firstOrNull() ?: return
    Box(modifier.fillMaxWidth().onSizeChanged { width = it.width }) {
        Surface(
            onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(14.dp), color = p.surface, border = BorderStroke(1.dp, p.outline),
        ) {
            Row(Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                current.color?.let { Box(Modifier.size(10.dp).clip(CircleShape).background(it)); Spacer(Modifier.width(10.dp)) }
                prefix?.let { Text("$it ", style = MaterialTheme.typography.bodyMedium, color = p.text3, maxLines = 1) }
                Text(
                    current.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                current.count?.let { CountBadge(it, current.color ?: MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.ExpandMore, "Choose", tint = p.text3)
            }
        }
        DropdownMenu(
            expanded = open, onDismissRequest = { open = false },
            modifier = Modifier.widthIn(min = with(LocalDensity.current) { width.toDp() }).background(p.surface),
        ) {
            options.forEach { o ->
                val on = o.key == selected
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            o.color?.let { Box(Modifier.size(9.dp).clip(CircleShape).background(it)); Spacer(Modifier.width(10.dp)) }
                            Text(o.label, Modifier.weight(1f), fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            o.count?.let { Spacer(Modifier.width(10.dp)); CountBadge(it, o.color ?: MaterialTheme.colorScheme.primary) }
                        }
                    },
                    trailingIcon = { if (on) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) else Spacer(Modifier.size(24.dp)) },
                    onClick = { open = false; onSelect(o.key) },
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
