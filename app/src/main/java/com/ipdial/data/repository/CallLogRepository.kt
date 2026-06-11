package com.ipdial.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ipdial.data.model.CallDirection
import com.ipdial.data.model.CallLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simple SharedPreferences-backed call-log repository.
 * No Room dependency needed; the log is small and JSON round-trips are fine.
 *
 * Entries are stored newest-first.  Maximum 200 entries are retained.
 */
class CallLogRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("call_log", Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow(load())
    val entries: Flow<List<CallLogEntry>> = _entries.asStateFlow()

    // ── Public API ─────────────────────────────────────────────────────────

    fun insert(entry: CallLogEntry) {
        _entries.update { current ->
            listOf(entry) + current.take(199)  // cap at 200
        }
        persist(_entries.value)
    }

    // ── Serialisation ──────────────────────────────────────────────────────

    private fun load(): List<CallLogEntry> = runCatching {
        val json = prefs.getString(KEY_LOG, null) ?: return emptyList()
        val arr  = JSONArray(json)
        (0 until arr.length()).map { i -> arr.getJSONObject(i).toEntry() }
    }.getOrDefault(emptyList())

    private fun persist(entries: List<CallLogEntry>) {
        val arr = JSONArray().apply { entries.forEach { put(it.toJson()) } }
        prefs.edit { putString(KEY_LOG, arr.toString()) }
    }

    // ── JSON helpers ───────────────────────────────────────────────────────

    private fun CallLogEntry.toJson() = JSONObject().apply {
        put("id",          id)
        put("accountId",   accountId)
        put("remoteUri",   remoteUri)
        put("remoteName",  remoteDisplayName)
        put("direction",   direction.name)
        put("missed",      missed)
        put("ts",          timestampMs)
        put("dur",         durationSeconds)
    }

    private fun JSONObject.toEntry() = CallLogEntry(
        id                  = getString("id"),
        accountId           = optString("accountId"),
        remoteUri           = optString("remoteUri"),
        remoteDisplayName   = optString("remoteName"),
        direction           = runCatching {
            CallDirection.valueOf(getString("direction"))
        }.getOrDefault(CallDirection.INCOMING),
        missed              = optBoolean("missed", false),
        timestampMs         = optLong("ts", System.currentTimeMillis()),
        durationSeconds     = optLong("dur", 0L),
    )

    companion object {
        private const val KEY_LOG = "entries_v1"
    }
}
