package com.sitewatch.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sitewatch.app.data.local.WatchedSite
import com.sitewatch.app.ui.common.hasNotificationPermission
import com.sitewatch.app.ui.common.isIgnoringBatteryOptimizations
import com.sitewatch.app.ui.common.monitorIcon
import com.sitewatch.app.ui.common.openBatteryOptimizationSettings
import com.sitewatch.app.ui.common.openNotificationSettings
import com.sitewatch.app.ui.common.relativeTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddSite: () -> Unit,
    onOpenSite: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SiteWatch") },
                actions = {
                    IconButton(onClick = onOpenNotifications) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notification history")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSite) {
                Icon(Icons.Default.Add, contentDescription = "Add site")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            BackgroundReliabilityBanner(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )

            when {
                state.isLoading -> Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                state.sites.isEmpty() -> EmptyState(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.sites, key = { it.id }) { site ->
                        SiteCard(
                            site = site,
                            onOpen = { onOpenSite(site.id) },
                            onToggle = { viewModel.toggleActive(site) },
                            onCheckNow = {
                                viewModel.checkNow(site)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Checking \"${site.label}\"…")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * A dismissible-looking advisory shown only when background checks are at risk:
 * the app isn't exempt from battery optimization, or the notification permission
 * is off. Re-checks on resume so it disappears once the user fixes it in Settings.
 */
@Composable
private fun BackgroundReliabilityBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var batteryOk by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    var notifOk by remember { mutableStateOf(hasNotificationPermission(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        batteryOk = isIgnoringBatteryOptimizations(context)
        notifOk = hasNotificationPermission(context)
    }

    if (batteryOk && notifOk) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BatteryAlert, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Background checks may be unreliable",
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            if (!batteryOk) {
                Text(
                    "Android can pause SiteWatch in the background to save battery. " +
                        "Allow background activity so checks keep running.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = { openBatteryOptimizationSettings(context) },
                    contentPadding = PaddingValues(0.dp),
                ) { Text("Allow background activity") }
            }

            if (!notifOk) {
                Text(
                    "Notifications are off, so you won't see change alerts.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = { openNotificationSettings(context) },
                    contentPadding = PaddingValues(0.dp),
                ) { Text("Turn on notifications") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteCard(
    site: WatchedSite,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onCheckNow: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), onClick = onOpen) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    monitorIcon(site.monitorType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = site.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(checked = site.isActive, onCheckedChange = { onToggle() })
            }

            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = statusLine(site),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (site.lastError != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCheckNow) {
                    Icon(Icons.Default.Refresh, contentDescription = "Check now")
                }
            }
        }
    }
}

private fun statusLine(site: WatchedSite): String {
    site.lastError?.let { return "Error: $it" }
    if (!site.isActive) return "Paused"
    return "Checked ${relativeTime(site.lastCheckedAt)} · every ${site.checkIntervalMinutes} min"
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(12.dp))
            Text("No sites yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(4.dp))
            Text(
                "Tap + to start watching a website for changes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
