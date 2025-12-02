package com.metrolist.music.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.MaxImageCacheSizeKey
import com.metrolist.music.constants.MaxSongCacheSizeKey
import com.metrolist.music.extensions.tryOrNull
import com.metrolist.music.ui.component.ActionPromptDialog
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.ListPreference
import com.metrolist.music.ui.component.Material3SettingsGroup
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.ui.component.PreferenceGroupTitle
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.formatFileSize
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val coroutineScope = rememberCoroutineScope()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearDownloads by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }
    var showSongCacheDialog by remember { mutableStateOf(false) }
    var showImageCacheDialog by remember { mutableStateOf(false) }

    var imageCacheSize by remember {
        mutableStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        mutableStateOf(tryOrNull { playerCache.cacheSpace } ?: 0)
    }
    var downloadCacheSize by remember {
        mutableStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0)
    }
    val imageCacheProgress by animateFloatAsState(
        targetValue = (imageCacheSize.toFloat() / imageDiskCache.maxSize).coerceIn(0f, 1f),
        label = "imageCacheProgress",
    )
    val playerCacheProgress by animateFloatAsState(
        targetValue = (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(
            0f,
            1f
        ),
        label = "playerCacheProgress",
    )

    LaunchedEffect(maxImageCacheSize) {
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.keys.forEach { key ->
                    playerCache.removeResource(key)
                }
            }
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache) {
        while (isActive) {
            delay(500)
            playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0
        }
    }
    LaunchedEffect(downloadCache) {
        while (isActive) {
            delay(500)
            downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        Material3SettingsGroup(
            title = stringResource(R.string.downloaded_songs),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(id = R.drawable.storage),
                    title = { Text(stringResource(R.string.storage_used)) },
                    description = { Text(stringResource(R.string.size_used, formatFileSize(downloadCacheSize))) },
                    onClick = { clearDownloads = true }
                )
            )
        )

        if (clearDownloads) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_all_downloads),
                onDismiss = { clearDownloads = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        downloadCache.keys.forEach { key ->
                            downloadCache.removeResource(key)
                        }
                    }
                    clearDownloads = false
                },
                onCancel = { clearDownloads = false },
                content = {
                    Text(text = stringResource(R.string.clear_downloads_dialog))
                }
            )
        }

        StorageUsageItem(
            icon = R.drawable.storage,
            title = stringResource(R.string.song_cache),
            progress = playerCacheProgress,
            usage = stringResource(
                R.string.size_used,
                "${formatFileSize(playerCacheSize)} / ${
                    formatFileSize(
                        maxSongCacheSize * 1024 * 1024L,
                    )
                }",
            )
        )
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(id = R.drawable.tune),
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    description = { Text(
                        when (maxSongCacheSize) {
                            0 -> stringResource(R.string.disable)
                            -1 -> stringResource(R.string.unlimited)
                            else -> formatFileSize(maxSongCacheSize * 1024 * 1024L)
                        }
                    ) },
                    onClick = { showSongCacheDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(id = R.drawable.delete),
                    title = { Text(stringResource(R.string.clear_song_cache)) },
                    onClick = { clearCacheDialog = true }
                )
            )
        )

        if (showSongCacheDialog) {
            val values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1)
            ListDialog(onDismiss = { showSongCacheDialog = false }) {
                items(values) {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMaxSongCacheSizeChange(it)
                                showSongCacheDialog = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = maxSongCacheSize == it,
                            onClick = null,
                        )
                        Text(
                            text =
                            when (it) {
                                0 -> stringResource(R.string.disable)
                                -1 -> stringResource(R.string.unlimited)
                                else -> formatFileSize(it * 1024 * 1024L)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }

        if (clearCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_song_cache),
                onDismiss = { clearCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        playerCache.keys.forEach { key ->
                            playerCache.removeResource(key)
                        }
                    }
                    clearCacheDialog = false
                },
                onCancel = { clearCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_song_cache_dialog))
                }
            )
        }

        StorageUsageItem(
            icon = R.drawable.storage,
            title = stringResource(R.string.image_cache),
            progress = imageCacheProgress,
            usage = stringResource(
                R.string.size_used,
                "${formatFileSize(imageCacheSize)} / ${formatFileSize(imageDiskCache.maxSize)}"
            )
        )
        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(id = R.drawable.tune),
                    title = { Text(stringResource(R.string.max_cache_size)) },
                    description = { Text(
                        when (maxImageCacheSize) {
                            0 -> stringResource(R.string.disable)
                            else -> formatFileSize(maxImageCacheSize * 1024 * 1024L)
                        }
                    ) },
                    onClick = { showImageCacheDialog = true }
                ),
                Material3SettingsItem(
                    icon = painterResource(id = R.drawable.delete),
                    title = { Text(stringResource(R.string.clear_image_cache)) },
                    onClick = { clearImageCacheDialog = true }
                )
            )
        )

        if (showImageCacheDialog) {
            val values = listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192)
            ListDialog(onDismiss = { showImageCacheDialog = false }) {
                items(values) {
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMaxImageCacheSizeChange(it)
                                showImageCacheDialog = false
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = maxImageCacheSize == it,
                            onClick = null,
                        )
                        Text(
                            text =
                            when (it) {
                                0 -> stringResource(R.string.disable)
                                else -> formatFileSize(it * 1024 * 1024L)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }

        if (clearImageCacheDialog) {
            ActionPromptDialog(
                title = stringResource(R.string.clear_image_cache),
                onDismiss = { clearImageCacheDialog = false },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        imageDiskCache.clear()
                    }
                    clearImageCacheDialog = false
                },
                onCancel = { clearImageCacheDialog = false },
                content = {
                    Text(text = stringResource(R.string.clear_image_cache_dialog))
                }
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.storage)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        }
    )
}

@Composable
private fun StorageUsageItem(
    icon: Int,
    title: String,
    progress: Float,
    usage: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = usage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
