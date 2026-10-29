package com.syed.slate.ui.reader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.syed.slate.content.QBlock
import com.syed.slate.ui.highlight.HText
import com.syed.slate.content.splitQuestionBlocks
import com.syed.slate.ui.rich.CodeTokens
import com.syed.slate.ui.rich.RemoteImage
import com.syed.slate.ui.rich.TokenKind
import com.syed.slate.ui.theme.LocalPalette
import com.syed.slate.ui.theme.Mono
import org.json.JSONArray
import org.json.JSONObject

private fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
private fun JSONArray.strings(): List<String> = List(length()) { optString(it) }

/** Code colours from ict-quiz, light and dark. The palette's own background tells which is in use. */
internal class CodeColors(val bg: androidx.compose.ui.graphics.Color, val text: androidx.compose.ui.graphics.Color, val kw: androidx.compose.ui.graphics.Color,
                         val str: androidx.compose.ui.graphics.Color, val num: androidx.compose.ui.graphics.Color, val comment: androidx.compose.ui.graphics.Color,
                         val fn: androidx.compose.ui.graphics.Color, val type: androidx.compose.ui.graphics.Color)

private fun c(hex: Long) = androidx.compose.ui.graphics.Color(0xFF000000 or hex)
internal val lightCode = CodeColors(c(0xE6EDF1), c(0x132029), c(0x22597A), c(0x2F7A56), c(0xA86E1F), c(0x7B8C98), c(0x6B4FA0), c(0x2E7599))
internal val darkCode = CodeColors(c(0x0D1419), c(0xEEF3F6), c(0x86C1DC), c(0x8FCBA8), c(0xE3B566), c(0x6B7C88), c(0xB9A3E0), c(0x6FB3D2))

/** [code] with its syntax colours, for a [Text] or as the `base` of a highlightable [HText]. */
internal fun codeAnnotated(code: String, lang: String?, colors: CodeColors): androidx.compose.ui.text.AnnotatedString = buildAnnotatedString {
    append(code)
    for (t in CodeTokens.tokenize(code, lang)) {
        val col = when (t.kind) {
            TokenKind.KEYWORD, TokenKind.META -> colors.kw; TokenKind.TYPE -> colors.type; TokenKind.STRING -> colors.str
            TokenKind.NUMBER -> colors.num; TokenKind.COMMENT -> colors.comment; TokenKind.FUNCTION -> colors.fn
        }
        addStyle(SpanStyle(color = col), t.start, t.end)
    }
}

/**
 * A syntax-coloured code listing. With a [uid] and [block] it is highlightable like the web's (`code`, `points.N.code`,
 * `headerCode`): the token colours are the `base` of an [HText], so saved offsets count the code text exactly.
 */
@Composable
fun CodeBlock(code: String, lang: String?, modifier: Modifier = Modifier, uid: String? = null, block: String? = null, hiddenSteps: List<IntRange> = emptyList()) {
    val dark = LocalPalette.current.bg.luminance() < .5f
    val colors = if (dark) darkCode else lightCode
    val shown = code.trimEnd()
    val plain = remember(code, lang, dark) { codeAnnotated(shown, lang ?: "c", colors) }
    // Eye mode: each still-hidden step drawn as a blur (transparent glyphs under a soft shadow), offsets unchanged.
    val text = remember(plain, hiddenSteps) {
        if (hiddenSteps.isEmpty()) plain else buildAnnotatedString {
            append(plain)
            val blur = SpanStyle(color = androidx.compose.ui.graphics.Color.Transparent, shadow = androidx.compose.ui.graphics.Shadow(colors.text.copy(alpha = .8f), blurRadius = 16f))
            for (r in hiddenSteps) if (r.first < shown.length) addStyle(blur, r.first, minOf(r.last + 1, shown.length))
        }
    }
    val style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Mono, fontSize = 12.5.sp, lineHeight = 18.sp)
    Box(modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(colors.bg).horizontalScroll(rememberScrollState()).padding(12.dp)) {
        if (uid != null && block != null) HText(uid, block, shown, base = text, color = colors.text, style = style, softWrap = false, raw = code, markLift = colors.comment to colors.text)
        else Text(text, color = colors.text, style = style, softWrap = false)
    }
}

/**
 * A Written answer's code with its label and the eye mode (as the web's CoverableCode): with [CodeCover] on, every logical
 * step is blurred and "পরের ধাপ" opens them one by one, so the logic can be recalled step by step. The eye in the label
 * row flips the one setting for every code block.
 */
@Composable
fun CoverableCode(label: String, code: String, lang: String?, uid: String?, block: String) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    remember { CodeCover.init(ctx) }
    val steps = remember(code) { CodeCover.steps(code.trimEnd()) }
    val active = CodeCover.on && steps.isNotEmpty()
    // Start hidden again on every switch into eye mode, and for new code.
    var open by remember(code, CodeCover.on) { mutableStateOf(emptySet<Int>()) }
    val next = steps.indices.firstOrNull { it !in open }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BlockLabel(label)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            androidx.compose.material3.IconButton(onClick = { CodeCover.set(!CodeCover.on) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (CodeCover.on) androidx.compose.material.icons.Icons.Filled.Visibility else androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                    if (CodeCover.on) "Show code" else "Cover code step by step",
                    Modifier.size(18.dp), tint = if (CodeCover.on) p.imp else p.text3,
                )
            }
        }
        if (active) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ধাপ ${open.size}/${steps.size}", style = MaterialTheme.typography.labelLarge, color = p.imp, fontFamily = Mono)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            androidx.compose.material3.FilledTonalButton(
                onClick = { next?.let { open = open + it } }, enabled = next != null,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) { Text("পরের ধাপ", style = MaterialTheme.typography.labelLarge) }
            androidx.compose.material3.TextButton(onClick = { open = steps.indices.toSet() }, enabled = next != null) { Text("সব", style = MaterialTheme.typography.labelLarge) }
            androidx.compose.material3.IconButton(onClick = { open = emptySet() }, enabled = open.isNotEmpty(), modifier = Modifier.size(32.dp)) {
                Icon(androidx.compose.material.icons.Icons.Filled.Refresh, "Cover again", Modifier.size(18.dp))
            }
        }
        CodeBlock(code, lang, uid = uid, block = block, hiddenSteps = if (active) steps.filterIndexed { i, _ -> i !in open } else emptyList())
    }
}

/** ASCII diagram: monospace, never wrapped, scrolls sideways. */
@Composable
fun DiagramBlock(text: String, modifier: Modifier = Modifier, uid: String? = null, block: String = "diagram", editable: Boolean = true) {
    val p = LocalPalette.current
    Box(modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(p.elevated).horizontalScroll(rememberScrollState()).padding(12.dp)) {
        // The web highlights the diagram's text as written (no trimming), so the block text is the original string.
        HText(uid, block, text, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Mono, fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = 0.sp), softWrap = false, editable = editable, raw = text)
    }
}

@Composable
private fun BlockLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = LocalPalette.current.text3)
}

/** Bullet dot, as the web's `.written-dot`: a 7px circle over its own soft glow, aligned with the first text line. */
@Composable
private fun Dot() {
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.padding(top = 3.dp).size(16.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(color.copy(alpha = .22f)))
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
    }
}

/**
 * Grid with equal-height cells. Columns share the width in proportion to how much text they hold (within limits), so a
 * short "yes/no" column doesn't take as much room as an explanation. It scrolls sideways only when even the minimum
 * widths don't fit.
 */
@Composable
fun DataTable(headers: List<String>?, rows: List<List<String>>, modifier: Modifier = Modifier, uid: String? = null, prefix: String? = null) {
    val p = LocalPalette.current
    val cols = maxOf(headers?.size ?: 0, rows.maxOfOrNull { it.size } ?: 0).coerceAtLeast(1)
    val weights = remember(headers, rows) {
        List(cols) { j ->
            val lens = rows.map { it.getOrNull(j).orEmpty().length } + (headers?.getOrNull(j)?.length ?: 0)
            lens.average().coerceIn(8.0, 45.0).toFloat()
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).border(1.dp, p.outline, MaterialTheme.shapes.small)) {
        // Each column gets the minimum, then the width that is left is shared by text length, so the table fits the
        // screen exactly when it can; only when the minimums alone are too wide does it scroll.
        val minCol = 96.dp
        val spare = (maxWidth - minCol * cols).coerceAtLeast(0.dp)
        val total = weights.sum()
        val widths: List<Dp> = weights.map { minCol + spare * (it / total) }
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            @Composable fun line(cells: List<String>, header: Boolean, shaded: Boolean, rowIndex: Int = -1) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    for (j in 0 until cols) {
                        Box(
                            Modifier.width(widths[j]).fillMaxHeight().background(if (header) MaterialTheme.colorScheme.primary.copy(alpha = .10f).compositeOver(p.elevated) else if (shaded) p.elevated else p.surface)
                                .border(.5.dp, p.outline).padding(10.dp),
                        ) {
                            if (!header && uid != null && prefix != null) HText(uid, "$prefix.r$rowIndex.c$j", cells.getOrElse(j) { "" }, style = MaterialTheme.typography.bodyMedium)
                            else Text(cells.getOrElse(j) { "" },
                                style = if (header) MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.24.sp) else MaterialTheme.typography.bodyMedium,
                                color = if (header) p.text2 else androidx.compose.ui.graphics.Color.Unspecified)
                        }
                    }
                }
            }
            if (headers != null) line(headers, header = true, shaded = false)
            rows.forEachIndexed { i, r -> line(r, header = false, shaded = i % 2 == 1, rowIndex = i) }
        }
    }
}

/** An image shipped inside the app (`/written-images/x.png`) or fetched from a URL. */
@Composable
private fun AnswerImage(path: String) {
    if (path.startsWith("http")) { RemoteImage(path); return }
    val ctx = LocalContext.current
    val bmp by produceState<android.graphics.Bitmap?>(null, path) {
        value = runCatching { ctx.assets.open(path.trimStart('/')).use { BitmapFactory.decodeStream(it) } }.getOrNull()
    }
    bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small), contentScale = ContentScale.FillWidth) }
}

/**
 * The question: paragraphs, hanging-indent list items, and gaps, as the web renders it. A one-line question is block `q`;
 * otherwise each line is `q.<index>` (the index counts the gaps too, so it matches the web's saved highlights).
 */
@Composable
fun QuestionTextBlocks(text: String, modifier: Modifier = Modifier, uid: String? = null, editable: Boolean = true, compact: Boolean = false, onTap: (() -> Unit)? = null) {
    val small = androidx.compose.ui.text.TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp)
    val blocks = remember(text) { splitQuestionBlocks(text) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        blocks.forEachIndexed { i, b ->
            val key = if (blocks.size <= 1) "q" else "q.$i"
            when (b) {
                is QBlock.Para -> HText(uid, key, b.text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium).let { if (compact) it.merge(small) else it }, editable = editable, onTap = onTap)
                is QBlock.ListItem -> HText(uid, key, b.text, Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyMedium.let { if (compact) it.merge(small) else it }, editable = editable, onTap = onTap)
                QBlock.Gap -> Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/**
 * The web's language switcher (WrittenCardBody `variants`): a program can carry the same solution in more than one
 * language. The answer's own fields are the base language (Java for the Program set), `variants` holds the others, and a
 * variant lists only the fields that differ; the rest come from the base. Tabs put C first, and the choice is shared by
 * every card and kept across launches. A field the variant supplies gets its highlight blocks in its own namespace
 * (`C:code`, `C:points.3`), exactly like the web, so a highlight on the C code never lands on the Java code.
 */
object WrittenLang {
    private const val KEY = "writtenLang"
    private var prefs: android.content.SharedPreferences? = null
    var current by mutableStateOf("C")
        private set
    fun init(ctx: android.content.Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        current = prefs?.getString(KEY, "C") ?: "C"
    }
    fun set(lang: String) { current = lang; prefs?.edit()?.putString(KEY, lang)?.apply() }
}

private val LANG_FIELDS = listOf("code", "codeLang", "image", "summary", "points", "diagram", "table", "mistakes", "mnemonic")

/** The answer, in the order the web shows it: language tabs, code, image, summary, points, diagram, table, mistakes, mnemonic, Java-vs-C table, extended. */
@Composable
fun WrittenBody(a: JSONObject, modifier: Modifier = Modifier, uid: String? = null) {
    val ctx = LocalContext.current
    remember { WrittenLang.init(ctx) }
    val variantsJson = a.optJSONArray("variants")
    val variants = remember(a) { if (variantsJson == null) emptyList() else List(variantsJson.length()) { variantsJson.optJSONObject(it) }.filterNotNull().filter { it.str("lang") != null } }
    if (variants.isEmpty()) { WrittenBodyContent(a, modifier, uid, { it }, a); return }
    val baseLang = a.str("lang") ?: a.str("codeLang")?.let { if (it.equals("c", true)) "C" else it } ?: "Code"
    val langs = (listOf(baseLang) + variants.map { it.getString("lang") }).sortedBy { if (it.equals("c", true)) 0 else 1 }
    val active = langs.firstOrNull { it.equals(WrittenLang.current, true) } ?: langs.first()
    val variant = variants.firstOrNull { it.getString("lang") == active }
    val merged = remember(a, variant) {
        if (variant == null) a else JSONObject(a.toString()).also { m -> LANG_FIELDS.filter { variant.has(it) }.forEach { m.put(it, variant.get(it)) } }
    }
    val bk: (String) -> String = { k -> if (variant != null && variant.has(k.substringBefore('.'))) "${variant.getString("lang")}:$k" else k }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LangTabs(langs, active) { WrittenLang.set(it) }
        WrittenBodyContent(merged, Modifier, uid, bk, a)
    }
}

/** Segmented control, as the web's `.written-lang-tabs`: the active tab filled with the topic colour. */
@Composable
private fun LangTabs(langs: List<String>, active: String, onPick: (String) -> Unit) {
    val p = LocalPalette.current
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier.clip(MaterialTheme.shapes.medium).background(p.elevated).border(1.dp, p.outline, MaterialTheme.shapes.medium).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        langs.forEach { l ->
            val on = l == active
            Box(
                Modifier.widthIn(min = 64.dp).clip(MaterialTheme.shapes.small).background(if (on) primary else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onPick(l) }.padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(l, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = if (on) MaterialTheme.colorScheme.onPrimary else p.text2)
            }
        }
    }
}

@Composable
private fun WrittenBodyContent(a: JSONObject, modifier: Modifier, uid: String?, bk: (String) -> String, base: JSONObject) {
    val p = LocalPalette.current
    val primary = MaterialTheme.colorScheme.primary
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        a.str("code")?.let { CoverableCode(a.str("codeLang") ?: "Code", it, a.str("codeLang"), uid, bk("code")) }
        a.str("image")?.let { AnswerImage(it) }

        // সংক্ষেপ — the summary, a string or a list of lines. Web `.written-summary`:
        // topic-coloured hairline border, small tracked label, lines divided by hairlines.
        a.opt("summary")?.let { s ->
            val lines = if (s is JSONArray) s.strings() else listOf(s.toString())
            if (lines.isNotEmpty()) Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(p.primaryContainer.copy(alpha = .55f))
                    .border(1.dp, primary.copy(alpha = .25f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "সংক্ষেপ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.1.sp),
                    color = primary,
                )
                if (s is JSONArray) lines.forEachIndexed { i, l ->
                    if (i > 0) HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
                    HText(uid, bk("summary.$i"), l, style = MaterialTheme.typography.bodyMedium)
                }
                else HText(uid, bk("summary"), lines.first(), style = MaterialTheme.typography.bodyMedium)
            }
        }

        a.optJSONArray("points")?.let { Points(it, uid, bk) }

        a.str("diagram")?.let { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { BlockLabel("Diagram"); DiagramBlock(it, uid = uid, block = bk("diagram")) } }

        a.optJSONObject("table")?.takeIf { (it.optJSONArray("rows")?.length() ?: 0) > 0 }?.let { t ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BlockLabel("তুলনা")
                DataTable(t.optJSONArray("headers")?.strings(), rows(t.getJSONArray("rows")), uid = uid, prefix = bk("table"))
            }
        }

        a.optJSONArray("mistakes")?.takeIf { it.length() > 0 }?.let { m ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BlockLabel("সাধারণ ভুল")
                DataTable(listOf("❌ ভুল ধারণা", "✅ আসল কথা"), rows(m), uid = uid, prefix = bk("mistakes"))
            }
        }

        a.str("mnemonic")?.let {
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(p.elevated).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Psychology, null, Modifier.size(20.dp), tint = primary)
                HText(uid, bk("mnemonic"), it, style = MaterialTheme.typography.bodyMedium)
            }
        }

        base.optJSONObject("langDiff")?.takeIf { (it.optJSONArray("rows")?.length() ?: 0) > 0 }?.let { d ->
            val headers = d.optJSONArray("headers")?.strings() ?: listOf("Java", "C")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BlockLabel(headers.take(2).joinToString(" বনাম "))
                DataTable(headers, rows(d.getJSONArray("rows")), uid = uid, prefix = "langDiff")
            }
        }

        a.optJSONObject("extended")?.let { Extended(it, uid) }
    }
}

private fun rows(a: JSONArray): List<List<String>> = List(a.length()) { i -> a.optJSONArray(i)?.strings() ?: listOf(a.optString(i)) }

/**
 * `points` is mostly bullets, but an item can be a `{sub}` indented bullet, a `{code}` block or a `{diagram}` block.
 * Blocks break the list in two, so a diagram sits right after the part it illustrates.
 */
@Composable
private fun Points(points: JSONArray, uid: String?, bk: (String) -> String = { it }) {
    // Web `.written-points` gap: 9px.
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        for (i in 0 until points.length()) {
            val pt = points.get(i)
            when {
                pt is JSONObject && pt.str("code") != null -> Column {
                    CoverableCode(pt.str("label") ?: pt.str("codeLang") ?: "Code", pt.getString("code"), pt.str("codeLang"), uid, bk("points.$i.code"))
                }
                pt is JSONObject && pt.str("diagram") != null -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BlockLabel(pt.str("label") ?: "Diagram"); DiagramBlock(pt.getString("diagram"), uid = uid, block = bk("points.$i.diagram"))
                }
                // Web `.written-subpoint`: a 2px bar in the topic colour at 40%, 3px out from the edge,
                // text indented 12px past it and slightly dimmed — not a dash.
                pt is JSONObject && pt.str("sub") != null -> Row(
                    Modifier.padding(start = 3.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.width(2.dp).fillMaxHeight().clip(MaterialTheme.shapes.extraSmall).background(MaterialTheme.colorScheme.primary.copy(alpha = .4f)))
                    HText(
                        uid, bk("points.$i"), pt.getString("sub"), Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .88f),
                    )
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Dot(); HText(uid, bk("points.$i"), pt.toString(), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun Extended(e: JSONObject, uid: String?) {
    val p = LocalPalette.current
    val primary = MaterialTheme.colorScheme.primary
    var open by rememberSaveable(e.str("title")) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).border(1.dp, p.outline, MaterialTheme.shapes.medium).clickable { open = !open }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = primary)
            Text(e.str("title") ?: "More", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (open) {
            e.optJSONArray("points")?.strings()?.forEachIndexed { i, t -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Dot(); HText(uid, "ext.points.$i", t, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium) } }
            e.optJSONArray("table")?.takeIf { it.length() > 0 }?.let { DataTable(e.optJSONArray("tableHeaders")?.strings(), rows(it), uid = uid, prefix = "ext.table") }
            e.str("diagram")?.let { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { BlockLabel("Diagram"); DiagramBlock(it, uid = uid, block = "ext.diagram") } }
        }
    }
}
