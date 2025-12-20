package com.metrolist.music.ui.screens.wrapped

import androidx.compose.runtime.compositionLocalOf

val LocalWrappedManager = compositionLocalOf<WrappedPlaybackManager> {
    error("No WrappedPlaybackManager provided")
}
