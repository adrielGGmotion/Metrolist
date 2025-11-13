package com.metrolist.music.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.metrolist.music.R

import androidx.navigation.NavController

data class SettingItem(
    val title: String,
    val summary: String?,
    val icon: Painter,
    val category: String,
    val keywords: List<String> = emptyList(),
    val onClick: () -> Unit
)

@Composable
fun getAllSettings(navController: NavController): List<SettingItem> {
    return listOf(
        SettingItem(
            title = "Theme",
            summary = "Enable dynamic theme, dark theme, pure black",
            icon = painterResource(R.drawable.palette),
            category = "Appearance",
            keywords = listOf("theme", "dark mode", "black", "dynamic"),
            onClick = { navController.navigate("settings/appearance") }
        ),
        SettingItem(
            title = "Mini Player",
            summary = "Pure black mini player, mini player outline",
            icon = painterResource(R.drawable.nav_bar),
            category = "Appearance",
            keywords = listOf("mini player", "pure black", "outline"),
            onClick = { navController.navigate("settings/appearance") }
        ),
        SettingItem(
            title = "Player",
            summary = "New player design, new mini player design",
            icon = painterResource(R.drawable.play),
            category = "Appearance",
            keywords = listOf("player", "design"),
            onClick = { navController.navigate("settings/appearance") }
        ),
        SettingItem(
            title = "Misc",
            summary = "Default open tab, default library chips, swipe gestures, slim navigation bar, grid cell size",
            icon = painterResource(R.drawable.settings),
            category = "Appearance",
            keywords = listOf("misc", "tab", "chips", "swipe", "grid", "navbar"),
            onClick = { navController.navigate("settings/appearance") }
        ),
        SettingItem(
            title = "Auto Playlists",
            summary = "Show liked, downloaded, top, cached, and uploaded playlists",
            icon = painterResource(R.drawable.playlist_play),
            category = "Appearance",
            keywords = listOf("playlists", "auto", "liked", "downloaded", "top", "cached", "uploaded"),
            onClick = { navController.navigate("settings/appearance") }
        ),
        SettingItem(
            title = "Audio Quality",
            summary = "Change the audio quality for streaming",
            icon = painterResource(R.drawable.graphic_eq),
            category = "Player",
            keywords = listOf("audio", "quality", "streaming"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "History Duration",
            summary = "Set the duration for a song to be considered in history",
            icon = painterResource(R.drawable.history),
            category = "Player",
            keywords = listOf("history", "duration"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Skip Silence",
            summary = "Automatically skip silence in songs",
            icon = painterResource(R.drawable.fast_forward),
            category = "Player",
            keywords = listOf("skip", "silence"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Audio Normalization",
            summary = "Normalize the audio volume",
            icon = painterResource(R.drawable.volume_up),
            category = "Player",
            keywords = listOf("audio", "normalization", "volume"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Audio Offload",
            summary = "Enable audio offload for better performance",
            icon = painterResource(R.drawable.graphic_eq),
            category = "Player",
            keywords = listOf("audio", "offload"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Seek Seconds Add-up",
            summary = "Add up seconds when seeking",
            icon = painterResource(R.drawable.arrow_forward),
            category = "Player",
            keywords = listOf("seek", "seconds"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Persistent Queue",
            summary = "Save the queue between sessions",
            icon = painterResource(R.drawable.queue_music),
            category = "Player",
            keywords = listOf("persistent", "queue"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Auto Load More",
            summary = "Automatically load more songs in the queue",
            icon = painterResource(R.drawable.playlist_add),
            category = "Player",
            keywords = listOf("auto", "load", "more", "queue"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Disable Load More When Repeat All",
            summary = "Disable loading more songs when repeat all is enabled",
            icon = painterResource(R.drawable.repeat),
            category = "Player",
            keywords = listOf("disable", "load", "more", "repeat"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Auto Download on Like",
            summary = "Automatically download songs when you like them",
            icon = painterResource(R.drawable.download),
            category = "Player",
            keywords = listOf("auto", "download", "like"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Enable Similar Content",
            summary = "Enable similar content recommendations",
            icon = painterResource(R.drawable.similar),
            category = "Player",
            keywords = listOf("similar", "content", "recommendations"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Auto Skip Next on Error",
            summary = "Automatically skip to the next song on error",
            icon = painterResource(R.drawable.skip_next),
            category = "Player",
            keywords = listOf("auto", "skip", "next", "error"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Stop Music on Task Clear",
            summary = "Stop music when the app is cleared from recent tasks",
            icon = painterResource(R.drawable.clear_all),
            category = "Player",
            keywords = listOf("stop", "music", "task", "clear"),
            onClick = { navController.navigate("settings/player") }
        ),
        SettingItem(
            title = "Content Language",
            summary = "Set the language for content",
            icon = painterResource(R.drawable.language),
            category = "Content",
            keywords = listOf("content", "language"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Content Country",
            summary = "Set the country for content",
            icon = painterResource(R.drawable.location_on),
            category = "Content",
            keywords = listOf("content", "country"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Hide Explicit",
            summary = "Hide explicit content",
            icon = painterResource(R.drawable.explicit),
            category = "Content",
            keywords = listOf("hide", "explicit", "content"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "App Language",
            summary = "Set the language for the app",
            icon = painterResource(R.drawable.language),
            category = "Content",
            keywords = listOf("app", "language"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Enable Proxy",
            summary = "Enable a proxy for network requests",
            icon = painterResource(R.drawable.wifi_proxy),
            category = "Content",
            keywords = listOf("enable", "proxy"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Configure Proxy",
            summary = "Configure the proxy settings",
            icon = painterResource(R.drawable.settings),
            category = "Content",
            keywords = listOf("configure", "proxy"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Enable LrcLib",
            summary = "Enable the LrcLib lyrics provider",
            icon = painterResource(R.drawable.lyrics),
            category = "Content",
            keywords = listOf("enable", "lrclib", "lyrics"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Enable KuGou",
            summary = "Enable the KuGou lyrics provider",
            icon = painterResource(R.drawable.lyrics),
            category = "Content",
            keywords = listOf("enable", "kugou", "lyrics"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Preferred Lyrics Provider",
            summary = "Set the preferred lyrics provider",
            icon = painterResource(R.drawable.lyrics),
            category = "Content",
            keywords = listOf("preferred", "lyrics", "provider"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Lyrics Romanization",
            summary = "Configure lyrics romanization",
            icon = painterResource(R.drawable.language_korean_latin),
            category = "Content",
            keywords = listOf("lyrics", "romanization"),
            onClick = { navController.navigate("settings/content/romanization") }
        ),
        SettingItem(
            title = "Top Length",
            summary = "Set the length of the top charts",
            icon = painterResource(R.drawable.trending_up),
            category = "Content",
            keywords = listOf("top", "length", "charts"),
            onClick = { navController.navigate("settings/content") }
        ),
        SettingItem(
            title = "Quick Picks",
            summary = "Set the quick picks on the home screen",
            icon = painterResource(R.drawable.home_outlined),
            category = "Content",
            keywords = listOf("quick", "picks", "home"),
            onClick = { navController.navigate("settings/content") }
        )
    )
}

fun String.fuzzyMatch(query: String): Boolean {
    val distance = levenshteinDistance(this, query)
    return distance <= 2
}

private fun levenshteinDistance(a: String, b: String): Int {
    val costs = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        var lastValue = i
        for (j in 1..b.length) {
            val newValue = if (a[i - 1] == b[j - 1]) {
                costs[j - 1]
            } else {
                1 + minOf(costs[j - 1], lastValue, costs[j])
            }
            costs[j - 1] = lastValue
            lastValue = newValue
        }
        costs[b.length] = lastValue
    }
    return costs[b.length]
}
