package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R

@Composable
fun ClonePlaylistDialog(
    onCloneLocally: () -> Unit,
    onSyncWithYouTube: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.clone_playlist)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.clone_playlist_confirm))
                TextButton(
                    onClick = {
                        onCloneLocally()
                        onDismiss()
                    },
                ) {
                    Text(text = stringResource(R.string.clone_locally))
                }
                TextButton(
                    onClick = {
                        onSyncWithYouTube()
                        onDismiss()
                    },
                ) {
                    Text(text = stringResource(R.string.sync_with_youtube))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}