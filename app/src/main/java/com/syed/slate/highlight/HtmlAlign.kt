package com.syed.slate.highlight

import com.syed.slate.ui.rich.Block
import com.syed.slate.ui.rich.HtmlParser

/**
 * HTML blocks (General's questions and explanations) are saved against the browser's `textContent`: every text node,
 * entities decoded, tags gone, whitespace untouched. Slate draws them as separate collapsed-whitespace paragraphs, so
 * this lines the two up: for each paragraph, which character of the raw text each displayed character is.
 */
object HtmlAlign {
    private val tag = Regex("<[^>]*>")
    private fun isWs(c: Char) = c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\u000C'

    /**
     * What the web calls the block's text: tags stripped, entities decoded, nothing collapsed. A browser's HTML parser
     * turns every `\r\n` (and lone `\r`) into `\n` before textContent exists, so this does too, or each one would push
     * the offsets after it one character away from the web's.
     */
    fun rawText(html: String): String = HtmlParser.decodeEntities(tag.replace(html.replace("\r\n", "\n").replace('\r', '\n'), ""))

    /** One entry per paragraph: the displayed character → raw offset map. Synthetic characters (a `<br>` newline, a list bullet) map to where they sit. */
    fun align(raw: String, paragraphs: List<Block.Paragraph>): List<IntArray> {
        var pos = 0
        return paragraphs.map { p ->
            while (pos < raw.length && isWs(raw[pos])) pos++ // whitespace between paragraphs
            val text = p.text
            IntArray(text.length) { i ->
                val d = text[i]
                when {
                    d == ' ' -> { val at = pos; while (pos < raw.length && isWs(raw[pos])) pos++; at.coerceAtMost(raw.length) }
                    pos < raw.length && raw[pos] == d -> pos++
                    else -> {
                        // Raw whitespace the display dropped (the space before a `<br>` is trimmed, the `<br>` shows as
                        // a newline) must not block the match: skip it and compare again. Without this, everything after
                        // the first such `<br>` mapped onto one spot and no highlight there could be drawn.
                        var q = pos
                        while (q < raw.length && isWs(raw[q])) q++
                        if (q < raw.length && raw[q] == d) { pos = q + 1; q }
                        else pos // synthetic: shown but not in the raw text
                    }
                }
            }
        }
    }
}
