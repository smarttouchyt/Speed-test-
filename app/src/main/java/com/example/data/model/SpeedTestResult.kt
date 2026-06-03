package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_test_results")
data class SpeedTestResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val downloadSpeed: Double, // in Mbps
    val uploadSpeed: Double, // in Mbps
    val ping: Double, // in ms
    val jitter: Double, // in ms
    val connectionType: String, // "WIFI", "MOBILE", or "UNKNOWN"
    val networkName: String, // ISP or Wifi Name
    val ipAddress: String, // Client IP Address
    val gatewayIp: String // Router Gateway IP
)
