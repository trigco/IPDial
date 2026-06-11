package com.ipdial.service

import android.content.Context
import android.util.Log
import com.ipdial.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.pjsip.pjsua2.*

/**
 * PJSIP engine singleton.
 * Manages the Endpoint lifecycle, account registration, and call sessions.
 */
object SipEngine {

    private const val TAG = "SipEngine"

    private var endpoint: Endpoint? = null
    private val accountMap = mutableMapOf<String, PjAccount>()   // accountId -> PjAccount
    private val callMap = mutableMapOf<Int, PjCall>()             // callId -> PjCall
    
    private var udpTransportId: Int = -1
    private var tcpTransportId: Int = -1
    private var tlsTransportId: Int = -1

    private val _callSession = MutableStateFlow<CallSession?>(null)
    val callSession: StateFlow<CallSession?> = _callSession.asStateFlow()

    private val _registrationEvents = MutableStateFlow<Pair<String, RegStatus>?>(null)
    val registrationEvents: StateFlow<Pair<String, RegStatus>?> = _registrationEvents.asStateFlow()

    var onIncomingCall: ((CallSession) -> Unit)? = null

    fun init(context: Context) {
        if (endpoint != null) return
        try {
            System.loadLibrary("pjsua2")
            endpoint = Endpoint().apply {
                libCreate()
                val epCfg = EpConfig().apply {
                    logConfig.level = 5
                    logConfig.consoleLevel = 5
                    medConfig.apply {
                        ecOptions = 0
                        ecTailLen = 200
                        noVad = false
                        clockRate = 16000
                        quality = 8
                    }
                    uaConfig.apply {
                        userAgent = "IPDial/1.0 (Android)"
                        maxCalls = 4
                        stunServer.add("stun.l.google.com:19302")
                    }
                }
                libInit(epCfg)

                val sipTpCfg = TransportConfig()
                sipTpCfg.port = 0
                udpTransportId = transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpCfg)
                
                val tcpTpCfg = TransportConfig()
                tcpTpCfg.port = 0
                tcpTransportId = transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, tcpTpCfg)
                
                val tlsTpCfg = TransportConfig()
                tlsTransportId = transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, tlsTpCfg)

                libStart()
                Log.i(TAG, "PJSIP started")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "PJSIP init failed: ${e.message}")
        }
    }

    fun addAccount(account: SipAccount) {
        endpoint?.libRegisterThread("SipEngineThread")
        try {
            accountMap[account.id]?.let { removeAccount(account.id) }

            val acfg = AccountConfig().apply {
                idUri = "sip:${account.username}@${account.domain}"
                regConfig.registrarUri = "sip:${account.domain}:${account.port}"
                regConfig.timeoutSec = 300

                val cred = AuthCredInfo("digest", "*", account.username, 0, account.password)
                sipConfig.authCreds.add(cred)

                if (account.proxy.isNotBlank()) {
                    sipConfig.proxies.add("sip:${account.proxy}")
                }

                sipConfig.transportId = when (account.transport) {
                    Transport.TCP -> tcpTransportId
                    Transport.TLS -> tlsTransportId
                    else -> udpTransportId
                }

                mediaConfig.apply {
                    srtpUse = if (account.transport == Transport.TLS)
                        pjmedia_srtp_use.PJMEDIA_SRTP_MANDATORY
                    else
                        pjmedia_srtp_use.PJMEDIA_SRTP_DISABLED
                }

                natConfig.iceEnabled = true
                natConfig.turnEnabled = false
                // Use default instead of disabled if disabled causes issues
                natConfig.sipStunUse = pjsua_stun_use.PJSUA_STUN_USE_DEFAULT
            }

            val pjAcc = PjAccount(account.id)
            pjAcc.create(acfg)
            accountMap[account.id] = pjAcc
            configureCodecs(account.codec, account.ecEnabled, account.nsEnabled, account.agcEnabled)
        } catch (e: Throwable) {
            Log.e(TAG, "addAccount failed: ${e.message}")
        }
    }

    fun removeAccount(accountId: String) {
        endpoint?.libRegisterThread("SipEngineThread")
        try {
            accountMap[accountId]?.delete()
            accountMap.remove(accountId)
        } catch (e: Throwable) {
            Log.e(TAG, "removeAccount failed: ${e.message}")
        }
    }

    fun makeCall(accountId: String, destination: String): Boolean {
        Log.d(TAG, "SipEngine.makeCall(acc=$accountId, dest=$destination)")
        endpoint?.libRegisterThread("SipEngineThread")
        return try {
            val pjAcc = accountMap[accountId]
            if (pjAcc == null) {
                Log.e(TAG, "pjAcc is null for $accountId. Current map keys: ${accountMap.keys}")
                return false
            }
            val destUri = if (destination.startsWith("sip:")) destination else "sip:$destination"
            val call = PjCall(pjAcc)
            val prm = CallOpParam(true).apply {
                opt.audioCount = 1
                opt.videoCount = 0
            }
            call.makeCall(destUri, prm)
            Log.i(TAG, "Calling $destUri from account $accountId")
            callMap[call.id] = call
            _callSession.value = CallSession(
                callId = call.id,
                accountId = accountId,
                remoteUri = destUri,
                direction = CallDirection.OUTGOING,
                state = CallState.CALLING
            )
            true
        } catch (e: Throwable) {
            Log.e(TAG, "makeCall failed with exception: ${e.message}", e)
            false
        }
    }

    fun answerCall(callId: Int) {
        endpoint?.libRegisterThread("SipEngineThread")
        callMap[callId]?.let { call ->
            try {
                val prm = CallOpParam().apply { statusCode = pjsip_status_code.PJSIP_SC_OK }
                call.answer(prm)
            } catch (e: Throwable) {
                Log.e(TAG, "answerCall failed: ${e.message}")
            }
        }
    }

    fun hangupCall(callId: Int = -1) {
        endpoint?.libRegisterThread("SipEngineThread")
        val id = if (callId >= 0) callId else _callSession.value?.callId ?: return
        callMap[id]?.let { call ->
            try {
                val prm = CallOpParam().apply { statusCode = pjsip_status_code.PJSIP_SC_DECLINE }
                call.hangup(prm)
            } catch (e: Throwable) {
                Log.e(TAG, "hangup failed: ${e.message}")
            }
        }
        _callSession.value = null
        callMap.remove(id)
    }

    fun setMute(muted: Boolean) {
        endpoint?.libRegisterThread("SipEngineThread")
        _callSession.value?.let { session ->
            callMap[session.callId]?.let { call ->
                try {
                    val ci = call.info
                    if (ci.media.size > 0) {
                        val mi = ci.media.get(0)
                        if (mi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO) {
                            val aud = AudioMedia.typecastFromMedia(call.getMedia(mi.index))
                            if (muted) aud.adjustTxLevel(0f) else aud.adjustTxLevel(1f)
                        }
                    }
                    _callSession.value = session.copy(isMuted = muted)
                } catch (e: Throwable) {
                    Log.e(TAG, "setMute failed: ${e.message}")
                }
            }
        }
    }

    fun setSpeaker(enabled: Boolean) {
        _callSession.value = _callSession.value?.copy(isSpeaker = enabled)
    }

    private var recorder: AudioMediaRecorder? = null

    fun toggleRecording() {
        endpoint?.libRegisterThread("SipEngineThread")
        val session = _callSession.value ?: return
        if (session.isRecording) {
            try {
                recorder?.delete()
                recorder = null
                _callSession.value = session.copy(isRecording = false)
            } catch (e: Throwable) { }
        } else {
            try {
                val fileName = "rec_${System.currentTimeMillis()}.wav"
                // Assuming we have access to context for cache dir or using external
                // For now, let's use a placeholder path or assume caller provides it.
                // But SipEngine is an object, it doesn't have context unless we pass it.
                // We'll use a fixed path for now in app's private files if we had context.
                // Let's assume startRecording(path) is better.
            } catch (e: Throwable) { }
        }
    }

    fun startRecording(filePath: String) {
        endpoint?.libRegisterThread("SipEngineThread")
        try {
            recorder?.delete()
            recorder = AudioMediaRecorder()
            recorder?.createRecorder(filePath)
            
            _callSession.value?.let { session ->
                callMap[session.callId]?.let { call ->
                    val ci = call.info
                    for (i in 0 until ci.media.size) {
                        val mi = ci.media.get(i)
                        if (mi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO && 
                            mi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) {
                            val aud = AudioMedia.typecastFromMedia(call.getMedia(mi.index))
                            // Record both sides
                            aud.startTransmit(recorder)
                            endpoint?.audDevManager()?.captureDevMedia?.startTransmit(recorder)
                        }
                    }
                }
                _callSession.value = session.copy(isRecording = true)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "startRecording failed: ${e.message}")
        }
    }

    fun stopRecording() {
        endpoint?.libRegisterThread("SipEngineThread")
        try {
            recorder?.delete()
            recorder = null
            _callSession.value = _callSession.value?.copy(isRecording = false)
        } catch (e: Throwable) { }
    }

    fun sendDtmf(digit: Char) {
        _callSession.value?.let { session ->
            callMap[session.callId]?.let { call ->
                try { call.dialDtmf(digit.toString()) } catch (e: Throwable) { }
            }
        }
    }

    fun holdCall(onHold: Boolean) {
        endpoint?.libRegisterThread("SipEngineThread")
        _callSession.value?.let { session ->
            callMap[session.callId]?.let { call ->
                try {
                    val prm = CallOpParam()
                    if (onHold) call.setHold(prm) else call.reinvite(prm)
                    _callSession.value = session.copy(isOnHold = onHold)
                } catch (e: Throwable) { }
            }
        }
    }

    private fun configureCodecs(preferred: PreferredCodec, ecEnabled: Boolean, nsEnabled: Boolean, agcEnabled: Boolean) {
        try {
            val ep = endpoint ?: return
            PreferredCodec.values().forEach { codec ->
                val prio = if (codec == preferred) codec.priority else (codec.priority - 100).coerceAtLeast(0)
                ep.codecSetPriority(codec.codecName, prio.toShort())
            }
        } catch (e: Throwable) { }
    }

    fun destroy() {
        try {
            callMap.values.forEach { it.delete() }
            callMap.clear()
            accountMap.values.forEach { it.delete() }
            accountMap.clear()
            endpoint?.libDestroy()
            endpoint = null
        } catch (e: Throwable) { }
    }

    class PjAccount(private val accountId: String) : Account() {
        override fun onRegState(prm: OnRegStateParam) {
            try {
                val ai = info
                val status = if (ai.regIsActive) RegStatus.REGISTERED else RegStatus.UNREGISTERED
                Log.i(TAG, "onRegState: $accountId -> $status")
                _registrationEvents.value = Pair(accountId, status)
            } catch (e: Throwable) {
                Log.e(TAG, "onRegState error: ${e.message}")
            }
        }

        override fun onIncomingCall(prm: OnIncomingCallParam) {
            try {
                val call = PjCall(this, prm.callId)
                callMap[prm.callId] = call
                
                // Send 180 Ringing immediately
                val opPrm = CallOpParam().apply { 
                    statusCode = pjsip_status_code.PJSIP_SC_RINGING 
                }
                call.answer(opPrm)

                val ci = call.info
                val session = CallSession(
                    callId = prm.callId,
                    accountId = accountId,
                    remoteUri = ci.remoteUri ?: "",
                    remoteDisplayName = ci.remoteContact ?: ci.remoteUri ?: "",
                    direction = CallDirection.INCOMING,
                    state = CallState.INCOMING
                )
                _callSession.value = session
                onIncomingCall?.invoke(session)
            } catch (e: Throwable) {
                Log.e(TAG, "Error in onIncomingCall: ${e.message}")
            }
        }
    }

    class PjCall(acct: Account, id: Int = -1) : Call(acct, id) {
        override fun onCallState(prm: OnCallStateParam) {
            try {
                val ci = info
                Log.i(TAG, "onCallState changed: ${ci.state} (${ci.stateText})")
                val newState = when (ci.state) {
                    pjsip_inv_state.PJSIP_INV_STATE_CALLING -> CallState.CALLING
                    pjsip_inv_state.PJSIP_INV_STATE_INCOMING -> CallState.INCOMING
                    pjsip_inv_state.PJSIP_INV_STATE_EARLY -> CallState.EARLY
                    pjsip_inv_state.PJSIP_INV_STATE_CONNECTING -> CallState.CONNECTING
                    pjsip_inv_state.PJSIP_INV_STATE_CONFIRMED -> CallState.CONFIRMED
                    pjsip_inv_state.PJSIP_INV_STATE_DISCONNECTED -> CallState.DISCONNECTED
                    else -> CallState.IDLE
                }
                
                // If call failed (not confirmed and moved to disconnected), 
                // we should ensure it's removed from map.
                if (newState == CallState.DISCONNECTED) {
                    Log.i(TAG, "Call disconnected. ID: ${this.id}")
                    callMap.remove(this.id)
                    _callSession.value = null
                } else {
                    _callSession.value = _callSession.value?.copy(state = newState)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "onCallState error: ${e.message}")
            }
        }

        override fun onCallMediaState(prm: OnCallMediaStateParam) {
            try {
                val ci = info
                for (i in 0 until ci.media.size) {
                    val mi = ci.media.get(i)
                    if (mi.type == pjmedia_type.PJMEDIA_TYPE_AUDIO &&
                        mi.status == pjsua_call_media_status.PJSUA_CALL_MEDIA_ACTIVE) {
                        val aud = AudioMedia.typecastFromMedia(getMedia(i.toLong()))
                        endpoint?.audDevManager()?.captureDevMedia?.startTransmit(aud)
                        aud.startTransmit(endpoint?.audDevManager()?.playbackDevMedia)
                    }
                }
            } catch (e: Throwable) { }
        }
    }
}
