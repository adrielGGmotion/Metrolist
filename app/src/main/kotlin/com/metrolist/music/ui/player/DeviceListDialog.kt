package com.metrolist.music.ui.player

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.music.R
import androidx.compose.ui.platform.LocalContext
import com.metrolist.music.LocalDatabase
import com.metrolist.music.discovery.Device
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.net.InetAddress

@Composable
fun DeviceListDialog(
    devices: List<Device>,
    onDismiss: () -> Unit,
    playerConnection: PlayerConnection,
    viewModel: DeviceListViewModel = hiltViewModel()
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.connect_to_device)) },
        text = {
            Column {
                DeviceItem(
                    device = Device(
                        name = "${Build.MANUFACTURER} ${Build.MODEL}",
                        host = InetAddress.getLocalHost().hostAddress,
                        port = 0
                    ),
                    isCurrentDevice = true,
                    onClick = { /* TODO */ }
                )
                LazyColumn {
                    items(devices) { device ->
                        DeviceItem(
                            device = device,
                            isCurrentDevice = false,
                            onClick = {
                                viewModel.connectToDevice(device, playerConnection)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
fun DeviceItem(
    device: Device,
    isCurrentDevice: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            if (isCurrentDevice) {
                Text(
                    text = stringResource(R.string.listening_on_this_device),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        leadingContent = {
            Icon(
                painter = painterResource(
                    if (isCurrentDevice) R.drawable.speaker
                    else R.drawable.cellphone
                ),
                contentDescription = null
            )
        }
    )
}

@Preview
@Composable
fun DeviceListDialogPreview() {
    var showDialog by remember { mutableStateOf(true) }
    if (showDialog) {
        val context = LocalContext.current
        val musicService = MusicService()
        val binder = musicService.MusicBinder()
        DeviceListDialog(
            devices = listOf(
                Device(
                    name = "Max's Pixel Phone",
                    host = InetAddress.getLoopbackAddress().hostAddress,
                    port = 1234
                ),
                Device(
                    name = "Max's Tablet",
                    host = InetAddress.getLoopbackAddress().hostAddress,
                    port = 5678
                ),
            ),
            onDismiss = { showDialog = false },
            playerConnection = PlayerConnection(context, binder, LocalDatabase.current, CoroutineScope(Dispatchers.Main))
        )
    }
}
