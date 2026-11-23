package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.desktop.DesktopEnvironment
import com.example.core.desktop.DesktopSessionState
import com.example.core.desktop.DesktopWindowApp
import com.example.core.desktop.DisplayBridge
import com.example.core.desktop.LinuxWindow
import com.example.core.desktop.ModifierKey
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberResource
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DesktopCyan
import com.example.ui.theme.DesktopCyanDim
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateSurfaceHighest
import com.example.ui.theme.TerminalEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun DesktopModeScreen(
    desktop: DesktopSessionState,
    onSelectEnvironment: (DesktopEnvironment) -> Unit,
    onSelectBridge: (DisplayBridge) -> Unit,
    onCycleResolution: () -> Unit,
    onStopDesktop: () -> Unit,
    onRestartDesktop: () -> Unit,
    onToggleTrackpad: () -> Unit,
    onMoveCursorBy: (Float, Float) -> Unit,
    onDirectCanvasTap: (Float, Float) -> Unit,
    onLeftClick: () -> Unit,
    onMiddleClick: () -> Unit,
    onRightClick: () -> Unit,
    onToggleDragLock: () -> Unit,
    onScrollDesktop: (Float) -> Unit,
    onToggleModifier: (ModifierKey) -> Unit,
    onTriggerShortcut: (String) -> Unit,
    onOpenWindow: (DesktopWindowApp) -> Unit,
    onFocusWindow: (String) -> Unit,
    onCloseWindow: (String) -> Unit,
    onInjectText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showKeyInjector by remember { mutableStateOf(false) }
    var keyDraft by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .navigationBarsPadding()
    ) {
        // Desktop Session Control Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // DE selector chips
            DesktopEnvironment.entries.forEach { env ->
                val active = desktop.environment == env
                FilterTagChip(
                    label = env.label,
                    selected = active,
                    activeColor = DesktopCyan,
                    activeBg = DesktopCyanDim,
                    onClick = { onSelectEnvironment(env) }
                )
            }

            // Bridge selector
            DisplayBridge.entries.forEach { bridge ->
                val active = desktop.bridge == bridge
                FilterTagChip(
                    label = bridge.label,
                    selected = active,
                    activeColor = TerminalEmerald,
                    activeBg = Color(0xFF0D382D),
                    onClick = { onSelectBridge(bridge) }
                )
            }

            // Resolution chip
            FilterTagChip(
                label = desktop.resolution.label,
                selected = false,
                activeColor = TextPrimary,
                activeBg = SlateSurfaceHighest,
                onClick = onCycleResolution
            )

            // Restart session
            FilterTagChip(
                label = "Reset",
                icon = Icons.Filled.Refresh,
                selected = false,
                activeColor = TextPrimary,
                activeBg = SlateSurfaceHighest,
                onClick = onRestartDesktop
            )

            // Stop Desktop -> saves battery & returns to Terminal
            FilterTagChip(
                label = "Kill X11",
                icon = Icons.Filled.PowerSettingsNew,
                selected = true,
                activeColor = CrimsonError,
                activeBg = Color(0xFF3A131C),
                onClick = onStopDesktop
            )
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

        // Linux Desktop Display Canvas (X11 / VNC Framebuffer Simulation)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, DesktopCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            DesktopDisplayViewport(
                desktop = desktop,
                onDirectTap = onDirectCanvasTap,
                onOpenWindow = onOpenWindow,
                onFocusWindow = onFocusWindow,
                onCloseWindow = onCloseWindow,
                onStopDesktop = onStopDesktop,
                onToggleAppMenu = { onToggleModifier(ModifierKey.SUPER) }
            )
        }

        // Collapse/Expand Trackpad Header + Live X11 Event Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .clickable(onClick = onToggleTrackpad)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("toggle_trackpad_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mouse,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = DesktopCyan
                )
                Text(
                    text = desktop.lastInputEvent,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val px = (desktop.cursorX * desktop.resolution.width).toInt()
                val py = (desktop.cursorY * desktop.resolution.height).toInt()
                Text(
                    text = "${px},${py}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DesktopCyan
                )
                Icon(
                    imageVector = if (desktop.trackpadExpanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                    contentDescription = "Toggle Trackpad Deck",
                    tint = TextPrimary
                )
            }
        }

        // Collapsible Trackpad, Mouse Click Buttons & Extra Modifier Keys Deck
        AnimatedVisibility(visible = desktop.trackpadExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlateSurface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Modifier Keys + X11 Shortcuts Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModifierKey.entries.forEach { mod ->
                        val active = mod in desktop.activeModifiers
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (active) DesktopCyanDim else SlateSurfaceHighest)
                                .border(
                                    1.dp,
                                    if (active) DesktopCyan else BorderSubtle,
                                    RoundedCornerShape(7.dp)
                                )
                                .clickable { onToggleModifier(mod) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mod.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) DesktopCyan else TextPrimary
                            )
                        }
                    }

                    listOf("Tab", "Esc", "Ctrl+Alt+T", "Alt+F4", "Ctrl+C").forEach { shortcut ->
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(SlateSurfaceElevated)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(7.dp))
                                .clickable { onTriggerShortcut(shortcut) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortcut,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (showKeyInjector) DesktopCyanDim else SlateSurfaceElevated)
                            .border(1.dp, if (showKeyInjector) DesktopCyan else BorderSubtle, RoundedCornerShape(7.dp))
                            .clickable { showKeyInjector = !showKeyInjector }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Keyboard,
                                contentDescription = "Type into desktop window",
                                modifier = Modifier.size(14.dp),
                                tint = if (showKeyInjector) DesktopCyan else TextPrimary
                            )
                            Text(
                                text = "Type",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (showKeyInjector) DesktopCyan else TextPrimary
                            )
                        }
                    }
                }

                // Optional inline X11 key injector
                AnimatedVisibility(visible = showKeyInjector) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianBg)
                            .border(1.dp, DesktopCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (keyDraft.isEmpty()) {
                                Text(
                                    text = "Type text to inject into focused X11 window...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextMuted
                                )
                            }
                            BasicTextField(
                                value = keyDraft,
                                onValueChange = { keyDraft = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.labelMedium.copy(
                                    color = TextPrimary,
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                cursorBrush = SolidColor(DesktopCyan),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = {
                                    onInjectText(keyDraft)
                                    keyDraft = ""
                                }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = DesktopCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DesktopCyanDim)
                                .clickable {
                                    onInjectText(keyDraft)
                                    keyDraft = ""
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Touch Trackpad + Dedicated Vertical Scroll Rail + Click Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Relative Touch Trackpad Surface
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ObsidianBg)
                            .border(
                                1.dp,
                                if (desktop.dragLock) AmberResource else BorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { onLeftClick() },
                                    onDoubleTap = { onLeftClick() }
                                )
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val sensitivity = 1.45f
                                    val dx = (dragAmount.x / size.width.toFloat()) * sensitivity
                                    val dy = (dragAmount.y / size.height.toFloat()) * sensitivity
                                    onMoveCursorBy(dx, dy)
                                }
                            }
                            .testTag("desktop_touch_trackpad")
                    ) {
                        // Subtle dot-matrix trackpad grid
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val step = 22.dp.toPx()
                            var x = step
                            while (x < size.width) {
                                var y = step
                                while (y < size.height) {
                                    drawCircle(
                                        color = Color(0xFF232D42),
                                        radius = 1.5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                    y += step
                                }
                                x += step
                            }
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = if (desktop.dragLock) AmberResource else TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (desktop.dragLock) "DRAG LOCK ACTIVE" else "TRACKPAD • DRAG TO MOVE • TAP TO CLICK",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (desktop.dragLock) AmberResource else TextMuted
                            )
                        }
                    }

                    // Dedicated Scroll Edge Strip
                    Box(
                        modifier = Modifier
                            .width(34.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateSurfaceElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onScrollDesktop(dragAmount.y)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 10.dp)
                        ) {
                            Text("▲", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text("SCR", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text("▼", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }

                // Mouse Click Buttons Row (Left, Drag Lock, Mid, Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MouseButton(
                        label = "Left Click",
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("mouse_left_click"),
                        accent = DesktopCyan,
                        onClick = onLeftClick
                    )
                    MouseButton(
                        label = if (desktop.dragLock) "Dragging" else "Drag",
                        icon = if (desktop.dragLock) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mouse_drag_lock"),
                        active = desktop.dragLock,
                        accent = AmberResource,
                        activeBg = AmberDim,
                        onClick = onToggleDragLock
                    )
                    MouseButton(
                        label = "Mid",
                        modifier = Modifier.weight(0.75f),
                        accent = TextPrimary,
                        onClick = onMiddleClick
                    )
                    MouseButton(
                        label = "Right Click",
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("mouse_right_click"),
                        accent = TerminalEmerald,
                        onClick = onRightClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopDisplayViewport(
    desktop: DesktopSessionState,
    onDirectTap: (Float, Float) -> Unit,
    onOpenWindow: (DesktopWindowApp) -> Unit,
    onFocusWindow: (String) -> Unit,
    onCloseWindow: (String) -> Unit,
    onStopDesktop: () -> Unit,
    onToggleAppMenu: () -> Unit
) {
    val clickRingRadius = remember { Animatable(0f) }
    val clickRingAlpha = remember { Animatable(0f) }

    LaunchedEffect(desktop.clickPulseTimestamp) {
        if (desktop.clickPulseTimestamp > 0L) {
            clickRingRadius.snapTo(4f)
            clickRingAlpha.snapTo(0.95f)
            clickRingRadius.animateTo(26f, animationSpec = tween(260))
            clickRingAlpha.animateTo(0f, animationSpec = tween(220))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fx = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val fy = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                    onDirectTap(fx, fy)
                }
            }
    ) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val isPanelTop = desktop.environment == DesktopEnvironment.XFCE4

        // Generated Linux Desktop Wallpaper
        Image(
            painter = painterResource(id = R.drawable.img_desktop_wallpaper),
            contentDescription = stringResource(R.string.cd_desktop_wallpaper),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle dark scrim for window readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33070A10))
        )

        // Render Linux Windows
        desktop.windows.filterNot { it.isMinimized }.forEach { win ->
            LinuxWindowCard(
                window = win,
                xOffsetPx = (win.xFraction * wPx).roundToInt(),
                yOffsetPx = (win.yFraction * hPx).roundToInt(),
                widthDp = maxWidth * win.widthFraction,
                heightDp = maxHeight * win.heightFraction,
                onFocus = { onFocusWindow(win.id) },
                onClose = { onCloseWindow(win.id) }
            )
        }

        // Desktop Environment Panel (Top for XFCE4, Bottom for LXQt)
        val panelAlignment = if (isPanelTop) Alignment.TopCenter else Alignment.BottomCenter
        Row(
            modifier = Modifier
                .align(panelAlignment)
                .fillMaxWidth()
                .height(30.dp)
                .background(Color(0xEB0E1420))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Applications launcher button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (desktop.appMenuOpen) DesktopCyanDim else Color(0xFF1A2336))
                        .clickable(onClick = onToggleAppMenu)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Apps,
                        contentDescription = "Applications",
                        modifier = Modifier.size(13.dp),
                        tint = DesktopCyan
                    )
                    Text(
                        text = if (isPanelTop) "Apps" else "Menu",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                // Taskbar window pills
                desktop.windows.take(3).forEach { win ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (win.isFocused) DesktopCyanDim else Color(0xFF171E2E))
                            .clickable { onFocusWindow(win.id) }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = win.title.substringBefore(" —"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (win.isFocused) DesktopCyan else TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }

            // System Tray readout
            Text(
                text = "${desktop.bridge.socketInfo} • 00:24",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TerminalEmerald
            )
        }

        // Applications Launcher Popover
        if (desktop.appMenuOpen) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xF5131A29),
                border = androidx.compose.foundation.BorderStroke(1.dp, DesktopCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(if (isPanelTop) Alignment.TopStart else Alignment.BottomStart)
                    .padding(start = 8.dp, top = if (isPanelTop) 34.dp else 0.dp, bottom = if (isPanelTop) 0.dp else 34.dp)
                    .width(184.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${desktop.environment.label} Applications",
                        style = MaterialTheme.typography.labelSmall,
                        color = DesktopCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                    AppMenuRow("Terminal Emulator", Icons.Filled.Terminal) {
                        onOpenWindow(DesktopWindowApp.TERMINAL)
                    }
                    AppMenuRow("Mousepad Code Editor", Icons.Filled.Keyboard) {
                        onOpenWindow(DesktopWindowApp.CODE_EDITOR)
                    }
                    AppMenuRow("Thunar File Manager", Icons.Filled.Folder) {
                        onOpenWindow(DesktopWindowApp.FILE_MANAGER)
                    }
                    HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 2.dp))
                    AppMenuRow("Exit to Terminal Mode", Icons.Filled.PowerSettingsNew, tint = CrimsonError) {
                        onStopDesktop()
                    }
                }
            }
        }

        // Right-click Context Menu
        desktop.contextMenuPos?.let { (cx, cy) ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xF5131A29),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                    .offset { IntOffset((cx * wPx).roundToInt(), (cy * hPx).roundToInt()) }
                    .width(164.dp)
            ) {
                Column(modifier = Modifier.padding(5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AppMenuRow("Open Terminal Here", Icons.Filled.Terminal) {
                        onOpenWindow(DesktopWindowApp.TERMINAL)
                    }
                    AppMenuRow("New File in Editor", Icons.Filled.Keyboard) {
                        onOpenWindow(DesktopWindowApp.CODE_EDITOR)
                    }
                    AppMenuRow("Open Thunar", Icons.Filled.Folder) {
                        onOpenWindow(DesktopWindowApp.FILE_MANAGER)
                    }
                }
            }
        }

        // Live X11 Mouse Cursor + Click Pulse Ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = desktop.cursorX * size.width
            val cy = desktop.cursorY * size.height

            if (clickRingAlpha.value > 0.01f) {
                drawCircle(
                    color = DesktopCyan.copy(alpha = clickRingAlpha.value),
                    radius = clickRingRadius.value,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            val arrow = Path().apply {
                moveTo(cx, cy)
                lineTo(cx, cy + 16.dp.toPx())
                lineTo(cx + 4.5.dp.toPx(), cy + 12.dp.toPx())
                lineTo(cx + 9.dp.toPx(), cy + 19.dp.toPx())
                lineTo(cx + 11.5.dp.toPx(), cy + 17.5.dp.toPx())
                lineTo(cx + 7.dp.toPx(), cy + 10.5.dp.toPx())
                lineTo(cx + 12.5.dp.toPx(), cy + 10.5.dp.toPx())
                close()
            }
            drawPath(arrow, color = Color.White)
            drawPath(arrow, color = Color.Black, style = Stroke(width = 1.2.dp.toPx()))
        }
    }
}

@Composable
private fun LinuxWindowCard(
    window: LinuxWindow,
    xOffsetPx: Int,
    yOffsetPx: Int,
    widthDp: androidx.compose.ui.unit.Dp,
    heightDp: androidx.compose.ui.unit.Dp,
    onFocus: () -> Unit,
    onClose: () -> Unit
) {
    val borderColor = if (window.isFocused) DesktopCyan else BorderSubtle
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xEB0E131D),
        border = androidx.compose.foundation.BorderStroke(if (window.isFocused) 1.5.dp else 1.dp, borderColor),
        shadowElevation = if (window.isFocused) 8.dp else 2.dp,
        modifier = Modifier
            .offset { IntOffset(xOffsetPx, yOffsetPx) }
            .size(width = widthDp, height = heightDp)
            .clickable(onClick = onFocus)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Window Titlebar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (window.isFocused) Color(0xFF1A253A) else Color(0xFF141B29))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = window.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold),
                    color = if (window.isFocused) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close ${window.title}",
                    modifier = Modifier
                        .size(13.dp)
                        .clickable(onClick = onClose),
                    tint = CrimsonError
                )
            }
            // Window Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                window.bodyLines.forEach { line ->
                    val color = when {
                        window.app == DesktopWindowApp.TERMINAL && line.startsWith("root@") -> TerminalEmerald
                        window.app == DesktopWindowApp.CODE_EDITOR && line.contains("def ") -> DesktopCyan
                        else -> TextPrimary
                    }
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AppMenuRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = tint)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

@Composable
private fun FilterTagChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    activeBg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) activeBg else SlateSurfaceElevated)
            .border(1.dp, if (selected) activeColor.copy(alpha = 0.6f) else BorderSubtle, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = activeColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) activeColor else TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun MouseButton(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    active: Boolean = false,
    activeBg: Color = SlateSurfaceHighest
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) activeBg else SlateSurfaceHighest)
            .border(
                1.dp,
                if (active) accent else BorderSubtle,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (active) accent else TextSecondary
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (active) accent else TextPrimary,
                maxLines = 1
            )
        }
    }
}
