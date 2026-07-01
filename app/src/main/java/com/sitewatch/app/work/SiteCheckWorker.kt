package com.sitewatch.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sitewatch.app.data.local.NotificationRecord
import com.sitewatch.app.data.repository.SiteRepository
import com.sitewatch.app.monitor.ChangeDescriber
import com.sitewatch.app.monitor.SiteMonitor
import com.sitewatch.app.monitor.SnapshotResult
import com.sitewatch.app.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodically captures a snapshot of one site and, if it differs from the
 * stored snapshot, posts a notification and records it in history.
 *
 * The site to check is identified by [KEY_SITE_ID] in the input data. Each
 * site's periodic work is uniquely named and tagged by its UUID (see
 * [WorkScheduler]) so it can be rescheduled or cancelled in isolation.
 */
@HiltWorker
class SiteCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: SiteRepository,
    private val monitor: SiteMonitor,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val siteId = inputData.getString(KEY_SITE_ID) ?: return Result.failure()
        val site = repository.getSite(siteId) ?: return Result.success() // deleted; nothing to do
        if (!site.isActive) return Result.success()

        val now = System.currentTimeMillis()

        when (val result = monitor.snapshot(site)) {
            is SnapshotResult.Failure -> {
                repository.recordCheck(
                    id = site.id,
                    checkedAt = now,
                    snapshot = site.lastSnapshot,
                    content = site.lastContent,
                    changedAt = site.lastChangedAt,
                    error = result.message,
                )
                // Retry transient failures (network/HTTP) with backoff; for
                // permanent failures (bad selector, etc.) just record and wait
                // for the next periodic run so we don't hammer the site.
                return if (result.retryable) Result.retry() else Result.success()
            }

            is SnapshotResult.Success -> {
                val previous = site.lastSnapshot
                val changed = previous != null && previous != result.hash

                repository.recordCheck(
                    id = site.id,
                    checkedAt = now,
                    snapshot = result.hash,
                    content = result.content,
                    changedAt = if (changed) now else site.lastChangedAt,
                    error = null,
                )

                if (changed) {
                    val message = ChangeDescriber.describe(
                        type = site.monitorType,
                        old = site.lastContent.orEmpty(),
                        new = result.content,
                    )
                    notificationHelper.notifySiteChanged(site.id, site.label, message)
                    repository.addNotification(
                        NotificationRecord(
                            siteId = site.id,
                            siteLabel = site.label,
                            url = site.url,
                            message = message,
                            createdAt = now,
                        )
                    )
                }
                return Result.success()
            }
        }
    }

    companion object {
        const val KEY_SITE_ID = "site_id"
    }
}
