package com.metrolist.music.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.LocalFoldersKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LocalMusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalMusicSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalMusicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val (localFolders, onLocalFoldersChange) = rememberPreference(LocalFoldersKey, emptySet())
    val isScanning by viewModel.isScanning.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val newFolders = localFolders + uri.toString()
            onLocalFoldersChange(newFolders)
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = androidx.compose.ui.res.stringResource(R.string.manage_folders),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.library_add),
                        title = { Text(androidx.compose.ui.res.stringResource(R.string.add_folder)) },
                        onClick = { folderPickerLauncher.launch(null) }
                    )
                )
                if (localFolders.isNotEmpty()) {
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.sync),
                            title = { Text(androidx.compose.ui.res.stringResource(R.string.scan_now)) },
                            onClick = { viewModel.scanLocalFiles(localFolders) },
                            trailingContent = {
                                if (isScanning) {
                                    CircularProgressIndicator()
                                }
                            }
                        )
                    )
                }
            }
        )

        if (localFolders.isNotEmpty()) {
            Material3SettingsGroup(
                title = androidx.compose.ui.res.stringResource(R.string.added_folders),
                items = localFolders.map { uriString ->
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.library_music),
                        title = { Text(Uri.parse(uriString).lastPathSegment ?: uriString) },
                        trailingContent = {
                            androidx.compose.material3.IconButton(
                                onClick = {
                                    val newFolders = localFolders - uriString
                                    onLocalFoldersChange(newFolders)
                                    try {
                                        context.contentResolver.releasePersistableUriPermission(
                                            Uri.parse(uriString),
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                        )
                                    } catch (e: Exception) {
                                        timber.log.Timber.w(e, "Failed to release persistable URI permission for %s", uriString)
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Remove"
                                )
                            }
                        }
                    )
                }
            )
        }
    }

    TopAppBar(
        title = { Text(androidx.compose.ui.res.stringResource(R.string.local_music)) },
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
        },
        scrollBehavior = scrollBehavior
    )
}
