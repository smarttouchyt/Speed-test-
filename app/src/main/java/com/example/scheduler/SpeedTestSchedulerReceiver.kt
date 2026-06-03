package com.example.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.SpeedTestResult
import com.example.data.repository.SpeedTestRepository
import com.example.engine.SpeedTestEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class SpeedTestSchedulerReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SpeedTestScheduler", "Automatic alarm triggered! Initiating test...")
        
        val pendingResult = goAsync()
        
        receiverScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = SpeedTestRepository(db.speedTestDao())
                val engine = SpeedTestEngine(context)

                // 1. Gather connection details
                val rawDetails = engine.getNetworkDetails()
                var publicIp = "Offline"
                var isp = rawDetails.ispName
                
                try {
                    val ipInfo = engine.fetchPublicIpInfo()
                    publicIp = ipInfo.first
                    isp = ipInfo.second
                } catch (e: Exception) {
                    Log.e("SpeedTestScheduler", "Failed to fetch public IP: ${e.message}")
                }

                // 2. Run Ping & Jitter
                val pingJitter = engine.runPingTest { }
                val ping = pingJitter.first
                val jitter = pingJitter.second

                // 3. Run Download Test (use silent dummy block for scheduler)
                val downloadSpeed = engine.runDownloadTest { }

                // 4. Run Upload Test (silent)
                val uploadSpeed = engine.runUploadTest { }

                // 5. Store speed test result in database
                val speedTestResult = SpeedTestResult(
                    timestamp = System.currentTimeMillis(),
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed,
                    ping = ping,
                    jitter = jitter,
                    connectionType = rawDetails.connectionType,
                    networkName = isp,
                    ipAddress = publicIp,
                    gatewayIp = rawDetails.gatewayIp
                )

                repository.insertResult(speedTestResult)
                Log.d("SpeedTestScheduler", "Successfully saved scheduled speed test result!")

                // 6. Push a status notification
                showNotification(
                    context,
                    downloadSpeed,
                    uploadSpeed,
                    ping,
                    isp
                )

            } catch (e: Exception) {
                Log.e("SpeedTestScheduler", "Error in automatic speed test scheduled check: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        download: Double,
        upload: Double,
        ping: Double,
        isp: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "scheduled_speed_test_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Automatic Speed Tests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when background auto-scheduled speed checks complete."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedDownload = String.format(Locale.US, "%.1f Mbps", download)
        val formattedUpload = String.format(Locale.US, "%.1f Mbps", upload)
        val formattedPing = String.format(Locale.US, "%.0f ms", ping)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done) // built-in drawable fallback for portability
            .setContentTitle("Auto Speed Test Complete")
            .setContentText("Download: $formattedDownload | Upload: $formattedUpload ($isp)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Network: $isp\nDownload Speed: $formattedDownload\nUpload Speed: $formattedUpload\nPing Latency: $formattedPing")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(777, notification)
    }
}
