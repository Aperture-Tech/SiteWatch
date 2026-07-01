package com.sitewatch.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sitewatch.app.data.local.NotificationRecord
import com.sitewatch.app.data.local.WatchedSite
import com.sitewatch.app.ui.common.absoluteTime
import com.sitewatch.app.ui.common.monitorIcon
import com.sitewatch.app.ui.common.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDeleted: () -> Unit,
    viewModel: SiteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val site = state.site

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(site?.label ?: "Site") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (site != null) {
                        IconButton(onClick = { onEdit(site.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }

            site == null -> Box(Modifier.fillMaxSize().padding(padding)) {
                Text(
                    "This site no longer exists.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> DetailContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                site = site,
                notifications = state.notifications,
                isChecking = state.isChecking,
                onToggleActive = viewModel::toggleActive,
                onCheckNow = viewModel::checkNow,
            )
        }
    }

    if (showDeleteDialog && site != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete site?") },
            text = { Text("\"${site.label}\" and its change history will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete(onDeleted)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DetailContent(
    modifier: Modifier,
    site: WatchedSite,
    notifications: List<NotificationRecord>,
    isChecking: Boolean,
    onToggleActive: () -> Unit,
    onCheckNow: () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                monitorIcon(site.monitorType),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f)) {
                Text(site.url, style = MaterialTheme.typography.bodyMedium)
                Text(
                    site.monitorType.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Status card
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Active", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (site.isActive) "Running every ${site.checkIntervalMinutes} min"
                            else "Paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = site.isActive, onCheckedChange = { onToggleActive() })
                }

                HorizontalDivider()

                InfoRow("Last checked", relativeTime(site.lastCheckedAt))
                InfoRow("Last changed", relativeTime(site.lastChangedAt))
                site.cssSelector?.let { InfoRow("Selector", it) }
                site.targetText?.let { InfoRow("Watching for", it) }
                site.lastSnapshot?.let {
                    InfoRow("Snapshot", it.take(16) + if (it.length > 16) "…" else "")
                }
                site.lastError?.let {
                    InfoRow("Last error", it, valueColor = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Manual check
        if (isChecking) {
            OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text("Checking…")
            }
        } else {
            Button(onClick = onCheckNow, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Check now")
            }
        }

        // Change history
        Text("Recent changes", style = MaterialTheme.typography.titleMedium)
        if (notifications.isEmpty()) {
            Text(
                "No changes detected yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            notifications.forEach { record ->
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(record.message, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        absoluteTime(record.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}
