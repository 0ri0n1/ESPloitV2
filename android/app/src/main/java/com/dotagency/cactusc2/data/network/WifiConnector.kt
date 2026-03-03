package com.dotagency.cactusc2.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Programmatic WiFi AP switcher for connecting to Cactus WHID devices.
 *
 * Uses two code paths depending on Android version:
 * - **API 26-28** (pre-Android 10): Legacy [WifiConfiguration] + [WifiManager.enableNetwork]
 * - **API 29+** (Android 10+): [WifiNetworkSpecifier] + [ConnectivityManager.requestNetwork]
 *
 * On Android 10+ the network is bound to this process via [ConnectivityManager.bindProcessToNetwork]
 * so HTTP traffic routes through the Cactus AP even if it has no internet.
 */
class WifiConnector(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /** Returns the SSID of the currently connected WiFi, or null. */
    fun getCurrentSSID(): String? {
        val info = wifiManager.connectionInfo
        return info.ssid?.removeSurrounding("\"")?.takeIf { it != "<unknown ssid>" }
    }

    @Suppress("DEPRECATION")
    suspend fun connectToDevice(ssid: String, password: String): Boolean {
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return connectApi29Plus(ssid, password)
        }

        // Pre-Android 10: use WifiConfiguration
        val existingConfig = wifiManager.configuredNetworks?.find {
            it.SSID == "\"$ssid\""
        }

        val netId = if (existingConfig != null) {
            existingConfig.networkId
        } else {
            val config = WifiConfiguration().apply {
                SSID = "\"$ssid\""
                preSharedKey = "\"$password\""
            }
            wifiManager.addNetwork(config)
        }

        if (netId == -1) return false

        wifiManager.disconnect()
        val success = wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()

        if (!success) return false

        return withTimeoutOrNull(15_000) {
            waitForConnection(ssid)
        } ?: false
    }

    private suspend fun connectApi29Plus(ssid: String, password: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        return withTimeoutOrNull(30_000) {
            suspendCancellableCoroutine { cont ->
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        connectivityManager.bindProcessToNetwork(network)
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onUnavailable() {
                        if (cont.isActive) cont.resume(false)
                    }
                }
                connectivityManager.requestNetwork(networkRequest, callback)
                cont.invokeOnCancellation {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }
        } ?: false
    }

    @Suppress("DEPRECATION")
    private suspend fun waitForConnection(ssid: String): Boolean {
        return suspendCancellableCoroutine { cont ->
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val currentSsid = getCurrentSSID()
                    if (currentSsid == ssid && cont.isActive) {
                        cont.resume(true)
                    }
                }
            }
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            cont.invokeOnCancellation {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }
    }

    fun disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(null)
        }
    }
}
