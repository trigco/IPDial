package com.ipdial.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipdial.data.model.RegStatus
import com.ipdial.data.model.SipAccount
import com.ipdial.ui.screens.clickableWithRipple

val DotGreen  = Color(0xFF2DA44E) // GitHub Success Green
val DotRed    = Color(0xFFCF222E)
val DotAmber  = Color(0xFFBF8700)
val DotGrey   = Color(0xFF57606A)

val ColorPro = Color(0xFFE3B341) // Clean Material Gold

@Composable
fun StartIoBanner(modifier: Modifier = Modifier, vm: SipViewModel? = null) {}

@Composable
fun RegStatusIndicator(
    accounts: List<SipAccount>, 
    vm: SipViewModel? = null,
    showAccountInfo: SipAccount? = null
) {
    val vmActiveAccount by (vm?.activeAccount?.collectAsState() ?: remember { mutableStateOf(null) })
    val activeAccount = showAccountInfo ?: vmActiveAccount ?: accounts.firstOrNull { it.isEnabled } ?: accounts.firstOrNull()

    val regDotColor = when {
        activeAccount != null -> when (activeAccount.regStatus) {
            RegStatus.REGISTERED  -> DotGreen
            RegStatus.REGISTERING -> DotAmber
            RegStatus.ERROR       -> DotRed
            else                  -> DotGrey
        }
        accounts.any { it.regStatus == RegStatus.REGISTERED }    -> DotGreen
        accounts.any { it.regStatus == RegStatus.REGISTERING }   -> DotAmber
        accounts.any { it.regStatus == RegStatus.ERROR }         -> DotRed
        else                                                      -> DotGrey
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(regDotColor)
        )
        if (activeAccount != null) {
            Spacer(Modifier.width(8.dp)) 
            Text(
                text = activeAccount.displayName,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), 
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPDialTopBar(
    accounts: List<SipAccount>,
    vm: SipViewModel? = null,
    onOpenDrawer: () -> Unit,
    onAddAccount: (() -> Unit)? = null
) {
    val isPro = vm?.isPro?.collectAsState()?.value ?: false
    val themeColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Left: Status Dot & Name
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                RegStatusIndicator(accounts = accounts, vm = vm)
            }

            // Center: Static Balance (Always Visible)
            val vmActiveAccount by (vm?.activeAccount?.collectAsState() ?: remember { mutableStateOf(null) })
            val activeAccount = vmActiveAccount ?: accounts.firstOrNull { it.isEnabled } ?: accounts.firstOrNull()
            
            if (activeAccount != null && (activeAccount.domain == "sip.amarip.net" || activeAccount.domain == "103.170.231.10" || activeAccount.domain == "103.129.202.202") && vm != null) {
                val balanceMap by vm.balances.collectAsState()
                val balance = balanceMap[activeAccount.id]
                
                LaunchedEffect(activeAccount.id) {
                    vm.fetchBalance(activeAccount, context)
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clickable { vm.fetchBalance(activeAccount, context) }
                ) {
                    val cleanBalance = (balance ?: "Loading...").replace("BDT", "").trim()
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cleanBalance,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (balance != null) {
                            Spacer(Modifier.width(2.dp))
                            Text("৳", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                // Pro Badge
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = if (isPro) "PRO" else "IPDial",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPro) ColorPro else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Right side items: Hamburger Menu
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = onOpenDrawer, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NumberPickerDialog(numbers: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select Number") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                numbers.forEach { number ->
                    TextButton(
                        onClick = { onPick(number); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = number, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AccountSelectionDialog(enabledAccounts: List<SipAccount>, onAccountSelected: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                enabledAccounts.forEach { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableWithRipple { onAccountSelected(account.id) }
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(text = account.label.ifBlank { account.domain }, style = MaterialTheme.typography.bodyLarge)
                            if (account.username.isNotBlank()) {
                                Text(text = account.username, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
