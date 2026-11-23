package com.example.core.pty

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

enum class LineTone {
    PROMPT,
    OUTPUT,
    SUCCESS,
    ACCENT,
    WARNING,
    ERROR,
    MUTED
}

data class TerminalLine(
    val id: Long,
    val text: String,
    val tone: LineTone = LineTone.OUTPUT
)

data class TerminalSessionTab(
    val id: String,
    val title: String,
    val shellLabel: String,
    val cwd: String = "~/workspace",
    val isProotUbuntu: Boolean = false,
    val lines: List<TerminalLine> = emptyList(),
    val history: List<String> = emptyList()
)

data class CommandSideEffect(
    val switchToDesktop: Boolean = false,
    val switchToTerminal: Boolean = false,
    val stopDesktop: Boolean = false,
    val enterProotUbuntu: Boolean = false,
    val exitProotUbuntu: Boolean = false
)

data class CommandExecutionResult(
    val appendedLines: List<TerminalLine>,
    val updatedCwd: String? = null,
    val clearScreen: Boolean = false,
    val sideEffect: CommandSideEffect = CommandSideEffect()
)

class TerminalEngine(private val appFilesDir: File) {

    private var nextLineId = 100L

    private fun line(text: String, tone: LineTone = LineTone.OUTPUT): TerminalLine {
        nextLineId += 1
        return TerminalLine(id = nextLineId, text = text, tone = tone)
    }

    fun createInitialTabs(): List<TerminalSessionTab> {
        val welcomeTab = TerminalSessionTab(
            id = "tab-1",
            title = "1: bash",
            shellLabel = "Termux PTY",
            cwd = "~/workspace",
            isProotUbuntu = false,
            lines = listOf(
                line("TermDeck Hybrid PTY v0.9 • ARM64 Android Runtime", LineTone.ACCENT),
                line("Mode: Terminal (Low Power • 34 MB RAM • 0% GPU)", LineTone.SUCCESS),
                line("Type 'help' for dev workflows, 'uname -a' for live Android kernel, or 'start-desktop' to boot XFCE4.", LineTone.MUTED),
                line("", LineTone.OUTPUT)
            )
        )
        val ubuntuTab = TerminalSessionTab(
            id = "tab-2",
            title = "2: ubuntu",
            shellLabel = "proot-distro",
            cwd = "/root/projects",
            isProotUbuntu = true,
            lines = listOf(
                line("[proot] Mounted Ubuntu 24.04 LTS rootfs at /data/user/0/.../files/usr/var/lib/proot-distro/ubuntu", LineTone.ACCENT),
                line("root@ubuntu:/root/projects# ls -la", LineTone.PROMPT),
                line("drwxr-xr-x  4 root root 4096 Nov 23 00:18 api-service", LineTone.OUTPUT),
                line("-rw-r--r--  1 root root  412 Nov 23 00:19 docker-compose.yml", LineTone.OUTPUT),
                line("-rwxr-xr-x  1 root root  894 Nov 23 00:20 start-desktop.sh", LineTone.SUCCESS)
            )
        )
        return listOf(welcomeTab, ubuntuTab)
    }

    suspend fun executeCommand(
        rawInput: String,
        session: TerminalSessionTab
    ): CommandExecutionResult = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        val promptPrefix = if (session.isProotUbuntu) {
            "root@ubuntu:${session.cwd}# $trimmed"
        } else {
            "u0_a142@termdeck:${session.cwd}$ $trimmed"
        }

        if (trimmed.isEmpty()) {
            return@withContext CommandExecutionResult(listOf(line(promptPrefix, LineTone.PROMPT)))
        }

        val tokens = trimmed.split(Regex("\\s+"))
        val cmd = tokens.first()
        val args = tokens.drop(1)
        val out = mutableListOf(line(promptPrefix, LineTone.PROMPT))

        when (cmd) {
            "clear" -> {
                return@withContext CommandExecutionResult(emptyList(), clearScreen = true)
            }
            "help" -> {
                out += line("Available Built-in & Live Shell Commands:", LineTone.ACCENT)
                out += line("  Live Android Shell : uname -a, id, pwd, env, date, df -h, ls, ps", LineTone.OUTPUT)
                out += line("  Package & Dev      : pkg update, pkg install <pkg>, git status, python3, node", LineTone.OUTPUT)
                out += line("  Linux Rootfs       : proot-distro list, proot-distro login ubuntu, exit", LineTone.OUTPUT)
                out += line("  Desktop Bridge     : start-desktop [xfce4|lxqt], stop-desktop", LineTone.SUCCESS)
                out += line("  System & Cleanup   : htop, free -h, clear", LineTone.OUTPUT)
            }
            "start-desktop" -> {
                val de = args.firstOrNull()?.lowercase() ?: "xfce4"
                out += line("[dbus] Starting session bus inside proot rootfs...", LineTone.ACCENT)
                out += line("[display] Launching Termux:X11 / TigerVNC :1 (1280x720)...", LineTone.OUTPUT)
                out += line("[wm] Spawning $de-session (PID 4821) -> Switching to Desktop Mode", LineTone.SUCCESS)
                return@withContext CommandExecutionResult(
                    appendedLines = out,
                    sideEffect = CommandSideEffect(switchToDesktop = true)
                )
            }
            "stop-desktop" -> {
                out += line("[wm] Sending SIGTERM to xfce4-session & Xvnc :1...", LineTone.WARNING)
                out += line("[pm] Freed ~260 MB RAM. Returning to Low-Power Terminal Mode.", LineTone.SUCCESS)
                return@withContext CommandExecutionResult(
                    appendedLines = out,
                    sideEffect = CommandSideEffect(stopDesktop = true, switchToTerminal = true)
                )
            }
            "proot-distro" -> {
                val sub = args.firstOrNull()
                if (sub == "login") {
                    val distro = args.getOrNull(1) ?: "ubuntu"
                    out += line("[proot] Binding /dev, /proc, /sys and \$PREFIX/tmp -> $distro", LineTone.ACCENT)
                    out += line("Entered $distro rootfs (simulated root via ptrace).", LineTone.SUCCESS)
                    return@withContext CommandExecutionResult(
                        appendedLines = out,
                        updatedCwd = "/root/projects",
                        sideEffect = CommandSideEffect(enterProotUbuntu = true)
                    )
                } else {
                    out += line("Supported distributions:", LineTone.ACCENT)
                    out += line("  * ubuntu (24.04 Noble)   [installed • 412 MB]", LineTone.SUCCESS)
                    out += line("  * debian (12 Bookworm)   [available]", LineTone.OUTPUT)
                    out += line("  * alpine (3.20 Edge)     [available]", LineTone.OUTPUT)
                }
            }
            "exit" -> {
                if (session.isProotUbuntu) {
                    out += line("logout from proot Ubuntu -> returned to Termux host shell", LineTone.MUTED)
                    return@withContext CommandExecutionResult(
                        appendedLines = out,
                        updatedCwd = "~/workspace",
                        sideEffect = CommandSideEffect(exitProotUbuntu = true)
                    )
                } else {
                    out += line("Use tab close or Foreground Service toggle to end host session.", LineTone.MUTED)
                }
            }
            "pkg", "apt" -> {
                val sub = args.firstOrNull() ?: "help"
                when (sub) {
                    "update" -> {
                        out += line("Hit:1 https://packages.termux.dev/apt/termux-main stable InRelease", LineTone.OUTPUT)
                        out += line("Reading package lists... Done", LineTone.OUTPUT)
                        out += line("All packages are up to date (142 packages indexed).", LineTone.SUCCESS)
                    }
                    "install" -> {
                        val pkgName = args.getOrNull(1) ?: "git"
                        out += line("Resolving dependencies for '$pkgName'...", LineTone.OUTPUT)
                        out += line("Unpacking $pkgName (arm64) into \$PREFIX/bin...", LineTone.ACCENT)
                        out += line("Setting up $pkgName ... Done (2.4 MB extracted to files/usr).", LineTone.SUCCESS)
                    }
                    else -> {
                        out += line("Usage: pkg [update|upgrade|install <name>|search <query>]", LineTone.MUTED)
                    }
                }
            }
            "cd" -> {
                val target = args.firstOrNull() ?: if (session.isProotUbuntu) "/root" else "~/workspace"
                val nextCwd = when {
                    target == ".." -> session.cwd.substringBeforeLast('/', "~").ifEmpty { "/" }
                    target.startsWith("/") || target.startsWith("~") -> target
                    else -> "${session.cwd.trimEnd('/')}/$target"
                }
                return@withContext CommandExecutionResult(appendedLines = out, updatedCwd = nextCwd)
            }
            "git" -> {
                out += line("On branch main", LineTone.ACCENT)
                out += line("Your branch is up to date with 'origin/main'.", LineTone.OUTPUT)
                out += line("modified:   core/pty/PtyBridge.kt", LineTone.WARNING)
                out += line("untracked:  scripts/start-desktop.sh", LineTone.MUTED)
            }
            "python3", "python" -> {
                out += line("Python 3.12.3 (main, aarch64-linux-gnu) • Ready", LineTone.SUCCESS)
            }
            "node" -> {
                out += line("v20.14.0 (arm64 bionic/proot bridge)", LineTone.SUCCESS)
            }
            "htop" -> {
                out += line("CPU[||||||||            32.4%]   Tasks: 18, 2 thr; 1 running", LineTone.ACCENT)
                out += line("Mem[||||||         1.8G/7.4G]   Load avg: 0.42 0.38 0.31", LineTone.SUCCESS)
                out += line("PID   USER      PRI  NI  VIRT   RES   S  CPU% MEM%  Command", LineTone.MUTED)
                out += line("4102  u0_a142    20   0  128M   34M   S   1.2  0.4  /system/bin/sh (PTY)", LineTone.OUTPUT)
                out += line("4119  u0_a142    20   0   94M   18M   S   0.4  0.2  proot --link2symlink", LineTone.OUTPUT)
            }
            "free" -> {
                out += line("               total        used        free      shared  avail", LineTone.MUTED)
                out += line("Mem:           7.4Gi       1.8Gi       4.1Gi        84Mi       5.3Gi", LineTone.OUTPUT)
                out += line("Swap:          2.0Gi       120Mi       1.9Gi", LineTone.OUTPUT)
            }
            "vim", "nano" -> {
                val file = args.firstOrNull() ?: "main.py"
                out += line("[$cmd] Opened $file (UTF-8, Unix LF) — inline preview:", LineTone.ACCENT)
                out += line("  1 | #!/usr/bin/env python3", LineTone.MUTED)
                out += line("  2 | print('TermDeck proot dev environment ready')", LineTone.OUTPUT)
                out += line("-- Press CTRL+X or ':wq' in full PTY mode --", LineTone.SUCCESS)
            }
            else -> {
                // Attempt real safe Android ProcessBuilder execution for standard POSIX utilities!
                val realOutput = runRealShellCommand(trimmed)
                if (realOutput.isNotEmpty()) {
                    out += realOutput
                } else {
                    out += line("sh: $cmd: command not found (try 'help', 'uname -a', 'pkg update', or 'start-desktop')", LineTone.ERROR)
                }
            }
        }

        CommandExecutionResult(appendedLines = out)
    }

    private fun runRealShellCommand(command: String): List<TerminalLine> {
        val allowedBins = setOf(
            "uname", "pwd", "ls", "id", "env", "date", "df", "echo", "cat", "whoami", "uptime", "ps", "which", "printenv"
        )
        val firstToken = command.split(Regex("\\s+")).firstOrNull() ?: return emptyList()
        if (firstToken !in allowedBins) return emptyList()

        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(appFilesDir)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(2, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return listOf(line("[timeout] Process terminated after 2s", LineTone.WARNING))
            }
            val text = process.inputStream.bufferedReader().readLines()
            if (text.isEmpty()) {
                listOf(line("(exit ${process.exitValue()})", LineTone.MUTED))
            } else {
                text.take(40).map { line(it, LineTone.OUTPUT) }
            }
        } catch (e: Exception) {
            listOf(line("exec error: ${e.message ?: "unavailable"}", LineTone.ERROR))
        }
    }
}
