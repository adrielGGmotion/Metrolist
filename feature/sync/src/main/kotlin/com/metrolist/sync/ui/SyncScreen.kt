package com.metrolist.sync.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.sync.DiscoveredDevice
import com.metrolist.sync.SyncViewModel

@Composable
fun SyncScreen(
    viewModel: SyncViewModel = hiltViewModel()
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    DiscoveredDevicesList(
        devices = discoveredDevices,
        onDeviceClick = { device ->
            viewModel.connectToDevice(device)
        }
    )
}

@Composable
fun DiscoveredDevicesList(
    devices: List<DiscoveredDevice>,
    onDeviceClick: (DiscoveredDevice) -> Unit
) {
    val filteredDevices = devices.filter { !it.isSelf }
    LazyColumn {
        items(filteredDevices) { device ->
            DeviceListItem(
                device = device,
                onClick = { onDeviceClick(device) }
            )
        }
    }
}

@Composable
fun DeviceListItem(
    device: DiscoveredDevice,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = device.deviceName)
    }
}
