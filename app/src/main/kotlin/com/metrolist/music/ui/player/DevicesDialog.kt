package com.metrolist.music.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.playback.PlayerConnection

@Composable
fun DevicesDialog(
    onDismissRequest: () -> Unit,
    playerConnection: PlayerConnection
) {
    val syncManager = playerConnection.service.syncManager
    if (syncManager == null) {
        onDismissRequest()
        return
    }

    val discoveredDevices by syncManager.discoveredDevices.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.connect_to_a_device)) },
        text = {
            LazyColumn {
                items(discoveredDevices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                syncManager.connectToDevice(device)
                                onDismissRequest()
                            }
                            .padding(vertical = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.cast),
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = device.serviceName)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = {
            Button(onClick = {
                syncManager.disconnect()
                onDismissRequest()
            }) {
                Text(stringResource(R.string.disconnect))
            }
        }
    )
}