package com.example.core.desktop

enum class WorkspaceMode {
    TERMINAL,
    DESKTOP
}

enum class DesktopEnvironment(val label: String, val ramEstimateMb: Int) {
    XFCE4("XFCE4", 296),
    LXQT("LXQt", 238)
}

enum class DisplayBridge(val label: String, val socketInfo: String) {
    TERMUX_X11("Termux:X11", "SharedMem :0"),
    TIGERVNC("TigerVNC", "127.0.0.1:5901")
}

enum class DesktopResolution(val label: String, val width: Int, val height: Int) {
    HD("1280×720", 1280, 720),
    FHD("1920×1080", 1920, 1080)
}

enum class DesktopWindowApp {
    TERMINAL,
    CODE_EDITOR,
    FILE_MANAGER
}

data class LinuxWindow(
    val id: String,
    val title: String,
    val app: DesktopWindowApp,
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float,
    val isFocused: Boolean = false,
    val isMinimized: Boolean = false,
    val bodyLines: List<String> = emptyList()
)

enum class ModifierKey(val label: String) {
    CTRL("CTRL"),
    ALT("ALT"),
    SUPER("SUPER"),
    SHIFT("SHIFT")
}

data class DesktopSessionState(
    val isRunning: Boolean = true,
    val environment: DesktopEnvironment = DesktopEnvironment.XFCE4,
    val bridge: DisplayBridge = DisplayBridge.TERMUX_X11,
    val resolution: DesktopResolution = DesktopResolution.HD,
    val cursorX: Float = 0.46f,
    val cursorY: Float = 0.44f,
    val activeModifiers: Set<ModifierKey> = emptySet(),
    val dragLock: Boolean = false,
    val appMenuOpen: Boolean = false,
    val contextMenuPos: Pair<Float, Float>? = null,
    val trackpadExpanded: Boolean = true,
    val lastInputEvent: String = "X11 pointer ready • 60 Hz",
    val clickPulseTimestamp: Long = 0L,
    val windows: List<LinuxWindow> = defaultDesktopWindows()
)

fun defaultDesktopWindows(): List<LinuxWindow> = listOf(
    LinuxWindow(
        id = "win-files",
        title = "Thunar — /home/dev/projects",
        app = DesktopWindowApp.FILE_MANAGER,
        xFraction = 0.05f,
        yFraction = 0.14f,
        widthFraction = 0.46f,
        heightFraction = 0.54f,
        isFocused = false,
        bodyLines = listOf(
            "📁 api-server/",
            "📁 termdeck-ui/",
            "📄 bootstrap-rootfs.sh",
            "📄 start-desktop.sh",
            "🔗 /sdcard/Download (SAF)"
        )
    ),
    LinuxWindow(
        id = "win-editor",
        title = "Mousepad — server.py",
        app = DesktopWindowApp.CODE_EDITOR,
        xFraction = 0.34f,
        yFraction = 0.18f,
        widthFraction = 0.58f,
        heightFraction = 0.62f,
        isFocused = true,
        bodyLines = listOf(
            "1  from fastapi import FastAPI",
            "2  app = FastAPI(title='TermDeck')",
            "3",
            "4  @app.get('/health')",
            "5  def health():",
            "6      return {'mode': 'xfce4', 'pty': 'ok'}"
        )
    ),
    LinuxWindow(
        id = "win-term",
        title = "xfce4-terminal — ubuntu",
        app = DesktopWindowApp.TERMINAL,
        xFraction = 0.14f,
        yFraction = 0.42f,
        widthFraction = 0.56f,
        heightFraction = 0.44f,
        isFocused = false,
        bodyLines = listOf(
            "root@ubuntu:~# DISPLAY=:0 xfce4-session &",
            "[x11] Connected via shared memory ringbuffer",
            "root@ubuntu:~# uvicorn server:app --port 8080",
            "INFO: Uvicorn running on http://127.0.0.1:8080"
        )
    )
)
