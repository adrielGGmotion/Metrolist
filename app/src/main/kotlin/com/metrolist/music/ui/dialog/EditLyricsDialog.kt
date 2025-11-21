package com.metrolist.music.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.viewmodels.LyricsMenuViewModel

@Composable
fun EditLyricsDialog(
    onDismiss: () -> Unit,
    lyricsMenuViewModel: LyricsMenuViewModel
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    var text by remember { mutableStateOf(currentLyrics?.lyrics ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Lyrics") },
        text = {
            Column {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    lyricsMenuViewModel.database.query {
                        currentLyrics?.let {
                            upsert(
                                LyricsEntity(
                                    id = it.id,
                                    lyrics = text
                                )
                            )
                        }
                    }
                    onDismiss()
                }
            ) {
                Text(text = "Save")
            }
        }
    )
}
