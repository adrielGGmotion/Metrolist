package com.metrolist.music.ui.screens.settings.integrations

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.EnableFloatingLyricsKey
import com.metrolist.music.constants.FloatingLyricsBackgroundStyle
import com.metrolist.music.constants.FloatingLyricsBackgroundStyleKey
import com.metrolist.music.constants.FloatingLyricsOpacityKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.ui.component.SelectorPreference
import com.metrolist.music.ui.component.SwitchPreference
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingLyricsSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    // Refresh permission state when resuming
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val (enableFloatingLyrics, onEnableFloatingLyricsChange) = rememberPreference(
        key = EnableFloatingLyricsKey,
        defaultValue = false
    )

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        val (backgroundStyle, onBackgroundStyleChange) = rememberEnumPreference(
            key = FloatingLyricsBackgroundStyleKey,
            defaultValue = FloatingLyricsBackgroundStyle.DEFAULT
        )

        SelectorPreference(
            title = { Text(stringResource(R.string.floating_lyrics_background_style)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selected = backgroundStyle,
            options = FloatingLyricsBackgroundStyle.entries,
            onOptionSelected = onBackgroundStyleChange,
            optionLabel = { style ->
                when (style) {
                    FloatingLyricsBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    FloatingLyricsBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    FloatingLyricsBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                }
            }
        )

        val (opacity, onOpacityChange) = rememberPreference(
            key = FloatingLyricsOpacityKey,
            defaultValue = 1f
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_floating_lyrics)) },
            description = stringResource(R.string.floating_lyrics_desc),
            checked = enableFloatingLyrics,
            onCheckedChange = {
                if (it && !hasOverlayPermission) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
                onEnableFloatingLyricsChange(it)
            },
            icon = { Icon(painterResource(R.drawable.lyrics), null) }
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.floating_lyrics_opacity)) },
            description = stringResource(R.string.percentage_format, (opacity * 100).roundToInt()),
            icon = { Icon(painterResource(R.drawable.contrast), null) },
            onClick = { }
        )
        
        Slider(
            value = opacity,
            onValueChange = onOpacityChange,
            valueRange = 0.1f..1f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )

        if (!hasOverlayPermission && enableFloatingLyrics) {
             PreferenceEntry(
                title = { Text(stringResource(R.string.grant_permission)) },
                description = stringResource(R.string.floating_lyrics_permission_desc),
                icon = { Icon(painterResource(R.drawable.warning), null) },
                onClick = {
                     val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.floating_lyrics)) },
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
