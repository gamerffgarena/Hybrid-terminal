package com.example.core.guide

data class ModuleSpec(
    val name: String,
    val path: String,
    val layer: String,
    val summary: String,
    val keyClasses: List<String>,
    val notes: String
)

data class ScriptAsset(
    val id: String,
    val title: String,
    val filename: String,
    val language: String,
    val description: String,
    val code: String
)

data class EngineeringNote(
    val title: String,
    val badge: String,
    val problem: String,
    val solution: String,
    val pitfalls: List<String>
)

object ArchitectureCatalog {

    val modules: List<ModuleSpec> = listOf(
        ModuleSpec(
            name = ":app",
            path = "app/src/main/java/com/example/",
            layer = "Shell & Navigation",
            summary = "Single-activity Compose host with edge-to-edge layout, permission requests (POST_NOTIFICATIONS), and Mode Switcher state coordination.",
            keyClasses = listOf("MainActivity.kt", "TermDeckViewModel.kt", "TopStatusModeBar.kt"),
            notes = "Never holds blocking I/O or raw file descriptors; observes StateFlows exposed by the Foreground Service and core engines."
        ),
        ModuleSpec(
            name = ":service:session",
            path = "app/src/main/java/com/example/service/",
            layer = "Android Lifecycle Anchor",
            summary = "Foreground Service (foregroundServiceType=\"dataSync\") maintaining process priority, partial WakeLock, and notification controls.",
            keyClasses = listOf("SessionForegroundService.kt", "SessionServiceState.kt"),
            notes = "Essential so Android Doze and background restrictions don't kill running builds, SSH tunnels, or Xvnc/X11 servers."
        ),
        ModuleSpec(
            name = ":core:pty",
            path = "app/src/main/java/com/example/core/pty/",
            layer = "JNI / PTY & Terminal Parser",
            summary = "Bridges Android Java/Kotlin streams to a Linux pseudo-terminal (/dev/ptmx via JNI forkpty) and parses VT100/ANSI escape sequences.",
            keyClasses = listOf("TerminalEngine.kt", "PtyBridge.cpp (JNI)", "AnsiBuffer.kt"),
            notes = "In production, pair JNI openpty()/forkpty() with a ringbuffer emulator so scrolling 10k lines stays 60fps in Compose."
        ),
        ModuleSpec(
            name = ":core:rootfs",
            path = "app/src/main/java/com/example/core/rootfs/",
            layer = "proot & POSIX Environment",
            summary = "Unpacks and mounts arm64 Linux rootfs inside app-private filesDir/usr/var/lib/proot-distro where execve() is permitted.",
            keyClasses = listOf("RootfsInstaller.kt", "ProotCommandBuilder.kt"),
            notes = "Uses --link2symlink and fake-root UID mapping because unprivileged Android apps cannot call chroot(2) or mount(2)."
        ),
        ModuleSpec(
            name = ":feature:terminal",
            path = "app/src/main/java/com/example/ui/screens/",
            layer = "Low-Power CLI Mode",
            summary = "Default fast terminal canvas with multi-session tabs, JetBrains Mono rendering, and sticky modifier Quick-Key bar.",
            keyClasses = listOf("TerminalModeScreen.kt", "QuickKeyBar.kt"),
            notes = "Consumes ~30-50 MB RAM with zero OpenGL surface overhead when Desktop Mode is stopped."
        ),
        ModuleSpec(
            name = ":feature:desktop",
            path = "app/src/main/java/com/example/ui/screens/",
            layer = "Display Bridge & Trackpad",
            summary = "Toggleable XFCE4/LXQt viewport (Termux:X11 SurfaceView or VNC framebuffer) paired with relative-delta touch trackpad and X11 key injection.",
            keyClasses = listOf("DesktopModeScreen.kt", "DesktopModels.kt", "X11InputDispatcher.kt"),
            notes = "Suspending or stopping xfce4-session immediately reclaims ~250 MB RAM and drops GPU wakeups when switching back to Terminal."
        )
    )

    val scripts: List<ScriptAsset> = listOf(
        ScriptAsset(
            id = "bootstrap",
            title = "Bootstrap proot Ubuntu Rootfs",
            filename = "bootstrap-rootfs.sh",
            language = "bash",
            description = "Initializes Termux prefix, installs proot-distro + Ubuntu 24.04, and prepares XFCE4/LXQt + TigerVNC + dev tools.",
            code = """#!/data/data/com.termdeck/files/usr/bin/bash
set -euo pipefail

# 1. Update host Termux packages & install proot + X11 bridge
pkg update -y && pkg upgrade -y
pkg install -y proot-distro x11-repo termux-x11-nightly pulseaudio

# 2. Install lightweight Ubuntu rootfs inside app-private filesDir
proot-distro install ubuntu

# 3. Provision XFCE4 + dev toolchain inside Ubuntu rootfs
proot-distro login ubuntu -- bash -c '
  apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y \
    xfce4 xfce4-terminal mousepad thunar dbus-x11 \
    tigervnc-standalone-server git curl python3 python3-pip build-essential
  mkdir -p ~/.vnc && chmod 700 ~/.vnc
'
echo "[OK] Rootfs & desktop packages ready.""""
        ),
        ScriptAsset(
            id = "start-desktop",
            title = "Start / Stop Linux Desktop Bridge",
            filename = "start-desktop.sh",
            language = "bash",
            description = "Launches DBus, PulseAudio over TCP, and XFCE4 on Termux:X11 (:0) or TigerVNC (:1) with clean teardown.",
            code = """#!/usr/bin/env bash
set -euo pipefail

DE="${'$'}{1:-xfce4}"        # xfce4 or lxqt
BRIDGE="${'$'}{2:-x11}"      # x11 or vnc
RES="${'$'}{3:-1280x720}"

export XDG_RUNTIME_DIR="${'$'}{TMPDIR:-/tmp}/runtime-root"
mkdir -p "${'$'}XDG_RUNTIME_DIR" && chmod 700 "${'$'}XDG_RUNTIME_DIR"

# Start audio bridge (optional, localhost only)
pulseaudio --start --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1" --exit-idle-time=-1 || true

if [[ "${'$'}BRIDGE" == "x11" ]]; then
  export DISPLAY=:0
  termux-x11 :0 -xstartup "dbus-launch --exit-with-session start${'$'}DE" &
else
  export DISPLAY=:1
  vncserver :1 -geometry "${'$'}RES" -depth 24 -localhost yes -SecurityTypes None
  dbus-launch --exit-with-session start${'$'}DE &
fi

echo "[TermDeck] ${'$'}DE running on DISPLAY=${'$'}DISPLAY (${'$'}BRIDGE)"
"""
        ),
        ScriptAsset(
            id = "proot-kt",
            title = "Kotlin proot Process Launcher",
            filename = "ProotCommandBuilder.kt",
            language = "kotlin",
            description = "Constructs safe ProcessBuilder invocation pointing strictly to app-private filesDir (never /sdcard due to noexec).",
            code = """fun buildProotCommand(
    filesDir: File,
    distroRoot: File,
    command: String = "/bin/bash -l"
): ProcessBuilder {
    val usr = File(filesDir, "usr")
    val prootBin = File(usr, "bin/proot").absolutePath
    val tmpDir = File(usr, "tmp").apply { mkdirs() }

    val args = listOf(
        prootBin,
        "--link2symlink",
        "-0", // Fake root UID/GID inside rootfs
        "-r", distroRoot.absolutePath,
        "-b", "/dev",
        "-b", "/proc",
        "-b", "/sys",
        "-b", "${'$'}{tmpDir.absolutePath}:/tmp",
        "-w", "/root",
        "/usr/bin/env", "-i",
        "HOME=/root",
        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
        "TERM=xterm-256color",
        "LANG=C.UTF-8",
        "/bin/bash", "-c", command
    )

    return ProcessBuilder(args).apply {
        environment()["PROOT_TMP_DIR"] = tmpDir.absolutePath
        environment()["LD_LIBRARY_PATH"] = File(usr, "lib").absolutePath
    }
}"""
        )
    )

    val engineeringNotes: List<EngineeringNote> = listOf(
        EngineeringNote(
            title = "Android Background Limits & Foreground Service",
            badge = "API 31–36 Critical",
            problem = "Android aggressively suspends cached apps (Doze, App Standby) and Android 12+ enforces the Phantom Process Killer (killing child processes like proot/node/Xvnc once an app spawns >32 child processes or goes to background).",
            solution = "Run PTY & X11/VNC lifecycles inside a dedicated Foreground Service (`foregroundServiceType=\"dataSync\"`) with an ongoing notification, partial WakeLock toggle, and battery-optimization exemption prompt.",
            pitfalls = listOf(
                "Target SDK 34+ requires declaring `FOREGROUND_SERVICE_DATA_SYNC` in AndroidManifest.xml and requesting `POST_NOTIFICATIONS` at runtime.",
                "Android 12–13 users hitting Signal 9 kills on child processes can enable 'Disable child process restrictions' in Developer Options (or `device_config put activity_manager max_phantom_processes 2147483647`).",
                "Keep Desktop Mode teardown deterministic: kill `xfce4-session` & `Xvnc` when switching back to Terminal Mode to save battery."
            )
        ),
        EngineeringNote(
            title = "Linux POSIX Permissions vs Android External Storage",
            badge = "SELinux & FUSE",
            problem = "External storage (`/sdcard`, `/storage/emulated/0`) uses FUSE/sdcardfs mounted with `noexec,nosuid,nodev` and strips Unix mode bits (`chmod +x`), ownership (`chown`), and POSIX symlinks.",
            solution = "Store the entire Linux rootfs, binaries, `.ssh` keys, and git repos with symlinks inside app-private `context.filesDir` (`/data/user/0/<pkg>/files/usr`), where SELinux `app_data_file` permits `execve()` for bundled/unpacked native binaries (note Play Store DCL rules if distributing via Play).",
            pitfalls = listOf(
                "Never unpack a Linux rootfs or `node_modules` with symlinks onto `/sdcard` — `tar` and `npm` will fail with `EPERM` on symlinks and `Permission denied` on `chmod +x`.",
                "Never request broad `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE` for media; expose `filesDir` safely via an Android `DocumentsProvider` (SAF) so external editors/files apps can read/write project files seamlessly.",
                "For shared downloads, copy artifacts to/from SAF `OpenDocument` / `CreateDocument` URIs."
            )
        )
    )
}
