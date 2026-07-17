package com.ipdial.data.repository

import android.content.Context
import com.ipdial.data.local.AppDatabase
import com.ipdial.data.local.CallLogEntity
import com.ipdial.data.model.CallLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CallLogRepository private constructor(context: Context) {
    
    // ডাটাবেজ কলের স্ট্যান্ডার্ড মেথড। (যদি আপনার প্রজেক্টে এর নাম অন্য কিছু হয়, তবে getDatabase পরিবর্তন করে সেটি বসিয়ে দেবেন)
    private val dao = AppDatabase.getDatabase(context).callLogDao()

    // SipViewModel এর জন্য 'entries' প্রপার্টি রিস্টোর করা হলো
    val entries: Flow<List<CallLogEntry>> = dao.getAllLogs().map { list -> 
        list.map { it.toDomain() } 
    }

    fun getCallLogs(accountId: String? = null): Flow<List<CallLogEntry>> {
        return if (accountId != null) {
            dao.getLogsForAccount(accountId).map { list -> list.map { it.toDomain() } }
        } else {
            entries
        }
    }

    suspend fun insert(entry: CallLogEntry) {
        dao.insert(CallLogEntity.fromDomain(entry))
    }

    // SipViewModel এর জন্য 'delete' মেথড রিস্টোর করা হলো
    suspend fun delete(entry: CallLogEntry) {
        dao.delete(CallLogEntity.fromDomain(entry))
    }

    // মাল্টিপল ডিলিট করার অপশন রাখা হলো
    suspend fun deleteLogs(logs: List<CallLogEntry>) {
        logs.forEach { dao.delete(CallLogEntity.fromDomain(it)) }
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun clearForAccount(accountId: String) {
        dao.clearAll()
    }

    companion object {
        @Volatile
        private var instance: CallLogRepository? = null

        fun getInstance(context: Context): CallLogRepository {
            return instance ?: synchronized(this) {
                instance ?: CallLogRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
