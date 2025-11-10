package com.metrolist.music.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.utils.dataStore
import com.metrolist.sync.api.DeviceType
import com.metrolist.sync.api.DiscoveredDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.UUID

class ServiceDiscoverer(
    private val context: Context,
) {
    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    private val serviceName = "Metrolist-${UUID.randomUUID()}"

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            Timber.tag(TAG).d("Service registered: %s", serviceInfo)
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Timber.tag(TAG).e("Service registration failed: %d", errorCode)
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            Timber.tag(TAG).d("Service unregistered: %s", serviceInfo)
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Timber.tag(TAG).e("Service unregistration failed: %d", errorCode)
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Timber.tag(TAG).d("Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Timber.tag(TAG).d("Service found: %s", service)
            if (service.serviceName == serviceName) {
                return
            }
            nsdManager.resolveService(service, createResolveListener())
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Timber.tag(TAG).d("Service lost: %s", service)
            _discoveredDevices.value = _discoveredDevices.value.filter { it.name != service.serviceName }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Timber.tag(TAG).d("Service discovery stopped")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Timber.tag(TAG).e("Service discovery start failed: %d", errorCode)
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Timber.tag(TAG).e("Service discovery stop failed: %d", errorCode)
        }
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Timber.tag(TAG).e("Resolve failed: %d", errorCode)
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Timber.tag(TAG).d("Service resolved: %s", serviceInfo)
                val email = serviceInfo.attributes["email"]?.toString(Charsets.UTF_8)
                val currentUserEmail = runBlocking { context.dataStore.data.first()[AccountEmailKey] }
                if (email != currentUserEmail) {
                    return
                }
                val deviceType = serviceInfo.attributes["deviceType"]?.toString(Charsets.UTF_8)?.let {
                    DeviceType.valueOf(it)
                } ?: DeviceType.PHONE
                val device = DiscoveredDevice(
                    name = serviceInfo.serviceName,
                    ip = serviceInfo.host.hostAddress,
                    port = serviceInfo.port,
                    deviceType = deviceType
                )
                _discoveredDevices.value = _discoveredDevices.value + device
            }
        }
    }

    fun registerService(port: Int) {
        val email = runBlocking { context.dataStore.data.first()[AccountEmailKey] } ?: return
        val deviceType = getDeviceType()
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@ServiceDiscoverer.serviceName
            serviceType = "_metrolist-sync._tcp."
            setPort(port)
            setAttribute("email", email)
            setAttribute("deviceType", deviceType.name)
        }
        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discoverServices() {
        nsdManager.discoverServices("_metrolist-sync._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    private fun getDeviceType(): DeviceType {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        return when (uiModeManager.currentModeType) {
            android.content.res.Configuration.UI_MODE_TYPE_TELEVISION -> DeviceType.TV
            android.content.res.Configuration.UI_MODE_TYPE_WATCH -> DeviceType.PHONE // No watch support
            else -> DeviceType.PHONE // TODO: Implement tablet detection
        }
    }

    companion object {
        private const val TAG = "ServiceDiscoverer"
    }
}
