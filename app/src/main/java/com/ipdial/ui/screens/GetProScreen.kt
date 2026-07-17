package com.ipdial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipdial.ui.IPDialTopBar
import com.ipdial.ui.SipViewModel
import com.ipdial.ui.theme.glass
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetProScreen(vm: SipViewModel, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val accounts by vm.accounts.collectAsState()
    val isPro by vm.isPro.collectAsState()

    Scaffold(
        topBar = {
            IPDialTopBar(accounts = accounts, vm = vm, onOpenDrawer = onOpenDrawer)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProStatusCard(isPro)
            }

            item {
                ProFeaturesList()
            }
            
            item {
                ReferralCard(vm = vm)
            }
        }
    }
}

@Composable
fun ReferralCard(vm: com.ipdial.ui.SipViewModel) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    val fullDeviceId by vm.deviceId.collectAsState()
    val referralCode = remember(fullDeviceId) { fullDeviceId.take(6) }
    val isGlass = com.ipdial.ui.theme.LocalGlassMode.current != com.ipdial.ui.theme.GlassMode.None

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isGlass) Modifier.glass() else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Referral Program", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Share IPDial with your friends and help them enjoy ad-free calling.")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Join IPDial")
                            putExtra(android.content.Intent.EXTRA_TEXT, "Use IPDial for best VoIP calls. Download here: https://github.com/trigco/IPDial")
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share IPDial"))
                    },
                    modifier = Modifier.weight(1f).then(if (isGlass) Modifier.glass(ButtonDefaults.shape) else Modifier),
                    colors = if (isGlass) ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White) else ButtonDefaults.buttonColors()
                ) {
                    Text("Share App")
                }
            }
        }
    }
}

@Composable
fun ProStatusCard(isPro: Boolean) {
    val proAccent = Color(0xFFFFC107) // Shiny Gold Color
    val isGlass = com.ipdial.ui.theme.LocalGlassMode.current != com.ipdial.ui.theme.GlassMode.None

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isGlass) Modifier.glass() else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) proAccent.copy(alpha = 0.15f) else (if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant)
        ),
        border = if (isPro) androidx.compose.foundation.BorderStroke(1.dp, proAccent.copy(alpha = 0.5f)) else (if (isGlass) null else null)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPro) Icons.Default.CheckCircle else Icons.Default.CardGiftcard,
                    contentDescription = null,
                    tint = if (isPro) proAccent else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isPro) "IPDial Pro Active" else "Free Version",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPro) proAccent else Color.Unspecified
                    )
                    if (isPro) {
                        Text(
                            text = "Lifetime Access Unlocked",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = proAccent.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProFeaturesList() {
    val features = listOf(
        "No Ads" to "Clean, ad-free calling.",
        "Multiple Accounts" to "Add unlimited SIP accounts.",
        "Unlimited Recordings" to "Record and share freely.",
        "Full Customization" to "Custom icons and keypad."
    )
    
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Text(
            text = "Pro Benefits", 
            style = MaterialTheme.typography.titleMedium, 
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        features.forEach { (title, desc) ->
            Row(
                verticalAlignment = Alignment.Top, 
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, 
                    contentDescription = null, 
                    tint = Color(0xFFFFC107), // Golden Tick
                    modifier = Modifier.size(24.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
