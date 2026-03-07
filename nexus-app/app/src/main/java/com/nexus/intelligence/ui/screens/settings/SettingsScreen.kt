package com.nexus.intelligence.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.ui.components.*
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.theme.NexusMonospace
import com.nexus.intelligence.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val apiEndpoint by viewModel.apiEndpoint.collectAsState()
    val monitoredFolders by viewModel.monitoredFolders.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val serverEnabled by viewModel.serverEnabled.collectAsState()
    val serverPort by viewModel.serverPort.collectAsState()
    val autoIndexEnabled by viewModel.autoIndexEnabled.collectAsState()
    val apiStatus by viewModel.apiStatus.collectAsState()
    val newFolderPath by viewModel.newFolderPath.collectAsState()

    HudGridBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = NexusColors.Cyan
                        )
                    }
                    Text(
                        text = "CONFIGURATION",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NexusColors.Cyan,
                        letterSpacing = 2.sp
                    )
                }
            }

            // ── API Configuration ────────────────────────────────
            item {
                HudSectionHeader(title = "API ENDPOINT")
                Spacer(modifier = Modifier.height(8.dp))
                HolographicCard(
                    borderColor = if (apiStatus) NexusColors.Green else NexusColors.Red
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusIndicator(
                            isOnline = apiStatus,
                            label = if (apiStatus) "CONNECTED" else "DISCONNECTED"
                        )
                        TextButton(onClick = { viewModel.testApiConnection() }) {
                            Text(
                                text = "[TEST]",
                                style = MaterialTheme.typography.labelMedium,
                                color = NexusColors.Cyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ENDPOINT URL:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusColors.TextDim
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    HudTextField(
                        value = apiEndpoint,
                        onValueChange = { viewModel.updateApiEndpoint(it) },
                        placeholder = "http://127.0.0.1:8080"
                    )
                }
            }

            // ── Monitored Folders ────────────────────────────────
            item {
                HudSectionHeader(title = "MONITORED FOLDERS")
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(monitoredFolders) { folder ->
                MonitoredFolderItem(
                    folder = folder,
                    onRemove = { viewModel.removeMonitoredFolder(folder.path) }
                )
            }

            item {
                // Add new folder
                HolographicCard(borderColor = NexusColors.Green) {
                    Text(
                        text = "ADD FOLDER TO MONITOR:",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusColors.Green.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HudTextField(
                            value = newFolderPath,
                            onValueChange = { viewModel.updateNewFolderPath(it) },
                            placeholder = "/storage/emulated/0/Documents",
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.addMonitoredFolder() }
                        ) {
                            Text(
                                text = "[ADD]",
                                style = MaterialTheme.typography.labelMedium,
                                color = NexusColors.Green
                            )
                        }
                    }
                }
            }

            // ── Network Server ───────────────────────────────────
            item {
                HudSectionHeader(title = "NETWORK SERVER")
                Spacer(modifier = Modifier.height(8.dp))
                HolographicCard(borderColor = NexusColors.Magenta) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "LOCAL SERVER",
                                style = MaterialTheme.typography.labelLarge,
                                color = NexusColors.Magenta
                            )
                            Text(
                                text = "Share documents on local network",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexusColors.TextDim
                            )
                        }
                        Switch(
                            checked = serverEnabled,
                            onCheckedChange = { viewModel.toggleServer(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexusColors.Magenta,
                                checkedTrackColor = NexusColors.MagentaGlow,
                                uncheckedThumbColor = NexusColors.TextDim,
                                uncheckedTrackColor = NexusColors.CardBackground
                            )
                        )
                    }

                    if (serverEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PORT:",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexusColors.TextDim
                        )
                        HudTextField(
                            value = serverPort,
                            onValueChange = { viewModel.updateServerPort(it) },
                            placeholder = "9090"
                        )
                    }
                }
            }

            // ── General Settings ─────────────────────────────────
            item {
                HudSectionHeader(title = "GENERAL")
                Spacer(modifier = Modifier.height(8.dp))
                HolographicCard(borderColor = NexusColors.Cyan) {
                    // Auto-index toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "AUTO-INDEX",
                                style = MaterialTheme.typography.labelLarge,
                                color = NexusColors.Cyan
                            )
                            Text(
                                text = "Automatically scan for new documents",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexusColors.TextDim
                            )
                        }
                        Switch(
                            checked = autoIndexEnabled,
                            onCheckedChange = { viewModel.toggleAutoIndex(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexusColors.Cyan,
                                checkedTrackColor = NexusColors.CyanGlow,
                                uncheckedThumbColor = NexusColors.TextDim,
                                uncheckedTrackColor = NexusColors.CardBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sound effects toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "SOUND EFFECTS",
                                style = MaterialTheme.typography.labelLarge,
                                color = NexusColors.Cyan
                            )
                            Text(
                                text = "Sci-fi interface sounds",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexusColors.TextDim
                            )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { viewModel.toggleSound(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexusColors.Cyan,
                                checkedTrackColor = NexusColors.CyanGlow,
                                uncheckedThumbColor = NexusColors.TextDim,
                                uncheckedTrackColor = NexusColors.CardBackground
                            )
                        )
                    }
                }
            }

            // ── Danger Zone ──────────────────────────────────────
            item {
                HudSectionHeader(title = "DANGER ZONE", color = NexusColors.Red)
                Spacer(modifier = Modifier.height(8.dp))
                HolographicCard(borderColor = NexusColors.Red) {
                    TextButton(
                        onClick = { viewModel.clearIndex() }
                    ) {
                        Text(
                            text = "[CLEAR ENTIRE INDEX]",
                            style = MaterialTheme.typography.labelLarge,
                            color = NexusColors.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "This will remove all indexed documents from the database. Files on disk will not be affected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NexusColors.TextDim
                    )
                }
            }

            // ── About ────────────────────────────────────────────
            item {
                HudSectionHeader(title = "ABOUT")
                Spacer(modifier = Modifier.height(8.dp))
                HolographicCard(borderColor = NexusColors.TextDim) {
                    Text(
                        text = "NEXUS v1.0.0",
                        style = MaterialTheme.typography.labelLarge,
                        color = NexusColors.Cyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Personal Document Intelligence System",
                        style = MaterialTheme.typography.bodySmall,
                        color = NexusColors.TextSecondary
                    )
                    Text(
                        text = "100% Local // Zero Telemetry // Zero External Servers",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusColors.Green.copy(alpha = 0.5f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ── Monitored Folder Item ────────────────────────────────────────

@Composable
private fun MonitoredFolderItem(
    folder: MonitoredFolderEntity,
    onRemove: () -> Unit
) {
    HolographicCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = NexusColors.Cyan
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = NexusColors.Cyan,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    if (folder.label.isNotBlank()) {
                        Text(
                            text = folder.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexusColors.TextPrimary
                        )
                    }
                    Text(
                        text = folder.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusColors.TextDim
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = NexusColors.Red,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ── HUD Text Field ───────────────────────────────────────────────

@Composable
fun HudTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        textStyle = TextStyle(
            fontFamily = NexusMonospace,
            fontSize = 12.sp,
            color = NexusColors.TextPrimary,
            letterSpacing = 0.5.sp
        ),
        singleLine = true,
        cursorBrush = SolidColor(NexusColors.Cyan),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = NexusMonospace,
                            fontSize = 12.sp,
                            color = NexusColors.TextDim
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}
