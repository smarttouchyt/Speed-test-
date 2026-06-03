package com.example.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.SpeedTestResult
import com.example.data.model.NetworkUsageRecord
import com.example.data.repository.SpeedTestRepository
import com.example.engine.SpeedTestEngine
import com.example.scheduler.SpeedTestSchedulerReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Random
import java.util.Locale

enum class TestState {
    IDLE,
    PINGING,
    DOWNLOADING,
    UPLOADING,
    COMPLETED,
    ERROR
}

class SpeedTestViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SpeedTestRepository
    private val engine: SpeedTestEngine
    private val sharedPrefs: SharedPreferences

    // UI States
    private val _testState = MutableStateFlow(TestState.IDLE)
    val testState = _testState.asStateFlow()

    // Gauge values
    private val _currentDownloadSpeed = MutableStateFlow(0.0)
    val currentDownloadSpeed = _currentDownloadSpeed.asStateFlow()

    private val _currentUploadSpeed = MutableStateFlow(0.0)
    val currentUploadSpeed = _currentUploadSpeed.asStateFlow()

    private val _currentPing = MutableStateFlow(0.0)
    val currentPing = _currentPing.asStateFlow()

    private val _currentJitter = MutableStateFlow(0.0)
    val currentJitter = _currentJitter.asStateFlow()

    // Real-time progress visual points for graphs
    private val _downloadPoints = MutableStateFlow<List<Double>>(emptyList())
    val downloadPoints = _downloadPoints.asStateFlow()

    private val _uploadPoints = MutableStateFlow<List<Double>>(emptyList())
    val uploadPoints = _uploadPoints.asStateFlow()

    // Database statistics and results history
    private val _history = MutableStateFlow<List<SpeedTestResult>>(emptyList())
    val history = _history.asStateFlow()

    // Live accumulated data usage historical series
    private val _usageRecords = MutableStateFlow<List<NetworkUsageRecord>>(emptyList())
    val usageRecords = _usageRecords.asStateFlow()

    // Network Metadata
    private val _networkInfo = MutableStateFlow<SpeedTestEngine.NetworkInfo?>(null)
    val networkInfo = _networkInfo.asStateFlow()

    // Scheduler states
    private val _isSchedulerEnabled = MutableStateFlow(false)
    val isSchedulerEnabled = _isSchedulerEnabled.asStateFlow()

    private val _schedulerIntervalHours = MutableStateFlow(6) // default: 6 hours
    val schedulerIntervalHours = _schedulerIntervalHours.asStateFlow()

    private val _nextSchedulerRun = MutableStateFlow("Not Scheduled")
    val nextSchedulerRun = _nextSchedulerRun.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SpeedTestRepository(database.speedTestDao())
        engine = SpeedTestEngine(application)
        sharedPrefs = application.getSharedPreferences("speed_test_prefs", Context.MODE_PRIVATE)

        // Read saved scheduler items
        val isEnabled = sharedPrefs.getBoolean("scheduler_enabled", false)
        val interval = sharedPrefs.getInt("scheduler_interval", 6)
        _isSchedulerEnabled.value = isEnabled
        _schedulerIntervalHours.value = interval
        calculateNextRunTime()

        // Sync history list from Room persistence
        viewModelScope.launch {
            repository.allResults.collectLatest { results ->
                _history.value = results
            }
        }

        // Sync and monitor statistics tracker history flow
        viewModelScope.launch {
            repository.allUsageRecords.collectLatest { usageList ->
                _usageRecords.value = usageList
            }
        }

        // Initialize connection network data and update general metrics
        refreshNetworkDetails()
    }

    fun refreshNetworkDetails() {
        viewModelScope.launch {
            // Get local transport attributes
            val details = engine.getNetworkDetails()
            _networkInfo.value = details

            // Fetch public IP details asynchronously to avoid blocking
            val ipDetails = engine.fetchPublicIpInfo()
            _networkInfo.value = details.copy(
                publicIpAddress = ipDetails.first,
                ispName = ipDetails.second
            )
            // Synchronize current device telemetry data usage
            updateUsageStats(0L, 0L)
        }
    }

    fun startSpeedTest() {
        if (_testState.value != TestState.IDLE && _testState.value != TestState.COMPLETED && _testState.value != TestState.ERROR) {
            return // test already in progress
        }

        viewModelScope.launch {
            try {
                // 1. Reset states
                _testState.value = TestState.PINGING
                _currentDownloadSpeed.value = 0.0
                _currentUploadSpeed.value = 0.0
                _currentPing.value = 0.0
                _currentJitter.value = 0.0
                _downloadPoints.value = emptyList()
                _uploadPoints.value = emptyList()

                // Refresh connection types right at the start
                val rawDetails = engine.getNetworkDetails()
                _networkInfo.value = rawDetails

                // Load public details asynchronously
                launch {
                    val ipDetails = engine.fetchPublicIpInfo()
                    _networkInfo.value = _networkInfo.value?.copy(
                        publicIpAddress = ipDetails.first,
                        ispName = ipDetails.second
                    )
                }

                // 2. Perform Ping and Jitter
                val pingJitter = engine.runPingTest { pingProgress ->
                    _currentPing.value = pingProgress
                }
                _currentPing.value = pingJitter.first
                _currentJitter.value = pingJitter.second

                // 3. Perform Download Speed Test
                _testState.value = TestState.DOWNLOADING
                val downloadSpeed = engine.runDownloadTest { speed ->
                    _currentDownloadSpeed.value = speed
                    _downloadPoints.value = _downloadPoints.value + speed
                }
                _currentDownloadSpeed.value = downloadSpeed

                // 4. Perform Upload Speed Test
                _testState.value = TestState.UPLOADING
                val uploadSpeed = engine.runUploadTest { speed ->
                    _currentUploadSpeed.value = speed
                    _uploadPoints.value = _uploadPoints.value + speed
                }
                _currentUploadSpeed.value = uploadSpeed

                // 5. Complete test and insert in Room History list
                _testState.value = TestState.COMPLETED
                
                val currentNet = _networkInfo.value
                val finalIsp = currentNet?.ispName ?: "Unknown"
                val finalPublicIp = currentNet?.publicIpAddress ?: "Unknown IP"
                val finalGateway = currentNet?.gatewayIp ?: "N/A"

                val testResult = SpeedTestResult(
                    timestamp = System.currentTimeMillis(),
                    downloadSpeed = downloadSpeed,
                    uploadSpeed = uploadSpeed,
                    ping = pingJitter.first,
                    jitter = pingJitter.second,
                    connectionType = currentNet?.connectionType ?: "WIFI",
                    networkName = finalIsp,
                    ipAddress = finalPublicIp,
                    gatewayIp = finalGateway
                )

                repository.insertResult(testResult)
                
                // Log actual speed test data packet bytes transferred
                val actualRxBytes = 2_097_152L  // 2MB Cloudflare download payload
                val actualTxBytes = 1_048_576L  // 1MB Cloudflare upload payload
                updateUsageStats(actualRxBytes, actualTxBytes)

                refreshNetworkDetails()

            } catch (e: Exception) {
                Log.e("SpeedTestViewModel", "Error running speed check: ${e.message}")
                _testState.value = TestState.ERROR
            }
        }
    }

    fun deleteHistoryRecord(id: Int) {
        viewModelScope.launch {
            repository.deleteResult(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllResults()
        }
    }

    // Scheduler Setup Updates
    fun toggleScheduler(enabled: Boolean) {
        _isSchedulerEnabled.value = enabled
        sharedPrefs.edit().putBoolean("scheduler_enabled", enabled).apply()
        
        applySchedulerToAndroid()
        calculateNextRunTime()
    }

    fun setSchedulerInterval(hours: Int) {
        _schedulerIntervalHours.value = hours
        sharedPrefs.edit().putInt("scheduler_interval", hours).apply()
        
        if (_isSchedulerEnabled.value) {
            applySchedulerToAndroid()
        }
        calculateNextRunTime()
    }

    private fun applySchedulerToAndroid() {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, SpeedTestSchedulerReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            12345,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (_isSchedulerEnabled.value) {
            val intervalMillis = _schedulerIntervalHours.value * 60 * 60 * 1000L
            val triggerTime = System.currentTimeMillis() + intervalMillis
            
            // Set periodic repeating background alarm
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intervalMillis,
                pendingIntent
            )
            Log.d("SpeedTestViewModel", "Successfully set AlarmManager interval: ${_schedulerIntervalHours.value} hours")
        } else {
            alarmManager.cancel(pendingIntent)
            Log.d("SpeedTestViewModel", "AlarmManager cancelled successfully")
        }
    }

    private fun calculateNextRunTime() {
        if (!_isSchedulerEnabled.value) {
            _nextSchedulerRun.value = "Disabled"
            return
        }

        val intervalMillis = _schedulerIntervalHours.value * 60 * 60 * 1000L
        val nextTime = System.currentTimeMillis() + intervalMillis
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        _nextSchedulerRun.value = sdf.format(Date(nextTime))
    }

    // Modern Network Data Usage Stats Engine
    fun updateUsageStats(speedTestDownloadBytes: Long, speedTestUploadBytes: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val totalRx = android.net.TrafficStats.getTotalRxBytes()
            val totalTx = android.net.TrafficStats.getTotalTxBytes()

            val diffRx: Long
            val diffTx: Long

            if (totalRx == android.net.TrafficStats.UNSUPPORTED.toLong() || totalRx <= 0) {
                // If TrafficStats is unsupported on this specific emulator profile, we do NOT fake data.
                // We only log the actual bytes consumed by our speed test suite.
                diffRx = 0L
                diffTx = 0L
            } else {
                // Read past snapshots to compute actual difference delta
                val prevRx = sharedPrefs.getLong("traffic_prev_rx", 0L)
                val prevTx = sharedPrefs.getLong("traffic_prev_tx", 0L)

                diffRx = if (prevRx in 1..totalRx) totalRx - prevRx else 0L
                diffTx = if (prevTx in 1..totalTx) totalTx - prevTx else 0L

                sharedPrefs.edit()
                    .putLong("traffic_prev_rx", totalRx)
                    .putLong("traffic_prev_tx", totalTx)
                    .apply()
            }

            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val existing = repository.getUsageRecord(dateKey)

            if (existing != null) {
                val updated = existing.copy(
                    bytesDownloaded = existing.bytesDownloaded + diffRx + speedTestDownloadBytes,
                    bytesUploaded = existing.bytesUploaded + diffTx + speedTestUploadBytes,
                    speedTestRxBytes = existing.speedTestRxBytes + speedTestDownloadBytes,
                    speedTestTxBytes = existing.speedTestTxBytes + speedTestUploadBytes
                )
                repository.insertUsageRecord(updated)
            } else {
                val record = NetworkUsageRecord(
                    dateKey = dateKey,
                    bytesDownloaded = diffRx + speedTestDownloadBytes,
                    bytesUploaded = diffTx + speedTestUploadBytes,
                    speedTestRxBytes = speedTestDownloadBytes,
                    speedTestTxBytes = speedTestUploadBytes
                )
                repository.insertUsageRecord(record)
            }
        }
    }

    fun clearAllUsageStats() {
        viewModelScope.launch {
            repository.clearAllUsageRecords()
        }
    }
}
