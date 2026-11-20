package com.example.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import com.example.config.DeviceProfileInfo
import com.example.config.GameProfile
import com.example.config.RootStatusResult
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object RootUtils {
  private val standardSuPaths =
    listOf(
      "/system/bin/su",
      "/system/xbin/su",
      "/sbin/su",
      "/su/bin/su",
      "/vendor/bin/su",
      "/data/local/su",
      "/data/local/bin/su",
      "/data/local/xbin/su",
    )

  fun findSuBinary(paths: List<String> = standardSuPaths): String? {
    return paths.firstOrNull { path ->
      runCatching { File(path).exists() }.getOrDefault(false)
    }
  }

  fun inferProviderHint(detectedPath: String?): String {
    if (detectedPath == null) return "Not Detected"
    return when {
      detectedPath.contains("/sbin/") || detectedPath.contains("/su/bin/") ->
        "Magisk / KernelSU / SuperSU"
      else -> "su binary"
    }
  }

  suspend fun checkRootAccess(): RootStatusResult =
    withContext(Dispatchers.IO) {
      val detectedPath = findSuBinary()
      val isGranted = executeSuIdentityCheck()

      RootStatusResult(
        isRootGranted = isGranted,
        binaryDetected = detectedPath != null || isGranted,
        detectedPath = detectedPath,
        providerHint =
          if (isGranted) "Granted (${inferProviderHint(detectedPath)})"
          else inferProviderHint(detectedPath),
      )
    }

  private fun executeSuIdentityCheck(): Boolean {
    return runCatching {
        val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
        val completed = process.waitFor(1500, TimeUnit.MILLISECONDS)
        if (!completed) {
          process.destroyForcibly()
          false
        } else {
          val output = process.inputStream.bufferedReader().use { it.readText() }
          process.exitValue() == 0 && output.contains("uid=0")
        }
      }
      .getOrDefault(false)
  }
}

object DeviceUtils {
  fun sha256Short(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.take(12).joinToString("") { "%02X".format(it) }
  }

  fun maskIdentifier(value: String): String {
    if (value.length <= 8) return "********"
    return "${value.take(4)}********${value.takeLast(4)}"
  }

  fun formatBytesGb(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f GB", gb)
  }

  fun isEmulatorEnvironment(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val brand = Build.BRAND.lowercase()
    val hardware = Build.HARDWARE.lowercase()
    return fingerprint.contains("generic") ||
      fingerprint.contains("emulator") ||
      model.contains("sdk") ||
      model.contains("emulator") ||
      hardware.contains("goldfish") ||
      hardware.contains("ranchu") ||
      brand.startsWith("generic")
  }

  private fun readRamSummary(context: Context): String {
    return runCatching {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        if (am != null) {
          am.getMemoryInfo(info)
          "${formatBytesGb(info.availMem)} / ${formatBytesGb(info.totalMem)}"
        } else {
          "Unavailable"
        }
      }
      .getOrDefault("Unavailable")
  }

  private fun readStorageSummary(): String {
    return runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeBytes = stat.availableBlocksLong * stat.blockSizeLong
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        "${formatBytesGb(freeBytes)} / ${formatBytesGb(totalBytes)}"
      }
      .getOrDefault("Unavailable")
  }

  @SuppressLint("HardwareIds")
  fun readDeviceProfile(context: Context): DeviceProfileInfo {
    val androidId =
      runCatching {
          Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }
        .getOrNull()
        ?.takeIf { it.isNotBlank() } ?: "UNAVAILABLE"

    val rawFingerprint = "${androidId}_${Build.BRAND}_${Build.BOARD}_${Build.MODEL}"
    val hwid = sha256Short(rawFingerprint)

    return DeviceProfileInfo(
      rawHwid = hwid,
      maskedHwid = maskIdentifier(hwid),
      androidVersion = "Android ${Build.VERSION.RELEASE ?: "Unknown"}",
      sdkLevel = Build.VERSION.SDK_INT,
      deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
      supportedAbis = Build.SUPPORTED_ABIS.joinToString(", ").ifBlank { "Unknown" },
      ramSummary = readRamSummary(context),
      storageSummary = readStorageSummary(),
      isEmulatorDetected = isEmulatorEnvironment(),
    )
  }
}

object ResourceUtils {
  fun isResourcePrepared(context: Context, profileId: String): Boolean {
    val dir = File(context.filesDir, "profiles")
    return File(dir, "$profileId.json").exists()
  }

  fun clearLocalResource(context: Context, profileId: String): Boolean {
    val file = File(File(context.filesDir, "profiles"), "$profileId.json")
    return if (file.exists()) file.delete() else false
  }

  suspend fun prepareLocalResource(
    context: Context,
    profile: GameProfile,
    onProgress: (Float) -> Unit,
  ): File =
    withContext(Dispatchers.IO) {
      val steps = 10
      for (step in 1..steps) {
        delay(80L)
        onProgress(step / steps.toFloat())
      }
      val dir = File(context.filesDir, "profiles").apply { mkdirs() }
      val targetFile = File(dir, "${profile.id}.json")
      val payload =
        """
        {
          "id": "${profile.id}",
          "name": "${profile.name}",
          "packageName": "${profile.packageName}",
          "edition": "${profile.edition}",
          "verifiedAtEpochMs": ${System.currentTimeMillis()}
        }
        """
          .trimIndent()
      targetFile.writeText(payload)
      targetFile
    }
}
