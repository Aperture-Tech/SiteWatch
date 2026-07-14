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
import kotlinx.coroutines.delay

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
                val suspectedChange = previous != null && previous != result.hash

                if (!suspectedChange) {
                    // First baseline or genuinely unchanged — just record it.
                    repository.recordCheck(
                        id = site.id,
                        checkedAt = now,
                        snapshot = result.hash,
                        content = result.content,
                        changedAt = site.lastChangedAt,
                        error = null,
                    )
                    return Result.success()
                }

                // A change is suspected. Sites that show a loading/splash screen
                // before their real content can momentarily hash differently, so
                // confirm with a second reading before alerting: only a stable
                // result that still differs from the baseline counts as real.
                delay(CONFIRM_DELAY_MS)
                val confirm = monitor.snapshot(site)

                if (confirm is SnapshotResult.Success &&
                    confirm.hash == result.hash &&
                    confirm.hash != previous
                ) {
                    // Confirmed, stable change.
                    val message = ChangeDescriber.describe(
                        type = site.monitorType,
                        old = site.lastContent.orEmpty(),
                        new = confirm.content,
                    )
                    repository.recordCheck(
                        id = site.id,
                        checkedAt = now,
                        snapshot = confirm.hash,
                        content = confirm.content,
                        changedAt = now,
                        error = null,
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
                } else {
                    // Unconfirmed — most likely a transient loading state. Keep the
                    // existing baseline so we neither alert on it nor lock in the
                    // transient value; the next run re-compares against the baseline.
                    repository.recordCheck(
                        id = site.id,
                        checkedAt = now,
                        snapshot = previous,
                        content = site.lastContent,
                        changedAt = site.lastChangedAt,
                        error = null,
                    )
                }
                return Result.success()
            }
        }
    }

    companion object {
        const val KEY_SITE_ID = "site_id"

        /** Delay before re-reading to confirm a suspected change (filters loading screens). */
        private const val CONFIRM_DELAY_MS = 4_000L
    }
}
