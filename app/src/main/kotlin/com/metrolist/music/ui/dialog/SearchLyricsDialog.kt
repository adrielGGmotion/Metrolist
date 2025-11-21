package com.metrolist.music.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.viewmodels.LyricsMenuViewModel

@Composable
fun SearchLyricsDialog(
    onDismiss: () -> Unit,
    lyricsMenuViewModel: LyricsMenuViewModel
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val results by lyricsMenuViewModel.results.collectAsState()
    val isLoading by lyricsMenuViewModel.isLoading.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Search Lyrics") },
        text = {
            Column {
                if (isLoading) {
                    Text(text = "Loading...")
                } else {
                    LazyColumn {
                        items(results) { result ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        lyricsMenuViewModel.database.query {
                                            mediaMetadata?.let {
                                                upsert(
                                                    LyricsEntity(
                                                        id = it.id,
                                                        lyrics = result.lyrics
                                                    )
                                                )
                                            }
                                        }
                                        onDismiss()
                                    }
                                    .padding(8.dp)
                            ) {
                                Text(text = result.providerName)
                                Text(text = result.lyrics)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
