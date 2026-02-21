/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.R
import com.metrolist.music.constants.DeveloperModeKey
import com.metrolist.music.devtools.DevToolsLogBuffer
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

/** Bottom padding for the DevTools FAB to avoid overlapping the mini-player. */
private val DevToolsFabBottomPadding = 120.dp

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PanelHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        LinearWavyProgressIndicator(
            progress = { 1f },
            amplitude = { 1f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DevToolsOverlay(
    logBuffer: DevToolsLogBuffer,
    isPlayerExpanded: Boolean
) {
    val devMode by rememberPreference(DeveloperModeKey, defaultValue = false)
    if (!devMode) return

    // Hide FAB when full-screen player is open to avoid clipping UI
    if (isPlayerExpanded) return

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var isPanelExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { isPanelExpanded = true },
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(-screenWidthPx + 150f, 0f)
                        offsetY = (offsetY + dragAmount.y).coerceIn(-screenHeightPx / 2f + 150f, screenHeightPx / 2f - 150f)
                    }
                }
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, bottom = DevToolsFabBottomPadding),
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        ) {
            Icon(
                painterResource(R.drawable.bug_report),
                contentDescription = stringResource(R.string.dev_tools)
            )
        }
    }

    if (isPanelExpanded) {
        Dialog(
            onDismissRequest = { isPanelExpanded = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(top = 40.dp) // Avoid status bar
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    var selectedTab by remember { mutableIntStateOf(0) }
                    val tabs = listOf(stringResource(R.string.dev_tab_logs), stringResource(R.string.dev_tab_player), stringResource(R.string.dev_tab_env), stringResource(R.string.dev_tab_db), stringResource(R.string.dev_tab_tools))

                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> LogViewerPanel(logBuffer)
                            1 -> PlayerStatePanel()
                            2 -> EnvironmentInfoPanel()
                            3 -> DatabaseInfoPanel()
                            4 -> ActionsPanel(logBuffer)
                        }
                    }
                }
            }
        }
    }
}
