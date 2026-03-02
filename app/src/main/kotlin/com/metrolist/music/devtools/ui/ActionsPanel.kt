/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import coil3.imageLoader
import com.metrolist.music.devtools.DevToolsLogBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

private val logTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

private val SENSITIVE_PATTERNS = listOf(
    Pattern.compile("([Aa]uth|[Tt]oken)[=:]\\s*[\\w\\-]{10,}", Pattern.CASE_INSENSITIVE),
    Pattern.compile("(cookie|session)[=:]\\s*[\\w\\-]{10,}", Pattern.CASE_INSENSITIVE),
    Pattern.compile("(visitorData)[=:]\\s*[\\w\\-]{20,}", Pattern.CASE_INSENSITIVE),
    Pattern.compile("(dataSyncId)[=:]\\s*[\\w\\-]{20,}", Pattern.CASE_INSENSITIVE),
    Pattern.compile("(SAPISID)[=:]\\s*[\\w\\-]{20,}", Pattern.CASE_INSENSITIVE),
    Pattern.compile("(__Secure-[A-Z]+)[=:]\\s*[\\w\\-]{10,}", Pattern.CASE_INSENSITIVE),
)

private fun redactSensitiveData(text: String): String {
    var result = text
    for (pattern in SENSITIVE_PATTERNS) {
        result = pattern.matcher(result).replaceAll("$1=<REDACTED>")
    }
    return result
}

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
                    coroutineScope.launch {
                        try {
                            val logs = buffer.logs.value
                            val runtime = Runtime.getRuntime()
                            val actManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                            val memInfo = android.app.ActivityManager.MemoryInfo().apply { actManager.getMemoryInfo(this) }
                            val totalRamGb = memInfo.totalMem / 1024 / 1024 / 1024
                            val availRamGb = memInfo.availMem / 1024 / 1024 / 1024
                            val maxHeapMb = runtime.maxMemory() / 1024 / 1024
                            val displayMetrics = context.resources.displayMetrics
                            val resolution = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
                            val density = displayMetrics.densityDpi
                            
                            val envHeader = context.getString(
                                R.string.dev_export_header,
                                com.metrolist.music.BuildConfig.VERSION_NAME,
                                com.metrolist.music.BuildConfig.VERSION_CODE.toString(),
                                if (com.metrolist.music.BuildConfig.DEBUG) context.getString(R.string.dev_debug) else context.getString(R.string.dev_release),
                                com.metrolist.music.BuildConfig.ARCHITECTURE,
                                android.os.Build.MANUFACTURER,
                                android.os.Build.MODEL,
                                android.os.Build.VERSION.RELEASE,
                                android.os.Build.VERSION.SDK_INT.toString(),
                                resolution,
                                density.toString(),
                                availRamGb.toString(),
                                totalRamGb.toString(),
                                maxHeapMb.toString()
                            )
        
                            val uri = withContext(Dispatchers.IO) {
                                val file = java.io.File(context.cacheDir, context.getString(R.string.dev_export_filename, System.currentTimeMillis().toString()))
                                file.bufferedWriter().use { writer ->
                                    writer.write(envHeader)
                                    logs.forEach { log ->
                                        val redactedMessage = redactSensitiveData(log.message)
                                        val redactedThrowable = log.throwable?.let { redactSensitiveData(it) }
                                        writer.write("${logTimeFormatter.format(Instant.ofEpochMilli(log.timestamp))} ${log.priorityLabel}/${log.tag}: $redactedMessage${redactedThrowable?.let { t -> "\n$t" } ?: ""}\n")
                                    }
                                }
                                androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
                            }
                            
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, uri)
                                type = "text/plain"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.export_devtools_logs))
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show()
                        }
                    }
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
                                val files = context.cacheDir.listFiles()
                                var allDeleted = true
                                files?.forEach { if (!it.deleteRecursively()) allDeleted = false }
                                if (!allDeleted) {
                                    // Some files couldn't be deleted, but we continue
                                }
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
                title = stringResource(R.string.dev_clear_image_cache),
                description = stringResource(R.string.dev_clear_image_cache_desc),
                buttonText = stringResource(R.string.dev_clear_image_cache),
                iconRes = R.drawable.delete,
                isDestructive = true,
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            context.imageLoader.memoryCache?.clear()
                            context.imageLoader.diskCache?.clear()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, context.getString(R.string.dev_cleared_image_cache), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, context.getString(R.string.failed_clear_cache, e.message), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
            
            val colors = if (isDestructive) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            } else {
                ButtonDefaults.filledTonalButtonColors()
            }

            FilledTonalButton(
                onClick = onClick,
                colors = colors,
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
