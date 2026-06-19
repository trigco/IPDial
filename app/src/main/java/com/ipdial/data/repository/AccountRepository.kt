package com.ipdial.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ipdial.data.model.SipAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ipdial_accounts")

class AccountRepository(private val context: Context) {

    private val gson = Gson()
    private val ACCOUNTS_KEY = stringPreferencesKey("accounts")
    private val RINGTONE_KEY = stringPreferencesKey("global_ringtone")
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val CALLING_CARDS_KEY = booleanPreferencesKey("calling_cards")
    private val DND_KEY = booleanPreferencesKey("dnd_enabled")
    private val VIBRATE_KEY = booleanPreferencesKey("global_vibrate")
    private val FONT_SIZE_KEY = stringPreferencesKey("font_size_multiplier")
    private val APP_ICON_KEY = stringPreferencesKey("app_icon_alias")
    private val KEYPAD_DESIGN_KEY = stringPreferencesKey("keypad_design")

    val accounts: Flow<List<SipAccount>> = context.dataStore.data.map { prefs ->
        val json = prefs[ACCOUNTS_KEY] ?: return@map emptyList()
        val type = object : TypeToken<List<SipAccount>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }

    val globalRingtone: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[RINGTONE_KEY] ?: "android.resource://${context.packageName}/raw/ipdial_ringtone"
    }

    val darkModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[DARK_MODE_KEY] ?: false }
    val callingCardsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[CALLING_CARDS_KEY] ?: true }
    val dndEnabled: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[DND_KEY] ?: false }
    val globalVibrate: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[VIBRATE_KEY] ?: true }

    val fontSizeMultiplier: Flow<Float> = context.dataStore.data.map { prefs -> 
        prefs[FONT_SIZE_KEY]?.toFloatOrNull() ?: 1.0f 
    }
    
    val appIconAlias: Flow<String> = context.dataStore.data.map { prefs -> 
        prefs[APP_ICON_KEY] ?: "Default"
    }

    val keypadDesign: Flow<String> = context.dataStore.data.map { prefs -> 
        prefs[KEYPAD_DESIGN_KEY] ?: "Grid"
    }

    suspend fun setDarkMode(enabled: Boolean) = context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    suspend fun setCallingCards(enabled: Boolean) = context.dataStore.edit { it[CALLING_CARDS_KEY] = enabled }
    suspend fun setDnd(enabled: Boolean) = context.dataStore.edit { it[DND_KEY] = enabled }
    suspend fun setGlobalVibrate(enabled: Boolean) = context.dataStore.edit { it[VIBRATE_KEY] = enabled }
    
    suspend fun setFontSizeMultiplier(multiplier: Float) = context.dataStore.edit { it[FONT_SIZE_KEY] = multiplier.toString() }
    suspend fun setAppIconAlias(alias: String) = context.dataStore.edit { it[APP_ICON_KEY] = alias }
    suspend fun setKeypadDesign(design: String) = context.dataStore.edit { it[KEYPAD_DESIGN_KEY] = design }

    suspend fun setGlobalRingtone(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(RINGTONE_KEY)
            else prefs[RINGTONE_KEY] = uri
        }
    }

    suspend fun saveAccount(account: SipAccount) {
        context.dataStore.edit { prefs ->
            val current = getAccountsList(prefs).toMutableList()
            val idx = current.indexOfFirst { it.id == account.id }
            if (idx >= 0) current[idx] = account else current.add(account)
            prefs[ACCOUNTS_KEY] = gson.toJson(current)
        }
    }

    suspend fun deleteAccount(accountId: String) {
        context.dataStore.edit { prefs ->
            val current = getAccountsList(prefs).filter { it.id != accountId }
            prefs[ACCOUNTS_KEY] = gson.toJson(current)
        }
    }

    suspend fun setDefault(accountId: String) {
        context.dataStore.edit { prefs ->
            val current = getAccountsList(prefs).map { acc ->
                acc.copy(isDefault = acc.id == accountId)
            }
            prefs[ACCOUNTS_KEY] = gson.toJson(current)
        }
    }

    suspend fun updateRegStatus(accountId: String, status: com.ipdial.data.model.RegStatus, text: String = "") {
        context.dataStore.edit { prefs ->
            val current = getAccountsList(prefs).map { acc ->
                if (acc.id == accountId) acc.copy(regStatus = status, regStatusText = text) else acc
            }
            prefs[ACCOUNTS_KEY] = gson.toJson(current)
        }
    }

    private fun getAccountsList(prefs: Preferences): List<SipAccount> {
        val json = prefs[ACCOUNTS_KEY] ?: return emptyList()
        val type = object : TypeToken<List<SipAccount>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
