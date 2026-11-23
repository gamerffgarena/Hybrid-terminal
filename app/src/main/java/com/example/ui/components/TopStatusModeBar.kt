package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Terminal
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.desktop.DesktopSessionState
import com.example.core.desktop.WorkspaceMode
import com.example.service.ForegroundSessionStatus
import com.example.ui.ResourceTelemetry
import com.example.ui.theme.AmberResource
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DesktopCyan
import com.example.ui.theme.DesktopCyanDim
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.TerminalEmerald
import com.example.ui.theme.TerminalEmeraldDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TopStatusModeBar(
    mode: WorkspaceMode,
    telemetry: ResourceTelemetry,
    serviceStatus: ForegroundSessionStatus,
    desktop: DesktopSessionState,
    onSelectMode: (WorkspaceMode) -> Unit,
    onToggleService: () -> Unit,
    onOpenBlueprint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "service_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SlateSurface)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary Row: Brand + Mode Switcher + Blueprint Guide button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (mode == WorkspaceMode.TERMINAL) TerminalEmeraldDim else DesktopCyanDim
                        )
                        .border(
                            width = 1.dp,
                            color = if (mode == WorkspaceMode.TERMINAL) TerminalEmerald else DesktopCyan,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ">_",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (mode == WorkspaceMode.TERMINAL) TerminalEmerald else DesktopCyan
                    )
                }
                Text(
                    text = "TermDeck",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
            }

            // Segmented Mode Switcher: Terminal vs Desktop
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModePill(
                        label = stringResource(R.string.mode_terminal),
                        icon = Icons.Filled.Terminal,
                        selected = mode == WorkspaceMode.TERMINAL,
                        activeColor = TerminalEmerald,
                        activeContainer = TerminalEmeraldDim,
                        testTag = "mode_terminal_button",
                        onClick = { onSelectMode(WorkspaceMode.TERMINAL) }
                    )
                    ModePill(
                        label = stringResource(R.string.mode_desktop),
                        icon = Icons.Filled.DesktopWindows,
                        selected = mode == WorkspaceMode.DESKTOP,
                        activeColor = DesktopCyan,
                        activeContainer = DesktopCyanDim,
                        testTag = "mode_desktop_button",
                        onClick = { onSelectMode(WorkspaceMode.DESKTOP) }
                    )
                }
            }

            // Architecture & Setup Scripts Guide trigger
            IconButton(
                onClick = onOpenBlueprint,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("blueprint_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountTree,
                    contentDescription = stringResource(R.string.cd_blueprint),
                    tint = TextPrimary
                )
            }
        }

        // Secondary Status & Resource Telemetry Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Foreground Service status chip (tappable)
            val svcActive = serviceStatus.isRunning
            val mins = serviceStatus.uptimeSeconds / 60
            val secs = serviceStatus.uptimeSeconds % 60
            val uptime = String.format("%02d:%02d", mins, secs)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateSurfaceElevated)
                    .border(
                        1.dp,
                        if (svcActive) TerminalEmerald.copy(alpha = 0.45f) else BorderSubtle,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onToggleService)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
                    .testTag("service_status_pill"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(if (svcActive) pulseAlpha else 1f)
                        .clip(CircleShape)
                        .background(if (svcActive) TerminalEmerald else TextMuted)
                )
                Text(
                    text = if (svcActive) "FGS $uptime" else "FGS Off",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (svcActive) TerminalEmerald else TextSecondary,
                    maxLines = 1
                )
            }

            // Runtime Backend Badge
            val backendText = if (mode == WorkspaceMode.TERMINAL) {
                "PTY • proot"
            } else {
                "${desktop.environment.label} • ${desktop.bridge.label}"
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BatteryChargingFull,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (mode == WorkspaceMode.TERMINAL) TerminalEmerald else AmberResource
                )
                Text(
                    text = "${telemetry.powerProfile} • $backendText",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // RAM & CPU Footprint Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (mode == WorkspaceMode.TERMINAL) DesktopCyan else AmberResource
                )
                Text(
                    text = "${telemetry.workspaceRamMb}MB • ${telemetry.cpuLoadPercent}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ModePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    activeColor: Color,
    activeContainer: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) activeContainer else Color.Transparent,
        animationSpec = tween(180),
        label = "mode_pill_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) activeColor else TextSecondary,
        animationSpec = tween(180),
        label = "mode_pill_fg"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = fg
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = fg,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(1.dp))
    }
}
