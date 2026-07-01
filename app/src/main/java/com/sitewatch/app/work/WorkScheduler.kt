package com.sitewatch.app.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sitewatch.app.data.local.WatchedSite
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the mapping between [WatchedSite]s and their WorkManager jobs.
 *
 * Each site gets one uniquely-named periodic job ("site-check-<uuid>") tagged
 * with its UUID, so scheduling is idempotent (REPLACE) and a single site can
 * be cancelled or manually triggered without touching the others.
 */
@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    fun schedule(site: WatchedSite) {
        if (!site.isActive) {
            cancel(site.id)
            return
        }

        // The user may pick any interval >= 1 min, but WorkManager won't run
        // periodic work more often than every 15 min, so clamp the actual job.
        val interval = site.checkIntervalMinutes.coerceAtLeast(PERIODIC_FLOOR_MINUTES)

        val request = PeriodicWorkRequestBuilder<SiteCheckWorker>(
            interval, TimeUnit.MINUTES,
        )
            .setConstraints(networkConstraints())
            .setInputData(siteData(site.id))
            .addTag(tagFor(site.id))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueName(site.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** Runs a one-off immediate check for a site (manual trigger / Phase 4). */
    fun runNow(siteId: String) {
        val request = OneTimeWorkRequestBuilder<SiteCheckWorker>()
            .setConstraints(networkConstraints())
            .setInputData(siteData(siteId))
            .addTag(tagFor(siteId))
            .build()

        workManager.enqueueUniqueWork(
            oneTimeName(siteId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(siteId: String) {
        workManager.cancelUniqueWork(uniqueName(siteId))
    }

    /**
     * Emits true while a manual ([runNow]) check for the site is enqueued or
     * running, so the UI can show a "checking…" state. Periodic work is excluded
     * because it sits permanently ENQUEUED between runs.
     */
    fun observeManualCheckRunning(siteId: String): Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(oneTimeName(siteId))
            .map { infos -> infos.any { !it.state.isFinished } }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun siteData(siteId: String): Data =
        Data.Builder().putString(SiteCheckWorker.KEY_SITE_ID, siteId).build()

    companion object {
        /** Smallest interval the UI lets the user enter. */
        const val MIN_INTERVAL_MINUTES = 1L

        /**
         * WorkManager's hard floor for periodic work (15 min). A site set below
         * this still polls at ~15 min in the background; "Check now" is instant.
         */
        const val PERIODIC_FLOOR_MINUTES = 15L

        fun tagFor(siteId: String) = "site-tag-$siteId"
        private fun uniqueName(siteId: String) = "site-check-$siteId"
        private fun oneTimeName(siteId: String) = "site-check-now-$siteId"
    }
}
