package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColorBorderMuted = androidx.compose.ui.graphics.Color(0xFF1D2638)

private val TermDeckDarkColorScheme = darkColorScheme(
    primary = TerminalEmerald,
    onPrimary = ObsidianBg,
    primaryContainer = TerminalEmeraldDim,
    onPrimaryContainer = TerminalEmerald,
    secondary = DesktopCyan,
    onSecondary = ObsidianBg,
    secondaryContainer = DesktopCyanDim,
    onSecondaryContainer = DesktopCyan,
    tertiary = AmberResource,
    onTertiary = ObsidianBg,
    tertiaryContainer = AmberDim,
    onTertiaryContainer = AmberResource,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceElevated,
    surfaceContainerHigh = SlateSurfaceHighest,
    outline = BorderSubtle,
    outlineVariant = ColorBorderMuted,
    error = CrimsonError,
    onError = ObsidianBg
)

@Composable
fun TermDeckTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TermDeckDarkColorScheme,
        typography = TermDeckTypography,
        content = content
    )
}
