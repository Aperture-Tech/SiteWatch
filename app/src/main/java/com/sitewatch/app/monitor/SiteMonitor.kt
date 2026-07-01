package com.sitewatch.app.monitor

import com.sitewatch.app.data.local.MonitorType
import com.sitewatch.app.data.local.WatchedSite
import org.jsoup.Jsoup
import org.jsoup.select.Selector
import javax.inject.Inject
import javax.inject.Singleton

/** The outcome of capturing a site's current state. */
sealed interface SnapshotResult {
    /**
     * A content hash representing the monitored portion of the page, plus the
     * human-readable [content] it was derived from (used to describe changes).
     */
    data class Success(val hash: String, val content: String) : SnapshotResult

    /**
     * The fetch or extraction failed; [message] is user-facing.
     *
     * [retryable] distinguishes transient failures (network/HTTP, worth a
     * backoff retry) from permanent ones (misconfiguration like a bad selector,
     * which should just be recorded until the user fixes it).
     */
    data class Failure(val message: String, val retryable: Boolean = true) : SnapshotResult
}

/**
 * Captures a comparable snapshot (hash) of a site according to its [MonitorType]:
 *  - [MonitorType.FULL_PAGE] — SHA-256 of the fetched HTML.
 *  - [MonitorType.TEXT] / [MonitorType.CSS_SELECTOR] — Jsoup extraction.
 *  - [MonitorType.VISUAL] — offscreen WebView render hashed with [PerceptualHash].
 */
@Singleton
class SiteMonitor @Inject constructor(
    private val fetcher: PageFetcher,
    private val visualCapture: VisualCapture,
) {
    suspend fun snapshot(site: WatchedSite): SnapshotResult {
        // Visual monitoring renders the page itself in a WebView, so it does
        // not go through the OkHttp fetch path used by the other types.
        if (site.monitorType == MonitorType.VISUAL) {
            return try {
                val bitmap = visualCapture.capture(site.url)
                val hash = PerceptualHash.dHash(bitmap)
                bitmap.recycle()
                // No readable content for a screenshot; change is described generically.
                SnapshotResult.Success(hash, content = "")
            } catch (e: Exception) {
                SnapshotResult.Failure(e.message ?: "Failed to render page")
            }
        }

        val html = try {
            fetcher.fetch(site.url)
        } catch (e: Exception) {
            return SnapshotResult.Failure(e.message ?: "Failed to fetch page")
        }

        return when (site.monitorType) {
            // Compare the page's visible block text (not raw HTML) so changes are
            // both less noisy and describable as added/removed lines.
            MonitorType.FULL_PAGE -> {
                val content = visibleBlocks(Jsoup.parse(html, site.url))
                SnapshotResult.Success(HashUtil.sha256(content), content)
            }

            // Extract the text matched by a CSS selector via Jsoup; a change in
            // that text (e.g. a price or status badge) triggers a notification.
            MonitorType.CSS_SELECTOR -> {
                val selector = site.cssSelector?.takeIf { it.isNotBlank() }
                    ?: return SnapshotResult.Failure("No CSS selector configured", retryable = false)
                try {
                    val matched = Jsoup.parse(html, site.url).select(selector)
                    if (matched.isEmpty()) {
                        return SnapshotResult.Failure(
                            "Selector matched nothing: $selector",
                            retryable = false,
                        )
                    }
                    val content = matched.text()
                    SnapshotResult.Success(HashUtil.sha256(content), content)
                } catch (e: Selector.SelectorParseException) {
                    SnapshotResult.Failure("Invalid CSS selector: ${e.message}", retryable = false)
                }
            }

            // Track whether a target string is present on the page; a notification
            // fires when its presence flips (appears or disappears).
            MonitorType.TEXT -> {
                val needle = site.targetText?.takeIf { it.isNotBlank() }
                    ?: return SnapshotResult.Failure("No target text configured", retryable = false)
                val present = Jsoup.parse(html, site.url).text().contains(needle, ignoreCase = true)
                val content = if (present) "present" else "absent"
                SnapshotResult.Success(HashUtil.sha256(content), content)
            }

            // Handled above (does not use the fetched HTML).
            MonitorType.VISUAL -> error("VISUAL handled before fetch")
        }
    }

    /**
     * The page's visible text as one trimmed, non-empty block per line. Uses each
     * element's own text (not descendants') to avoid duplicating nested content,
     * giving a stable, line-diffable representation of what a reader sees.
     */
    private fun visibleBlocks(doc: org.jsoup.nodes.Document): String {
        val body = doc.body() ?: return doc.text()
        return body.select("h1,h2,h3,h4,h5,h6,p,li,td,th,a,span,div,blockquote,figcaption")
            .asSequence()
            .map { it.ownText().trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")
            .ifBlank { body.text() }
    }
}
