package com.syed.slate.ui.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Code eye mode, as ict-quiz's lib/codeCover.js (keep the two rules in step): a program is covered one logical step at a
 * time — a run of non-blank lines — so its logic can be recalled before peeking. Boilerplate stays visible as the frame
 * (#include / import, class and main headers, a lone brace, `return 0;`). One remembered setting for every code block.
 */
object CodeCover {
    private const val KEY = "ict-code-cover"
    private var prefs: android.content.SharedPreferences? = null
    var on by mutableStateOf(false)
        private set
    fun init(ctx: android.content.Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        on = prefs?.getBoolean(KEY, false) ?: false
    }
    fun set(v: Boolean) { on = v; prefs?.edit()?.putBoolean(KEY, v)?.apply() }

    private val SCAFFOLD = listOf(
        Regex("""^\s*$"""),
        Regex("""^\s*#\s*include\b.*"""),
        Regex("""^\s*import\s+[\w.*]+\s*;\s*$"""),
        Regex("""^\s*(public\s+|final\s+|abstract\s+)*class\s+\w+[^{]*\{?\s*$"""),
        Regex("""^\s*(public\s+)?static\s+void\s+main\s*\([^)]*\)\s*(throws\s+[\w.,\s]+)?\{?\s*$"""),
        Regex("""^\s*int\s+main\s*\([^)]*\)\s*\{?\s*$"""),
        Regex("""^\s*[{}]\s*;?\s*$"""),
        Regex("""^\s*return\s+0\s*;\s*$"""),
    )
    private fun scaffold(line: String) = SCAFFOLD.any { it.matches(line) }

    /** The covered steps of [code], in order, as [start, end) ranges: first logic character to the end of the last logic line. */
    fun steps(code: String): List<IntRange> {
        val lines = code.split('\n')
        val starts = IntArray(lines.size)
        var at = 0
        lines.forEachIndexed { i, l -> starts[i] = at; at += l.length + 1 }
        val out = mutableListOf<IntRange>()
        var i = 0
        while (i < lines.size) {
            if (lines[i].isBlank()) { i++; continue }
            var j = i
            while (j + 1 < lines.size && lines[j + 1].isNotBlank()) j++
            var a = i; var b = j
            while (a <= b && scaffold(lines[a])) a++
            while (b >= a && scaffold(lines[b])) b--
            if (a <= b) {
                val first = lines[a]
                out += (starts[a] + (first.length - first.trimStart().length)) until (starts[b] + lines[b].length)
            }
            i = j + 1
        }
        return out
    }
}
