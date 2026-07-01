package com.sitewatch.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sitewatch.app.data.local.NotificationRecord
import com.sitewatch.app.data.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationHistoryViewModel @Inject constructor(
    private val repository: SiteRepository,
) : ViewModel() {

    val records: StateFlow<List<NotificationRecord>> =
        repository.observeNotifications()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun clearAll() {
        viewModelScope.launch { repository.clearNotifications() }
    }
}
