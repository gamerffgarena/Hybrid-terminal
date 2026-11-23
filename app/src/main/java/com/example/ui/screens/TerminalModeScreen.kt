package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.pty.LineTone
import com.example.core.pty.TerminalSessionTab
import com.example.ui.theme.AmberResource
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DesktopCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.SlateSurfaceHighest
import com.example.ui.theme.TerminalEmerald
import com.example.ui.theme.TerminalEmeraldDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrompt

@Composable
fun TerminalModeScreen(
    tabs: List<TerminalSessionTab>,
    activeTab: TerminalSessionTab?,
    commandInput: String,
    stickyCtrl: Boolean,
    stickyAlt: Boolean,
    onSelectTab: (String) -> Unit,
    onAddTab: () -> Unit,
    onCloseTab: (String) -> Unit,
    onUpdateInput: (String) -> Unit,
    onSubmitCommand: (String?) -> Unit,
    onQuickKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lines = activeTab?.lines.orEmpty()

    LaunchedEffect(lines.size, activeTab?.id) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Session Tab Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { tab ->
                val selected = tab.id == activeTab?.id
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) SlateSurfaceHighest else SlateSurfaceElevated)
                        .border(
                            1.dp,
                            if (selected) TerminalEmerald.copy(alpha = 0.6f) else BorderSubtle,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectTab(tab.id) }
                        .padding(start = 10.dp, end = if (tabs.size > 1) 4.dp else 10.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) TerminalEmerald else TextSecondary
                    )
                    Text(
                        text = tab.shellLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tab.isProotUbuntu) DesktopCyan else TextMuted
                    )
                    if (tabs.size > 1) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close ${tab.title}",
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onCloseTab(tab.id) }
                                .padding(2.dp),
                            tint = TextMuted
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateSurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddTab)
                    .testTag("new_terminal_tab_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_new_tab),
                    modifier = Modifier.size(16.dp),
                    tint = TextPrimary
                )
            }
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

        // Terminal Output Buffer
        SelectionContainer(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(lines, key = { it.id }) { item ->
                    val lineColor = when (item.tone) {
                        LineTone.PROMPT -> if (activeTab?.isProotUbuntu == true) VioletPrompt else TerminalEmerald
                        LineTone.OUTPUT -> TextPrimary
                        LineTone.SUCCESS -> TerminalEmerald
                        LineTone.ACCENT -> DesktopCyan
                        LineTone.WARNING -> AmberResource
                        LineTone.ERROR -> CrimsonError
                        LineTone.MUTED -> TextSecondary
                    }
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = if (item.tone == LineTone.PROMPT) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = lineColor
                    )
                }
            }
        }

        // Quick Sample Workflow Chips
        val sampleCommands = listOf(
            "uname -a",
            "ls -la",
            "pkg update",
            "proot-distro login ubuntu",
            "htop",
            "start-desktop"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface.copy(alpha = 0.7f))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RUN:",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            sampleCommands.forEach { cmd ->
                val isDesktopCmd = cmd == "start-desktop"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDesktopCmd) TerminalEmeraldDim else SlateSurfaceElevated)
                        .border(
                            1.dp,
                            if (isDesktopCmd) TerminalEmerald.copy(alpha = 0.5f) else BorderSubtle,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSubmitCommand(cmd) }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isDesktopCmd) TerminalEmerald else DesktopCyan
                    )
                    Text(
                        text = cmd,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDesktopCmd) TerminalEmerald else TextPrimary
                    )
                }
            }
        }

        // Interactive Prompt Input Bar
        Surface(
            color = SlateSurfaceElevated,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val promptTag = if (activeTab?.isProotUbuntu == true) {
                    "root@ubuntu:${activeTab.cwd}#"
                } else {
                    "${activeTab?.cwd ?: "~"} $"
                }
                Text(
                    text = promptTag,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (activeTab?.isProotUbuntu == true) VioletPrompt else TerminalEmerald
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (commandInput.isEmpty()) {
                        Text(
                            text = "Enter command (e.g. help, uname -a)...",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextMuted
                        )
                    }
                    BasicTextField(
                        value = commandInput,
                        onValueChange = onUpdateInput,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.labelLarge.copy(
                            color = TextPrimary,
                            fontFamily = JetBrainsMonoFontFamily
                        ),
                        cursorBrush = SolidColor(TerminalEmerald),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSubmitCommand(null) }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("terminal_command_input")
                    )
                }
                IconButton(
                    onClick = { onSubmitCommand(null) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("terminal_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_run_command),
                        tint = TerminalEmerald
                    )
                }
            }
        }

        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

        // Termux-inspired Quick-Key Bar (Row 1 & Row 2 compact bar)
        val quickKeys = listOf(
            "ESC", "TAB", "CTRL", "ALT", "/", "-", "|", "~", "←", "↓", "↑", "→", "^C", "^D"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            quickKeys.forEach { key ->
                val isStickyActive = (key == "CTRL" && stickyCtrl) || (key == "ALT" && stickyAlt)
                Box(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            if (isStickyActive) TerminalEmeraldDim else SlateSurfaceHighest
                        )
                        .border(
                            1.dp,
                            if (isStickyActive) TerminalEmerald else BorderSubtle,
                            RoundedCornerShape(7.dp)
                        )
                        .clickable { onQuickKey(key) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isStickyActive) TerminalEmerald else TextPrimary
                    )
                }
            }
        }
    }
}
