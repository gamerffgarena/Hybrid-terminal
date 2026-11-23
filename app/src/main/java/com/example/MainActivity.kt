package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.desktop.WorkspaceMode
import com.example.service.SessionForegroundService
import com.example.ui.TermDeckViewModel
import com.example.ui.components.TopStatusModeBar
import com.example.ui.screens.ArchitectureGuideScreen
import com.example.ui.screens.DesktopModeScreen
import com.example.ui.screens.TerminalModeScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TermDeckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(ObsidianBg.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(ObsidianBg.toArgb())
        )
        setContent {
            TermDeckTheme {
                TermDeckApp()
            }
        }
    }
}

@Composable
fun TermDeckApp(
    viewModel: TermDeckViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Foreground service starts even if user declines notification visibility on API 33+
        if (!state.serviceStatus.isRunning) {
            SessionForegroundService.start(context, "Terminal (Low Power)")
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                SessionForegroundService.start(context, "Terminal (Low Power)")
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            SessionForegroundService.start(context, "Terminal (Low Power)")
        }
    }

    if (state.showArchitectureGuide) {
        ArchitectureGuideScreen(
            onClose = { viewModel.toggleArchitectureGuide(false) }
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1000.dp)
            ) {
                TopStatusModeBar(
                    mode = state.mode,
                    telemetry = state.telemetry,
                    serviceStatus = state.serviceStatus,
                    desktop = state.desktop,
                    onSelectMode = viewModel::switchMode,
                    onToggleService = viewModel::toggleForegroundService,
                    onOpenBlueprint = { viewModel.toggleArchitectureGuide(true) }
                )

                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                Crossfade(
                    targetState = state.mode,
                    animationSpec = tween(220),
                    modifier = Modifier.weight(1f),
                    label = "workspace_mode_crossfade"
                ) { activeMode ->
                    when (activeMode) {
                        WorkspaceMode.TERMINAL -> TerminalModeScreen(
                            tabs = state.tabs,
                            activeTab = state.activeTab,
                            commandInput = state.commandInput,
                            stickyCtrl = state.stickyCtrl,
                            stickyAlt = state.stickyAlt,
                            onSelectTab = viewModel::selectTab,
                            onAddTab = viewModel::addNewTab,
                            onCloseTab = viewModel::closeTab,
                            onUpdateInput = viewModel::updateCommandInput,
                            onSubmitCommand = viewModel::submitCommand,
                            onQuickKey = viewModel::onQuickKeyPressed
                        )
                        WorkspaceMode.DESKTOP -> DesktopModeScreen(
                            desktop = state.desktop,
                            onSelectEnvironment = viewModel::selectDesktopEnvironment,
                            onSelectBridge = viewModel::selectDisplayBridge,
                            onCycleResolution = viewModel::cycleResolution,
                            onStopDesktop = viewModel::stopDesktopSession,
                            onRestartDesktop = viewModel::restartDesktopSession,
                            onToggleTrackpad = viewModel::toggleTrackpadExpanded,
                            onMoveCursorBy = viewModel::moveCursorBy,
                            onDirectCanvasTap = viewModel::setCursorDirect,
                            onLeftClick = viewModel::performLeftClick,
                            onMiddleClick = viewModel::performMiddleClick,
                            onRightClick = viewModel::performRightClick,
                            onToggleDragLock = viewModel::toggleDragLock,
                            onScrollDesktop = viewModel::scrollDesktop,
                            onToggleModifier = viewModel::toggleDesktopModifier,
                            onTriggerShortcut = viewModel::triggerDesktopShortcut,
                            onOpenWindow = viewModel::openDesktopAppWindow,
                            onFocusWindow = viewModel::focusDesktopWindow,
                            onCloseWindow = viewModel::closeDesktopWindow,
                            onInjectText = viewModel::injectTextIntoFocusedWindow
                        )
                    }
                }
            }
        }
    }
}
