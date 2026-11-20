package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.OverlayBadgeService
import com.example.R
import com.example.config.GameProfile
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.WhitePure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDetailScreen(
  game: GameProfile,
  isPrepared: Boolean,
  isPreparing: Boolean,
  progress: Float,
  canDrawOverlays: Boolean,
  isOverlayRunning: Boolean,
  showOverlaySection: Boolean,
  sparkleBackdrop: Boolean,
  onBack: () -> Unit,
  onPrepareResource: () -> Unit,
  onClearResource: () -> Unit,
  onSetOverlayRunning: (Boolean) -> Unit,
  onRefreshOverlayState: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current
  val accent = MaterialTheme.colorScheme.primary

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            KawaiiEmblemAvatar(size = 32.dp)
            Text(text = game.name)
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("detail_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.cd_back),
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = WhitePure,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = accent,
          ),
      )
    },
  ) { innerPadding ->
    KawaiiBackdrop(
      enableSparkles = sparkleBackdrop,
      modifier = Modifier.padding(innerPadding),
    ) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
          modifier =
            Modifier.widthIn(max = 600.dp)
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = WhitePure),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(18.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              GameIconAvatar(gameId = game.id, size = 64.dp)
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  PastelStatusChip(label = game.edition, isPositive = false)
                  PastelStatusChip(label = "${game.resourceSizeMb} MB", isPositive = isPrepared)
                }
                Text(
                  text = game.name,
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = game.packageName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = WhitePure),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(
                text = "Resource Bundle",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )

              if (isPreparing || progress > 0f) {
                LinearProgressIndicator(
                  progress = { progress },
                  modifier = Modifier.fillMaxWidth().height(10.dp),
                  color = accent,
                  trackColor = PinkContainer,
                  strokeCap = StrokeCap.Round,
                )
              }

              if (isPrepared && !isPreparing) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MintSuccess,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = stringResource(R.string.resource_ready_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MintSuccess,
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = onPrepareResource,
                  enabled = !isPreparing,
                  shape = RoundedCornerShape(999.dp),
                  modifier =
                    Modifier.weight(1f)
                      .minimumInteractiveComponentSize()
                      .testTag("download_resource_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text =
                      if (isPreparing) stringResource(R.string.btn_preparing_resource)
                      else stringResource(R.string.btn_prepare_resource)
                  )
                }

                if (isPrepared && !isPreparing) {
                  OutlinedButton(
                    onClick = onClearResource,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier =
                      Modifier.minimumInteractiveComponentSize().testTag("clear_resource_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.DeleteOutline,
                      contentDescription = stringResource(R.string.btn_clear_resource),
                      tint = accent,
                    )
                  }
                }
              }
            }
          }

          if (showOverlaySection) {
            Card(
              shape = RoundedCornerShape(22.dp),
              colors = CardDefaults.cardColors(containerColor = WhitePure),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = accent,
                  )
                  Text(
                    text =
                      if (canDrawOverlays) stringResource(R.string.overlay_granted)
                      else stringResource(R.string.overlay_not_granted),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canDrawOverlays) MintSuccess else MaterialTheme.colorScheme.onSurface,
                  )
                }

                if (canDrawOverlays) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      KawaiiEmblemAvatar(size = 34.dp)
                      Text(
                        text = stringResource(R.string.overlay_badge_toggle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                    }
                    Switch(
                      checked = isOverlayRunning,
                      onCheckedChange = { enable ->
                        val svcIntent = Intent(context, OverlayBadgeService::class.java)
                        if (enable) {
                          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(svcIntent)
                          } else {
                            context.startService(svcIntent)
                          }
                        } else {
                          context.stopService(svcIntent)
                        }
                        onSetOverlayRunning(enable)
                      },
                      colors =
                        SwitchDefaults.colors(
                          checkedThumbColor = WhitePure,
                          checkedTrackColor = accent,
                          uncheckedThumbColor = accent,
                          uncheckedTrackColor = PinkContainer,
                          uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                      modifier = Modifier.testTag("switch_overlay_badge"),
                    )
                  }
                }

                OutlinedButton(
                  onClick = {
                    val intent =
                      Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                      )
                    context.startActivity(intent)
                    onRefreshOverlayState()
                  },
                  shape = RoundedCornerShape(999.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                  modifier =
                    Modifier.fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .testTag("overlay_permission_button"),
                ) {
                  Text(text = stringResource(R.string.btn_overlay_permission))
                }
              }
            }
          }
        }
      }
    }
  }
}
