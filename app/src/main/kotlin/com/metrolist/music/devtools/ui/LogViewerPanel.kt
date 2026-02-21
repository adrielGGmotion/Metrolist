/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools.ui

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.devtools.DevToolsLog
import com.metrolist.music.devtools.DevToolsLogBuffer
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val logTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

@androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerPanel(buffer: DevToolsLogBuffer) {
    val logs by buffer.logs.collectAsState()
    
    var selectedLevels by remember { mutableStateOf(setOf<Int>()) }
    var selectedTagGroups by remember { mutableStateOf(setOf<String>()) }
    var selectedLogIds by remember { mutableStateOf(setOf<Long>()) }
    var showFilters by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Expanded)
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val tagPlayer = stringResource(R.string.dev_filter_player)
    val tagUi = stringResource(R.string.dev_filter_ui)
    val tagDb = stringResource(R.string.dev_filter_db)
    val tagIntegration = stringResource(R.string.dev_filter_integration)

    val filteredLogs by remember(tagPlayer, tagUi, tagDb, tagIntegration) {
        derivedStateOf {
            logs.filter { log ->
                val levelMatch = selectedLevels.isEmpty() || log.priority in selectedLevels
                val tagMatch = selectedTagGroups.isEmpty() || selectedTagGroups.any { group ->
                    when (group) {
                        tagPlayer -> log.tag.contains("Player") || log.tag.contains("ExoPlayer") || log.tag.contains("MusicService")
                        tagUi -> log.tag.contains("Screen") || log.tag.contains("Activity")
                        tagDb -> log.tag.contains("Room") || log.tag.contains("Database") || log.tag.contains("Dao")
                        tagIntegration -> log.tag.contains("Discord") || log.tag.contains("LastFM") || log.tag.contains("Kizzy")
                        else -> true
                    }
                }
                val query = textFieldState.text.toString()
                val textMatch = query.isBlank() ||
                    log.message.contains(query, ignoreCase = true) ||
                    log.tag.contains(query, ignoreCase = true) ||
                    (log.throwable?.contains(query, ignoreCase = true) == true)
                levelMatch && tagMatch && textMatch
            }
        }
    }
    
    // Auto-scroll to the latest log entry whenever the list grows
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.scrollToItem(filteredLogs.lastIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PanelHeader(title = stringResource(R.string.logs), subtitle = stringResource(R.string.logs_subtitle))

            // M3 SearchBar with expand/collapse animation
            SearchBar(
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        textFieldState = textFieldState,
                        searchBarState = searchBarState,
                        onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
                        placeholder = { Text(stringResource(R.string.search_logs)) },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.search),
                                contentDescription = stringResource(R.string.search_logs)
                            )
                        },
                        trailingIcon = {
                            if (textFieldState.text.isNotEmpty()) {
                                IconButton(onClick = { textFieldState.clearText() }) {
                                    Icon(
                                        painterResource(R.drawable.close),
                                        contentDescription = stringResource(R.string.clear_search)
                                    )
                                }
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Contextual Toolbar (Only show when selecting logs)
            AnimatedVisibility(visible = selectedLogIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.selected_count, selectedLogIds.size),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        val logsToCopy = logs.filter { it.id in selectedLogIds }
                        val text = logsToCopy.joinToString("\n\n") { log ->
                            "${logTimeFormatter.format(Instant.ofEpochMilli(log.timestamp))} ${log.priorityLabel}/${log.tag}\n${log.message}${log.throwable?.let { "\n$it" } ?: ""}"
                        }
                        clipboardManager.setText(AnnotatedString(text))
                        selectedLogIds = emptySet()
                    }) {
                        Icon(painterResource(R.drawable.content_copy), contentDescription = stringResource(R.string.copy_selected), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    IconButton(onClick = { selectedLogIds = emptySet() }) {
                        Icon(painterResource(R.drawable.close), contentDescription = stringResource(R.string.clear_selection), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            
            // Collapsible Filters
            AnimatedVisibility(visible = showFilters) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        // Filters - Levels
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedLevels.isEmpty(),
                                onClick = { selectedLevels = emptySet() },
                                label = { Text(stringResource(R.string.all_levels)) }
                            )
                            val levels = listOf(
                                Log.VERBOSE to stringResource(R.string.log_verbose),
                                Log.DEBUG to stringResource(R.string.log_debug),
                                Log.INFO to stringResource(R.string.log_info),
                                Log.WARN to stringResource(R.string.log_warn),
                                Log.ERROR to stringResource(R.string.log_error)
                            )
                            levels.forEach { (level, name) ->
                                FilterChip(
                                    selected = level in selectedLevels,
                                    onClick = { 
                                        selectedLevels = if (level in selectedLevels) {
                                            selectedLevels - level
                                        } else {
                                            selectedLevels + level
                                        }
                                    },
                                    label = { Text(name) }
                                )
                            }
                        }
                        
                        // Filters - Subsystems
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedTagGroups.isEmpty(),
                                onClick = { selectedTagGroups = emptySet() },
                                label = { Text(stringResource(R.string.all_tags)) }
                            )
                            listOf(tagPlayer, tagUi, tagDb, tagIntegration).forEach { group ->
                                FilterChip(
                                    selected = group in selectedTagGroups,
                                    onClick = { 
                                        selectedTagGroups = if (group in selectedTagGroups) {
                                            selectedTagGroups - group
                                        } else {
                                            selectedTagGroups + group
                                        }
                                    },
                                    label = { Text(group) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Log List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(filteredLogs, key = { it.id }) { log ->
                    val isSelected = log.id in selectedLogIds
                    LogRow(
                        log = log,
                        isSelected = isSelected,
                        onToggleSelect = {
                            selectedLogIds = if (isSelected) selectedLogIds - log.id else selectedLogIds + log.id
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // padding for FABs
            }
        }
        
        // FAB Actions (Filter and Clear)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showFilters = !showFilters },
                containerColor = if (showFilters) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    painterResource(R.drawable.tune), 
                    contentDescription = stringResource(R.string.toggle_filters),
                    tint = if (showFilters) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            SmallFloatingActionButton(
                onClick = { buffer.clear() },
                containerColor = MaterialTheme.colorScheme.errorContainer
            ) {
                Icon(
                    painterResource(R.drawable.delete), 
                    contentDescription = stringResource(R.string.clear_logs),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
fun LogRow(log: DevToolsLog, isSelected: Boolean, onToggleSelect: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    val color = when (log.priority) {
        Log.VERBOSE -> MaterialTheme.colorScheme.outline
        Log.DEBUG -> MaterialTheme.colorScheme.primary
        Log.INFO -> MaterialTheme.colorScheme.secondary
        Log.WARN -> MaterialTheme.colorScheme.tertiary
        Log.ERROR, Log.ASSERT -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    
    val timeStr = remember(log.timestamp) {
        logTimeFormatter.format(Instant.ofEpochMilli(log.timestamp))
    }

    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
            modifier = Modifier.clickable { 
                if (isSelected) onToggleSelect() else expanded = !expanded 
            }
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Priority Color Indicator Line
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(color)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeStr,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = log.tag,
                            color = color,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        
                        Spacer(Modifier.weight(1f))
                        
                        IconButton(
                            onClick = onToggleSelect,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(if (isSelected) R.drawable.check else R.drawable.radio_button_unchecked),
                                contentDescription = stringResource(R.string.select_log),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    SelectionContainer {
                        Column {
                            Text(
                                text = log.message,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp,
                                maxLines = if (expanded) Int.MAX_VALUE else 3
                            )
                            if (expanded && log.throwable != null) {
                                Text(
                                    text = log.throwable,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
