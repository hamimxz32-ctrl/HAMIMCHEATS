package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GamesScreen
import com.example.ui.KawaiiBackdrop
import com.example.ui.KawaiiEmblemAvatar
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.MaintenanceOverlayScreen
import com.example.ui.MenuDetailScreen
import com.example.ui.ProfileScreen
import com.example.ui.RootInfoDialog
import com.example.ui.SettingsScreen
import com.example.ui.theme.CodigosTheme
import com.example.ui.theme.PinkContainer
import com.example.ui.theme.WhitePure

class MainActivity : ComponentActivity() {
  private var mainViewModel: MainViewModel? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
    )
    val repository = (application as App).repository

    setContent {
      val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(repository))
      mainViewModel = vm
      val uiState by vm.uiState.collectAsStateWithLifecycle()

      CodigosTheme(accentHex = uiState.remoteConfig.ui.accentColorHex) {
        if (uiState.remoteConfig.maintenance.enabled) {
          KawaiiBackdrop(enableSparkles = uiState.settings.sparkleBackdrop) {
            MaintenanceOverlayScreen(
              maintenance = uiState.remoteConfig.maintenance,
              isSyncing = uiState.isSyncingRemote,
              onSync = vm::syncRemoteConfig,
            )
          }
        } else {
          val selectedGame = uiState.selectedGame
          if (selectedGame != null) {
            MenuDetailScreen(
              game = selectedGame,
              isPrepared = uiState.preparedIds.contains(selectedGame.id),
              isPreparing = uiState.isPreparingResource,
              progress = uiState.prepareProgress,
              canDrawOverlays = uiState.canDrawOverlays,
              isOverlayRunning = uiState.isOverlayServiceRunning,
              showOverlaySection = uiState.remoteConfig.ui.showOverlaySection,
              sparkleBackdrop = uiState.settings.sparkleBackdrop,
              onBack = vm::closeGameDetail,
              onPrepareResource = vm::prepareSelectedResource,
              onClearResource = vm::clearSelectedResource,
              onSetOverlayRunning = vm::setOverlayRunning,
              onRefreshOverlayState = vm::refreshOverlayState,
            )
          } else {
            MainScaffold(
              appTitle = uiState.remoteConfig.appTitle,
              currentTab = uiState.currentTab,
              isSyncing = uiState.isSyncingRemote,
              sparkleBackdrop = uiState.settings.sparkleBackdrop,
              onSync = vm::syncRemoteConfig,
              onSelectTab = vm::selectTab,
              content = { innerPadding ->
                when (uiState.currentTab) {
                  MainTab.GAMES ->
                    GamesScreen(
                      uiState = uiState,
                      onRecheckRoot = vm::recheckRoot,
                      onOpenRootInfo = { vm.setRootInfoDialogVisible(true) },
                      onDismissAnnouncement = vm::dismissAnnouncement,
                      onSelectGame = vm::openGameDetail,
                      modifier = Modifier.padding(innerPadding),
                    )
                  MainTab.PROFILE ->
                    ProfileScreen(
                      uiState = uiState,
                      modifier = Modifier.padding(innerPadding),
                    )
                  MainTab.SETTINGS ->
                    SettingsScreen(
                      uiState = uiState,
                      onToggleSecureMode = vm::updateSecureMode,
                      onToggleEmulatorMode = vm::updateEmulatorMode,
                      onToggleAutoCheckRoot = vm::updateAutoCheckRoot,
                      onToggleSparkles = vm::updateSparkleBackdrop,
                      onRemoteUrlChange = vm::updateRemoteConfigUrl,
                      onSyncRemote = vm::syncRemoteConfig,
                      modifier = Modifier.padding(innerPadding),
                    )
                }
              },
            )
          }
        }

        if (uiState.showRootInfoDialog) {
          RootInfoDialog(
            onDismiss = { vm.setRootInfoDialogVisible(false) },
            onRecheck = vm::recheckRoot,
          )
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    mainViewModel?.refreshOverlayState()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
  appTitle: String,
  currentTab: MainTab,
  isSyncing: Boolean,
  sparkleBackdrop: Boolean,
  onSync: () -> Unit,
  onSelectTab: (MainTab) -> Unit,
  content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
  val accent = MaterialTheme.colorScheme.primary
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            KawaiiEmblemAvatar(size = 34.dp)
            Text(text = appTitle, color = accent)
          }
        },
        actions = {
          IconButton(
            onClick = onSync,
            enabled = !isSyncing,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("topbar_sync_button"),
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = accent,
              )
            } else {
              Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = stringResource(R.string.cd_sync),
                tint = accent,
              )
            }
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = WhitePure,
            titleContentColor = accent,
          ),
      )
    },
    bottomBar = {
      NavigationBar(containerColor = WhitePure) {
        NavigationBarItem(
          selected = currentTab == MainTab.GAMES,
          onClick = { onSelectTab(MainTab.GAMES) },
          icon = {
            Icon(
              imageVector =
                if (currentTab == MainTab.GAMES) Icons.Rounded.SportsEsports
                else Icons.Outlined.SportsEsports,
              contentDescription = stringResource(R.string.nav_games),
            )
          },
          label = { Text(stringResource(R.string.nav_games)) },
          colors = navItemColors(),
          modifier = Modifier.testTag("nav_tab_games"),
        )

        NavigationBarItem(
          selected = currentTab == MainTab.PROFILE,
          onClick = { onSelectTab(MainTab.PROFILE) },
          icon = {
            Icon(
              imageVector =
                if (currentTab == MainTab.PROFILE) Icons.Rounded.Person
                else Icons.Outlined.Person,
              contentDescription = stringResource(R.string.nav_profile),
            )
          },
          label = { Text(stringResource(R.string.nav_profile)) },
          colors = navItemColors(),
          modifier = Modifier.testTag("nav_tab_profile"),
        )

        NavigationBarItem(
          selected = currentTab == MainTab.SETTINGS,
          onClick = { onSelectTab(MainTab.SETTINGS) },
          icon = {
            Icon(
              imageVector =
                if (currentTab == MainTab.SETTINGS) Icons.Rounded.Settings
                else Icons.Outlined.Settings,
              contentDescription = stringResource(R.string.nav_settings),
            )
          },
          label = { Text(stringResource(R.string.nav_settings)) },
          colors = navItemColors(),
          modifier = Modifier.testTag("nav_tab_settings"),
        )
      }
    },
    content = { innerPadding ->
      KawaiiBackdrop(enableSparkles = sparkleBackdrop) {
        content(innerPadding)
      }
    },
  )
}

@Composable
private fun navItemColors() =
  NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    indicatorColor = PinkContainer,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
  )
