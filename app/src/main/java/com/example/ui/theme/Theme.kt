package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun parseAccentColorOrDefault(hex: String): Color {
  return runCatching { Color(android.graphics.Color.parseColor(hex)) }
    .getOrDefault(RosePrimary)
}

@Composable
fun CodigosTheme(
  accentHex: String = "#FF4D8D",
  content: @Composable () -> Unit,
) {
  val accent = parseAccentColorOrDefault(accentHex)
  val colorScheme =
    lightColorScheme(
      primary = accent,
      onPrimary = WhitePure,
      primaryContainer = PinkContainer,
      onPrimaryContainer = OnPinkContainer,
      secondary = RoseSecondary,
      onSecondary = WhitePure,
      secondaryContainer = PetalSurfaceVariant,
      onSecondaryContainer = BerryTextPrimary,
      background = BlossomBackground,
      onBackground = BerryTextPrimary,
      surface = WhitePure,
      onSurface = BerryTextPrimary,
      surfaceVariant = PetalSurfaceVariant,
      onSurfaceVariant = PlumTextSecondary,
      outline = PetalOutline,
      error = StrawberryError,
      onError = WhitePure,
      errorContainer = StrawberryContainer,
      onErrorContainer = OnStrawberryContainer,
    )

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
