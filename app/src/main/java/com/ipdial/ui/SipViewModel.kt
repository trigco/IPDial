package com.ipdial.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ipdial.data.model.*
import com.ipdial.data.repository.AccountRepository
import com.ipdial.data.repository.CallLogRepository
import com.ipdial.data.repository.ContactsRepository
import com.ipdial.service.SipEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SipViewModel(app: Application) : AndroidViewModel(app) {

    val repo = AccountRepository(app)
    private val logRepo = CallLogRepository(app)
    private val contactsRepo = ContactsRepository(app)

    val accounts: StateFlow<List<SipAccount>> = repo.accounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLog: StateFlow<List<CallLogEntry>> = logRepo.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callSession: StateFlow<CallSession?> = SipEngine.callSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Contacts state
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dialer state
    private val _dialString = MutableStateFlow("")
    val dialString: StateFlow<String> = _dialString.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    private var searchJob: Job? = null

    init {
        // Auto-select default account
        viewModelScope.launch {
            accounts.collectLatest { list ->
                // Synchronize accounts with SipEngine whenever the list changes
                list.forEach { account ->
                    if (account.isEnabled) {
                        SipEngine.addAccount(account)
                    } else {
                        SipEngine.removeAccount(account.id)
                    }
                }
                if (_selectedAccountId.value == null) {
                    _selectedAccountId.value = list.firstOrNull { it.isDefault }?.id
                        ?: list.firstOrNull()?.id
                }
            }
        }
        refreshContacts()

        // Clear keypad after call ends
        viewModelScope.launch {
            callSession.map { it == null }.distinctUntilChanged().collect { isNull ->
                if (isNull) {
                    _dialString.value = ""
                }
            }
        }
    }

    fun refreshContacts() {
        viewModelScope.launch {
            _contacts.value = contactsRepo.getContacts(_searchQuery.value)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            refreshContacts()
        }
    }

    fun dialPad(char: Char) {
        _dialString.value += char
        // If in call, send DTMF
        if (callSession.value?.state == CallState.CONFIRMED) {
            SipEngine.sendDtmf(char)
        }
    }

    fun backspace() {
        val s = _dialString.value
        if (s.isNotEmpty()) _dialString.value = s.dropLast(1)
    }

    fun clearDial() { _dialString.value = "" }

    fun selectAccount(id: String) { _selectedAccountId.value = id }

    fun makeCall(overrideNumber: String? = null) {
        var dest = (overrideNumber ?: _dialString.value).trim()
        android.util.Log.d("SipViewModel", "makeCall pressed. dest=$dest")
        if (dest.isBlank()) return
        val accId = _selectedAccountId.value ?: accounts.value.firstOrNull()?.id
        android.util.Log.d("SipViewModel", "Selected Account ID: $accId")
        if (accId == null) return
        
        val account = accounts.value.find { it.id == accId }
        android.util.Log.d("SipViewModel", "Account found: ${account != null}, enabled: ${account?.isEnabled}")
        if (account != null && account.isEnabled) {
            if (!dest.contains("@") && !dest.startsWith("sip:")) {
                dest = "sip:$dest@${account.domain}"
            }
            android.util.Log.d("SipViewModel", "Final dest: $dest")
            
            if (account.regStatus != RegStatus.REGISTERED) {
                android.util.Log.w("SipViewModel", "Account status is ${account.regStatus}. Call might be rejected by provider.")
            }

            val success = SipEngine.makeCall(accId, dest)
            android.util.Log.d("SipViewModel", "SipEngine.makeCall success: $success")
        }
    }

    fun answerCall() {
        val id = callSession.value?.callId ?: return
        SipEngine.answerCall(id)
    }

    fun hangup() { SipEngine.hangupCall() }
    fun toggleMute() { SipEngine.setMute(!(callSession.value?.isMuted ?: false)) }
    fun toggleSpeaker() { SipEngine.setSpeaker(!(callSession.value?.isSpeaker ?: false)) }
    fun toggleHold() { SipEngine.holdCall(!(callSession.value?.isOnHold ?: false)) }

    fun toggleRecording() {
        val session = callSession.value ?: return
        if (session.isRecording) {
            SipEngine.stopRecording()
        } else {
            val file = java.io.File(getApplication<Application>().filesDir, "recordings")
            if (!file.exists()) file.mkdirs()
            val recFile = java.io.File(file, "call_${System.currentTimeMillis()}.wav")
            SipEngine.startRecording(recFile.absolutePath)
        }
    }

    fun saveAccount(account: SipAccount) = viewModelScope.launch {
        repo.saveAccount(account)
        if (account.isEnabled) SipEngine.addAccount(account)
        else SipEngine.removeAccount(account.id)
    }

    fun deleteAccount(id: String) = viewModelScope.launch {
        SipEngine.removeAccount(id)
        repo.deleteAccount(id)
    }

    fun setDefaultAccount(id: String) = viewModelScope.launch { repo.setDefault(id) }

    fun toggleContactFavorite(contact: Contact) = viewModelScope.launch {
        val newFavoriteStatus = !contact.isFavorite
        // Optimistic update
        _contacts.value = _contacts.value.map {
            if (it.id == contact.id) it.copy(isFavorite = newFavoriteStatus) else it
        }
        contactsRepo.toggleFavorite(contact.id, newFavoriteStatus)
    }

    /** Initiate a call-back from a call-log entry. */
    fun callBack(entry: CallLogEntry) {
        val accId = entry.accountId.ifBlank {
            _selectedAccountId.value ?: accounts.value.firstOrNull()?.id ?: return
        }
        _selectedAccountId.value = accId
        makeCall(entry.remoteUri)
    }

    /** Log a completed/missed call.  Called from SipService after call ends. */
    fun logCall(entry: CallLogEntry) = viewModelScope.launch { logRepo.insert(entry) }
}
