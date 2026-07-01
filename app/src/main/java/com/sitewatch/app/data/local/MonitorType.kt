package com.sitewatch.app.data.local

/**
 * The strategy used to detect a change on a watched site.
 *
 * Phase 1 implements [FULL_PAGE]. The remaining types are defined now so the
 * data model and UI are stable across build phases:
 *  - [TEXT] / [CSS_SELECTOR] are handled by Jsoup in Phase 2.
 *  - [VISUAL] is handled by a WebView screenshot hash in Phase 3.
 */
enum class MonitorType(val label: String, val description: String) {
    FULL_PAGE(
        label = "Full page",
        description = "Notify on any change to the page's content."
    ),
    TEXT(
        label = "Specific text",
        description = "Notify when a piece of text appears or disappears."
    ),
    CSS_SELECTOR(
        label = "CSS selector",
        description = "Notify when the content matched by a selector changes."
    ),
    VISUAL(
        label = "Visual",
        description = "Notify when the rendered page looks different."
    );

    companion object {
        /** Types wired up in the current build. */
        val implemented: List<MonitorType> = listOf(FULL_PAGE, TEXT, CSS_SELECTOR, VISUAL)
    }
}
