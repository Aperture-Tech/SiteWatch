package com.sitewatch.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sitewatch.app.data.local.NotificationRecord
import com.sitewatch.app.data.local.WatchedSite
import com.sitewatch.app.data.repository.SiteRepository
import com.sitewatch.app.ui.navigation.Routes
import com.sitewatch.app.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SiteDetailUiState(
    val site: WatchedSite? = null,
    val notifications: List<NotificationRecord> = emptyList(),
    val isChecking: Boolean = false,
    val loading: Boolean = true,
)

@HiltViewModel
class SiteDetailViewModel @Inject constructor(
    private val repository: SiteRepository,
    private val scheduler: WorkScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val siteId: String = checkNotNull(savedStateHandle[Routes.ARG_SITE_ID])

    val uiState: StateFlow<SiteDetailUiState> = combine(
        repository.observeSite(siteId),
        repository.observeNotificationsForSite(siteId),
        scheduler.observeManualCheckRunning(siteId),
    ) { site, notifications, isChecking ->
        SiteDetailUiState(
            site = site,
            notifications = notifications,
            isChecking = isChecking,
            loading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SiteDetailUiState(),
    )

    fun checkNow() = scheduler.runNow(siteId)

    fun toggleActive() {
        val site = uiState.value.site ?: return
        viewModelScope.launch {
            val updated = site.copy(isActive = !site.isActive)
            repository.upsert(updated)
            scheduler.schedule(updated)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            scheduler.cancel(siteId)
            repository.deleteById(siteId)
            onDeleted()
        }
    }
}
