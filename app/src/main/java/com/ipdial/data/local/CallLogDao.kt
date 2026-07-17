package com.ipdial.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestampMs DESC")
    fun getAllLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE accountId = :accountId ORDER BY timestampMs DESC")
    fun getLogsForAccount(accountId: String): Flow<List<CallLogEntity>>

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CallLogEntity)

    @Delete
    suspend fun delete(entry: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE id NOT IN (SELECT id FROM call_logs ORDER BY timestampMs DESC LIMIT :limit)")
    suspend fun trim(limit: Int)
}
