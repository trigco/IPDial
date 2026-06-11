package com.ipdial.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ipdial.MainActivity
import com.ipdial.R
import com.ipdial.data.model.CallDirection
import com.ipdial.data.model.CallState
import com.ipdial.data.model.RegStatus
import com.ipdial.data.repository.AccountRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class SipService : Service() {

    companion object {
        const val NOTIF_CHANNEL_SIP = "sip_service"
        const val NOTIF_CHANNEL_CALL = "incoming_call"
        const val NOTIF_ID_SERVICE = 1001
        const val NOTIF_ID_INCOMING = 1002

        const val ACTION_ANSWER = "com.ipdial.ANSWER"
        const val ACTION_DECLINE = "com.ipdial.DECLINE"
        const val ACTION_HANGUP = "com.ipdial.HANGUP"
        const val ACTION_START = "com.ipdial.START"
        const val ACTION_STOP = "com.ipdial.STOP"

        fun start(context: Context) {
            val intent = Intent(context, SipService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var audioManager: AudioManager
    private lateinit var repo: AccountRepository
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        repo = AccountRepository(applicationContext)
        createNotificationChannels()
        SipEngine.init(applicationContext)
        SipEngine.onIncomingCall = { session -> showIncomingCallNotification(session.remoteDisplayName, session.callId) }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID_SERVICE, buildServiceNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        } else {
            startForeground(NOTIF_ID_SERVICE, buildServiceNotification())
        }
        
        registerAccountsFromDataStore()
        observeCallState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ANSWER -> {
                val callId = intent.getIntExtra("callId", -1)
                SipEngine.answerCall(callId)
                routeAudioToEarpiece()
            }
            ACTION_DECLINE -> {
                val callId = intent.getIntExtra("callId", -1)
                SipEngine.hangupCall(callId)
                cancelIncomingNotification()
            }
            ACTION_HANGUP -> SipEngine.hangupCall()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun registerAccountsFromDataStore() {
        scope.launch {
            repo.accounts.collectLatest { accounts ->
                // Sync engine accounts
                accounts.filter { it.isEnabled }.forEach { acc ->
                    SipEngine.addAccount(acc)
                }
            }
        }
        // Observe registration events to update DataStore
        scope.launch {
            SipEngine.registrationEvents.collectLatest { event ->
                event?.let { (accountId, status) ->
                    repo.updateRegStatus(accountId, status)
                }
            }
        }
    }

    private var lastWasConfirmed = false
    private var callStartTime = 0L

    private var lastSession: com.ipdial.data.model.CallSession? = null

    private fun observeCallState() {
        scope.launch {
            SipEngine.callSession.collectLatest { session ->
                if (session == null) {
                    lastSession?.let {
                        val duration = if (callStartTime > 0) (System.currentTimeMillis() - callStartTime) / 1000 else 0L
                        val entry = com.ipdial.data.model.CallLogEntry(
                            accountId = it.accountId,
                            remoteUri = it.remoteUri,
                            remoteDisplayName = it.remoteDisplayName,
                            direction = it.direction,
                            timestampMs = System.currentTimeMillis(),
                            durationSeconds = duration,
                            missed = !lastWasConfirmed && it.direction == CallDirection.INCOMING
                        )
                        scope.launch {
                            com.ipdial.data.repository.CallLogRepository(applicationContext).insert(entry)
                        }
                    }
                    restoreAudio()
                    releaseWakeLock()
                    cancelIncomingNotification()
                    lastWasConfirmed = false
                    callStartTime = 0
                    lastSession = null
                } else {
                    lastSession = session
                    when (session.state) {
                        CallState.CONFIRMED -> {
                            cancelIncomingNotification()
                            routeAudioToEarpiece()
                            acquireWakeLock()
                            lastWasConfirmed = true
                            if (callStartTime == 0L) callStartTime = System.currentTimeMillis()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun routeAudioToEarpiece() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = false
    }

    fun routeAudioToSpeaker(on: Boolean) {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = on
    }

    private fun restoreAudio() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "IPDial:call"
        ).apply { acquire(60 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            nm.createNotificationChannel(
                NotificationChannel(NOTIF_CHANNEL_SIP, "SIP Service", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Background SIP registration"
                    setShowBadge(false)
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(NOTIF_CHANNEL_CALL, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Incoming VoIP call alerts"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(true)
                }
            )
        }
    }

    private fun buildServiceNotification(): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_SIP)
            .setContentTitle("IPDial")
            .setContentText("Ready to receive calls")
            .setSmallIcon(R.drawable.ic_notif_call)
            .setContentIntent(intent)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    private fun showIncomingCallNotification(callerName: String, callId: Int) {
        val fullscreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "com.ipdial.ACTION_INCOMING_CALL"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullscreenPi = PendingIntent.getActivity(this, 0, fullscreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val answerPi = PendingIntent.getService(this, 1,
            Intent(this, SipService::class.java).apply {
                action = ACTION_ANSWER
                putExtra("callId", callId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val declinePi = PendingIntent.getService(this, 2,
            Intent(this, SipService::class.java).apply {
                action = ACTION_DECLINE
                putExtra("callId", callId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(this, NOTIF_CHANNEL_CALL)
            .setContentTitle("Incoming Call")
            .setContentText(callerName)
            .setSmallIcon(R.drawable.ic_notif_call)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullscreenPi, true)
            .addAction(R.drawable.ic_call_answer, "Answer", answerPi)
            .addAction(R.drawable.ic_call_end, "Decline", declinePi)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_INCOMING, notif)
    }

    private fun cancelIncomingNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID_INCOMING)
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        SipEngine.destroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
