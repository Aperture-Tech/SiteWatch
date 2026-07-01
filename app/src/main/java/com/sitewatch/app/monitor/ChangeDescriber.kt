package com.sitewatch.app.monitor

import com.sitewatch.app.data.local.MonitorType

/**
 * Produces a short, human-readable description of what changed between two
 * snapshots, tailored to the [MonitorType]. Kept compact enough to sit in a
 * notification and the history list.
 */
object ChangeDescriber {

    private const val MAX_LENGTH = 240
    private const val SNIPPET_LENGTH = 60
    private const val MAX_EXAMPLES = 2

    fun describe(type: MonitorType, old: String, new: String): String {
        val text = when (type) {
            MonitorType.VISUAL -> "The page's appearance changed."

            MonitorType.TEXT -> when {
                new == "present" -> "The watched text appeared."
                new == "absent" -> "The watched text disappeared."
                else -> "The watched text changed."
            }

            MonitorType.CSS_SELECTOR -> {
                val from = snippet(old.ifBlank { "(empty)" })
                val to = snippet(new.ifBlank { "(empty)" })
                "Changed from \"$from\" to \"$to\""
            }

            MonitorType.FULL_PAGE -> lineDiff(old, new)
        }
        return text.take(MAX_LENGTH)
    }

    private fun lineDiff(old: String, new: String): String {
        val oldLines = old.lineSet()
        val newLines = new.lineSet()
        val added = (newLines - oldLines).toList()
        val removed = (oldLines - newLines).toList()

        if (added.isEmpty() && removed.isEmpty()) return "The page content changed."

        return buildString {
            if (added.isNotEmpty()) append(summary("Added", added))
            if (removed.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(summary("Removed", removed))
            }
        }
    }

    private fun summary(label: String, items: List<String>): String {
        val shown = items.take(MAX_EXAMPLES).joinToString(" / ") { snippet(it) }
        val extra = items.size - MAX_EXAMPLES
        return if (extra > 0) "$label: $shown (+$extra more)" else "$label: $shown"
    }

    private fun String.lineSet(): Set<String> =
        lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun snippet(value: String): String =
        if (value.length > SNIPPET_LENGTH) value.take(SNIPPET_LENGTH - 1) + "…" else value
}
