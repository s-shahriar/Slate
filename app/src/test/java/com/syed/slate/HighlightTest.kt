package com.syed.slate

import com.syed.slate.highlight.Anchor
import com.syed.slate.highlight.Highlight
import com.syed.slate.highlight.HtmlAlign
import com.syed.slate.ui.rich.Block
import com.syed.slate.ui.rich.HtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HighlightTest {
    private fun h(id: String, s: Int, e: Int, q: String, c: String = "mint") = Highlight(id, "u", "b", s, e, q, c)

    @Test fun offsetsStillPointingAtTheQuoteAreUsed() {
        assertEquals(4 until 9, Anchor.resolve("the quick brown fox", h("1", 4, 9, "quick")))
    }

    @Test fun shiftedTextIsFoundByQuoteOnlyWhenUnambiguous() {
        assertEquals(12 until 17, Anchor.resolve("a new intro quick brown", h("1", 4, 9, "quick")))
        val t = "some prefix, quick fox"; val at = t.indexOf("quick")
        assertEquals(at until at + 5, Anchor.resolve(t, h("1", 4, 9, "quick")))
        assertNull(Anchor.resolve("quick and quick again", h("1", 40, 45, "quick")))   // appears twice: orphaned
        assertNull(Anchor.resolve("nothing here", h("1", 0, 5, "quick")))               // gone: orphaned, never deleted
    }

    @Test fun overlappingAndTouchingMarksMergeAndKeepTheEarliestColour() {
        val bands = Anchor.bandsFor("0123456789", listOf(h("b", 4, 8, "4567", "rose"), h("a", 2, 5, "234", "amber"), h("c", 9, 10, "9", "mint")))
        assertEquals(2, bands.size)
        assertEquals(listOf("a", "b"), bands[0].ids); assertEquals(2, bands[0].start); assertEquals(8, bands[0].end); assertEquals("amber", bands[0].color)
    }

    @Test fun trimmingDropsEdgeWhitespace() {
        assertEquals(Triple(3, 8, "hello"), Anchor.trimmed("a  hello  b", 1, 10))
        assertNull(Anchor.trimmed("   ", 0, 3))
    }

    @Test fun htmlRawTextIsTheBrowsersTextContent() {
        // A browser's HTML parser turns CRLF into LF before textContent exists (checked in Chrome on a real explanation).
        assertEquals("one\ntwo & three", HtmlAlign.rawText("<p>one</p>\r\n<p>two &amp; three</p>"))
    }

    @Test fun displayedParagraphsAlignToRawOffsets() {
        val html = "<p>Hello   <b>big</b> world</p>\r\n<p>Second one</p>"
        val raw = HtmlAlign.rawText(html)
        val paras = HtmlParser.parse(html).filterIsInstance<Block.Paragraph>()
        val map = HtmlAlign.align(raw, paras)
        // "Hello big world": 'b' of "big" is raw index 9 ("Hello   " is 8 chars, then "big")
        assertEquals("Hello big world", paras[0].text)
        assertEquals('b', raw[map[0][paras[0].text.indexOf("big")]])
        assertEquals('S', raw[map[1][0]])
        assertEquals(raw.indexOf("Second"), map[1][0])
    }

    // A real LiveMCQ explanation (q2cda94wmy2c): CRLF between paragraphs, "text <br>" line breaks, <sub>. The web saved
    // this mark at 227..250; Slate could find it but drew nothing, because the aligner stalled at the first "<br>".
    @Test fun markAfterALineBreakIsDrawnOnTheRightText() {
        val html = "<p>Question: If a line with slope 3 passes through (x, 5) and (- 1, 11), what could be the value of x?</p>\r\n" +
            "<p>Solution: <br>দেওয়া আছে, <br>রেখার ঢাল (m) = 3 <br>প্রথম বিন্দু = (x, 5) <br>দ্বিতীয় বিন্দু = (- 1, 11)</p>\r\n" +
            "<p>আমরা জানি, <br>দুটি বিন্দুগামী রেখার ঢাল m = (y<sub>2</sub> - y<sub>1</sub>)/(x<sub>2</sub> - x<sub>1</sub>)</p>"
        val raw = HtmlAlign.rawText(html)
        val mark = Highlight("a", "u", "explanation", 227, 250, "m = (y2 - y1)/(x2 - x1)", "mint")
        assertEquals(227 until 250, Anchor.resolve(raw, mark))   // the browser's offsets, no quote search needed
        val paras = HtmlParser.parse(html).filterIsInstance<Block.Paragraph>()
        val map = HtmlAlign.align(raw, paras)[2]
        val band = Anchor.bandsFor(raw, listOf(mark)).single()
        val s = map.indexOfFirst { it >= band.start && it < band.end }
        val e = map.indexOfLast { it >= band.start && it < band.end } + 1
        assertEquals("m = (y2 - y1)/(x2 - x1)", paras[2].text.substring(s, e))
        // Every paragraph still starts on its own first character.
        paras.forEachIndexed { i, p -> assertEquals(p.text.first(), raw[HtmlAlign.align(raw, paras)[i][0]]) }
    }
}
