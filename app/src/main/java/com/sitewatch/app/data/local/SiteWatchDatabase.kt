package com.sitewatch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [WatchedSite::class, NotificationRecord::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class SiteWatchDatabase : RoomDatabase() {
    abstract fun watchedSiteDao(): WatchedSiteDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val NAME = "sitewatch.db"
    }
}
