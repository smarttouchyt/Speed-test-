package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SpeedTestResult
import com.example.data.model.NetworkUsageRecord
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.example.engine.SpeedTestEngine
import com.example.ui.components.HistoryChart
import com.example.ui.components.SpeedometerGauge
import com.example.ui.theme.*
import com.example.ui.viewmodel.SpeedTestViewModel
import com.example.ui.viewmodel.TestState
import java.text.SimpleDateFormat
import java.util.*

enum class DashboardTab {
    TEST,
    HISTORY,
    NETWORK,
    SCHEDULER
}

@Composable
fun DashboardScreen(
    viewModel: SpeedTestViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(DashboardTab.TEST) }
    val context = LocalContext.current

    val testState by viewModel.testState.collectAsStateWithLifecycle()
    val currentDownloadSpeed by viewModel.currentDownloadSpeed.collectAsStateWithLifecycle()
    val currentUploadSpeed by viewModel.currentUploadSpeed.collectAsStateWithLifecycle()
    val currentPing by viewModel.currentPing.collectAsStateWithLifecycle()
    val currentJitter by viewModel.currentJitter.collectAsStateWithLifecycle()

    val downloadPoints by viewModel.downloadPoints.collectAsStateWithLifecycle()
    val uploadPoints by viewModel.uploadPoints.collectAsStateWithLifecycle()

    val historyEntries by viewModel.history.collectAsStateWithLifecycle()
    val usageRecords by viewModel.usageRecords.collectAsStateWithLifecycle()
    val networkInfo by viewModel.networkInfo.collectAsStateWithLifecycle()

    val isSchedulerEnabled by viewModel.isSchedulerEnabled.collectAsStateWithLifecycle()
    val schedulerIntervalHours by viewModel.schedulerIntervalHours.collectAsStateWithLifecycle()
    val nextSchedulerRun by viewModel.nextSchedulerRun.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = CyberBlackSecondary,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f)),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick state scheduler overview row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AUTO-SCHEDULER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isSchedulerEnabled) "Next test: $nextSchedulerRun" else "Paused",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )
                        }

                        Switch(
                            checked = isSchedulerEnabled,
                            onCheckedChange = { viewModel.toggleScheduler(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = CyberSlateLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    HorizontalDivider(
                        color = ElegantBorder.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavButton(
                            label = "Speed",
                            icon = Icons.Default.PlayArrow,
                            isSelected = activeTab == DashboardTab.TEST,
                            activeColor = NeonCyan,
                            onClick = { activeTab = DashboardTab.TEST }
                        )
                        BottomNavButton(
                            label = "History",
                            icon = Icons.Default.List,
                            isSelected = activeTab == DashboardTab.HISTORY,
                            activeColor = NeonCyan,
                            onClick = { activeTab = DashboardTab.HISTORY }
                        )
                        BottomNavButton(
                            label = "Gateway",
                            icon = Icons.Default.Info,
                            isSelected = activeTab == DashboardTab.NETWORK,
                            activeColor = NeonCyan,
                            onClick = { activeTab = DashboardTab.NETWORK }
                        )
                        BottomNavButton(
                            label = "Scheduler",
                            icon = Icons.Default.Settings,
                            isSelected = activeTab == DashboardTab.SCHEDULER,
                            activeColor = NeonCyan,
                            onClick = { activeTab = DashboardTab.SCHEDULER }
                        )
                    }
                }
            }
        },
        containerColor = CyberBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Dashboard Header
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CONNECTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (networkInfo != null && networkInfo?.connectionType != "OFFLINE") {
                            networkInfo?.ispName ?: "Unknown Network"
                        } else {
                            "Offline Connection"
                        },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .background(CyberSlate, RoundedCornerShape(50))
                        .border(BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f)), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            viewModel.refreshNetworkDetails()
                            Toast.makeText(context, "Connection parameters updated", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "IP: ${networkInfo?.publicIpAddress ?: "0.0.0.0"}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (networkInfo?.connectionType == "OFFLINE") Color.Red else GreenPulse,
                            shape = RoundedCornerShape(50)
                        )
                )
                Text(
                    text = if (networkInfo != null && networkInfo?.connectionType != "OFFLINE") {
                        "${networkInfo?.connectionType} • ${networkInfo?.signalStrength ?: "Stable Connection"}"
                    } else {
                        "No internet association detected"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab rendering with transitions
            Crossfade(
                targetState = activeTab,
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    DashboardTab.TEST -> {
                        SpeedTestTab(
                            testState = testState,
                            downloadSpeed = currentDownloadSpeed,
                            uploadSpeed = currentUploadSpeed,
                            ping = currentPing,
                            jitter = currentJitter,
                            downloadPoints = downloadPoints,
                            uploadPoints = uploadPoints,
                            networkDetails = networkInfo,
                            onStartTest = { viewModel.startSpeedTest() }
                        )
                    }
                    DashboardTab.HISTORY -> {
                        HistoryTab(
                            historyEntries = historyEntries,
                            onDelete = { id -> viewModel.deleteHistoryRecord(id) },
                            onClearAll = { viewModel.clearAllHistory() }
                        )
                    }
                    DashboardTab.NETWORK -> {
                        NetworkInfoTab(
                            networkDetails = networkInfo,
                            usageRecords = usageRecords,
                            historyEntries = historyEntries,
                            onRefresh = { viewModel.refreshNetworkDetails() },
                            onClearUsage = { viewModel.clearAllUsageStats() }
                        )
                    }
                    DashboardTab.SCHEDULER -> {
                        SchedulerTab(
                            enabled = isSchedulerEnabled,
                            intervalHours = schedulerIntervalHours,
                            nextScheduledRun = nextSchedulerRun,
                            onToggle = { enabled -> viewModel.toggleScheduler(enabled) },
                            onIntervalSelected = { hours -> viewModel.setSchedulerInterval(hours) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) activeColor else TextSecondary.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextSecondary.copy(alpha = 0.6f)
        )
    }
}

// -------------------------------------------------------------
// TAB 1: Speed Test center
// -------------------------------------------------------------
@Composable
fun SpeedTestTab(
    testState: TestState,
    downloadSpeed: Double,
    uploadSpeed: Double,
    ping: Double,
    jitter: Double,
    downloadPoints: List<Double>,
    uploadPoints: List<Double>,
    networkDetails: SpeedTestEngine.NetworkInfo?,
    onStartTest: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Speedometer Gauge with Embedded Floating Action Trigger
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val displaySpeed = when (testState) {
                TestState.PINGING -> ping
                TestState.DOWNLOADING -> downloadSpeed
                TestState.UPLOADING -> uploadSpeed
                else -> 0.0
            }

            val scaleToMax = if (displaySpeed > 100) 1000.0 else 100.0

            SpeedometerGauge(
                currentSpeed = displaySpeed,
                maxSpeed = scaleToMax,
                state = testState
            )

            // Floating Refresh Circle Button matched to the HTML bottom-right pulse design
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 24.dp)
                    .size(48.dp)
                    .background(CyberSlate, RoundedCornerShape(50))
                    .border(BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f)), RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        if (testState == TestState.IDLE || testState == TestState.COMPLETED || testState == TestState.ERROR) {
                            onStartTest()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Trigger speed run",
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Live Speed status metrics: elegant 3-column stats cards resembling the HTML template
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = CyberSlate,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Ping",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.0f", ping)} ms",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = CyberSlate,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Jitter",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.0f", jitter)} ms",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = CyberSlate,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Upload",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1f", uploadSpeed),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Real-Time Graphic Area Plot
        AnimatedVisibility(
            visible = testState == TestState.DOWNLOADING || testState == TestState.UPLOADING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                color = CyberSlate.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (testState == TestState.DOWNLOADING) "Download Progress Graph" else "Upload Progress Graph",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (testState == TestState.DOWNLOADING) NeonCyan else NeonViolet,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    HistoryChart(
                        dataPoints = if (testState == TestState.DOWNLOADING) downloadPoints else uploadPoints,
                        lineColor = if (testState == TestState.DOWNLOADING) NeonCyan else NeonViolet
                    )
                }
            }
        }

        // Share button on completed result
        AnimatedVisibility(
            visible = testState == TestState.COMPLETED,
            enter = fadeIn()
        ) {
            Button(
                onClick = {
                    val fakeResult = SpeedTestResult(
                        timestamp = System.currentTimeMillis(),
                        downloadSpeed = downloadSpeed,
                        uploadSpeed = uploadSpeed,
                        ping = ping,
                        jitter = jitter,
                        connectionType = networkDetails?.connectionType ?: "WIFI",
                        networkName = networkDetails?.ispName ?: "Local Data",
                        ipAddress = networkDetails?.publicIpAddress ?: "N/A",
                        gatewayIp = networkDetails?.gatewayIp ?: "N/A"
                    )
                    shareSpeedResult(context, fakeResult)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberSlateLight
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Speed Results", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Action Trigger Button
        Button(
            onClick = onStartTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (testState) {
                    TestState.IDLE, TestState.COMPLETED, TestState.ERROR -> NeonCyan
                    else -> CyberSlateLight
                }
            ),
            shape = RoundedCornerShape(16.dp),
            enabled = testState == TestState.IDLE || testState == TestState.COMPLETED || testState == TestState.ERROR
        ) {
            Text(
                text = when (testState) {
                    TestState.IDLE -> "BEGIN SPEED TEST"
                    TestState.PINGING -> "Handshaking..."
                    TestState.DOWNLOADING -> "Downloading Packages..."
                    TestState.UPLOADING -> "Uploading Packets..."
                    TestState.COMPLETED -> "RE-TEST NETWORK"
                    TestState.ERROR -> "TEST FAILED. RETRY?"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (testState == TestState.IDLE || testState == TestState.COMPLETED || testState == TestState.ERROR) CyberBlack else TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// -------------------------------------------------------------
// TAB 2: History database entries
// -------------------------------------------------------------
@Composable
fun HistoryTab(
    historyEntries: List<SpeedTestResult>,
    onDelete: (Int) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current

    if (historyEntries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "Empty History",
                    modifier = Modifier.size(64.dp),
                    tint = TextSecondary.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No speed test history recorded yet.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    // Accumulate total data metrics
    val totalDownload = historyEntries.sumOf { it.downloadSpeed }
    val totalUpload = historyEntries.sumOf { it.uploadSpeed }
    val avgPing = historyEntries.map { it.ping }.average()

    Column(modifier = Modifier.fillMaxSize()) {
        // Data Usage Tracker Visual card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            color = CyberSlate,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "DATA HISTORY OVERVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Cumulative Down", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.1f Gb", totalDownload * 0.002), // Estimated 2MB downloaded per standard run
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                    Column {
                        Text("Cumulative Up", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.1f Gb", totalUpload * 0.001), // Estimated 1MB uploaded
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                    Column {
                        Text("Avg Ping Rate", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.0f ms", if (avgPing.isNaN()) 0.0 else avgPing),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonAmber
                        )
                    }
                }
            }
        }

        // List Header with Clear options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PAST RUNS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Text(
                text = "CLEAR ALL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Red,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onClearAll() }
                    .padding(4.dp)
            )
        }

        // History items
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(historyEntries, key = { it.id }) { item ->
                HistoryItemRow(
                    item = item,
                    onDelete = { onDelete(item.id) },
                    onShare = { shareSpeedResult(context, item) }
                )
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    item: SpeedTestResult,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MM/dd h:mm a", Locale.getDefault()) }
    val formattedDate = remember(item.timestamp) { sdf.format(Date(item.timestamp)) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CyberSlate.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.networkName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.connectionType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.connectionType == "WIFI") NeonCyan else NeonViolet,
                        modifier = Modifier
                            .background(
                                color = (if (item.connectionType == "WIFI") NeonCyan else NeonViolet).copy(
                                    alpha = 0.1f
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text("DOWN", fontSize = 9.sp, color = TextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.1f M", item.downloadSpeed),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text("UP", fontSize = 9.sp, color = TextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.1f M", item.uploadSpeed),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonViolet,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text("PING", fontSize = 9.sp, color = TextSecondary)
                        Text(
                            text = String.format(Locale.US, "%.0f ms", item.ping),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonAmber,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = Color.Red.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: Network details & Gateway IP portal admin panel route
// -------------------------------------------------------------
@Composable
fun NetworkInfoTab(
    networkDetails: SpeedTestEngine.NetworkInfo?,
    usageRecords: List<NetworkUsageRecord>,
    historyEntries: List<SpeedTestResult>,
    onRefresh: () -> Unit,
    onClearUsage: () -> Unit
) {
    val context = LocalContext.current
    var subTab by remember { mutableStateOf(0) } // 0 = Diagnostics, 1 = Data Tracker

    if (networkDetails == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonCyan)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nested tab row selector
        TabRow(
            selectedTabIndex = subTab,
            containerColor = Color.Transparent,
            contentColor = NeonCyan,
            divider = { HorizontalDivider(color = ElegantBorder.copy(alpha = 0.2f)) },
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty() && subTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[subTab]),
                        color = NeonCyan
                    )
                }
            }
        ) {
            Tab(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                text = { Text("DIAGNOSTICS", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            )
            Tab(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                text = { Text("DATA TRACKER", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            )
        }

        if (subTab == 0) {
            // DIAGNOSTICS SUB-TAB
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Connected Card Indicator
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberSlate,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ACTIVE TRANSIT PROFILE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = networkDetails.ispName,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "SSID: ${networkDetails.ssid}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (networkDetails.connectionType == "WIFI") NeonCyan else NeonViolet
                                )
                            }

                            Icon(
                                imageVector = if (networkDetails.connectionType == "WIFI") Icons.Default.Home else Icons.Default.Info,
                                contentDescription = "Signal status",
                                tint = if (networkDetails.connectionType == "WIFI") NeonCyan else NeonViolet,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }

                item {
                    // Detailed Metadata Grid List
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberSlate.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NetworkParamRow(label = "Protocol Connection Type", value = networkDetails.connectionType)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.05f))
                            NetworkParamRow(label = "Wi-Fi Connected SSID", value = networkDetails.ssid)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.05f))
                            NetworkParamRow(label = "Link Stability / RSSI", value = networkDetails.signalStrength)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.05f))
                            NetworkParamRow(label = "Link Speed", value = networkDetails.linkSpeed)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.05f))
                            NetworkParamRow(label = "Local Client IP", value = networkDetails.localIpAddress)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.05f))
                            NetworkParamRow(label = "Public WAN IP", value = networkDetails.publicIpAddress)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.05f))
                            NetworkParamRow(label = "Router Default Gateway", value = networkDetails.gatewayIp)
                            HorizontalDivider(color = TextSecondary.copy(alpha = 0.05f))
                            NetworkParamRow(label = "Link Frequency Channel", value = networkDetails.frequency)
                        }
                    }
                }

                if (networkDetails.connectionType == "WIFI" && networkDetails.gatewayIp != "0.0.0.0") {
                    item {
                        // ROUTER ADMIN PANEL ACCESS - CRUCIAL FEATURE!
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonCyan.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Router",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "WIFI ROUTER ADMIN PORTAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Direct Access link to log in to your WiFi router's gateway configuration page.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        val ip = networkDetails.gatewayIp
                                        val url = if (ip.startsWith("http")) ip else "http://$ip"
                                        try {
                                            val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(openUrlIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open gateway URL: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Login Router Dashboard (${networkDetails.gatewayIp})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyberBlack
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Export CSV Trigger Button
                    Button(
                        onClick = {
                            shareCsvData(context, usageRecords, historyEntries)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberSlate
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export admin metrics",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Diagnostic Logs (CSV)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            // DATA USAGE TRACKER SUB-TAB
            var periodSelection by remember { mutableStateOf(0) } // 0 = Daily, 1 = Weekly, 2 = Monthly

            val totalDownload: Long
            val totalUpload: Long
            val testDownload: Long
            val testUpload: Long

            when (periodSelection) {
                0 -> { // Daily (Today)
                    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val todayRecord = usageRecords.firstOrNull { it.dateKey == todayKey } ?: usageRecords.firstOrNull()
                    totalDownload = todayRecord?.bytesDownloaded ?: 0L
                    totalUpload = todayRecord?.bytesUploaded ?: 0L
                    testDownload = todayRecord?.speedTestRxBytes ?: 0L
                    testUpload = todayRecord?.speedTestTxBytes ?: 0L
                }
                1 -> { // Weekly (Past 7 days)
                    val subset = usageRecords.take(7)
                    totalDownload = subset.sumOf { it.bytesDownloaded }
                    totalUpload = subset.sumOf { it.bytesUploaded }
                    testDownload = subset.sumOf { it.speedTestRxBytes }
                    testUpload = subset.sumOf { it.speedTestTxBytes }
                }
                else -> { // Monthly (Past 30 days)
                    totalDownload = usageRecords.sumOf { it.bytesDownloaded }
                    totalUpload = usageRecords.sumOf { it.bytesUploaded }
                    testDownload = usageRecords.sumOf { it.speedTestRxBytes }
                    testUpload = usageRecords.sumOf { it.speedTestTxBytes }
                }
            }

            val totalSumBytes = totalDownload + totalUpload
            val speedSumBytes = testDownload + testUpload
            val generalSumBytes = maxOf(0L, totalSumBytes - speedSumBytes)
            val speedRatio = if (totalSumBytes > 0) (speedSumBytes.toFloat() / totalSumBytes.toFloat()) else 0.0f
            val generalRatio = if (totalSumBytes > 0) (generalSumBytes.toFloat() / totalSumBytes.toFloat()) else 0.0f

            // Interval selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberSlate, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val ranges = listOf("DAILY", "WEEKLY", "MONTHLY")
                ranges.forEachIndexed { idx, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (periodSelection == idx) CyberSlateLight else Color.Transparent)
                            .clickable { periodSelection = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (periodSelection == idx) NeonCyan else TextSecondary
                        )
                    }
                }
            }

            // Summary statistics card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CyberSlate,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACCUMULATED TRANSFERS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(NeonCyan, RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Downloads", fontSize = 11.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatBytes(totalDownload), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(NeonViolet, RoundedCornerShape(50)))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Uploads", fontSize = 11.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(formatBytes(totalUpload), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Combined transfer progress proportion bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(CyberBlackSecondary)
                    ) {
                        if (totalSumBytes > 0) {
                            val downPct = totalDownload.toFloat() / totalSumBytes
                            val upPct = totalUpload.toFloat() / totalSumBytes
                            Box(modifier = Modifier.weight(downPct).fillMaxHeight().background(NeonCyan))
                            Box(modifier = Modifier.weight(upPct).fillMaxHeight().background(NeonViolet))
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(CyberSlateLight))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Combined Traffic:", fontSize = 11.sp, color = TextSecondary)
                        Text(formatBytes(totalSumBytes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                    }
                }
            }

            // Ratio details card (browsing vs tests)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CyberSlate.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "TRAFFIC DISTRIBUTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Speed Test Transits", fontSize = 11.sp, color = NeonCyan)
                            Text(formatBytes(speedSumBytes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f%%", speedRatio * 100),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = ElegantBorder.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Other General Activity", fontSize = 11.sp, color = NeonViolet)
                            Text(formatBytes(generalSumBytes), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f%%", generalRatio * 100),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonViolet
                        )
                    }
                }
            }

            // Historical Date breakdown items header
            Text(
                text = "LOGGED HISTORY BY DATE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            // Historic items vertical list block
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (usageRecords.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No usage records stored", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                } else {
                    items(usageRecords, key = { it.dateKey }) { r ->
                        val prettyDate = try {
                            val inFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                            val outFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                            val parsedDate = inFormat.parse(r.dateKey)
                            if (parsedDate != null) outFormat.format(parsedDate) else r.dateKey
                        } catch (e: Exception) {
                            r.dateKey
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = CyberSlate.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = prettyDate,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tests direct utilization: ${formatBytes(r.speedTestRxBytes + r.speedTestTxBytes)}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "D: ${formatBytes(r.bytesDownloaded)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeonCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "U: ${formatBytes(r.bytesUploaded)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeonViolet,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Admin Maintenance Operations Block
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = Color.Red.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ADMIN MAINTAINER WIPES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Secure operations to clean the active data logs and telemetry databases.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onClearUsage,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Reset Usage Statistics Database",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Share helper for Diagnostic CSV Exports
fun shareCsvData(
    context: Context,
    usageList: List<NetworkUsageRecord>,
    resultsList: List<SpeedTestResult>
) {
    val csvString = StringBuilder()

    csvString.append("=== METRIC DIAGNOSTIC LOG EXPORT ===\n")
    csvString.append("Generated At: 2206-06-03 (Admin Export Mode)\n\n")

    csvString.append("--- SPEED TEST RESULTS HISTORY ---\n")
    csvString.append("Timestamp,ISP/Network,Type,Download (Mbps),Upload (Mbps),Ping (ms),Jitter (ms),IP\n")
    resultsList.forEach { r ->
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dateStr = sdf.format(Date(r.timestamp))
        csvString.append("$dateStr,\"${r.networkName}\",${r.connectionType},${r.downloadSpeed},${r.uploadSpeed},${r.ping},${r.jitter},${r.ipAddress}\n")
    }

    csvString.append("\n\n")

    csvString.append("--- TELEMETRY DATA USAGE TRACKER ---\n")
    csvString.append("DateKey,Download (Bytes),Upload (Bytes),SpeedTest Rx (Bytes),SpeedTest Tx (Bytes)\n")
    usageList.forEach { u ->
        csvString.append("${u.dateKey},${u.bytesDownloaded},${u.bytesUploaded},${u.speedTestRxBytes},${u.speedTestTxBytes}\n")
    }

    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "NetDiagnostics Admin System Export")
            putExtra(Intent.EXTRA_TEXT, csvString.toString())
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export Diagnostic Database (CSV)")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not trigger share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0.0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    if (mb < 1024) {
        return String.format(Locale.US, "%.1f MB", mb)
    }
    val gb = mb / 1024
    return String.format(Locale.US, "%.2f GB", gb)
}

@Composable
fun NetworkParamRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

// -------------------------------------------------------------
// TAB 4: Scheduler
// -------------------------------------------------------------
@Composable
fun SchedulerTab(
    enabled: Boolean,
    intervalHours: Int,
    nextScheduledRun: String,
    onToggle: (Boolean) -> Unit,
    onIntervalSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Feature Introduction Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSlate,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ElegantBorder.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "AUTOMATIC SPEED MONITOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Keep a hands-free record of your internet bandwidth. Results are test-run safely in the background and notified instantly.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Toggle card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CyberSlate.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Background Auto Scheduler",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (enabled) "Scheduling system initiated" else "Scheduling system paused",
                        fontSize = 12.sp,
                        color = if (enabled) NeonCyan else TextSecondary.copy(alpha = 0.7f)
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonAmber,
                        checkedTrackColor = NeonAmber.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = CyberSlateLight
                    )
                )
            }
        }

        // Configuration details with Interval Selector
        AnimatedVisibility(visible = enabled) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CyberSlate.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "MONITOR FREQUENCY INTERVAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 6, 12, 24).forEach { hr ->
                                val isSelected = intervalHours == hr
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (isSelected) NeonAmber.copy(alpha = 0.15f) else CyberSlateLight,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onIntervalSelected(hr) }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${hr}h",
                                        color = if (isSelected) NeonAmber else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Next Scheduled Date Display Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CyberSlate.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Next Target Check",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = nextScheduledRun,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonAmber,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun shareSpeedResult(context: Context, result: SpeedTestResult) {
    val shareText = """
        🚀 Internet Speed Test Result
        📶 Network: ${result.networkName} (${result.connectionType})
        
        ⬇️ Download Speed: ${String.format(Locale.US, "%.1f Mbps", result.downloadSpeed)}
        ⬆️ Upload Speed: ${String.format(Locale.US, "%.1f Mbps", result.uploadSpeed)}
        ⚡ Round-trip Ping: ${String.format(Locale.US, "%.0f ms", result.ping)}
        ⏳ Network Jitter: ${String.format(Locale.US, "%.1f ms", result.jitter)}
        💻 Client WAN IP: ${result.ipAddress}
        
        Tested with Net Speed Monitor App!
    """.trimIndent()

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, "Share Speed Results via")
    context.startActivity(shareIntent)
}
