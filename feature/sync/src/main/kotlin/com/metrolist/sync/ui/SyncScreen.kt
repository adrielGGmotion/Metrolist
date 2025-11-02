package com.metrolist.sync.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.metrolist.sync.api.DiscoveredDevice
import com.metrolist.sync.api.SyncState
import com.metrolist.sync.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    syncState: SyncState,
    viewModel: SyncViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel, syncState) {
        viewModel.init(syncState)
    }

    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sync Devices") },
                actions = {
                    IconButton(onClick = { viewModel.refreshDiscovery() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            DiscoveredDevicesList(
                devices = discoveredDevices,
                onDeviceClick = { device ->
                    viewModel.connectToDevice(device)
                }
            )
        }
    }
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
