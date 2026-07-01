package com.sitewatch.app.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromMonitorType(type: MonitorType): String = type.name

    @TypeConverter
    fun toMonitorType(value: String): MonitorType =
        runCatching { MonitorType.valueOf(value) }.getOrDefault(MonitorType.FULL_PAGE)
}
