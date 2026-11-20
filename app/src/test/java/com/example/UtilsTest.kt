package com.example

import com.example.config.ConfigParser
import com.example.utils.DeviceUtils
import com.example.utils.RootUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UtilsTest {
  @Test
  fun parseRemoteConfig_parsesRemoteFieldsAndGamesCorrectly() {
    val json =
      """
      {
        "schemaVersion": 1,
        "appTitle": "Codigos Client",
        "version": {
          "displayVersion": "1.2.0",
          "latestBuild": 2,
          "updateBannerEnabled": true,
          "updateTitle": "Update Ready",
          "updateMessage": "New config",
          "updateUrl": "https://example.com"
        },
        "maintenance": {
          "enabled": false,
          "title": "Maintenance",
          "message": "Paused"
        },
        "announcement": {
          "enabled": true,
          "title": "Notice",
          "message": "Hello",
          "severity": "info"
        },
        "ui": {
          "accentColorHex": "#FF4D8D",
          "sparkleBackdropDefault": true,
          "showQuickStats": false,
          "showOverlaySection": true
        },
        "games": [
          {
            "id": "free_fire",
            "name": "Free Fire",
            "packageName": "com.dts.freefireth",
            "edition": "Standard",
            "resourceSizeMb": 18.4,
            "enabled": true
          }
        ]
      }
      """
        .trimIndent()

    val config = ConfigParser.parseRemoteConfig(json)
    assertEquals("Codigos Client", config.appTitle)
    assertEquals("1.2.0", config.version.displayVersion)
    assertTrue(config.version.updateBannerEnabled)
    assertTrue(config.announcement.enabled)
    assertEquals("#FF4D8D", config.ui.accentColorHex)
    assertEquals(1, config.games.size)
    assertEquals("Free Fire", config.games.first().name)
  }

  @Test
  fun maskIdentifier_hidesMiddleCharacters() {
    val masked = DeviceUtils.maskIdentifier("ABCDEF1234567890")
    assertTrue(masked.startsWith("ABCD"))
    assertTrue(masked.endsWith("7890"))
    assertTrue(masked.contains("********"))
  }

  @Test
  fun findSuBinary_returnsNullWhenPathsMissing() {
    val result = RootUtils.findSuBinary(listOf("/nonexistent/path/su"))
    assertNull(result)
  }
}
