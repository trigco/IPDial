package com.ipdial.data.model

import java.util.UUID

enum class RegStatus {
    REGISTERING, REGISTERED, UNREGISTERED, ERROR
}

enum class Transport {
    UDP, TCP, TLS
}

enum class PreferredCodec {
    AUTO, G729, OPUS, G722, G711A, G711U
}

enum class CallDirection {
    INCOMING, OUTGOING
}

enum class CallState {
    IDLE, CALLING, INCOMING, EARLY, CONNECTING, CONFIRMED, DISCONNECTED
}

enum class ThemeMode {
    System, Light, Dark, Obsidian, Quartz
}

enum class KeypadDesign {
    Grid, Rounded
}

enum class AudioDeviceMode {
    EARPIECE, SPEAKER, BLUETOOTH
}

data class SipAccount(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val displayName: String = "", // Fixed: Restored for AccountsScreen & CallScreen
    val username: String = "",
    val password: String = "",
    val domain: String = "",
    val proxy: String = "",
    val port: Int? = null,
    val transport: Transport = Transport.UDP,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val regStatus: RegStatus = RegStatus.UNREGISTERED,
    val regStatusText: String = "", // Fixed: Restored for AccountRepository
    val codec: PreferredCodec = PreferredCodec.AUTO,
    val ecEnabled: Boolean = true,
    val nsEnabled: Boolean = true,
    val agcEnabled: Boolean = true
)

data class CallSession(
    val callId: Int,
    val accountId: String,
    val remoteUri: String,
    val remoteDisplayName: String = "",
    val direction: CallDirection,
    val state: CallState,
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isOnHold: Boolean = false,
    val isRecording: Boolean = false,
    val rxVolume: Float = 1.0f
)

data class CallLogEntry(
    val id: Int = 0,
    val accountId: String,
    val remoteUri: String,
    val remoteDisplayName: String,
    val direction: CallDirection,
    val timestampMs: Long,
    val durationSeconds: Long,
    val missed: Boolean
)
