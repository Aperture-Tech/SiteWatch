package com.sitewatch.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sitewatch.app.data.local.WatchedSite
import com.sitewatch.app.data.repository.SiteRepository
import com.sitewatch.app.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val sites: List<WatchedSite> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SiteRepository,
    private val scheduler: WorkScheduler,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> =
        repository.observeSites()
            .map { DashboardUiState(sites = it, isLoading = false) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardUiState(),
            )

    fun toggleActive(site: WatchedSite) {
        viewModelScope.launch {
            val updated = site.copy(isActive = !site.isActive)
            repository.upsert(updated)
            scheduler.schedule(updated)
        }
    }

    fun delete(site: WatchedSite) {
        viewModelScope.launch {
            scheduler.cancel(site.id)
            repository.deleteById(site.id)
        }
    }

    fun checkNow(site: WatchedSite) {
        scheduler.runNow(site.id)
    }
}
