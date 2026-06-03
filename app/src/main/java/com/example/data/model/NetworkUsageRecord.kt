package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_usage_records")
data class NetworkUsageRecord(
    @PrimaryKey val dateKey: String, // format: "YYYY-MM-DD"
    val bytesDownloaded: Long,      // General rx bytes for this date
    val bytesUploaded: Long,        // General tx bytes for this date
    val speedTestRxBytes: Long,     // Download bytes specifically consumed by our speed tests
    val speedTestTxBytes: Long      // Upload bytes specifically consumed by our speed tests
)
