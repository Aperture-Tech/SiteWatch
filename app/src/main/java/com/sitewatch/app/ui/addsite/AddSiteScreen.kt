package com.sitewatch.app.ui.addsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sitewatch.app.data.local.MonitorType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSiteScreen(
    onDone: () -> Unit,
    viewModel: AddSiteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit site" else "Add site") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.label,
                onValueChange = viewModel::onLabelChange,
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("URL") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                isError = state.urlError != null,
                supportingText = state.urlError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            MonitorTypeSelector(
                selected = state.monitorType,
                onSelected = viewModel::onMonitorTypeChange,
            )

            when (state.monitorType) {
                MonitorType.CSS_SELECTOR -> OutlinedTextField(
                    value = state.cssSelector,
                    onValueChange = viewModel::onCssSelectorChange,
                    label = { Text("CSS selector") },
                    placeholder = { Text("#price, .status") },
                    singleLine = true,
                    isError = state.selectorError != null,
                    supportingText = {
                        Text(state.selectorError ?: "Notifies when the matched element's text changes")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                MonitorType.TEXT -> OutlinedTextField(
                    value = state.targetText,
                    onValueChange = viewModel::onTargetTextChange,
                    label = { Text("Text to watch for") },
                    singleLine = true,
                    isError = state.targetTextError != null,
                    supportingText = {
                        Text(state.targetTextError ?: "Notifies when this text appears or disappears")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                MonitorType.VISUAL -> Text(
                    text = "Renders the page in a hidden browser and compares how the " +
                        "top of the page looks. Uses more battery and data than the other types.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                else -> Unit
            }

            OutlinedTextField(
                value = state.intervalMinutes,
                onValueChange = viewModel::onIntervalChange,
                label = { Text("Check interval (minutes)") },
                singleLine = true,
                isError = state.intervalError != null,
                supportingText = state.intervalError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Active", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Run periodic checks for this site",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.isActive, onCheckedChange = viewModel::onActiveChange)
            }

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isEditing) "Save changes" else "Add site")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonitorTypeSelector(
    selected: MonitorType,
    onSelected: (MonitorType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Monitor type") },
            supportingText = { Text(selected.description) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MonitorType.entries.forEach { type ->
                val enabled = type in MonitorType.implemented
                DropdownMenuItem(
                    enabled = enabled,
                    text = {
                        Text(
                            text = if (enabled) type.label else "${type.label} (coming soon)",
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}
