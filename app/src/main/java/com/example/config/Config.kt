package com.example.config

import org.json.JSONObject

data class GameProfile(
  val id: String,
  val name: String,
  val packageName: String,
  val edition: String,
  val resourceSizeMb: Double,
  val enabled: Boolean = true,
)

data class RemoteVersionConfig(
  val displayVersion: String = "1.0.0",
  val latestBuild: Int = 1,
  val updateBannerEnabled: Boolean = false,
  val updateTitle: String = "Update Ready",
  val updateMessage: String = "",
  val updateUrl: String = "",
)

data class RemoteMaintenanceConfig(
  val enabled: Boolean = false,
  val title: String = "Maintenance",
  val message: String = "Remote services are temporarily paused.",
)

data class RemoteAnnouncementConfig(
  val enabled: Boolean = false,
  val title: String = "",
  val message: String = "",
  val severity: String = "info",
)

data class RemoteUiConfig(
  val accentColorHex: String = "#FF4D8D",
  val sparkleBackdropDefault: Boolean = true,
  val showQuickStats: Boolean = true,
  val showOverlaySection: Boolean = true,
)

data class RemoteAppConfig(
  val schemaVersion: Int = 1,
  val updatedAt: String = "",
  val appTitle: String = "Codigos Client",
  val version: RemoteVersionConfig = RemoteVersionConfig(),
  val maintenance: RemoteMaintenanceConfig = RemoteMaintenanceConfig(),
  val announcement: RemoteAnnouncementConfig = RemoteAnnouncementConfig(),
  val ui: RemoteUiConfig = RemoteUiConfig(),
  val games: List<GameProfile> = emptyList(),
)

data class RootStatusResult(
  val isRootGranted: Boolean,
  val binaryDetected: Boolean,
  val detectedPath: String?,
  val providerHint: String,
)

data class DeviceProfileInfo(
  val rawHwid: String,
  val maskedHwid: String,
  val androidVersion: String,
  val sdkLevel: Int,
  val deviceModel: String,
  val supportedAbis: String,
  val ramSummary: String,
  val storageSummary: String,
  val isEmulatorDetected: Boolean,
)

data class UserSettings(
  val secureMode: Boolean = false,
  val emulatorMode: Boolean = false,
  val autoCheckRoot: Boolean = true,
  val sparkleBackdrop: Boolean = true,
)

object ConfigParser {
  fun parseRemoteConfig(jsonString: String): RemoteAppConfig {
    val root = JSONObject(jsonString)
    val versionObj = root.optJSONObject("version")
    val maintObj = root.optJSONObject("maintenance")
    val annObj = root.optJSONObject("announcement")
    val uiObj = root.optJSONObject("ui")
    val gamesArray = root.optJSONArray("games")

    val games = buildList {
      if (gamesArray != null) {
        for (i in 0 until gamesArray.length()) {
          val item = gamesArray.optJSONObject(i) ?: continue
          add(
            GameProfile(
              id = item.optString("id", "profile_$i"),
              name = item.optString("name", "Profile $i"),
              packageName = item.optString("packageName", ""),
              edition = item.optString("edition", "Standard"),
              resourceSizeMb = item.optDouble("resourceSizeMb", 12.0),
              enabled = item.optBoolean("enabled", true),
            )
          )
        }
      }
    }

    val rawAccent = uiObj?.optString("accentColorHex", "#FF4D8D") ?: "#FF4D8D"
    val safeAccent =
      if (Regex("^#[0-9A-Fa-f]{6}$").matches(rawAccent)) rawAccent else "#FF4D8D"

    return RemoteAppConfig(
      schemaVersion = root.optInt("schemaVersion", 1),
      updatedAt = root.optString("updatedAt", ""),
      appTitle = root.optString("appTitle", "Codigos Client").ifBlank { "Codigos Client" },
      version =
        RemoteVersionConfig(
          displayVersion = versionObj?.optString("displayVersion", "1.0.0") ?: "1.0.0",
          latestBuild = versionObj?.optInt("latestBuild", 1) ?: 1,
          updateBannerEnabled = versionObj?.optBoolean("updateBannerEnabled", false) ?: false,
          updateTitle = versionObj?.optString("updateTitle", "Update Ready") ?: "Update Ready",
          updateMessage = versionObj?.optString("updateMessage", "") ?: "",
          updateUrl = versionObj?.optString("updateUrl", "") ?: "",
        ),
      maintenance =
        RemoteMaintenanceConfig(
          enabled = maintObj?.optBoolean("enabled", false) ?: false,
          title = maintObj?.optString("title", "Maintenance") ?: "Maintenance",
          message =
            maintObj?.optString("message", "Remote services are temporarily paused.")
              ?: "Remote services are temporarily paused.",
        ),
      announcement =
        RemoteAnnouncementConfig(
          enabled = annObj?.optBoolean("enabled", false) ?: false,
          title = annObj?.optString("title", "") ?: "",
          message = annObj?.optString("message", "") ?: "",
          severity = annObj?.optString("severity", "info") ?: "info",
        ),
      ui =
        RemoteUiConfig(
          accentColorHex = safeAccent,
          sparkleBackdropDefault = uiObj?.optBoolean("sparkleBackdropDefault", true) ?: true,
          showQuickStats = uiObj?.optBoolean("showQuickStats", true) ?: true,
          showOverlaySection = uiObj?.optBoolean("showOverlaySection", true) ?: true,
        ),
      games = games,
    )
  }
}
