package com.sitewatch.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedSiteDao {

    @Query("SELECT * FROM watched_sites ORDER BY label COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<WatchedSite>>

    @Query("SELECT * FROM watched_sites WHERE id = :id")
    fun observeById(id: String): Flow<WatchedSite?>

    @Query("SELECT * FROM watched_sites WHERE id = :id")
    suspend fun getById(id: String): WatchedSite?

    @Query("SELECT * FROM watched_sites WHERE isActive = 1")
    suspend fun getActive(): List<WatchedSite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(site: WatchedSite)

    @Update
    suspend fun update(site: WatchedSite)

    @Delete
    suspend fun delete(site: WatchedSite)

    @Query("DELETE FROM watched_sites WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        UPDATE watched_sites
        SET lastCheckedAt = :checkedAt,
            lastSnapshot = :snapshot,
            lastContent = :content,
            lastChangedAt = :changedAt,
            lastError = :error
        WHERE id = :id
        """
    )
    suspend fun recordCheck(
        id: String,
        checkedAt: Long,
        snapshot: String?,
        content: String?,
        changedAt: Long?,
        error: String?,
    )

    @Query("UPDATE watched_sites SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: String, active: Boolean)
}
