package com.metrolist.music.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdDiscoverer(
    private val context: Context,
    private val onServiceResolved: (NsdServiceInfo) -> Unit,
    private val onServiceLost: (NsdServiceInfo) -> Unit
    ) {

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    val discoveredServices = mutableMapOf<String, NsdServiceInfo>()

    private val SERVICE_TYPE = "_metrolist-sync._tcp"
    private val TAG = "NsdDiscoverer"

    fun startDiscovery() {
        stopDiscovery()
        discoveredServices.clear()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: $service")
                if (service.serviceType != SERVICE_TYPE) {
                    Log.d(TAG, "Unknown Service Type: ${service.serviceType}")
                } else {
                    nsdManager.resolveService(service, createResolveListener())
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e(TAG, "Service lost: $service")
                discoveredServices.remove(service.serviceName)
                onServiceLost.invoke(service)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery start failed. Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery stop failed. Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun createResolveListener(): NsdManager.ResolveListener {
        return object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}. Error code: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolve Succeeded. ${serviceInfo.host}:${serviceInfo.port}")
                discoveredServices[serviceInfo.serviceName] = serviceInfo
                onServiceResolved.invoke(serviceInfo)
            }
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            nsdManager.stopServiceDiscovery(it)
            discoveryListener = null
        }
    }
}