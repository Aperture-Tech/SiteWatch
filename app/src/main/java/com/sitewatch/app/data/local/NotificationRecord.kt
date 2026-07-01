package com.sitewatch.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A persisted record of a change notification, powering the Notification
 * History screen. Kept independent from [WatchedSite] (it stores a snapshot
 * of the label/url) so history survives even if the site is later deleted.
 */
@Entity(tableName = "notification_history")
data class NotificationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val siteId: String,
    val siteLabel: String,
    val url: String,
    val message: String,
    val createdAt: Long,
)
