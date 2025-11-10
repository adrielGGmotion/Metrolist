package com.metrolist.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.sync.api.DeviceType
import com.metrolist.sync.api.DiscoveredDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionPopup(
    viewModel: SyncViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    onDeviceSelected: (DiscoveredDevice) -> Unit
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val showDialog by viewModel.showDeviceSelection.collectAsState()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Connect to a device") },
            text = {
                LazyColumn {
                    items(discoveredDevices) { device ->
                        ListItem(
                            headlineText = { Text(device.name) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(
                                        id = when (device.deviceType) {
                                            DeviceType.PHONE -> R.drawable.ic_phone
                                            DeviceType.TABLET -> R.drawable.ic_tablet
                                            DeviceType.TV -> R.drawable.ic_tv
                                        }
                                    ),
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable {
                                onDeviceSelected(device)
                                onDismiss()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        )
    }
}
