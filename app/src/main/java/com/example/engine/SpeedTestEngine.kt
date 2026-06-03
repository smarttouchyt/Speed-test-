package com.example.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.BufferedSink
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SpeedTestEngine(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val downloadUrl = "https://speed.cloudflare.com/__down?bytes=2097152" // 2MB
    private val uploadUrl = "https://speed.cloudflare.com/__up"
    private val pingUrl = "https://speed.cloudflare.com/"

    // Details of the current connection
    data class NetworkInfo(
        val connectionType: String, // "WIFI", "MOBILE", "OFFLINE"
        val ispName: String,
        val publicIpAddress: String,
        val localIpAddress: String,
        val gatewayIp: String,
        val signalStrength: String, // Good, Excellent, Strong, etc.
        val ssid: String = "N/A",
        val frequency: String = "N/A",
        val linkSpeed: String = "N/A"
    )

    fun getNetworkDetails(): NetworkInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        var connectionType = "OFFLINE"
        var localIpAddress = "N/A"
        var gatewayIp = "N/A"
        var networkName = "Disconnected"
        var ssid = "N/A"
        var signalStrength = "N/A"
        var frequency = "N/A"
        var linkSpeed = "N/A"

        if (capabilities != null) {
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                    connectionType = "WIFI"
                    networkName = "Wi-Fi Connection"
                    try {
                        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val connectionInfo = wifiManager.connectionInfo
                        val ipAddressInt = connectionInfo.ipAddress
                        if (ipAddressInt != 0) {
                            localIpAddress = formatIpAddress(ipAddressInt)
                        }
                        
                        val dhcpInfo = wifiManager.dhcpInfo
                        if (dhcpInfo != null && dhcpInfo.gateway != 0) {
                            gatewayIp = formatIpAddress(dhcpInfo.gateway)
                        }

                        // Detailed WiFi stats
                        var rawSsid = connectionInfo.ssid ?: ""
                        if (rawSsid == "<unknown ssid>" || rawSsid.isEmpty() || rawSsid == "0x") {
                            rawSsid = "Active Wi-Fi Link"
                        } else {
                            rawSsid = rawSsid.replace("\"", "")
                        }
                        ssid = rawSsid
                        
                        val rssi = connectionInfo.rssi
                        val level = WifiManager.calculateSignalLevel(rssi, 5)
                        signalStrength = when (level) {
                            4 -> "Excellent ($rssi dBm)"
                            3 -> "Good ($rssi dBm)"
                            2 -> "Fair ($rssi dBm)"
                            1 -> "Weak ($rssi dBm)"
                            else -> "Poor ($rssi dBm)"
                        }

                        frequency = if (connectionInfo.frequency > 4000) "5 GHz (${connectionInfo.frequency} MHz)" else "2.4 GHz (${connectionInfo.frequency} MHz)"
                        linkSpeed = if (connectionInfo.linkSpeed > 0) "${connectionInfo.linkSpeed} Mbps" else "N/A"
                    } catch (e: Exception) {
                        Log.e("SpeedTestEngine", "Error fetching WiFi details: ${e.message}")
                    }
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    connectionType = "CELLULAR"
                    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                    val carrierName = telephonyManager?.networkOperatorName ?: ""
                    networkName = if (carrierName.isNotEmpty()) carrierName else "Mobile Network Provider"
                    localIpAddress = getMobileIPAddress() ?: "N/A"
                    gatewayIp = "N/A"
                    ssid = networkName
                    signalStrength = "Active cellular signal"
                    frequency = "LTE / 5G Band"
                    linkSpeed = "Dynamic"
                }
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                    connectionType = "ETHERNET"
                    networkName = "Ethernet Connection"
                    ssid = "Wired Link"
                    signalStrength = "Excellent (Wired)"
                    frequency = "Copper Core"
                    linkSpeed = "1000 Mbps"
                }
            }
        }

        return NetworkInfo(
            connectionType = connectionType,
            ispName = networkName,
            publicIpAddress = "Detecting...", // Loaded asynchronously via IP API for accuracy
            localIpAddress = localIpAddress,
            gatewayIp = gatewayIp,
            signalStrength = signalStrength,
            ssid = ssid,
            frequency = frequency,
            linkSpeed = linkSpeed
        )
    }

    suspend fun fetchPublicIpInfo(): Pair<String, String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://ip-api.com/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    // Simple parse since we want to keep compiles fast and avoid complex JSON structures
                    val ipRegex = "\"query\":\"([^\"]+)\"".toRegex()
                    val ispRegex = "\"isp\":\"([^\"]+)\"".toRegex()
                    val ipMatch = ipRegex.find(bodyString)
                    val ispMatch = ispRegex.find(bodyString)

                    val ip = ipMatch?.groupValues?.get(1) ?: "Unknown IP"
                    val isp = ispMatch?.groupValues?.get(1) ?: "Unknown ISP"
                    Pair(ip, isp)
                } else {
                    Pair("Unknown Public IP", "Unknown ISP")
                }
            }
        } catch (e: Exception) {
            Pair("No Internet Connection", "Offline Network")
        }
    }

    suspend fun runPingTest(
        onUpdate: (currentPing: Double) -> Unit
    ): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val pings = mutableListOf<Long>()
        val request = Request.Builder()
            .url(pingUrl)
            .head() // Use HEAD request to keep packets super lightweight!
            .build()

        var isOffline = false

        for (i in 1..5) {
            val startTime = System.nanoTime()
            try {
                client.newCall(request).execute().use { response ->
                    val endTime = System.nanoTime()
                    if (response.isSuccessful) {
                        val durationMs = (endTime - startTime) / 1_000_000L
                        pings.add(durationMs)
                        onUpdate(durationMs.toDouble())
                    }
                }
            } catch (e: Exception) {
                isOffline = true
                Log.e("SpeedTestEngine", "Ping failed: ${e.message}")
                break
            }
            delay(150)
        }

        if (pings.isEmpty() || isOffline) {
            throw IOException("Ping speed check connection failed. Device is hosting an offline connection or server unreachable.")
        }

        val averagePing = pings.average()
        
        // Jitter is the average absolute difference of sequential ping values
        var jitterTotal = 0.0
        if (pings.size > 1) {
            for (i in 0 until pings.size - 1) {
                jitterTotal += abs(pings[i] - pings[i + 1])
            }
        }
        val jitter = if (pings.size > 1) jitterTotal / (pings.size - 1) else 0.0

        Pair(averagePing, jitter)
    }

    suspend fun runDownloadTest(
        onProgress: (currentSpeedMbps: Double) -> Unit
    ): Double = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(downloadUrl)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Download speed check failed. Server returned unexpected code: ${response.code}")
                }

                val body = response.body ?: throw IOException("Empty payload response received from download edge node.")
                val inputStream: InputStream = body.byteStream()
                val buffer = ByteArray(16384) // 16KB blocks
                var bytesRead = 0L
                val startTime = System.nanoTime()
                var lastNotifyTime = System.nanoTime()

                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    bytesRead += read

                    val now = System.nanoTime()
                    // Throttle callbacks to 100ms for smooth UI animations
                    if (now - lastNotifyTime > 100_000_000L) {
                        val elapsedSec = (now - startTime) / 1_000_000_000.0
                        if (elapsedSec > 0) {
                            val speedMbps = (bytesRead * 8.0) / (elapsedSec * 1_000_000.0)
                            onProgress(speedMbps)
                        }
                        lastNotifyTime = now
                    }
                }

                val finalElapsedSec = (System.nanoTime() - startTime) / 1_000_000_000.0
                val finalSpeedMbps = if (finalElapsedSec > 0) {
                    (bytesRead * 8.0) / (finalElapsedSec * 1_000_000.0)
                } else {
                    0.0
                }

                if (finalSpeedMbps < 0.01) {
                    throw IOException("Network transfer bandwidth is too slow to measure accurate download throughput.")
                }
                finalSpeedMbps
            }
        } catch (e: Exception) {
            Log.e("SpeedTestEngine", "Download failed: ${e.message}")
            throw e
        }
    }

    suspend fun runUploadTest(
        onProgress: (currentSpeedMbps: Double) -> Unit
    ): Double = withContext(Dispatchers.IO) {
        // Upload a 1MB payload of zeros
        val uploadBytesSize = 1_048_576L
        val progressRequestBody = ProgressRequestBody(
            contentType = "application/octet-stream".toMediaTypeOrNull(),
            contentSize = uploadBytesSize,
            onProgress = onProgress
        )

        val request = Request.Builder()
            .url(uploadUrl)
            .post(progressRequestBody)
            .build()

        try {
            val startTime = System.nanoTime()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val endTime = System.nanoTime()
                    val elapsedSec = (endTime - startTime) / 1_000_000_000.0
                    val speedMbps = (uploadBytesSize * 8.0) / (elapsedSec * 1_000_000.0)
                    if (speedMbps < 0.01) {
                        throw IOException("Network transfer bandwidth is too slow to measure accurate upload throughput.")
                    }
                    speedMbps
                } else {
                    throw IOException("Upload speed check failed. Server returned unexpected code: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("SpeedTestEngine", "Upload failed: ${e.message}")
            throw e
        }
    }

    class ProgressRequestBody(
        private val contentType: MediaType?,
        private val contentSize: Long,
        private val onProgress: (speedMbps: Double) -> Unit
    ) : RequestBody() {
        override fun contentType(): MediaType? = contentType
        override fun contentLength(): Long = contentSize

        override fun writeTo(sink: BufferedSink) {
            val buffer = ByteArray(16384)
            var bytesWritten = 0L
            val startTime = System.nanoTime()
            var lastNotifyTime = System.nanoTime()

            while (bytesWritten < contentSize) {
                val toWrite = minOf(buffer.size.toLong(), contentSize - bytesWritten).toInt()
                sink.write(buffer, 0, toWrite)
                bytesWritten += toWrite

                val now = System.nanoTime()
                if (now - lastNotifyTime > 50_000_000L || bytesWritten == contentSize) {
                    val elapsedSec = (now - startTime) / 1_000_000_000.0
                    if (elapsedSec > 0) {
                        val speedMbps = (bytesWritten * 8.0) / (elapsedSec * 1_000_000.0)
                        onProgress(speedMbps)
                    }
                    lastNotifyTime = now
                }
            }
        }
    }

    private fun formatIpAddress(ip: Int): String {
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            ip and 0xff,
            (ip shr 8) and 0xff,
            (ip shr 16) and 0xff,
            (ip shr 24) and 0xff
        )
    }

    private fun getMobileIPAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (networkInterface in interfaces) {
                val addresses = Collections.list(networkInterface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress) {
                        val sAddr = address.hostAddress
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("SpeedTestEngine", "Error getting cellular IP: ${ex.message}")
        }
        return null
    }
}
