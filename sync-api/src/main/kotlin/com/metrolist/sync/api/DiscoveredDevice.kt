package com.metrolist.sync.api

data class DiscoveredDevice(
    val serviceName: String,
    val deviceName: String,
    val hostAddress: String,
    val port: Int,
    val isSelf: Boolean = false
)
