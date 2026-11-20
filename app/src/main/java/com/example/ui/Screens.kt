package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.config.GameProfile
import com.example.config.RemoteVersionConfig
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.StrawberryContainer
import com.example.ui.theme.StrawberryError
import com.example.ui.theme.WhitePure

@Composable
fun GamesScreen(
  uiState: MainUiState,
  onRecheckRoot: () -> Unit,
  onOpenRootInfo: () -> Unit,
  onDismissAnnouncement: () -> Unit,
  onSelectGame: (GameProfile) -> Unit,
  modifier: Modifier = Modifier,
) {
  val visibleGames = uiState.remoteConfig.games.filter { it.enabled }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        HeroHeaderCard(
          appTitle = uiState.remoteConfig.appTitle,
          displayVersion = uiState.remoteConfig.version.displayVersion,
          isRootGranted = uiState.rootStatus.isRootGranted,
          isCheckingRoot = uiState.isCheckingRoot,
          secureMode = uiState.settings.secureMode,
          preparedCount = uiState.preparedIds.size,
          totalCount = visibleGames.size,
          onRecheckRoot = onRecheckRoot,
        )
      }

      if (uiState.remoteConfig.announcement.enabled && !uiState.announcementDismissed) {
        item {
          AnnouncementBannerCard(
            announcement = uiState.remoteConfig.announcement,
            onDismiss = onDismissAnnouncement,
          )
        }
      }

      if (uiState.remoteConfig.version.updateBannerEnabled) {
        item {
          RemoteUpdateBanner(version = uiState.remoteConfig.version)
        }
      }

      item {
        AnimatedVisibility(
          visible = !uiState.isCheckingRoot && !uiState.rootStatus.isRootGranted,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically(),
        ) {
          RootRequiredBanner(
            onInfoClick = onOpenRootInfo,
            onRecheckClick = onRecheckRoot,
          )
        }
      }

      if (uiState.remoteConfig.ui.showQuickStats && uiState.deviceProfile != null) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            QuickStatPill(
              icon = Icons.Rounded.Memory,
              label = stringResource(R.string.profile_ram_label),
              value = uiState.deviceProfile.ramSummary,
              modifier = Modifier.weight(1f),
            )
            QuickStatPill(
              icon = Icons.Rounded.Storage,
              label = stringResource(R.string.profile_storage_label),
              value = uiState.deviceProfile.storageSummary,
              modifier = Modifier.weight(1f),
            )
          }
        }
      }

      item {
        Text(
          text = stringResource(R.string.games_section_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      if (uiState.isLoadingCatalog) {
        item {
          Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        }
      } else {
        items(visibleGames, key = { it.id }) { game ->
          GameProfileCard(
            game = game,
            isPrepared = uiState.preparedIds.contains(game.id),
            onClick = { onSelectGame(game) },
          )
        }
      }
    }
  }
}

@Composable
private fun HeroHeaderCard(
  appTitle: String,
  displayVersion: String,
  isRootGranted: Boolean,
  isCheckingRoot: Boolean,
  secureMode: Boolean,
  preparedCount: Int,
  totalCount: Int,
  onRecheckRoot: () -> Unit,
) {
  val accent = MaterialTheme.colorScheme.primary
  val gradientBrush = Brush.linearGradient(colors = listOf(PinkContainer, WhitePure))

  Card(
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    colors = CardDefaults.cardColors(containerColor = WhitePure),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(modifier = Modifier.background(gradientBrush).padding(18.dp)) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            KawaiiEmblemAvatar(size = 52.dp)
            Column {
              Text(
                text = appTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text =
                  if (isCheckingRoot) "Checking Root"
                  else if (isRootGranted) "${stringResource(R.string.root_granted_title)} • v$displayVersion"
                  else "Standard Mode • v$displayVersion",
                style = MaterialTheme.typography.labelMedium,
                color = if (isRootGranted) MintSuccess else accent,
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = WhitePure,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier =
              Modifier.size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onRecheckRoot)
                .testTag("recheck_root_icon_button"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              if (isCheckingRoot) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp,
                  color = accent,
                )
              } else {
                Icon(
                  imageVector =
                    if (isRootGranted) Icons.Rounded.VerifiedUser
                    else Icons.Rounded.Refresh,
                  contentDescription = stringResource(R.string.btn_recheck),
                  tint = if (isRootGranted) MintSuccess else accent,
                )
              }
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          PastelStatusChip(
            label = if (isRootGranted) "Root: Active" else "Root: None",
            isPositive = isRootGranted,
          )
          PastelStatusChip(
            label = if (secureMode) "Secure: On" else "Secure: Off",
            isPositive = secureMode,
          )
          PastelStatusChip(
            label = "Ready: $preparedCount/$totalCount",
            isPositive = totalCount > 0 && preparedCount == totalCount,
          )
        }
      }
    }
  }
}

@Composable
private fun RemoteUpdateBanner(version: RemoteVersionConfig) {
  val context = LocalContext.current
  val accent = MaterialTheme.colorScheme.primary
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = WhitePure),
    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth().testTag("remote_update_banner"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.SystemUpdate,
        contentDescription = null,
        tint = accent,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = "${version.updateTitle} (v${version.displayVersion})",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (version.updateMessage.isNotBlank()) {
          Text(
            text = version.updateMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      if (version.updateUrl.startsWith("https://")) {
        Button(
          onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(version.updateUrl)))
          },
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(text = stringResource(R.string.btn_open_update))
        }
      }
    }
  }
}

@Composable
private fun QuickStatPill(
  icon: ImageVector,
  label: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  val accent = MaterialTheme.colorScheme.primary
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = WhitePure),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = PinkContainer,
        modifier = Modifier.size(32.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = value,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
fun RootRequiredBanner(
  onInfoClick: () -> Unit,
  onRecheckClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = StrawberryContainer),
    border = BorderStroke(1.5.dp, StrawberryError.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth().testTag("root_required_banner"),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        Icon(
          imageVector = Icons.Rounded.ErrorOutline,
          contentDescription = stringResource(R.string.cd_root_warning),
          tint = StrawberryError,
        )
        Text(
          text = stringResource(R.string.root_required_title),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onErrorContainer,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onInfoClick,
          shape = RoundedCornerShape(999.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = StrawberryError,
              contentColor = WhitePure,
            ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("root_info_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = stringResource(R.string.btn_info))
        }

        OutlinedButton(
          onClick = onRecheckClick,
          shape = RoundedCornerShape(999.dp),
          border = BorderStroke(1.dp, StrawberryError.copy(alpha = 0.55f)),
          colors =
            ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.minimumInteractiveComponentSize().testTag("recheck_root_button"),
        ) {
          Text(text = stringResource(R.string.btn_recheck))
        }
      }
    }
  }
}

@Composable
private fun GameProfileCard(
  game: GameProfile,
  isPrepared: Boolean,
  onClick: () -> Unit,
) {
  val accent = MaterialTheme.colorScheme.primary
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = WhitePure),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .clickable(onClick = onClick)
        .testTag("game_card_${game.id}"),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      GameIconAvatar(gameId = game.id, size = 54.dp)

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = game.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (isPrepared) {
            Icon(
              imageVector = Icons.Rounded.CheckCircle,
              contentDescription = stringResource(R.string.resource_ready_label),
              tint = MintSuccess,
              modifier = Modifier.size(16.dp),
            )
          }
        }
        Text(
          text = "${game.edition} • ${game.resourceSizeMb} MB",
          style = MaterialTheme.typography.labelMedium,
          color = accent,
        )
        Text(
          text = game.packageName,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = accent,
      )
    }
  }
}

@Composable
fun ProfileScreen(uiState: MainUiState, modifier: Modifier = Modifier) {
  val device = uiState.deviceProfile
  val secureMode = uiState.settings.secureMode
  val emulatorMode = uiState.settings.emulatorMode
  val clipboardManager = LocalClipboardManager.current
  var copiedRecently by remember { mutableStateOf(false) }
  val accent = MaterialTheme.colorScheme.primary

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = WhitePure),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier =
              Modifier.background(Brush.horizontalGradient(listOf(PinkContainer, WhitePure)))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            KawaiiEmblemAvatar(size = 54.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
              PastelStatusChip(
                label = if (uiState.rootStatus.isRootGranted) "Root Active" else "Standard User",
                isPositive = uiState.rootStatus.isRootGranted,
              )
            }
          }
        }
      }

      if (device != null) {
        item {
          val shownHwid = if (secureMode) device.maskedHwid else device.rawHwid
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WhitePure),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Security,
                contentDescription = null,
                tint = accent,
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text =
                    if (copiedRecently) stringResource(R.string.copied_label)
                    else stringResource(R.string.profile_hwid_label),
                  style = MaterialTheme.typography.labelMedium,
                  color =
                    if (copiedRecently) MintSuccess
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = shownHwid,
                  style = MaterialTheme.typography.titleMedium,
                  color = accent,
                )
              }
              IconButton(
                onClick = {
                  clipboardManager.setText(AnnotatedString(shownHwid))
                  copiedRecently = true
                },
                modifier = Modifier.minimumInteractiveComponentSize().testTag("copy_hwid_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.ContentCopy,
                  contentDescription = stringResource(R.string.cd_copy_hwid),
                  tint = accent,
                )
              }
            }
          }
        }
        item {
          TelemetryRowCard(
            icon = Icons.Rounded.PhoneAndroid,
            label = stringResource(R.string.profile_android_label),
            value = "${device.androidVersion} • API ${device.sdkLevel}",
          )
        }
        item {
          TelemetryRowCard(
            icon = Icons.Rounded.Memory,
            label = stringResource(R.string.profile_model_label),
            value = device.deviceModel,
          )
        }
        item {
          TelemetryRowCard(
            icon = Icons.Rounded.Memory,
            label = stringResource(R.string.profile_abi_label),
            value = device.supportedAbis,
          )
        }
        item {
          TelemetryRowCard(
            icon = Icons.Rounded.Memory,
            label = stringResource(R.string.profile_ram_label),
            value = device.ramSummary,
          )
        }
        item {
          TelemetryRowCard(
            icon = Icons.Rounded.Storage,
            label = stringResource(R.string.profile_storage_label),
            value = device.storageSummary,
          )
        }
        item {
          TelemetryRowCard(
            icon = Icons.Rounded.VerifiedUser,
            label = stringResource(R.string.profile_root_label),
            value = uiState.rootStatus.providerHint,
          )
        }
        if (emulatorMode) {
          item {
            TelemetryRowCard(
              icon = Icons.Rounded.Info,
              label = stringResource(R.string.profile_env_label),
              value = if (device.isEmulatorDetected) "Emulator" else "Physical Device",
              highlightAccent = true,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TelemetryRowCard(
  icon: ImageVector,
  label: String,
  value: String,
  highlightAccent: Boolean = false,
) {
  val accent = MaterialTheme.colorScheme.primary
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = WhitePure),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = accent,
      )
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = value,
          style = MaterialTheme.typography.titleMedium,
          color = if (highlightAccent) accent else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
fun SettingsScreen(
  uiState: MainUiState,
  onToggleSecureMode: (Boolean) -> Unit,
  onToggleEmulatorMode: (Boolean) -> Unit,
  onToggleAutoCheckRoot: (Boolean) -> Unit,
  onToggleSparkles: (Boolean) -> Unit,
  onRemoteUrlChange: (String) -> Unit,
  onSyncRemote: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = MaterialTheme.colorScheme.primary
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text(
          text = stringResource(R.string.settings_title),
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = WhitePure),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = stringResource(R.string.remote_config_section),
              style = MaterialTheme.typography.titleMedium,
              color = accent,
            )
            OutlinedTextField(
              value = uiState.remoteConfigUrl,
              onValueChange = onRemoteUrlChange,
              label = { Text(stringResource(R.string.remote_url_label)) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("remote_url_input"),
            )
            if (uiState.syncStatusMessage.isNotBlank()) {
              Text(
                text = uiState.syncStatusMessage,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Button(
              onClick = onSyncRemote,
              enabled = !uiState.isSyncingRemote,
              shape = RoundedCornerShape(999.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .testTag("settings_sync_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  if (uiState.isSyncingRemote) stringResource(R.string.btn_syncing)
                  else stringResource(R.string.btn_sync)
              )
            }
          }
        }
      }

      item {
        SettingToggleCard(
          title = stringResource(R.string.setting_secure_mode),
          checked = uiState.settings.secureMode,
          onCheckedChange = onToggleSecureMode,
          testTag = "switch_secure_mode",
        )
      }

      item {
        SettingToggleCard(
          title = stringResource(R.string.setting_emulator_mode),
          checked = uiState.settings.emulatorMode,
          onCheckedChange = onToggleEmulatorMode,
          testTag = "switch_emulator_mode",
        )
      }

      item {
        SettingToggleCard(
          title = stringResource(R.string.setting_auto_root),
          checked = uiState.settings.autoCheckRoot,
          onCheckedChange = onToggleAutoCheckRoot,
          testTag = "switch_auto_root",
        )
      }

      item {
        SettingToggleCard(
          title = stringResource(R.string.setting_sparkles),
          checked = uiState.settings.sparkleBackdrop,
          onCheckedChange = onToggleSparkles,
          testTag = "switch_sparkles",
        )
      }

      item {
        Spacer(modifier = Modifier.height(4.dp))
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = WhitePure),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            KawaiiEmblemAvatar(size = 44.dp)
            Column {
              Text(
                text = stringResource(R.string.about_section_title),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
              )
              Text(
                text = "${uiState.remoteConfig.appTitle} v${uiState.remoteConfig.version.displayVersion}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingToggleCard(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  val accent = MaterialTheme.colorScheme.primary
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = WhitePure),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )

      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors =
          SwitchDefaults.colors(
            checkedThumbColor = WhitePure,
            checkedTrackColor = accent,
            uncheckedThumbColor = accent,
            uncheckedTrackColor = PinkContainer,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
          ),
        modifier = Modifier.testTag(testTag),
      )
    }
  }
}

@Composable
fun RootInfoDialog(onDismiss: () -> Unit, onRecheck: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = WhitePure,
    title = {
      Text(
        text = stringResource(R.string.root_info_dialog_title),
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      Text(
        text = stringResource(R.string.root_info_dialog_body),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    confirmButton = {
      Button(
        onClick = {
          onDismiss()
          onRecheck()
        },
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.testTag("dialog_recheck_button"),
      ) {
        Text(stringResource(R.string.btn_recheck))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("dialog_close_button"),
      ) {
        Text(stringResource(R.string.btn_close))
      }
    },
  )
}
