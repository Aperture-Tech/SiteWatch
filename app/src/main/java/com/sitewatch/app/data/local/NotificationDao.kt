package com.sitewatch.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notification_history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NotificationRecord>>

    @Query("SELECT * FROM notification_history WHERE siteId = :siteId ORDER BY createdAt DESC")
    fun observeForSite(siteId: String): Flow<List<NotificationRecord>>

    @Insert
    suspend fun insert(record: NotificationRecord)

    @Query("DELETE FROM notification_history")
    suspend fun clear()

    @Query("DELETE FROM notification_history WHERE siteId = :siteId")
    suspend fun deleteForSite(siteId: String)
}
