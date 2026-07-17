package com.ipdial.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipdial.data.model.AudioDeviceMode
import com.ipdial.data.model.CallSession
import com.ipdial.data.model.CallState
import com.ipdial.ui.SipViewModel
import kotlinx.coroutines.delay

@Composable
fun CallScreen(vm: SipViewModel, session: CallSession) {
    val accounts by vm.accounts.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val audioDeviceMode by vm.audioDeviceMode.collectAsState()

    val account = accounts.firstOrNull { it.id == session.accountId }
    val simLabel = account?.displayName ?: ""

    val contact = remember(session.remoteUri, contacts) {
        val cleanedSessionUriDigits = vm.cleanUri(session.remoteUri).filter { it.isDigit() }
        if (cleanedSessionUriDigits.length < 10) { 
            null
        } else {
            contacts.find { c ->
                c.numbers.any { n ->
                    val cleanedContactNumberDigits = n.filter { it.isDigit() }
                    cleanedContactNumberDigits.length >= 10 && 
                    (cleanedSessionUriDigits.contains(cleanedContactNumberDigits) || cleanedContactNumberDigits.contains(cleanedSessionUriDigits))
                }
            }
        }
    }
    val displayName = contact?.name ?: vm.cleanDisplayName(session.remoteDisplayName, session.remoteUri)

    var showDialpad by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(session.state) {
        if (session.state == CallState.CONFIRMED) {
            vm.updateBluetoothAvailability()
        }
    }

    LaunchedEffect(session) {
        if (session.state == CallState.CONFIRMED) {
            while (session.state == CallState.CONFIRMED) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))

            // Subtitle (Account Name)
            Text(
                text = simLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            // Contact Name
            Text(
                text = displayName,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
                maxLines = 1
            )

            // Phone Number (If contact is saved)
            if (displayName != vm.cleanUri(session.remoteUri)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = vm.cleanUri(session.remoteUri),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Call Status / Timer
            if (session.state == CallState.CONFIRMED) {
                Text(
                    text = formatDuration(elapsedSeconds),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Text(
                    text = when (session.state) {
                        CallState.CALLING -> "Calling…"
                        CallState.INCOMING -> "Incoming"
                        CallState.EARLY -> "Ringing…"
                        CallState.CONNECTING -> "Connecting…"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Big Clean Avatar (No Pulse, No Glitch)
            Spacer(Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (contact?.photoUri != null) {
                    AsyncImage(
                        model = contact.photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Controls
            AnimatedContent(targetState = showDialpad, label = "dialpad_toggle") { showDp ->
                if (showDp) {
                    InCallDialpad(vm = vm) {
                        showDialpad = false
                    }
                } else {
                    CallControls(
                        session = session,
                        isActive = session.state == CallState.CONFIRMED,
                        onKeypad = { showDialpad = true },
                        onMute = { vm.toggleMute() },
                        onSpeaker = { vm.cycleAudioDevice() },
                        onRecord = { vm.toggleRecording() },
                        audioDeviceMode = audioDeviceMode
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Standard Google End Call Button
            FloatingActionButton(
                onClick = { vm.hangup() },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 64.dp)
                    .size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun CallControls(
    session: CallSession,
    isActive: Boolean,
    onKeypad: () -> Unit,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onRecord: () -> Unit,
    audioDeviceMode: AudioDeviceMode = AudioDeviceMode.EARPIECE,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CallControlButton(
            icon = Icons.Default.Dialpad,
            label = "Keypad",
            onClick = onKeypad
        )
        CallControlButton(
            icon = if (session.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = "Mute",
            active = session.isMuted,
            enabled = isActive,
            onClick = onMute
        )

        val audioIcon = when (audioDeviceMode) {
            AudioDeviceMode.SPEAKER -> Icons.AutoMirrored.Filled.VolumeUp
            AudioDeviceMode.BLUETOOTH -> Icons.Default.Bluetooth
            else -> Icons.Default.PhoneInTalk
        }
        val audioLabel = when (audioDeviceMode) {
            AudioDeviceMode.SPEAKER -> "Speaker"
            AudioDeviceMode.BLUETOOTH -> "Bluetooth"
            else -> "Earpiece"
        }

        CallControlButton(
            icon = audioIcon,
            label = audioLabel,
            active = audioDeviceMode != AudioDeviceMode.EARPIECE,
            enabled = true,
            onClick = onSpeaker
        )

        CallControlButton(
            icon = Icons.Default.RadioButtonChecked,
            label = if (session.isRecording) "Recording" else "Record",
            active = session.isRecording,
            enabled = isActive,
            onClick = onRecord
        )
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (active) MaterialTheme.colorScheme.onPrimary 
                           else if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun InCallDialpad(vm: SipViewModel, onHide: () -> Unit) {
    var dtmfString by remember { mutableStateOf("") }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = dtmfString,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp),
            textAlign = TextAlign.Center
        )

        TextButton(onClick = onHide) {
            Text("Hide keypad")
        }
        val keys = listOf(
            "1","2","3","4","5","6","7","8","9","*","0","#"
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            keys.chunked(3).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { digit ->
                        Surface(
                            onClick = { 
                                dtmfString += digit
                                vm.dialPad(digit[0]) 
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = digit,
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
