package com.ipdial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipdial.ui.IPDialTopBar
import com.ipdial.ui.SipViewModel
import com.ipdial.ui.theme.glass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(vm: SipViewModel, onOpenDrawer: () -> Unit) {
    val accounts by vm.accounts.collectAsState()

    Scaffold(
        topBar = {
            IPDialTopBar(accounts = accounts, vm = vm, onOpenDrawer = onOpenDrawer)
        },
        bottomBar = {}
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Privacy Policy",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            PrivacySection(
                title = "Information Collection",
                content = "IPDial is a VoIP application. We collect minimal information required to provide our services, such as your Device ID for account synchronization. Your SIP account credentials are stored locally and securely on your device; they are never uploaded to our servers or stored in Firestore."
            )
            
            PrivacySection(
                title = "Permissions",
                content = "The app requires Microphone access for calls, Contacts access to display your phonebook, and Phone state access to manage calls effectively."
            )
            
            PrivacySection(
                title = "Data Security",
                content = "Your SIP credentials and call logs are treated with high priority for security. When using Firestore synchronization, only your pro points and expiration data are stored securely using Firebase's infrastructure. Your SIP credentials remain exclusively on your device."
            )
            
            Spacer(Modifier.height(32.dp))
            
            Text(
                text = "Last updated June, 2026",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    val isGlass = com.ipdial.ui.theme.LocalGlassMode.current != com.ipdial.ui.theme.GlassMode.None
    
    Surface(
        color = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .then(if (isGlass) Modifier.glass(RoundedCornerShape(16.dp)) else Modifier)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
