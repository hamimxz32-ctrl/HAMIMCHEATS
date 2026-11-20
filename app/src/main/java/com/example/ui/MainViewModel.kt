package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.OverlayBadgeService
import com.example.config.DeviceProfileInfo
import com.example.config.GameProfile
import com.example.config.RemoteAppConfig
import com.example.config.RootStatusResult
import com.example.config.UserSettings
import com.example.data.AppRepository
import com.example.data.RemoteSyncOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
  GAMES,
  PROFILE,
  SETTINGS,
}

data class MainUiState(
  val currentTab: MainTab = MainTab.GAMES,
  val selectedGame: GameProfile? = null,
  val remoteConfig: RemoteAppConfig = RemoteAppConfig(),
  val remoteConfigUrl: String = "",
  val isSyncingRemote: Boolean = false,
  val syncStatusMessage: String = "",
  val announcementDismissed: Boolean = false,
  val isLoadingCatalog: Boolean = true,
  val isCheckingRoot: Boolean = true,
  val rootStatus: RootStatusResult =
    RootStatusResult(
      isRootGranted = false,
      binaryDetected = false,
      detectedPath = null,
      providerHint = "Checking",
    ),
  val showRootInfoDialog: Boolean = false,
  val deviceProfile: DeviceProfileInfo? = null,
  val settings: UserSettings = UserSettings(),
  val preparedIds: Set<String> = emptySet(),
  val isPreparingResource: Boolean = false,
  val prepareProgress: Float = 0f,
  val canDrawOverlays: Boolean = false,
  val isOverlayServiceRunning: Boolean = false,
)

class MainViewModel(private val repository: AppRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(MainUiState())
  val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

  init {
    val initialSettings = repository.loadSettings()
    val deviceProfile = repository.getDeviceProfile()
    val remoteUrl = repository.getRemoteConfigUrl()
    _uiState.update {
      it.copy(
        settings = initialSettings,
        deviceProfile = deviceProfile,
        remoteConfigUrl = remoteUrl,
        canDrawOverlays = repository.canDrawOverlays(),
        isOverlayServiceRunning = OverlayBadgeService.isRunning,
      )
    }
    loadInitialAndSync()
    if (initialSettings.autoCheckRoot) {
      recheckRoot()
    } else {
      _uiState.update {
        it.copy(
          isCheckingRoot = false,
          rootStatus =
            RootStatusResult(
              isRootGranted = false,
              binaryDetected = false,
              detectedPath = null,
              providerHint = "Not Checked",
            ),
        )
      }
    }
  }

  private fun loadInitialAndSync() {
    viewModelScope.launch {
      val initial = runCatching { repository.loadInitialRemoteConfig() }.getOrDefault(RemoteAppConfig())
      val prepared = initial.games.filter { repository.isResourcePrepared(it.id) }.map { it.id }.toSet()
      _uiState.update {
        it.copy(
          remoteConfig = initial,
          isLoadingCatalog = false,
          preparedIds = prepared,
        )
      }
      if (_uiState.value.remoteConfigUrl.isNotBlank()) {
        syncRemoteConfig()
      }
    }
  }

  fun syncRemoteConfig() {
    if (_uiState.value.isSyncingRemote) return
    viewModelScope.launch {
      _uiState.update { it.copy(isSyncingRemote = true, syncStatusMessage = "Syncing") }
      when (val outcome = repository.syncRemoteConfig(_uiState.value.remoteConfigUrl)) {
        is RemoteSyncOutcome.Success -> {
          val config = outcome.config
          val prepared =
            config.games.filter { repository.isResourcePrepared(it.id) }.map { it.id }.toSet()
          _uiState.update { state ->
            state.copy(
              remoteConfig = config,
              isSyncingRemote = false,
              announcementDismissed = false,
              preparedIds = prepared,
              syncStatusMessage = "Synced (${outcome.sourceLabel})",
            )
          }
        }
        is RemoteSyncOutcome.Error -> {
          _uiState.update {
            it.copy(isSyncingRemote = false, syncStatusMessage = outcome.message)
          }
        }
      }
    }
  }

  fun updateRemoteConfigUrl(url: String) {
    repository.saveRemoteConfigUrl(url)
    _uiState.update { it.copy(remoteConfigUrl = url) }
  }

  fun dismissAnnouncement() {
    _uiState.update { it.copy(announcementDismissed = true) }
  }

  fun selectTab(tab: MainTab) {
    val updatedDevice =
      if (tab == MainTab.PROFILE || tab == MainTab.GAMES) repository.getDeviceProfile()
      else _uiState.value.deviceProfile
    _uiState.update { it.copy(currentTab = tab, selectedGame = null, deviceProfile = updatedDevice) }
  }

  fun openGameDetail(game: GameProfile) {
    _uiState.update {
      it.copy(
        selectedGame = game,
        canDrawOverlays = repository.canDrawOverlays(),
        isOverlayServiceRunning = OverlayBadgeService.isRunning,
        prepareProgress = if (it.preparedIds.contains(game.id)) 1f else 0f,
      )
    }
  }

  fun closeGameDetail() {
    _uiState.update { it.copy(selectedGame = null, isPreparingResource = false) }
  }

  fun recheckRoot() {
    viewModelScope.launch {
      _uiState.update { it.copy(isCheckingRoot = true) }
      val result = repository.checkRootStatus()
      _uiState.update { it.copy(isCheckingRoot = false, rootStatus = result) }
    }
  }

  fun setRootInfoDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(showRootInfoDialog = visible) }
  }

  fun updateSecureMode(enabled: Boolean) {
    val updated = _uiState.value.settings.copy(secureMode = enabled)
    repository.saveSettings(updated)
    _uiState.update { it.copy(settings = updated) }
  }

  fun updateEmulatorMode(enabled: Boolean) {
    val updated = _uiState.value.settings.copy(emulatorMode = enabled)
    repository.saveSettings(updated)
    _uiState.update { it.copy(settings = updated) }
  }

  fun updateAutoCheckRoot(enabled: Boolean) {
    val updated = _uiState.value.settings.copy(autoCheckRoot = enabled)
    repository.saveSettings(updated)
    _uiState.update { it.copy(settings = updated) }
  }

  fun updateSparkleBackdrop(enabled: Boolean) {
    val updated = _uiState.value.settings.copy(sparkleBackdrop = enabled)
    repository.saveSettings(updated)
    _uiState.update { it.copy(settings = updated) }
  }

  fun refreshOverlayState() {
    _uiState.update {
      it.copy(
        canDrawOverlays = repository.canDrawOverlays(),
        isOverlayServiceRunning = OverlayBadgeService.isRunning,
      )
    }
  }

  fun setOverlayRunning(running: Boolean) {
    _uiState.update { it.copy(isOverlayServiceRunning = running) }
  }

  fun prepareSelectedResource() {
    val game = _uiState.value.selectedGame ?: return
    if (_uiState.value.isPreparingResource) return

    viewModelScope.launch {
      _uiState.update { it.copy(isPreparingResource = true, prepareProgress = 0f) }
      repository.prepareResource(game) { progress ->
        _uiState.update { it.copy(prepareProgress = progress) }
      }
      _uiState.update {
        it.copy(
          isPreparingResource = false,
          prepareProgress = 1f,
          preparedIds = it.preparedIds + game.id,
        )
      }
    }
  }

  fun clearSelectedResource() {
    val game = _uiState.value.selectedGame ?: return
    if (_uiState.value.isPreparingResource) return
    repository.clearResource(game.id)
    _uiState.update {
      it.copy(
        prepareProgress = 0f,
        preparedIds = it.preparedIds - game.id,
      )
    }
  }

  class Factory(private val repository: AppRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return MainViewModel(repository) as T
    }
  }
}
