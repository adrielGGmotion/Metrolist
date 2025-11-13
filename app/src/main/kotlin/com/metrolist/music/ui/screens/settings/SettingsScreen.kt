package com.metrolist.music.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.ReleaseNotesCard
import com.metrolist.music.ui.component.SearchBar
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.Updater

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    var isSearchActive by rememberSaveable {
        mutableStateOf(false)
    }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        AnimatedVisibility(isSearchActive) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_in_settings),
                modifier = Modifier.focusRequester(focusRequester)
            )
        }

        if (isSearchActive) {
            val allSettings = getAllSettings(navController)
            val filteredSettings = allSettings.filter { setting ->
                val query = searchQuery.lowercase()
                val titleMatch = setting.title.lowercase().contains(query)
                val summaryMatch = setting.summary?.lowercase()?.contains(query) ?: false
                val keywordMatch = setting.keywords.any { it.lowercase().contains(query) }
                val fuzzyMatch = setting.title.lowercase().fuzzyMatch(query)
                titleMatch || summaryMatch || keywordMatch || fuzzyMatch
            }

@OptIn(ExperimentalFoundationApi::class)
            val groupedSettings = filteredSettings.groupBy { it.category }

            LazyColumn {
                groupedSettings.forEach { (category, settings) ->
                    item {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(settings, key = { it.title }) { setting ->
                        AnimatedVisibility(visible = true) {
                            Material3SettingsItem(
                                modifier = Modifier.animateItemPlacement(),
                                icon = setting.icon,
                                title = { Text(setting.title) },
                                description = { setting.summary?.let { Text(it) } },
                                onClick = setting.onClick
                            )
                        }
                    }
                }
            }
        } else {
            // User Interface Section
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_ui),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.palette),
                        title = { Text(stringResource(R.string.appearance)) },
                        onClick = { navController.navigate("settings/appearance") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Player & Content Section (moved up and combined with content)
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_player_content),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.play),
                        title = { Text(stringResource(R.string.player_and_audio)) },
                        onClick = { navController.navigate("settings/player") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.language),
                        title = { Text(stringResource(R.string.content)) },
                        onClick = { navController.navigate("settings/content") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy & Security Section
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_privacy),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.security),
                        title = { Text(stringResource(R.string.privacy)) },
                        onClick = { navController.navigate("settings/privacy") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Storage & Data Section
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_storage),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.storage),
                        title = { Text(stringResource(R.string.storage)) },
                        onClick = { navController.navigate("settings/storage") }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.restore),
                        title = { Text(stringResource(R.string.backup_restore)) },
                        onClick = { navController.navigate("settings/backup_restore") }
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // System & About Section
            Material3SettingsGroup(
                title = stringResource(R.string.settings_section_system),
                items = buildList {
                    if (isAndroid12OrLater) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.link),
                                title = { Text(stringResource(R.string.default_links)) },
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                            "package:${context.packageName}".toUri()
                                        )
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        when (e) {
                                            is ActivityNotFoundException -> {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        R.string.open_app_settings_error,
                                                        Toast.LENGTH_LONG
                                                    )
                                                    .show()
                                            }

                                            is SecurityException -> {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        R.string.open_app_settings_error,
                                                        Toast.LENGTH_LONG
                                                    )
                                                    .show()
                                            }

                                            else -> {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        R.string.open_app_settings_error,
                                                        Toast.LENGTH_LONG
                                                    )
                                                    .show()
                                            }
                                        }
                                    }
                                }
                            )
                        )
                    }
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.update),
                            title = { Text(stringResource(R.string.updater)) },
                            onClick = { navController.navigate("settings/updater") }
                        )
                    )
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.info),
                            title = { Text(stringResource(R.string.about)) },
                            onClick = { navController.navigate("settings/about") }
                        )
                    )
                    if (latestVersionName != BuildConfig.VERSION_NAME) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.update),
                                title = {
                                    Text(
                                        text = stringResource(R.string.new_version_available),
                                    )
                                },
                                description = {
                                    Text(
                                        text = latestVersionName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                showBadge = true,
                                onClick = { uriHandler.openUri(Updater.getLatestDownloadUrl()) }
                            )
                        )
                    }
                }
            )

            if (latestVersionName != BuildConfig.VERSION_NAME) {
                Spacer(modifier = Modifier.height(16.dp))
                ReleaseNotesCard()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        },
        actions = {
            if (isSearchActive) {
                IconButton(
                    onClick = {
                        searchQuery = ""
                        isSearchActive = false
                        keyboardController?.hide()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null
                    )
                }
            } else {
                IconButton(
                    onClick = { isSearchActive = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior
    )
}
