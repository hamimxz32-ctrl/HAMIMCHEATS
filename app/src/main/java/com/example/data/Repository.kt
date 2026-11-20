package com.example.data

import android.content.Context
import android.provider.Settings
import com.example.config.ConfigParser
import com.example.config.DeviceProfileInfo
import com.example.config.GameProfile
import com.example.config.RemoteAppConfig
import com.example.config.RootStatusResult
import com.example.config.UserSettings
import com.example.utils.DeviceUtils
import com.example.utils.ResourceUtils
import com.example.utils.RootUtils
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface RemoteSyncOutcome {
  data class Success(val config: RemoteAppConfig, val sourceLabel: String) : RemoteSyncOutcome
  data class Error(val message: String) : RemoteSyncOutcome
}

class AppRepository(private val appContext: Context) {
  private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(8, TimeUnit.SECONDS)
      .readTimeout(8, TimeUnit.SECONDS)
      .build()

  suspend fun loadInitialRemoteConfig(): RemoteAppConfig =
    withContext(Dispatchers.IO) {
      val cached = prefs.getString(KEY_CACHED_REMOTE_JSON, null)
      if (!cached.isNullOrBlank()) {
        runCatching { ConfigParser.parseRemoteConfig(cached) }.getOrNull()?.let { return@withContext it }
      }
      val bundled = appContext.assets.open("config.json").bufferedReader().use { it.readText() }
      ConfigParser.parseRemoteConfig(bundled)
    }

  suspend fun syncRemoteConfig(urlOverride: String? = null): RemoteSyncOutcome =
    withContext(Dispatchers.IO) {
      val targetUrl = (urlOverride ?: getRemoteConfigUrl()).trim()
      if (targetUrl.isBlank()) {
        val local = loadInitialRemoteConfig()
        return@withContext RemoteSyncOutcome.Success(local, "Local config")
      }
      if (!targetUrl.startsWith("https://")) {
        return@withContext RemoteSyncOutcome.Error("Remote URL must start with https://")
      }

      return@withContext runCatching {
          val request =
            Request.Builder()
              .url(targetUrl)
              .header("Accept", "application/json")
              .header("Cache-Control", "no-cache")
              .build()
          httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              return@runCatching RemoteSyncOutcome.Error("Sync failed (HTTP ${response.code})")
            }
            val body = response.body?.string().orEmpty()
            val parsed = ConfigParser.parseRemoteConfig(body)
            prefs.edit().putString(KEY_CACHED_REMOTE_JSON, body).apply()
            RemoteSyncOutcome.Success(parsed, "GitHub Remote")
          }
        }
        .getOrElse { err ->
          RemoteSyncOutcome.Error(err.message ?: "Unable to reach remote config")
        }
    }

  fun getRemoteConfigUrl(): String =
    prefs.getString(KEY_REMOTE_CONFIG_URL, DEFAULT_REMOTE_URL) ?: DEFAULT_REMOTE_URL

  fun saveRemoteConfigUrl(url: String) {
    prefs.edit().putString(KEY_REMOTE_CONFIG_URL, url.trim()).apply()
  }

  suspend fun checkRootStatus(): RootStatusResult = RootUtils.checkRootAccess()

  fun getDeviceProfile(): DeviceProfileInfo = DeviceUtils.readDeviceProfile(appContext)

  fun loadSettings(): UserSettings {
    return UserSettings(
      secureMode = prefs.getBoolean(KEY_SECURE_MODE, false),
      emulatorMode = prefs.getBoolean(KEY_EMULATOR_MODE, false),
      autoCheckRoot = prefs.getBoolean(KEY_AUTO_CHECK_ROOT, true),
      sparkleBackdrop = prefs.getBoolean(KEY_SPARKLE_BACKDROP, true),
    )
  }

  fun saveSettings(settings: UserSettings) {
    prefs
      .edit()
      .putBoolean(KEY_SECURE_MODE, settings.secureMode)
      .putBoolean(KEY_EMULATOR_MODE, settings.emulatorMode)
      .putBoolean(KEY_AUTO_CHECK_ROOT, settings.autoCheckRoot)
      .putBoolean(KEY_SPARKLE_BACKDROP, settings.sparkleBackdrop)
      .apply()
  }

  fun isResourcePrepared(profileId: String): Boolean =
    ResourceUtils.isResourcePrepared(appContext, profileId)

  fun clearResource(profileId: String): Boolean =
    ResourceUtils.clearLocalResource(appContext, profileId)

  suspend fun prepareResource(profile: GameProfile, onProgress: (Float) -> Unit) {
    ResourceUtils.prepareLocalResource(appContext, profile, onProgress)
  }

  fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(appContext)

  companion object {
    private const val PREFS_NAME = "codigos_client_prefs"
    private const val KEY_SECURE_MODE = "secure_mode"
    private const val KEY_EMULATOR_MODE = "emulator_mode"
    private const val KEY_AUTO_CHECK_ROOT = "auto_check_root"
    private const val KEY_SPARKLE_BACKDROP = "sparkle_backdrop"
    private const val KEY_REMOTE_CONFIG_URL = "remote_config_url"
    private const val KEY_CACHED_REMOTE_JSON = "cached_remote_json"
    private const val DEFAULT_REMOTE_URL = ""
  }
}
