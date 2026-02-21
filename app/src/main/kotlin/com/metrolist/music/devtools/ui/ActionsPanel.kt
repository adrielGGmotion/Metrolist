/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.devtools.DevToolsLogBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionsPanel(buffer: DevToolsLogBuffer) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(
            title = stringResource(R.string.dev_actions),
            subtitle = stringResource(R.string.dev_actions_subtitle)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            ActionCard(
                title = stringResource(R.string.export_logs),
                description = stringResource(R.string.export_logs_desc),
                buttonText = stringResource(R.string.export_now),
                iconRes = R.drawable.share,
                onClick = {
                    val logs = buffer.logs.value
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())
                    val logString = logs.joinToString("\n") { 
                        "${formatter.format(Instant.ofEpochMilli(it.timestamp))} ${it.priorityLabel}/${it.tag}: ${it.message}${it.throwable?.let { t -> "\n$t" } ?: ""}" 
                    }
                    
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, logString)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.export_devtools_logs))
                    context.startActivity(shareIntent)
                }
            )

            ActionCard(
                title = stringResource(R.string.clear_cache),
                description = stringResource(R.string.clear_cache_desc),
                buttonText = stringResource(R.string.clear_cache),
                iconRes = R.drawable.delete,
                isDestructive = true,
                onClick = {
                    coroutineScope.launch {
                        try {
                            val sizeMb = withContext(Dispatchers.IO) {
                                val size = context.cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                size / 1024 / 1024
                            }
                            Toast.makeText(context, context.getString(R.string.cleared_cache_mb, sizeMb), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.failed_clear_cache, e.message), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            ActionCard(
                title = stringResource(R.string.test_crash_handler),
                description = stringResource(R.string.test_crash_handler_desc),
                buttonText = stringResource(R.string.force_crash),
                iconRes = R.drawable.bug_report,
                isDestructive = true,
                onClick = {
                    throw RuntimeException("Developer Triggered Crash (from DevTools overlay)")
                }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionCard(
    title: String,
    description: String,
    buttonText: String,
    iconRes: Int,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            
            FilledTonalButton(
                onClick = onClick,
                colors = if (isDestructive) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText)
            }
        }
    }
}
