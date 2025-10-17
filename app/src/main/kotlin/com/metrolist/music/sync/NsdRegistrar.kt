package com.metrolist.music.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdRegistrar(private val context: Context) {

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var serviceName: String? = null

    private val SERVICE_TYPE = "_metrolist-sync._tcp"
    private val TAG = "NsdRegistrar"

    fun registerService(port: Int, deviceName: String) {
        unregisterService()

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                serviceName = nsdServiceInfo.serviceName
                Log.d(TAG, "Service registered with name: ${nsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed. Error code: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${arg0.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed. Error code: $errorCode")
            }
        }

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = deviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun unregisterService() {
        registrationListener?.let {
            nsdManager.unregisterService(it)
            registrationListener = null
            serviceName = null
        }
    }
}