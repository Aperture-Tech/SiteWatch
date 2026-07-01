package com.sitewatch.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A single website the user is monitoring.
 *
 * [id] is a stable UUID string also used to tag the site's periodic
 * WorkManager job, so the worker for a site can be cancelled/replaced
 * independently of the others.
 */
@Entity(tableName = "watched_sites")
data class WatchedSite(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val url: String,
    val label: String,
    val monitorType: MonitorType = MonitorType.FULL_PAGE,

    /** CSS selector, only meaningful for [MonitorType.CSS_SELECTOR]. */
    val cssSelector: String? = null,

    /** Target text, only meaningful for [MonitorType.TEXT]. */
    val targetText: String? = null,

    val checkIntervalMinutes: Long = 60L,
    val isActive: Boolean = true,

    /** Epoch millis of the last completed check, or null if never checked. */
    val lastCheckedAt: Long? = null,

    /** Hash of the content captured on the last check. */
    val lastSnapshot: String? = null,

    /**
     * The human-readable content behind [lastSnapshot] (visible page text,
     * matched selector text, or presence flag). Stored so the next change can
     * be described as a diff, not just detected. Null for visual monitoring.
     */
    val lastContent: String? = null,

    /** Epoch millis of the last time a change was detected. */
    val lastChangedAt: Long? = null,

    /** Last error message from a failed check, or null if the last check was ok. */
    val lastError: String? = null,
)
