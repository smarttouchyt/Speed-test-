package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SpeedTestResult
import com.example.data.model.NetworkUsageRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {
    @Query("SELECT * FROM speed_test_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<SpeedTestResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: SpeedTestResult)

    @Query("DELETE FROM speed_test_results WHERE id = :id")
    suspend fun deleteResult(id: Int)

    @Query("DELETE FROM speed_test_results")
    suspend fun clearAllResults()

    // Data usage statistics queries
    @Query("SELECT * FROM network_usage_records ORDER BY dateKey DESC")
    fun getAllUsageRecords(): Flow<List<NetworkUsageRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageRecord(record: NetworkUsageRecord)

    @Query("SELECT * FROM network_usage_records WHERE dateKey = :dateKey")
    suspend fun getUsageRecord(dateKey: String): NetworkUsageRecord?

    @Query("DELETE FROM network_usage_records")
    suspend fun clearAllUsageRecords()
}
