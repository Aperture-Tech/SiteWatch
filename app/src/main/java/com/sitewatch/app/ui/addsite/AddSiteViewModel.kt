package com.sitewatch.app.ui.addsite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sitewatch.app.data.local.MonitorType
import com.sitewatch.app.data.local.WatchedSite
import com.sitewatch.app.data.repository.SiteRepository
import com.sitewatch.app.ui.navigation.Routes
import com.sitewatch.app.work.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddSiteUiState(
    val isEditing: Boolean = false,
    val label: String = "",
    val url: String = "",
    val monitorType: MonitorType = MonitorType.FULL_PAGE,
    val cssSelector: String = "",
    val targetText: String = "",
    val intervalMinutes: String = "60",
    val isActive: Boolean = true,
    val saved: Boolean = false,
) {
    val urlError: String? = when {
        url.isBlank() -> null // don't nag before the user types
        !looksLikeUrl(url) -> "Enter a valid URL"
        else -> null
    }

    val intervalError: String? = when (val minutes = intervalMinutes.toLongOrNull()) {
        null -> "Enter a number"
        else -> if (minutes < WorkScheduler.MIN_INTERVAL_MINUTES) {
            "Minimum is ${WorkScheduler.MIN_INTERVAL_MINUTES} minute"
        } else {
            null
        }
    }

    /** Required only for [MonitorType.CSS_SELECTOR]. */
    val selectorError: String? =
        if (monitorType == MonitorType.CSS_SELECTOR && cssSelector.isBlank()) {
            "Enter a CSS selector"
        } else null

    /** Required only for [MonitorType.TEXT]. */
    val targetTextError: String? =
        if (monitorType == MonitorType.TEXT && targetText.isBlank()) {
            "Enter the text to watch for"
        } else null

    val canSave: Boolean =
        label.isNotBlank() &&
            url.isNotBlank() &&
            urlError == null &&
            intervalError == null &&
            selectorError == null &&
            targetTextError == null
}

private fun looksLikeUrl(value: String): Boolean {
    val candidate = if (value.contains("://")) value else "https://$value"
    return runCatching { java.net.URL(candidate) }.isSuccess && candidate.contains(".")
}

@HiltViewModel
class AddSiteViewModel @Inject constructor(
    private val repository: SiteRepository,
    private val scheduler: WorkScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val siteId: String? = savedStateHandle[Routes.ARG_SITE_ID]

    private val _uiState = MutableStateFlow(AddSiteUiState(isEditing = siteId != null))
    val uiState: StateFlow<AddSiteUiState> = _uiState.asStateFlow()

    init {
        siteId?.let { id ->
            viewModelScope.launch {
                repository.getSite(id)?.let { site ->
                    _uiState.update {
                        it.copy(
                            label = site.label,
                            url = site.url,
                            monitorType = site.monitorType,
                            cssSelector = site.cssSelector.orEmpty(),
                            targetText = site.targetText.orEmpty(),
                            intervalMinutes = site.checkIntervalMinutes.toString(),
                            isActive = site.isActive,
                        )
                    }
                }
            }
        }
    }

    fun onLabelChange(value: String) = _uiState.update { it.copy(label = value) }
    fun onUrlChange(value: String) = _uiState.update { it.copy(url = value) }
    fun onMonitorTypeChange(type: MonitorType) = _uiState.update { it.copy(monitorType = type) }
    fun onCssSelectorChange(value: String) = _uiState.update { it.copy(cssSelector = value) }
    fun onTargetTextChange(value: String) = _uiState.update { it.copy(targetText = value) }
    fun onIntervalChange(value: String) =
        _uiState.update { it.copy(intervalMinutes = value.filter { c -> c.isDigit() }) }
    fun onActiveChange(value: Boolean) = _uiState.update { it.copy(isActive = value) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        viewModelScope.launch {
            val existing = siteId?.let { repository.getSite(it) }
            val site = (existing ?: WatchedSite(url = "", label = "")).copy(
                label = state.label.trim(),
                url = state.url.trim(),
                monitorType = state.monitorType,
                cssSelector = state.cssSelector.trim().ifBlank { null },
                targetText = state.targetText.trim().ifBlank { null },
                checkIntervalMinutes = state.intervalMinutes.toLongOrNull()
                    ?.coerceAtLeast(WorkScheduler.MIN_INTERVAL_MINUTES)
                    ?: 60L,
                isActive = state.isActive,
            )

            repository.upsert(site)
            scheduler.schedule(site)
            _uiState.update { it.copy(saved = true) }
        }
    }
}
