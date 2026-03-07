package com.nexus.intelligence.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nexus.intelligence.domain.model.DashboardStats
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.IndexingProgress
import com.nexus.intelligence.ui.components.*
import com.nexus.intelligence.ui.theme.NexusColors
import com.nexus.intelligence.ui.theme.NexusMonospace
import com.nexus.intelligence.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onSearch: (String) -> Unit,
    onDocumentClick: (DocumentInfo) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()
    val indexingProgress by viewModel.indexingProgress.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    HudGridBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Title Header ─────────────────────────────────────
            item {
                DashboardHeader()
            }

            // ── Search Bar ───────────────────────────────────────
            item {
                HudSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = { onSearch(searchQuery) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ── Stats Grid ───────────────────────────────────────
            item {
                HudSectionHeader(title = "SYSTEM STATUS")
                Spacer(modifier = Modifier.height(8.dp))
                StatsGrid(stats = stats)
            }

            // ── Indexing Progress ─────────────────────────────────
            if (indexingProgress.isRunning) {
                item {
                    HudSectionHeader(title = "INDEXING PROGRESS", color = NexusColors.Amber)
                    Spacer(modifier = Modifier.height(8.dp))
                    IndexingProgressPanel(progress = indexingProgress)
                }
            }

            // ── Radar + API Status ───────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Radar
                    HolographicCard(
                        modifier = Modifier.weight(1f),
                        borderColor = NexusColors.Cyan
                    ) {
                        Text(
                            text = "SCAN RADAR",
                            style = MaterialTheme.typography.labelMedium,
                            color = NexusColors.Cyan.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedRadar(
                                isActive = stats.isScanning || indexingProgress.isRunning,
                                size = 100.dp
                            )
                        }
                    }

                    // Status panel
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HolographicCard(borderColor = if (stats.apiOnline) NexusColors.Green else NexusColors.Red) {
                            StatusIndicator(
                                isOnline = stats.apiOnline,
                                label = if (stats.apiOnline) "API ONLINE" else "API OFFLINE"
                            )
                        }
                        HolographicCard(borderColor = NexusColors.Cyan) {
                            StatusIndicator(
                                isOnline = stats.networkNodes > 0,
                                label = "NODES: ${stats.networkNodes}"
                            )
                        }
                        HolographicCard(borderColor = NexusColors.Green) {
                            StatusIndicator(
                                isOnline = stats.activeWatchers > 0,
                                label = "WATCHERS: ${stats.activeWatchers}"
                            )
                        }
                    }
                }
            }

            // ── Document Type Distribution ───────────────────────
            if (stats.documentsByType.isNotEmpty()) {
                item {
                    HudSectionHeader(title = "DOCUMENT DISTRIBUTION")
                    Spacer(modifier = Modifier.height(8.dp))
                    DocumentTypeDistribution(distribution = stats.documentsByType)
                }
            }

            // ── Recent Documents ─────────────────────────────────
            if (recentDocs.isNotEmpty()) {
                item {
                    HudSectionHeader(title = "RECENT DOCUMENTS")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(recentDocs.take(10)) { doc ->
                    DocumentCard(
                        document = doc,
                        onClick = { onDocumentClick(doc) }
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

// ── Dashboard Header ─────────────────────────────────────────────

@Composable
private fun DashboardHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "header")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headerGlow"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "N E X U S",
            style = MaterialTheme.typography.displayLarge,
            color = NexusColors.Cyan.copy(alpha = glowAlpha),
            fontFamily = NexusMonospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 8.sp
        )
        Text(
            text = "PERSONAL DOCUMENT INTELLIGENCE SYSTEM",
            style = MaterialTheme.typography.labelMedium,
            color = NexusColors.TextDim,
            letterSpacing = 3.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "v1.0.0 // LOCAL MODE // ZERO TELEMETRY",
            style = MaterialTheme.typography.labelSmall,
            color = NexusColors.Green.copy(alpha = 0.5f),
            letterSpacing = 1.sp
        )
    }
}

// ── Stats Grid ───────────────────────────────────────────────────

@Composable
private fun StatsGrid(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatPanel(
                label = "TOTAL INDEXED",
                value = "${stats.totalDocuments}",
                color = NexusColors.Cyan,
                modifier = Modifier.weight(1f)
            )
            StatPanel(
                label = "LAST SCAN",
                value = if (stats.lastScanTime > 0) formatTimestampRelative(stats.lastScanTime) else "NEVER",
                color = NexusColors.Green,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Indexing Progress Panel ──────────────────────────────────────

@Composable
private fun IndexingProgressPanel(progress: IndexingProgress) {
    HolographicCard(borderColor = NexusColors.Amber) {
        HudProgressBar(
            progress = progress.progressFraction,
            color = NexusColors.Amber,
            label = "SCANNING: ${progress.processedFiles}/${progress.totalFiles}"
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = ">> ${progress.currentFile}",
            style = MaterialTheme.typography.bodySmall,
            color = NexusColors.Amber.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

// ── Document Type Distribution ───────────────────────────────────

@Composable
private fun DocumentTypeDistribution(distribution: Map<String, Int>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(distribution.entries.toList()) { (type, count) ->
            val color = getDocTypeColor(type)
            StatPanel(
                label = type,
                value = "$count",
                color = color,
                modifier = Modifier.width(100.dp)
            )
        }
    }
}
