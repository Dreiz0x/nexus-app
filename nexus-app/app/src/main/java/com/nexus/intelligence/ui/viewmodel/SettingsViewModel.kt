package com.nexus.intelligence.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.intelligence.data.embeddings.EmbeddingService
import com.nexus.intelligence.data.local.entity.MonitoredFolderEntity
import com.nexus.intelligence.domain.usecase.ManageSettingsUseCase
import com.nexus.intelligence.service.network.NexusLocalServer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manageSettingsUseCase: ManageSettingsUseCase,
    private val embeddingService: EmbeddingService,
    private val localServer: NexusLocalServer
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nexus_settings", Context.MODE_PRIVATE)

    private val _apiEndpoint = MutableStateFlow(prefs.getString("api_endpoint", "http://127.0.0.1:8080") ?: "http://127.0.0.1:8080")
    val apiEndpoint: StateFlow<String> = _apiEndpoint.asStateFlow()

    private val _monitoredFolders = MutableStateFlow<List<MonitoredFolderEntity>>(emptyList())
    val monitoredFolders: StateFlow<List<MonitoredFolderEntity>> = _monitoredFolders.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound_enabled", true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _serverEnabled = MutableStateFlow(prefs.getBoolean("server_enabled", false))
    val serverEnabled: StateFlow<Boolean> = _serverEnabled.asStateFlow()

    private val _serverPort = MutableStateFlow(prefs.getString("server_port", "9090") ?: "9090")
    val serverPort: StateFlow<String> = _serverPort.asStateFlow()

    private val _autoIndexEnabled = MutableStateFlow(prefs.getBoolean("auto_index", true))
    val autoIndexEnabled: StateFlow<Boolean> = _autoIndexEnabled.asStateFlow()

    private val _apiStatus = MutableStateFlow(false)
    val apiStatus: StateFlow<Boolean> = _apiStatus.asStateFlow()

    private val _newFolderPath = MutableStateFlow("")
    val newFolderPath: StateFlow<String> = _newFolderPath.asStateFlow()

    init {
        viewModelScope.launch {
            manageSettingsUseCase.getMonitoredFolders().collect { folders ->
                _monitoredFolders.value = folders
            }
        }
        testApiConnection()
    }

    fun updateApiEndpoint(endpoint: String) {
        _apiEndpoint.value = endpoint
        prefs.edit().putString("api_endpoint", endpoint).apply()
        embeddingService.setBaseUrl(endpoint)
    }

    fun testApiConnection() {
        viewModelScope.launch {
            embeddingService.setBaseUrl(_apiEndpoint.value)
            _apiStatus.value = embeddingService.isApiAvailable()
        }
    }

    fun updateNewFolderPath(path: String) {
        _newFolderPath.value = path
    }

    fun addMonitoredFolder() {
        val path = _newFolderPath.value.trim()
        if (path.isBlank()) return
        viewModelScope.launch {
            manageSettingsUseCase.addMonitoredFolder(path, path.substringAfterLast("/"))
            _newFolderPath.value = ""
        }
    }

    fun removeMonitoredFolder(path: String) {
        viewModelScope.launch {
            manageSettingsUseCase.removeMonitoredFolder(path)
        }
    }

    fun toggleServer(enabled: Boolean) {
        _serverEnabled.value = enabled
        prefs.edit().putBoolean("server_enabled", enabled).apply()
        if (enabled) {
            val port = _serverPort.value.toIntOrNull() ?: 9090
            localServer.start(port)
        } else {
            localServer.stop()
        }
    }

    fun updateServerPort(port: String) {
        _serverPort.value = port
        prefs.edit().putString("server_port", port).apply()
    }

    fun toggleAutoIndex(enabled: Boolean) {
        _autoIndexEnabled.value = enabled
        prefs.edit().putBoolean("auto_index", enabled).apply()
    }

    fun toggleSound(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun clearIndex() {
        viewModelScope.launch {
            manageSettingsUseCase.clearIndex()
        }
    }
}
