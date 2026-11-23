package com.example.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.desktop.DesktopEnvironment
import com.example.core.desktop.DesktopResolution
import com.example.core.desktop.DesktopSessionState
import com.example.core.desktop.DesktopWindowApp
import com.example.core.desktop.DisplayBridge
import com.example.core.desktop.LinuxWindow
import com.example.core.desktop.ModifierKey
import com.example.core.desktop.WorkspaceMode
import com.example.core.desktop.defaultDesktopWindows
import com.example.core.pty.LineTone
import com.example.core.pty.TerminalEngine
import com.example.core.pty.TerminalLine
import com.example.core.pty.TerminalSessionTab
import com.example.service.ForegroundSessionStatus
import com.example.service.SessionForegroundService
import com.example.service.SessionServiceState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ResourceTelemetry(
    val workspaceRamMb: Int = 34,
    val systemAvailGb: Float = 4.2f,
    val powerProfile: String = "Low Power",
    val cpuLoadPercent: Int = 6
)

data class TermDeckUiState(
    val mode: WorkspaceMode = WorkspaceMode.TERMINAL,
    val showArchitectureGuide: Boolean = false,
    val tabs: List<TerminalSessionTab> = emptyList(),
    val activeTabId: String = "tab-1",
    val commandInput: String = "",
    val historyCursor: Int = -1,
    val stickyCtrl: Boolean = false,
    val stickyAlt: Boolean = false,
    val desktop: DesktopSessionState = DesktopSessionState(),
    val telemetry: ResourceTelemetry = ResourceTelemetry(),
    val serviceStatus: ForegroundSessionStatus = ForegroundSessionStatus()
) {
    val activeTab: TerminalSessionTab?
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.firstOrNull()
}

class TermDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TerminalEngine(application.filesDir)
    private var tabCounter = 2
    private var windowCounter = 10

    private val _uiState = MutableStateFlow(
        TermDeckUiState(
            tabs = engine.createInitialTabs(),
            activeTabId = "tab-1"
        )
    )
    val uiState: StateFlow<TermDeckUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            SessionServiceState.status.collect { serviceStatus ->
                _uiState.update { it.copy(serviceStatus = serviceStatus) }
            }
        }
        viewModelScope.launch {
            while (isActive) {
                refreshTelemetry()
                delay(2000L)
            }
        }
    }

    private fun refreshTelemetry() {
        val context = getApplication<Application>()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val availGb = if (memInfo.availMem > 0L) {
            memInfo.availMem / (1024f * 1024f * 1024f)
        } else {
            4.1f
        }

        val current = _uiState.value
        val isDesktopActive = current.mode == WorkspaceMode.DESKTOP && current.desktop.isRunning
        val ramMb = if (isDesktopActive) {
            current.desktop.environment.ramEstimateMb + (current.tabs.size * 14)
        } else {
            24 + (current.tabs.size * 10)
        }
        val cpu = if (isDesktopActive) 28 else 5
        val profile = if (isDesktopActive) "X11 Active" else "Low Power"

        _uiState.update {
            it.copy(
                telemetry = ResourceTelemetry(
                    workspaceRamMb = ramMb,
                    systemAvailGb = availGb,
                    powerProfile = profile,
                    cpuLoadPercent = cpu
                )
            )
        }
    }

    fun switchMode(targetMode: WorkspaceMode) {
        _uiState.update { state ->
            val updatedDesktop = if (targetMode == WorkspaceMode.DESKTOP && !state.desktop.isRunning) {
                state.desktop.copy(
                    isRunning = true,
                    lastInputEvent = "Started ${state.desktop.environment.label} via ${state.desktop.bridge.label}"
                )
            } else {
                state.desktop
            }
            state.copy(mode = targetMode, desktop = updatedDesktop)
        }
        val modeLabel = if (targetMode == WorkspaceMode.DESKTOP) {
            val d = _uiState.value.desktop
            "Desktop (${d.environment.label} • ${d.bridge.label})"
        } else {
            "Terminal (Low Power)"
        }
        SessionForegroundService.updateMode(getApplication(), modeLabel)
        refreshTelemetry()
    }

    fun toggleArchitectureGuide(open: Boolean) {
        _uiState.update { it.copy(showArchitectureGuide = open) }
    }

    fun toggleForegroundService() {
        val context = getApplication<Application>()
        if (_uiState.value.serviceStatus.isRunning) {
            SessionForegroundService.stop(context)
        } else {
            val modeLabel = if (_uiState.value.mode == WorkspaceMode.DESKTOP) {
                "Desktop (${_uiState.value.desktop.environment.label})"
            } else {
                "Terminal (Low Power)"
            }
            SessionForegroundService.start(context, modeLabel)
        }
    }

    // ---- Terminal Actions ----

    fun selectTab(tabId: String) {
        _uiState.update { it.copy(activeTabId = tabId, historyCursor = -1) }
    }

    fun addNewTab() {
        tabCounter += 1
        val id = "tab-$tabCounter"
        val newTab = TerminalSessionTab(
            id = id,
            title = "$tabCounter: bash",
            shellLabel = "Termux PTY",
            cwd = "~/workspace",
            lines = listOf(
                TerminalLine(
                    id = System.nanoTime(),
                    text = "New PTY session ($id) initialized in ~/workspace",
                    tone = LineTone.ACCENT
                )
            )
        )
        _uiState.update {
            it.copy(
                tabs = it.tabs + newTab,
                activeTabId = id,
                historyCursor = -1
            )
        }
        refreshTelemetry()
    }

    fun closeTab(tabId: String) {
        _uiState.update { state ->
            if (state.tabs.size <= 1) return@update state
            val remaining = state.tabs.filterNot { it.id == tabId }
            val nextActive = if (state.activeTabId == tabId) remaining.last().id else state.activeTabId
            state.copy(tabs = remaining, activeTabId = nextActive)
        }
        refreshTelemetry()
    }

    fun updateCommandInput(value: String) {
        _uiState.update { it.copy(commandInput = value) }
    }

    fun submitCommand(explicitCommand: String? = null) {
        val state = _uiState.value
        val tab = state.activeTab ?: return
        val command = (explicitCommand ?: state.commandInput)

        viewModelScope.launch {
            val result = engine.executeCommand(command, tab)
            _uiState.update { current ->
                val updatedTabs = current.tabs.map { item ->
                    if (item.id != tab.id) item
                    else {
                        val nextLines = if (result.clearScreen) emptyList()
                        else (item.lines + result.appendedLines).takeLast(200)
                        val nextHistory = if (command.isNotBlank()) {
                            (item.history + command.trim()).takeLast(50)
                        } else item.history
                        val nextIsProot = when {
                            result.sideEffect.enterProotUbuntu -> true
                            result.sideEffect.exitProotUbuntu -> false
                            else -> item.isProotUbuntu
                        }
                        item.copy(
                            lines = nextLines,
                            cwd = result.updatedCwd ?: item.cwd,
                            history = nextHistory,
                            isProotUbuntu = nextIsProot,
                            shellLabel = if (nextIsProot) "proot-distro" else "Termux PTY",
                            title = if (nextIsProot) "${item.title.substringBefore(':')}: ubuntu" else "${item.title.substringBefore(':')}: bash"
                        )
                    }
                }
                current.copy(
                    tabs = updatedTabs,
                    commandInput = if (explicitCommand == null) "" else current.commandInput,
                    historyCursor = -1,
                    stickyCtrl = false,
                    stickyAlt = false
                )
            }

            if (result.sideEffect.switchToDesktop) {
                switchMode(WorkspaceMode.DESKTOP)
            } else if (result.sideEffect.stopDesktop) {
                stopDesktopSession()
            } else if (result.sideEffect.switchToTerminal) {
                switchMode(WorkspaceMode.TERMINAL)
            }
        }
    }

    fun onQuickKeyPressed(key: String) {
        when (key) {
            "CTRL" -> _uiState.update { it.copy(stickyCtrl = !it.stickyCtrl) }
            "ALT" -> _uiState.update { it.copy(stickyAlt = !it.stickyAlt) }
            "ESC" -> {
                _uiState.update { it.copy(stickyCtrl = false, stickyAlt = false) }
                appendTerminalNote("[ESC] sent to PTY")
            }
            "TAB" -> {
                val input = _uiState.value.commandInput
                val completed = when {
                    input.startsWith("proot") -> "proot-distro login ubuntu"
                    input.startsWith("start") -> "start-desktop xfce4"
                    input.startsWith("pkg u") -> "pkg update"
                    input.startsWith("un") -> "uname -a"
                    else -> "$input  "
                }
                _uiState.update { it.copy(commandInput = completed) }
            }
            "^C" -> {
                _uiState.update { it.copy(commandInput = "", stickyCtrl = false, stickyAlt = false) }
                appendTerminalNote("^C (SIGINT)", LineTone.WARNING)
            }
            "^D" -> {
                submitCommand("exit")
            }
            "↑" -> navigateHistory(up = true)
            "↓" -> navigateHistory(up = false)
            "←", "→" -> {
                // Visual cursor feedback for mobile bar
                appendTerminalNote("[$key] cursor step", LineTone.MUTED)
            }
            else -> {
                val current = _uiState.value.commandInput
                if (_uiState.value.stickyCtrl && key.equals("c", ignoreCase = true)) {
                    onQuickKeyPressed("^C")
                } else {
                    _uiState.update {
                        it.copy(
                            commandInput = current + key,
                            stickyCtrl = false,
                            stickyAlt = false
                        )
                    }
                }
            }
        }
    }

    private fun navigateHistory(up: Boolean) {
        val tab = _uiState.value.activeTab ?: return
        if (tab.history.isEmpty()) return
        val currentCursor = _uiState.value.historyCursor
        val nextCursor = if (up) {
            if (currentCursor == -1) tab.history.lastIndex else (currentCursor - 1).coerceAtLeast(0)
        } else {
            if (currentCursor == -1 || currentCursor >= tab.history.lastIndex) -1 else currentCursor + 1
        }
        val nextText = if (nextCursor == -1) "" else tab.history[nextCursor]
        _uiState.update { it.copy(historyCursor = nextCursor, commandInput = nextText) }
    }

    private fun appendTerminalNote(message: String, tone: LineTone = LineTone.MUTED) {
        val activeId = _uiState.value.activeTabId
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs.map { tab ->
                    if (tab.id != activeId) tab
                    else tab.copy(lines = (tab.lines + TerminalLine(System.nanoTime(), message, tone)).takeLast(200))
                }
            )
        }
    }

    // ---- Desktop Mode Actions ----

    fun selectDesktopEnvironment(env: DesktopEnvironment) {
        _uiState.update {
            it.copy(
                desktop = it.desktop.copy(
                    environment = env,
                    isRunning = true,
                    lastInputEvent = "Switched DE to ${env.label}"
                )
            )
        }
        SessionForegroundService.updateMode(
            getApplication(),
            "Desktop (${env.label} • ${_uiState.value.desktop.bridge.label})"
        )
        refreshTelemetry()
    }

    fun selectDisplayBridge(bridge: DisplayBridge) {
        _uiState.update {
            it.copy(
                desktop = it.desktop.copy(
                    bridge = bridge,
                    lastInputEvent = "Bridge: ${bridge.label} (${bridge.socketInfo})"
                )
            )
        }
    }

    fun cycleResolution() {
        _uiState.update {
            val nextRes = if (it.desktop.resolution == DesktopResolution.HD) {
                DesktopResolution.FHD
            } else {
                DesktopResolution.HD
            }
            it.copy(
                desktop = it.desktop.copy(
                    resolution = nextRes,
                    lastInputEvent = "XRandR mode -> ${nextRes.label}"
                )
            )
        }
    }

    fun stopDesktopSession() {
        _uiState.update {
            it.copy(
                mode = WorkspaceMode.TERMINAL,
                desktop = it.desktop.copy(
                    isRunning = false,
                    appMenuOpen = false,
                    contextMenuPos = null,
                    lastInputEvent = "Desktop stopped • RAM reclaimed"
                )
            )
        }
        SessionForegroundService.updateMode(getApplication(), "Terminal (Low Power)")
        refreshTelemetry()
    }

    fun restartDesktopSession() {
        _uiState.update {
            it.copy(
                desktop = it.desktop.copy(
                    isRunning = true,
                    windows = defaultDesktopWindows(),
                    appMenuOpen = false,
                    contextMenuPos = null,
                    lastInputEvent = "Restarted ${it.desktop.environment.label} session"
                )
            )
        }
        refreshTelemetry()
    }

    fun toggleTrackpadExpanded() {
        _uiState.update {
            it.copy(desktop = it.desktop.copy(trackpadExpanded = !it.desktop.trackpadExpanded))
        }
    }

    fun moveCursorBy(dxFraction: Float, dyFraction: Float) {
        _uiState.update { state ->
            val d = state.desktop
            val nx = (d.cursorX + dxFraction).coerceIn(0.02f, 0.98f)
            val ny = (d.cursorY + dyFraction).coerceIn(0.02f, 0.98f)

            val updatedWindows = if (d.dragLock) {
                d.windows.map { win ->
                    if (!win.isFocused || win.isMinimized) win
                    else win.copy(
                        xFraction = (win.xFraction + dxFraction).coerceIn(0f, 1f - win.widthFraction),
                        yFraction = (win.yFraction + dyFraction).coerceIn(0.09f, 0.90f - win.heightFraction)
                    )
                }
            } else {
                d.windows
            }

            val px = (nx * d.resolution.width).toInt()
            val py = (ny * d.resolution.height).toInt()

            state.copy(
                desktop = d.copy(
                    cursorX = nx,
                    cursorY = ny,
                    windows = updatedWindows,
                    lastInputEvent = if (d.dragLock) "Dragging window ($px, $py)" else "Pointer ($px, $py)"
                )
            )
        }
    }

    fun setCursorDirect(xFraction: Float, yFraction: Float) {
        _uiState.update { state ->
            val d = state.desktop
            val nx = xFraction.coerceIn(0.02f, 0.98f)
            val ny = yFraction.coerceIn(0.02f, 0.98f)
            state.copy(desktop = d.copy(cursorX = nx, cursorY = ny))
        }
        performLeftClick()
    }

    fun performLeftClick() {
        _uiState.update { state ->
            val d = state.desktop
            val cx = d.cursorX
            val cy = d.cursorY

            // Check top/bottom panel App Menu button
            val panelTop = d.environment == DesktopEnvironment.XFCE4
            val inPanel = if (panelTop) cy <= 0.10f else cy >= 0.90f
            if (inPanel && cx <= 0.24f) {
                return@update state.copy(
                    desktop = d.copy(
                        appMenuOpen = !d.appMenuOpen,
                        contextMenuPos = null,
                        clickPulseTimestamp = System.currentTimeMillis(),
                        lastInputEvent = "Left Click -> Applications Menu"
                    )
                )
            }

            // Hit test windows from top (last in list) to bottom
            val hitWindow = d.windows.lastOrNull { w ->
                !w.isMinimized &&
                    cx in w.xFraction..(w.xFraction + w.widthFraction) &&
                    cy in w.yFraction..(w.yFraction + w.heightFraction)
            }

            val updatedWindows = if (hitWindow != null) {
                val reordered = d.windows.filterNot { it.id == hitWindow.id } + hitWindow.copy(isFocused = true)
                reordered.mapIndexed { idx, w -> w.copy(isFocused = idx == reordered.lastIndex) }
            } else {
                d.windows
            }

            val eventText = if (hitWindow != null) {
                "Left Click -> Focused ${hitWindow.title.substringBefore(" —")}"
            } else {
                "Left Click on desktop (${(cx * d.resolution.width).toInt()}, ${(cy * d.resolution.height).toInt()})"
            }

            state.copy(
                desktop = d.copy(
                    windows = updatedWindows,
                    appMenuOpen = false,
                    contextMenuPos = null,
                    clickPulseTimestamp = System.currentTimeMillis(),
                    lastInputEvent = eventText
                )
            )
        }
    }

    fun performMiddleClick() {
        _uiState.update { state ->
            state.copy(
                desktop = state.desktop.copy(
                    contextMenuPos = null,
                    clickPulseTimestamp = System.currentTimeMillis(),
                    lastInputEvent = "Middle Click -> X11 Primary Paste"
                )
            )
        }
    }

    fun performRightClick() {
        _uiState.update { state ->
            val d = state.desktop
            state.copy(
                desktop = d.copy(
                    appMenuOpen = false,
                    contextMenuPos = d.cursorX.coerceIn(0.08f, 0.68f) to d.cursorY.coerceIn(0.14f, 0.65f),
                    clickPulseTimestamp = System.currentTimeMillis(),
                    lastInputEvent = "Right Click -> Context Menu"
                )
            )
        }
    }

    fun toggleDragLock() {
        _uiState.update { state ->
            val next = !state.desktop.dragLock
            state.copy(
                desktop = state.desktop.copy(
                    dragLock = next,
                    lastInputEvent = if (next) "Drag Lock ON (drag pad moves focused window)" else "Drag Lock OFF"
                )
            )
        }
    }

    fun scrollDesktop(deltaY: Float) {
        val direction = if (deltaY < 0f) "Up" else "Down"
        _uiState.update { state ->
            state.copy(
                desktop = state.desktop.copy(
                    lastInputEvent = "X11 Wheel Scroll $direction"
                )
            )
        }
    }

    fun toggleDesktopModifier(mod: ModifierKey) {
        _uiState.update { state ->
            val current = state.desktop.activeModifiers
            val next = if (mod in current) current - mod else current + mod
            state.copy(
                desktop = state.desktop.copy(
                    activeModifiers = next,
                    appMenuOpen = if (mod == ModifierKey.SUPER) !state.desktop.appMenuOpen else state.desktop.appMenuOpen,
                    lastInputEvent = "Modifier ${mod.label} ${if (mod in next) "LATCHED" else "released"}"
                )
            )
        }
    }

    fun triggerDesktopShortcut(shortcut: String) {
        when (shortcut) {
            "Ctrl+Alt+T" -> openDesktopAppWindow(DesktopWindowApp.TERMINAL)
            "Alt+F4" -> closeFocusedDesktopWindow()
            "Tab" -> cycleFocusedDesktopWindow()
            "Esc" -> _uiState.update {
                it.copy(
                    desktop = it.desktop.copy(
                        appMenuOpen = false,
                        contextMenuPos = null,
                        activeModifiers = emptySet(),
                        lastInputEvent = "Key: ESC"
                    )
                )
            }
            "Ctrl+C" -> _uiState.update {
                it.copy(
                    desktop = it.desktop.copy(
                        activeModifiers = emptySet(),
                        lastInputEvent = "Sent Ctrl+C to focused X11 window"
                    )
                )
            }
        }
    }

    fun openDesktopAppWindow(app: DesktopWindowApp) {
        windowCounter += 1
        val (title, lines) = when (app) {
            DesktopWindowApp.TERMINAL -> "xfce4-terminal — #$windowCounter" to listOf(
                "root@ubuntu:~# neofetch --stdout",
                "OS: Ubuntu 24.04 aarch64 (proot)",
                "DE: ${_uiState.value.desktop.environment.label}",
                "root@ubuntu:~# _"
            )
            DesktopWindowApp.CODE_EDITOR -> "Mousepad — app.kt" to listOf(
                "1  package com.termdeck.desktop",
                "2  fun bridgeDisplay() = \"${_uiState.value.desktop.bridge.label}\""
            )
            DesktopWindowApp.FILE_MANAGER -> "Thunar — /root" to listOf(
                "📁 workspace/",
                "📁 .config/xfce4/",
                "📄 server.py"
            )
        }
        val offset = ((windowCounter % 4) * 0.04f)
        val newWin = LinuxWindow(
            id = "win-$windowCounter",
            title = title,
            app = app,
            xFraction = 0.12f + offset,
            yFraction = 0.18f + offset,
            widthFraction = 0.56f,
            heightFraction = 0.48f,
            isFocused = true,
            bodyLines = lines
        )
        _uiState.update { state ->
            val unfocused = state.desktop.windows.map { it.copy(isFocused = false) }
            state.copy(
                desktop = state.desktop.copy(
                    windows = unfocused + newWin,
                    appMenuOpen = false,
                    contextMenuPos = null,
                    lastInputEvent = "Launched ${title.substringBefore(" —")}"
                )
            )
        }
    }

    fun focusDesktopWindow(windowId: String) {
        _uiState.update { state ->
            val target = state.desktop.windows.firstOrNull { it.id == windowId } ?: return@update state
            val reordered = state.desktop.windows.filterNot { it.id == windowId }.map { it.copy(isFocused = false) } +
                target.copy(isFocused = true, isMinimized = false)
            state.copy(
                desktop = state.desktop.copy(
                    windows = reordered,
                    appMenuOpen = false,
                    contextMenuPos = null,
                    lastInputEvent = "Window: ${target.title.substringBefore(" —")}"
                )
            )
        }
    }

    fun closeDesktopWindow(windowId: String) {
        _uiState.update { state ->
            val remaining = state.desktop.windows.filterNot { it.id == windowId }
            val withFocus = remaining.mapIndexed { idx, w -> w.copy(isFocused = idx == remaining.lastIndex) }
            state.copy(
                desktop = state.desktop.copy(
                    windows = withFocus,
                    lastInputEvent = "Closed X11 window"
                )
            )
        }
    }

    private fun closeFocusedDesktopWindow() {
        val focusedId = _uiState.value.desktop.windows.lastOrNull { it.isFocused }?.id ?: return
        closeDesktopWindow(focusedId)
    }

    private fun cycleFocusedDesktopWindow() {
        _uiState.update { state ->
            val wins = state.desktop.windows
            if (wins.size <= 1) return@update state
            val rotated = wins.drop(1) + wins.first()
            val updated = rotated.mapIndexed { idx, w -> w.copy(isFocused = idx == rotated.lastIndex, isMinimized = false) }
            state.copy(
                desktop = state.desktop.copy(
                    windows = updated,
                    lastInputEvent = "Alt+Tab -> ${updated.last().title.substringBefore(" —")}"
                )
            )
        }
    }

    fun injectTextIntoFocusedWindow(text: String) {
        if (text.isBlank()) return
        _uiState.update { state ->
            val updated = state.desktop.windows.map { win ->
                if (!win.isFocused) win
                else win.copy(bodyLines = (win.bodyLines + text).takeLast(10))
            }
            state.copy(
                desktop = state.desktop.copy(
                    windows = updated,
                    lastInputEvent = "X11 KeySym injected: \"$text\""
                )
            )
        }
    }
}
