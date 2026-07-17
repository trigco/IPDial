package com.ipdial.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ipdial.data.model.CallDirection
import com.ipdial.data.model.CallLogEntry

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val accountId: String,
    val remoteUri: String,
    val remoteDisplayName: String,
    val direction: CallDirection,
    val timestampMs: Long,
    val durationSeconds: Long,
    val missed: Boolean
) {
    fun toDomain(): CallLogEntry {
        return CallLogEntry(
            id = id,
            accountId = accountId,
            remoteUri = remoteUri,
            remoteDisplayName = remoteDisplayName,
            direction = direction,
            timestampMs = timestampMs,
            durationSeconds = durationSeconds,
            missed = missed
        )
    }

    companion object {
        fun fromDomain(entry: CallLogEntry): CallLogEntity {
            return CallLogEntity(
                id = entry.id,
                accountId = entry.accountId,
                remoteUri = entry.remoteUri,
                remoteDisplayName = entry.remoteDisplayName,
                direction = entry.direction,
                timestampMs = entry.timestampMs,
                durationSeconds = entry.durationSeconds,
                missed = entry.missed
            )
        }
    }
}
