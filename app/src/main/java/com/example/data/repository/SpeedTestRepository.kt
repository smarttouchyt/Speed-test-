package com.example.data.repository

import com.example.data.local.SpeedTestDao
import com.example.data.model.SpeedTestResult
import com.example.data.model.NetworkUsageRecord
import kotlinx.coroutines.flow.Flow

class SpeedTestRepository(private val speedTestDao: SpeedTestDao) {
    val allResults: Flow<List<SpeedTestResult>> = speedTestDao.getAllResults()
    val allUsageRecords: Flow<List<NetworkUsageRecord>> = speedTestDao.getAllUsageRecords()

    suspend fun insertResult(result: SpeedTestResult) {
        speedTestDao.insertResult(result)
    }

    suspend fun deleteResult(id: Int) {
        speedTestDao.deleteResult(id)
    }

    suspend fun clearAllResults() {
        speedTestDao.clearAllResults()
    }

    suspend fun insertUsageRecord(record: NetworkUsageRecord) {
        speedTestDao.insertUsageRecord(record)
    }

    suspend fun getUsageRecord(dateKey: String): NetworkUsageRecord? {
        return speedTestDao.getUsageRecord(dateKey)
    }

    suspend fun clearAllUsageRecords() {
        speedTestDao.clearAllUsageRecords()
    }
}
