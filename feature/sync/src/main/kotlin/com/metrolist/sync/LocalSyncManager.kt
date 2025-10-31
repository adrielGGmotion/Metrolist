package com.metrolist.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.metrolist.common.data.DataStoreUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreUtil: DataStoreUtil
) {
    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var registrationListener: NsdManager.RegistrationListener? = null

    fun registerService(port: Int) {
        coroutineScope.launch {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "Metrolist Sync"
                serviceType = "_metrolist-sync._tcp"
                this.port = port
                setAttribute("email", dataStoreUtil.getEmail().first())
                setAttribute("device", Build.MODEL)
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(nsdServiceInfo: NsdServiceInfo) {
                    Timber.d("Service registered: %s", nsdServiceInfo)
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Timber.e("Service registration failed: %d", errorCode)
                }

                override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                    Timber.d("Service unregistered: %s", arg0)
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Timber.e("Service unregistration failed: %d", errorCode)
                }
            }

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }
    }

    fun unregisterService() {
        registrationListener?.let {
            nsdManager.unregisterService(it)
            registrationListener = null
        }
    }
}
