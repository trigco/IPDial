package com.ipdial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipdial.data.model.CallSession
import com.ipdial.ui.SipViewModel
import com.ipdial.ui.theme.EndRed
import com.ipdial.ui.theme.ForestGreen

@Composable
fun IncomingCallScreen(vm: SipViewModel, session: CallSession) {
    val accounts by vm.accounts.collectAsState()
    val contacts by vm.contacts.collectAsState()
    
    val account = accounts.firstOrNull { it.id == session.accountId }
    // "GP 92" style label from account label or domain
    val viaLine  = account?.label?.ifBlank { account.domain } ?: "SIP"
    // Display name / username of the registered account
    val callVia  = account?.let {
        it.label.ifBlank { it.username }
    } ?: ""

    val contact = remember(session.remoteUri, contacts) {
        contacts.find { c -> c.numbers.any { n -> session.remoteUri.contains(n.filter { it.isDigit() }) } }
    }
    val displayName = contact?.name ?: session.remoteDisplayName.ifBlank { cleanUri(session.remoteUri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))

        // "Incoming Call via GP 92 > Call via <username>"
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Incoming Call via $viaLine",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (callVia.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Call via ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = callVia,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Caller number / name — large, matches screenshot
        Text(
            text = displayName,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 34.sp
            ),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        if (displayName != cleanUri(session.remoteUri)) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = cleanUri(session.remoteUri),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(32.dp))
        
        // Avatar circle
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
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
                    text = (displayName.firstOrNull() ?: '?').uppercaseCharCompat(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Message button (middle)
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .clip(RoundedCornerShape(50))
                .clickable { /* TODO: send quick message */ }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = "Message",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Decline | [Call icon] | Answer  — matches screenshot layout
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 0.dp)
                .padding(bottom = 56.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline
                TextButton(onClick = { vm.hangup() }) {
                    Text(
                        "Decline",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Central call icon button (white circle)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { vm.answerCall() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Answer",
                        tint = ForestGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Answer
                TextButton(onClick = { vm.answerCall() }) {
                    Text(
                        "Answer",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
