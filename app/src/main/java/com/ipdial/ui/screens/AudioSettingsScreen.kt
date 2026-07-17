package com.ipdial.ui.screens

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipdial.data.model.PreferredCodec
import com.ipdial.ui.IPDialTopBar
import com.ipdial.ui.SipViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(vm: SipViewModel, onOpenDrawer: () -> Unit) {
    val accounts by vm.accounts.collectAsState()
    val activeAccount by vm.activeAccount.collectAsState()
    val context = LocalContext.current

    // Voice Demo Setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        tts = textToSpeech
        onDispose { textToSpeech.shutdown() }
    }

    Scaffold(
        topBar = {
            IPDialTopBar(accounts = accounts, vm = vm, onOpenDrawer = onOpenDrawer)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Audio & Codec Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (activeAccount == null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Please add and enable a SIP Account first.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                val acc = activeAccount!!

                // VOICE DEMO CARD
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable {
                        val effects = mutableListOf<String>()
                        if (acc.ecEnabled) effects.add("Echo Cancellation")
                        if (acc.nsEnabled) effects.add("Noise Suppression")
                        if (acc.agcEnabled) effects.add("Auto Gain Control")
                        
                        val effectStr = if (effects.isEmpty()) "No hardware effects are active." else "Active effects are: ${effects.joinToString(", ")}."
                        
                        Toast.makeText(context, "Playing Audio Demo...", Toast.LENGTH_SHORT).show()
                        tts?.speak("This is an audio demo. $effectStr Your codec is set to ${acc.codec.name}.", TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Play Audio Demo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Listen to how your voice and effects will sound.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }

                // 1. Codec Selection (With AUTO)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HighQuality, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Voice Codec", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Auto mode uses G729 by default for best BD networks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        PreferredCodec.entries.forEach { codec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.saveAccount(acc.copy(codec = codec)) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = acc.codec == codec,
                                    onClick = { vm.saveAccount(acc.copy(codec = codec)) }
                                )
                                Spacer(Modifier.width(8.dp))
                                val label = when(codec) {
                                    PreferredCodec.AUTO -> "Auto (Best Match & G729)"
                                    PreferredCodec.OPUS -> "OPUS (HD)"
                                    PreferredCodec.G722 -> "G722 (HD)"
                                    PreferredCodec.G729 -> "G729 (Standard)"
                                    else -> codec.name
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (codec == PreferredCodec.AUTO) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // 2. Echo Cancellation
                AudioFeatureCard(
                    title = "Acoustic Echo Cancellation",
                    description = "Eliminates voice echoing and repeating during loudspeaker or normal calls.",
                    icon = Icons.Default.Hearing,
                    checked = acc.ecEnabled,
                    onCheckedChange = { vm.saveAccount(acc.copy(ecEnabled = it)) }
                )

                // 3. Noise Suppression
                AudioFeatureCard(
                    title = "AI Noise Suppression (NS)",
                    description = "Filters out background noises like wind, traffic, or fan sounds.",
                    icon = Icons.Default.VolumeOff,
                    checked = acc.nsEnabled,
                    onCheckedChange = { vm.saveAccount(acc.copy(nsEnabled = it)) }
                )

                // 4. Automatic Gain Control
                AudioFeatureCard(
                    title = "Auto Gain Control (AGC)",
                    description = "Automatically boosts your voice volume if you speak too quietly.",
                    icon = Icons.Default.GraphicEq,
                    checked = acc.agcEnabled,
                    onCheckedChange = { vm.saveAccount(acc.copy(agcEnabled = it)) }
                )
                
                Text(
                    text = "* Note: Changes apply to your next call.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AudioFeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
