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
import androidx.compose.ui.unit.sp
import com.ipdial.ui.IPDialTopBar
import com.ipdial.ui.SipViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetProScreen(vm: SipViewModel, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val accounts by vm.accounts.collectAsState()
    val proPoints by vm.proPoints.collectAsState()
    val proExpiration by vm.proExpiration.collectAsState()
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProStatusCard(isPro, proExpiration)
            }

            item {
                PointsBalanceCard(proPoints) {
                    vm.watchRewardedAd(context) {
                        // Reward handled in VM
                    }
                }
            }

            item {
                Text(
                    "Redeem Points for Pro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                RedemptionOptions(proPoints) { days ->
                    vm.redeemPoints(days)
                }
            }
            
            item {
                ProFeaturesList()
            }
        }
    }
}

@Composable
fun ProStatusCard(isPro: Boolean, expiration: Long) {
    val remainingDays = if (isPro) {
        val diff = expiration - System.currentTimeMillis()
        maxOf(0, TimeUnit.MILLISECONDS.toDays(diff) + 1)
    } else 0

    val proAccent = Color(0xFFBC4749)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPro) proAccent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isPro) androidx.compose.foundation.BorderStroke(1.dp, proAccent.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isPro) proAccent else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
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
                            text = "$remainingDays Days Remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = proAccent.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PointsBalanceCard(points: Int, onWatchAd: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Available Points", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = points.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Button(
                onClick = onWatchAd,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Get 1 Point", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun RedemptionOptions(currentPoints: Int, onRedeem: (Int) -> Unit) {
    val tiers = listOf(
        Triple(1, 1, "1 Day"),
        Triple(7, 5, "7 Days"),
        Triple(30, 20, "1 Month"),
        Triple(90, 50, "3 Months")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiers.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (days, cost, label) ->
                    val canAfford = currentPoints >= cost
                    Surface(
                        onClick = { if (canAfford) onRedeem(days) },
                        enabled = canAfford,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(80.dp),
                        color = if (canAfford) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp) 
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp, 
                            color = if (canAfford) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("$cost Points", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            
                            // Reserve space for the icon so height remains consistent
                            if (canAfford) {
                                Spacer(Modifier.height(4.dp))
                                Icon(Icons.Default.CardGiftcard, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            } else {
                                Spacer(Modifier.height(20.dp))
                            }
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ProFeaturesList() {
    val features = listOf(
        "Ad-Free Experience" to "No more banner or interstitial ads",
        "Unlimited Accounts" to "Connect as many SIP accounts as you need",
        "HD Audio Codecs" to "Unlock Opus and G.722 for crystal clear calls",
        "Custom Keypad" to "Switch between different dialpad designs",
        "Premium Icons" to "Personalize your home screen with unique icons",
        "Advanced Branding" to "Enhanced UI with premium Pro styling"
    )
    
    Column(modifier = Modifier.padding(top = 8.dp)) {
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
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, 
                    contentDescription = null, 
                    tint = Color(0xFF4CAF50), 
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
