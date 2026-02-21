/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.devtools.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import java.text.DecimalFormat

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EnvironmentInfoPanel() {
    val context = LocalContext.current
    val runtime = remember { Runtime.getRuntime() }
    val format = remember { DecimalFormat("#.##") }
    val maxHeapMb = remember { format.format(runtime.maxMemory() / 1024.0 / 1024.0) }
    val memInfo = remember(context) { 
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        ActivityManager.MemoryInfo().apply { actManager.getMemoryInfo(this) }
    }
    val totalRamGb = remember(memInfo) { format.format(memInfo.totalMem / 1024.0 / 1024.0 / 1024.0) }
    val availRamGb = remember(memInfo) { format.format(memInfo.availMem / 1024.0 / 1024.0 / 1024.0) }
    val displayMetrics = remember(context) { context.resources.displayMetrics }
    val resolution = remember(displayMetrics) { "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}" }
    val density = remember(displayMetrics) { displayMetrics.densityDpi }

    Column(modifier = Modifier.fillMaxSize()) {
        PanelHeader(
            title = stringResource(R.string.environment),
            subtitle = stringResource(R.string.environment_subtitle)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoCard(title = stringResource(R.string.app_info)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoRow(stringResource(R.string.app_version), BuildConfig.VERSION_NAME, modifier = Modifier.weight(1f))
                        InfoRow(stringResource(R.string.version_code), BuildConfig.VERSION_CODE.toString(), modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoRow(stringResource(R.string.build_type), if (BuildConfig.DEBUG) stringResource(R.string.dev_build_type_debug) else stringResource(R.string.dev_build_type_release), modifier = Modifier.weight(1f))
                        InfoRow(stringResource(R.string.architecture), BuildConfig.ARCHITECTURE, modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                InfoCard(title = stringResource(R.string.device_info)) {
                    InfoRow(stringResource(R.string.device_model), "${Build.MANUFACTURER} ${Build.MODEL}", modifier = Modifier.fillMaxWidth())
                    InfoRow(stringResource(R.string.android_os), "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoRow(stringResource(R.string.resolution), resolution, modifier = Modifier.weight(1f))
                        InfoRow(stringResource(R.string.density), stringResource(R.string.dev_dpi_format, density), modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                InfoCard(title = stringResource(R.string.memory_metrics)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        InfoRow(stringResource(R.string.device_ram), stringResource(R.string.dev_ram_format, totalRamGb, availRamGb), modifier = Modifier.weight(1f))
                        InfoRow(stringResource(R.string.app_heap_limit), stringResource(R.string.dev_mb_format, maxHeapMb), modifier = Modifier.weight(1f))
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InfoCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
