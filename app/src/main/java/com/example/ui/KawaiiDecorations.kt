package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.config.RemoteAnnouncementConfig
import com.example.config.RemoteMaintenanceConfig
import com.example.ui.theme.BlossomBackground
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.RoseSecondary
import com.example.ui.theme.StrawberryContainer
import com.example.ui.theme.StrawberryError
import com.example.ui.theme.WhitePure

@Composable
fun KawaiiBackdrop(
  enableSparkles: Boolean,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "kawaii_sparkles")
  val pulse by
    infiniteTransition.animateFloat(
      initialValue = 0.45f,
      targetValue = 1.0f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2400),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "sparkle_pulse",
    )

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(BlossomBackground, Color(0xFFFFF1F7), BlossomBackground)
          )
        )
        .drawBehind {
          drawCircle(
            color = PinkContainer.copy(alpha = 0.55f),
            radius = size.width * 0.42f,
            center = Offset(size.width * 0.88f, size.height * 0.08f),
          )
          drawCircle(
            color = PinkContainer.copy(alpha = 0.38f),
            radius = size.width * 0.34f,
            center = Offset(size.width * 0.10f, size.height * 0.86f),
          )

          if (enableSparkles) {
            val sparklePoints =
              listOf(
                Triple(0.14f, 0.16f, 10.dp.toPx()),
                Triple(0.84f, 0.24f, 12.dp.toPx()),
                Triple(0.22f, 0.54f, 8.dp.toPx()),
                Triple(0.88f, 0.68f, 11.dp.toPx()),
                Triple(0.12f, 0.82f, 9.dp.toPx()),
              )
            sparklePoints.forEachIndexed { index, (fx, fy, radius) ->
              val alpha = if (index % 2 == 0) pulse * 0.55f else (1.35f - pulse) * 0.45f
              drawFourPointSparkle(
                center = Offset(size.width * fx, size.height * fy),
                radius = radius,
                color = RoseSecondary.copy(alpha = alpha.coerceIn(0.15f, 0.65f)),
              )
            }
          }
        }
  ) {
    content()
  }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFourPointSparkle(
  center: Offset,
  radius: Float,
  color: Color,
) {
  val inner = radius * 0.28f
  val path =
    Path().apply {
      moveTo(center.x, center.y - radius)
      quadraticTo(center.x + inner, center.y - inner, center.x + radius, center.y)
      quadraticTo(center.x + inner, center.y + inner, center.x, center.y + radius)
      quadraticTo(center.x - inner, center.y + inner, center.x - radius, center.y)
      quadraticTo(center.x - inner, center.y - inner, center.x, center.y - radius)
      close()
    }
  drawPath(path = path, color = color)
}

@Composable
fun KawaiiEmblemAvatar(size: Dp = 52.dp, modifier: Modifier = Modifier) {
  Surface(
    shape = CircleShape,
    color = WhitePure,
    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
    modifier = modifier.size(size),
  ) {
    Image(
      painter = painterResource(id = R.drawable.ic_codigos_logo),
      contentDescription = stringResource(R.string.cd_app_emblem),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().clip(CircleShape),
    )
  }
}

@Composable
fun GameIconAvatar(gameId: String, size: Dp = 54.dp, modifier: Modifier = Modifier) {
  val resId =
    when (gameId) {
      "free_fire_max" -> R.drawable.ic_game_ffmax
      else -> R.drawable.ic_game_ff
    }
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = PinkContainer,
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.size(size),
  ) {
    Image(
      painter = painterResource(id = resId),
      contentDescription = stringResource(R.string.cd_game_icon),
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
    )
  }
}

@Composable
fun PastelStatusChip(
  label: String,
  isPositive: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(999.dp),
    color = if (isPositive) MintContainer else MaterialTheme.colorScheme.surfaceVariant,
    border =
      BorderStroke(
        1.dp,
        if (isPositive) MintSuccess.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outline,
      ),
    modifier = modifier,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (isPositive) MintSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
    )
  }
}

@Composable
fun AnnouncementBannerCard(
  announcement: RemoteAnnouncementConfig,
  onDismiss: () -> Unit,
) {
  val isWarning = announcement.severity == "warning"
  val containerColor = if (isWarning) StrawberryContainer else PinkContainer
  val accentColor = if (isWarning) StrawberryError else MaterialTheme.colorScheme.primary

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth().testTag("announcement_banner"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = if (isWarning) Icons.Rounded.WarningAmber else Icons.Rounded.Campaign,
        contentDescription = null,
        tint = accentColor,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (announcement.title.isNotBlank()) {
          Text(
            text = announcement.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        if (announcement.message.isNotBlank()) {
          Text(
            text = announcement.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      IconButton(
        onClick = onDismiss,
        modifier = Modifier.minimumInteractiveComponentSize().testTag("dismiss_announcement_btn"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = stringResource(R.string.btn_close),
          tint = accentColor,
        )
      }
    }
  }
}

@Composable
fun MaintenanceOverlayScreen(
  maintenance: RemoteMaintenanceConfig,
  isSyncing: Boolean,
  onSync: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = WhitePure),
      border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth().testTag("maintenance_card"),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        KawaiiEmblemAvatar(size = 76.dp)
        Text(
          text = maintenance.title.ifBlank { "Maintenance" },
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
        )
        if (maintenance.message.isNotBlank()) {
          Text(
            text = maintenance.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onSync,
          enabled = !isSyncing,
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("maintenance_sync_button"),
        ) {
          if (isSyncing) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = WhitePure,
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.Sync,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text =
              if (isSyncing) stringResource(R.string.btn_syncing)
              else stringResource(R.string.btn_sync)
          )
        }
      }
    }
  }
}
