package com.nexus.intelligence.ui.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.intelligence.domain.model.DashboardStats
import com.nexus.intelligence.domain.model.DocumentInfo
import com.nexus.intelligence.domain.model.IndexingProgress
import com.nexus.intelligence.domain.usecase.GetDashboardStatsUseCase
import com.nexus.intelligence.domain.usecase.IndexDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val indexDocumentsUseCase: IndexDocumentsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dashboardStats = MutableStateFlow(DashboardStats())
    val dashboardStats: StateFlow<DashboardStats> = _dashboardStats.asStateFlow()

    private val _recentDocuments = MutableStateFlow<List<DocumentInfo>>(emptyList())
    val recentDocuments: StateFlow<List<DocumentInfo>> = _recentDocuments.asStateFlow()

    val indexingProgress: StateFlow<IndexingProgress> = indexDocumentsUseCase.progress
        .stateIn(viewModelScope, SharingStarted.Lazily, IndexingProgress())

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        // Collect document count
        viewModelScope.launch {
            getDashboardStatsUseCase.getDocumentCount().collect { count ->
                _dashboardStats.update { it.copy(totalDocuments = count) }
            }
        }

        // Collect recent documents
        viewModelScope.launch {
            getDashboardStatsUseCase.getRecentDocuments(20).collect { docs ->
                _recentDocuments.value = docs
            }
        }

        // Collect all documents for type distribution
        viewModelScope.launch {
            getDashboardStatsUseCase.getAllDocuments().collect { docs ->
                val byType = docs.groupBy { it.fileType }.mapValues { it.value.size }
                _dashboardStats.update { it.copy(documentsByType = byType) }
            }
        }

        // Collect indexing stats
        viewModelScope.launch {
            getDashboardStatsUseCase.getIndexingStats().collect { stats ->
                if (stats != null) {
                    _dashboardStats.update {
                        it.copy(
                            lastScanTime = stats.lastScanTimestamp,
                            isScanning = stats.isCurrentlyScanning
                        )
                    }
                }
            }
        }

        // Check API status
        viewModelScope.launch {
            val apiOnline = getDashboardStatsUseCase.isApiAvailable()
            _dashboardStats.update { it.copy(apiOnline = apiOnline) }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startScan() {
        viewModelScope.launch {
            _dashboardStats.update { it.copy(isScanning = true) }
            val dirs = listOf(Environment.getExternalStorageDirectory().absolutePath)
            indexDocumentsUseCase.fullScan(dirs)
            _dashboardStats.update { it.copy(isScanning = false) }
            loadDashboardData()
        }
    }

    fun refreshApiStatus() {
        viewModelScope.launch {
            val apiOnline = getDashboardStatsUseCase.isApiAvailable()
            _dashboardStats.update { it.copy(apiOnline = apiOnline) }
        }
    }
}
