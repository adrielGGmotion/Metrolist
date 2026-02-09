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
import com.metrolist.music.constants.FloatingLyricsAutoFetchKey
import com.metrolist.music.constants.FloatingLyricsBackgroundStyle
import com.metrolist.music.constants.FloatingLyricsBackgroundStyleKey
import com.metrolist.music.constants.FloatingLyricsOpacityKey
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.PreferenceEntry
import com.metrolist.music.ui.component.EnumListPreference
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

    val (enableFloatingLyrics, onEnableFloatingLyricsChange) = rememberPreference(
        key = EnableFloatingLyricsKey,
        defaultValue = false
    )

    // Refresh permission state when resuming
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasPermission = Settings.canDrawOverlays(context)
                hasOverlayPermission = hasPermission
                if (!hasPermission && enableFloatingLyrics) {
                    onEnableFloatingLyricsChange(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

        EnumListPreference(
            title = { Text(stringResource(R.string.floating_lyrics_background_style)) },
            icon = { Icon(painterResource(R.drawable.palette), null) },
            selectedValue = backgroundStyle,
            onValueSelected = onBackgroundStyleChange,
            valueText = { style ->
                when (style) {
                    FloatingLyricsBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    FloatingLyricsBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    FloatingLyricsBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                }
            }
        )

        val (autoFetchLyrics, onAutoFetchLyricsChange) = rememberPreference(
            key = FloatingLyricsAutoFetchKey,
            defaultValue = false
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.floating_lyrics_auto_fetch)) },
            description = stringResource(R.string.floating_lyrics_auto_fetch_desc),
            checked = autoFetchLyrics,
            onCheckedChange = onAutoFetchLyricsChange,
            icon = { Icon(painterResource(R.drawable.sync), null) }
        )

        val (opacity, onOpacityChange) = rememberPreference(
            key = FloatingLyricsOpacityKey,
            defaultValue = 1f
        )

        SwitchPreference(
            title = { Text(stringResource(R.string.enable_floating_lyrics)) },
            description = stringResource(R.string.floating_lyrics_desc),
            checked = enableFloatingLyrics,
            onCheckedChange = { enabled ->
                if (enabled) {
                    if (hasOverlayPermission) {
                        onEnableFloatingLyricsChange(true)
                    } else {
                        // Request permission, but don't enable yet. 
                        // The onResume check will enable it if granted, or we rely on user manually toggling again?
                        // Better: Launch intent, let user grant. When they return, ON_RESUME will update hasOverlayPermission.
                        // We should probably NOT toggle the switch to true unless permission is already there.
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                        // Don't call onEnableFloatingLyricsChange(true) here. 
                        // Let the user enable it after granting permission.
                        // Or, we can set it to true, but the ON_RESUME block will set it back to false if they didn't grant it.
                        // Current behavior: `it` is true. `if (it && !hasPermission)` block runs. `onChange(it)` runs.
                        // So it sets to true. 
                        // The user goes to settings -> denies -> comes back.
                        // ON_RESUME runs -> sets hasOverlayPermission = false.
                        // My new code in ON_RESUME will see (false && true) -> set enabled to false.
                        // So the flow is covered.
                        onEnableFloatingLyricsChange(true)
                    }
                } else {
                    onEnableFloatingLyricsChange(false)
                }
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
