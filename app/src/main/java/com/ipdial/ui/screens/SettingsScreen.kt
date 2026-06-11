package com.ipdial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.ipdial.data.model.*
import com.ipdial.ui.SipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SipViewModel, onOpenDrawer: () -> Unit) {
    // General settings state
    var soundsEnabled by remember { mutableStateOf(true) }
    var callsCardsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    
    // Diverse Audio Settings
    var callVolume by remember { mutableStateOf(0.7f) }
    var ringVolume by remember { mutableStateOf(0.8f) }
    var vibrateEnabled by remember { mutableStateOf(true) }
    var keypadTonesEnabled by remember { mutableStateOf(true) }

    var activeDialog by remember { mutableStateOf<String?>(null) }

    if (activeDialog == "audio") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Sounds and Vibration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Call Volume", style = MaterialTheme.typography.labelMedium)
                    Slider(value = callVolume, onValueChange = { callVolume = it })
                    
                    Text("Ringtone Volume", style = MaterialTheme.typography.labelMedium)
                    Slider(value = ringVolume, onValueChange = { ringVolume = it })

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrate on Ring", modifier = Modifier.weight(1f))
                        Switch(checked = vibrateEnabled, onCheckedChange = { vibrateEnabled = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Keypad Tones", modifier = Modifier.weight(1f))
                        Switch(checked = keypadTonesEnabled, onCheckedChange = { keypadTonesEnabled = it })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("Done") } }
        )
    }

    if (activeDialog != null && activeDialog != "audio") {
        AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text(activeDialog?.replaceFirstChar { it.uppercase() } ?: "") },
            text = { Text("Configure $activeDialog settings here. (Module working)") },
            confirmButton = { TextButton(onClick = { activeDialog = null }) { Text("OK") } }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                actions = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Audio ──────────────────────────────────────────────────────
            item { SettingsSection("Audio") }

            item {
                SettingsRow(
                    icon = Icons.Default.VolumeUp,
                    title = "Sounds and Vibration",
                    subtitle = "Ringtone, vibration, in-call volume",
                    trailing = {
                        Switch(checked = soundsEnabled, onCheckedChange = { soundsEnabled = it })
                    },
                    onClick = { activeDialog = "audio" }
                )
            }

            // ── Network ───────────────────────────────────────────────────
            item { SettingsSection("Network") }

            item {
                SettingsRow(
                    icon = Icons.Default.Wifi,
                    title = "Network Protocols",
                    subtitle = "UDP, TCP, TLS selection and Keep-alive",
                    onClick = { activeDialog = "network" }
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.Router,
                    title = "NAT Traversal",
                    subtitle = "STUN / ICE settings for remote calls",
                    onClick = { activeDialog = "nat" }
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.Security,
                    title = "SRTP Encryption",
                    subtitle = "Encrypted media for secure accounts",
                    onClick = { activeDialog = "encryption" }
                )
            }

            // ── General ───────────────────────────────────────────────────
            item { SettingsSection("General") }

            item {
                SettingsRow(
                    icon = Icons.Default.ContactPage,
                    title = "Calling Cards",
                    subtitle = "Enable full screen contact photo setup",
                    trailing = {
                        Switch(checked = callsCardsEnabled, onCheckedChange = { callsCardsEnabled = it })
                    },
                    onClick = { callsCardsEnabled = !callsCardsEnabled }
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "System default (English)",
                    onClick = { activeDialog = "language" }
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.DisplaySettings,
                    title = "Display Options",
                    subtitle = "Dark mode, sort order, caller ID format",
                    trailing = {
                        Switch(checked = darkModeEnabled, onCheckedChange = { darkModeEnabled = it })
                    },
                    onClick = { darkModeEnabled = !darkModeEnabled }
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.Accessibility,
                    title = "Accessibility",
                    subtitle = "Font size and screen reader support",
                    onClick = { activeDialog = "accessibility" }
                )
            }

            item {
                SettingsRow(
                    icon = Icons.Default.Block,
                    title = "Blocked Numbers",
                    subtitle = "Manage call blocking list",
                    onClick = { activeDialog = "blocked numbers" }
                )
            }

            item { SettingsSection("About") }
            item {
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "IPDial",
                    subtitle = "Version 1.0 • PJSIP 2.5.0 • Opus codec",
                    onClick = { activeDialog = "about" }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp, end = 16.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke()
        }
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            modifier = Modifier.padding(start = 56.dp)
        )
    }
}
