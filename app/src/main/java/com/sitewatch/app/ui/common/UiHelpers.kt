package com.sitewatch.app.ui.common

import android.text.format.DateUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.graphics.vector.ImageVector
import com.sitewatch.app.data.local.MonitorType
import java.text.DateFormat
import java.util.Date

/** "5 minutes ago" style relative time, falling back to "Never". */
fun relativeTime(epochMillis: Long?): String {
    if (epochMillis == null) return "Never"
    return DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
}

/** Absolute date+time, or "—" if null. */
fun absoluteTime(epochMillis: Long?): String {
    if (epochMillis == null) return "—"
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
}

fun monitorIcon(type: MonitorType): ImageVector = when (type) {
    MonitorType.FULL_PAGE -> Icons.Default.Article
    MonitorType.TEXT -> Icons.Default.TextFields
    MonitorType.CSS_SELECTOR -> Icons.Default.Code
    MonitorType.VISUAL -> Icons.Default.Image
}
