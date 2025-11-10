package com.metrolist.sync.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DiscoveredDevice(
    val name: String,
    val ip: String,
    val port: Int,
    val deviceType: DeviceType
) : Parcelable

enum class DeviceType {
    PHONE,
    TABLET,
    TV
}
