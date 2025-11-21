package com.metrolist.music.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.LyricsRomanizeCyrillicByLineKey
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.dialog.EditLyricsDialog
import com.metrolist.music.ui.dialog.SearchLyricsDialog
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LyricsMenuViewModel

@Composable
fun LyricsMenu(onDismiss: () -> Unit, lyricsMenuViewModel: LyricsMenuViewModel = hiltViewModel()) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var showTextPositionDialog by remember {
        mutableStateOf(false)
    }
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        com.metrolist.music.constants.LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (romanizeLyrics, onRomanizeLyricsChange) = rememberPreference(
        LyricsRomanizeCyrillicByLineKey,
        defaultValue = false
    )

    var showSearchLyricsDialog by remember { mutableStateOf(false) }
    var showEditLyricsDialog by remember { mutableStateOf(false) }

    if (showTextPositionDialog) {
        EnumDialog(
            onDismiss = { showTextPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showTextPositionDialog = false
            },
            current = lyricsPosition,
            title = stringResource(R.string.lyrics_text_position),
            values = LyricsPosition.values().toList(),
            valueText = { stringResource(it.title) }
        )
    }

    if (showSearchLyricsDialog) {
        SearchLyricsDialog(
            onDismiss = { showSearchLyricsDialog = false },
            lyricsMenuViewModel = lyricsMenuViewModel
        )
    }

    if (showEditLyricsDialog) {
        EditLyricsDialog(
            onDismiss = { showEditLyricsDialog = false },
            lyricsMenuViewModel = lyricsMenuViewModel
        )
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        ListItem(
            headlineContent = { Text(text = "Refetch") },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.sync),
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickable {
                playerConnection.refetchLyrics()
                onDismiss()
            }
        )
        ListItem(
            headlineContent = { Text(text = "Text Position") },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.format_align_center),
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickable {
                showTextPositionDialog = true
            }
        )
        ListItem(
            headlineContent = { Text(text = "Romanize Lyrics") },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.language),
                    contentDescription = null,
                )
            },
            trailingContent = {
                Switch(
                    checked = romanizeLyrics,
                    onCheckedChange = { onRomanizeLyricsChange(!romanizeLyrics) }
                )
            },
            modifier = Modifier.clickable {
                onRomanizeLyricsChange(!romanizeLyrics)
            }
        )
        ListItem(
            headlineContent = { Text(text = "Edit") },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickable {
                showEditLyricsDialog = true
            }
        )
        ListItem(
            headlineContent = { Text(text = "Search") },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                )
            },
            modifier = Modifier.clickable {
                showSearchLyricsDialog = true
            }
        )
    }
}
