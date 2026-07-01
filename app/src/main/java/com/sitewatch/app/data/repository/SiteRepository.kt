package com.sitewatch.app.data.repository

import com.sitewatch.app.data.local.NotificationDao
import com.sitewatch.app.data.local.NotificationRecord
import com.sitewatch.app.data.local.WatchedSite
import com.sitewatch.app.data.local.WatchedSiteDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Single point of access to persisted sites and notification history. */
@Singleton
class SiteRepository @Inject constructor(
    private val siteDao: WatchedSiteDao,
    private val notificationDao: NotificationDao,
) {
    fun observeSites(): Flow<List<WatchedSite>> = siteDao.observeAll()

    fun observeSite(id: String): Flow<WatchedSite?> = siteDao.observeById(id)

    suspend fun getSite(id: String): WatchedSite? = siteDao.getById(id)

    suspend fun getActiveSites(): List<WatchedSite> = siteDao.getActive()

    suspend fun upsert(site: WatchedSite) = siteDao.upsert(site)

    suspend fun deleteById(id: String) {
        siteDao.deleteById(id)
        notificationDao.deleteForSite(id)
    }

    suspend fun setActive(id: String, active: Boolean) = siteDao.setActive(id, active)

    suspend fun recordCheck(
        id: String,
        checkedAt: Long,
        snapshot: String?,
        content: String?,
        changedAt: Long?,
        error: String?,
    ) = siteDao.recordCheck(id, checkedAt, snapshot, content, changedAt, error)

    fun observeNotifications(): Flow<List<NotificationRecord>> = notificationDao.observeAll()

    fun observeNotificationsForSite(siteId: String): Flow<List<NotificationRecord>> =
        notificationDao.observeForSite(siteId)

    suspend fun addNotification(record: NotificationRecord) = notificationDao.insert(record)

    suspend fun clearNotifications() = notificationDao.clear()
}
