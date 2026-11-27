package com.syed.slate.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.syed.slate.ui.theme.LocalPalette

/** Previous / next page at the foot of a reading page (the web's shared/TopicPager.jsx): (key, label) or null. */
@Composable
fun TopicPager(prev: Pair<String, String>?, next: Pair<String, String>?, onGo: (String) -> Unit, modifier: Modifier = Modifier) {
    if (prev == null && next == null) return
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(prev, false, onGo, Modifier.weight(1f))
        Box(next, true, onGo, Modifier.weight(1f))
    }
}

@Composable
private fun Box(target: Pair<String, String>?, forward: Boolean, onGo: (String) -> Unit, modifier: Modifier) {
    if (target == null) { Spacer(modifier); return }
    val p = LocalPalette.current
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.heightIn(min = 64.dp).clip(shape).background(p.surface).border(BorderStroke(1.5.dp, accent.copy(alpha = .45f)), shape)
            .clickable { onGo(target.first) }.padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (forward) Arrangement.End else Arrangement.Start,
    ) {
        if (!forward) Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, Modifier.size(22.dp), tint = accent)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp), horizontalAlignment = if (forward) Alignment.End else Alignment.Start) {
            Text(if (forward) "পরের" else "আগের", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
            Text(
                target.second, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = p.text,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = if (forward) TextAlign.End else TextAlign.Start,
            )
        }
        if (forward) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(22.dp), tint = accent)
    }
}
