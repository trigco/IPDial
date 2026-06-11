package com.ipdial.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipdial.data.model.*
import androidx.compose.material.icons.filled.Menu
import com.ipdial.ui.SipViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─── Dot colours ───────────────────────────────────────────────────────────────
private val DotGreen  = Color(0xFF4CAF50)
private val DotRed    = Color(0xFFF44336)
private val DotAmber  = Color(0xFFFF9800)
private val DotGrey   = Color(0xFF9E9E9E)

@Composable
fun HomeScreen(
    vm: SipViewModel, 
    onOpenDrawer: () -> Unit
) {
    val accounts  by vm.accounts.collectAsState()
    val callLog   by vm.callLog.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val contactsState by vm.contacts.collectAsState()
    val favourites = remember(contactsState) { contactsState.filter { it.isFavorite } }
    
    // Create a lookup map for faster contact matching in call log
    val contactLookup = remember(contactsState) {
        val map = mutableMapOf<String, Contact>()
        contactsState.forEach { contact ->
            contact.numbers.forEach { number ->
                val clean = number.filter { it.isDigit() }
                if (clean.isNotEmpty()) map[clean] = contact
            }
        }
        map
    }

    // Group call log by date
    val grouped = remember(callLog, searchQuery) {
        val filtered = if (searchQuery.isBlank()) callLog 
                      else callLog.filter { it.remoteDisplayName.contains(searchQuery, ignoreCase = true) || it.remoteUri.contains(searchQuery) }
        
        filtered.groupBy { entry ->
            val cal = Calendar.getInstance().apply { timeInMillis = entry.timestampMs }
            val now = Calendar.getInstance()
            
            when {
                isSameDay(cal, now) -> "Today"
                isSameDay(cal, now.apply { add(Calendar.DATE, -1) }) -> "Yesterday"
                else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(entry.timestampMs))
            }
        }.toList().sortedByDescending { (label, entries) ->
            entries.firstOrNull()?.timestampMs ?: 0L
        }
    }

    var showEditFavorites by remember { mutableStateOf(false) }

    if (showEditFavorites) {
        FavoritesEditDialog(
            contacts = contactsState,
            onDismiss = { showEditFavorites = false },
            onToggleFavorite = { vm.toggleContactFavorite(it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Search bar (with registration-dot instead of mic) ───────────────
        SearchBarRow(
            query = searchQuery,
            onQueryChange = { vm.onSearchQueryChanged(it) },
            accounts = accounts,
            onOpenDrawer = onOpenDrawer
        )

        // ── Filter chips (Full Width) ───────────────────────────────────────
        FilterChipRow(modifier = Modifier.fillMaxWidth())

        // ── Scrollable body ─────────────────────────────────────────────────
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Favourites horizontal strip ──────────────────────────────────
            if (favourites.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Favourites",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showEditFavorites = true }) {
                            Text("Edit", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                item {
                    FavouritesRow(contacts = favourites, vm = vm)
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Call log groups ──────────────────────────────────────────────
            if (grouped.isEmpty() && favourites.isEmpty() && searchQuery.isBlank()) {
                item { EmptyLogPrompt() }
            } else {
                grouped.forEach { (label, entries) ->
                    item {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                        )
                    }
                        items(entries, key = { it.id }) { entry ->
                            val cleanUri = entry.remoteUri.filter { it.isDigit() }
                            val contact = contactLookup[cleanUri] ?: contactLookup.entries.find { cleanUri.contains(it.key) || it.key.contains(cleanUri) }?.value

                            CallLogRow(
                                entry   = entry,
                                account = accounts.firstOrNull { it.id == entry.accountId },
                                contact = contact,
                                onCall  = { vm.callBack(entry) }
                            )
                        }
                }
            }

            item { Spacer(Modifier.height(80.dp)) } // FAB clearance
        }
    }
}

@Composable
fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    accounts: List<SipAccount>,
    onOpenDrawer: () -> Unit
) {
    val regDotColor = when {
        accounts.any { it.regStatus == RegStatus.REGISTERED }    -> DotGreen
        accounts.any { it.regStatus == RegStatus.REGISTERING }   -> DotAmber
        accounts.any { it.regStatus == RegStatus.ERROR }         -> DotRed
        else                                                      -> DotGrey
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Reg dot moved to left
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(regDotColor.copy(alpha = 0.18f))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(regDotColor)
                )
            }

            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search contacts") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true
            )

            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Hamburger on right
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipRow(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(0) }
    val chips = listOf("All", "Missed", "Contacts")
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEachIndexed { index, label ->
            FilterChip(
                selected = selected == index,
                onClick  = { selected = index },
                label    = { Text(label, style = MaterialTheme.typography.labelLarge) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun FavouritesRow(contacts: List<Contact>, vm: SipViewModel) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        contacts.forEach { contact ->
            FavouriteAvatar(contact = contact, onClick = { 
                contact.numbers.firstOrNull()?.let { num ->
                    vm.makeCall(num)
                }
            })
        }
    }
}

@Composable
fun FavouriteAvatar(contact: Contact, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                if (contact.photoUri != null) {
                    AsyncImage(
                        model = contact.photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = contact.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = contact.name,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CallLogRow(
    entry: CallLogEntry,
    account: SipAccount?,
    contact: Contact?,
    onCall: () -> Unit
) {
    val viaLabel  = account?.label?.ifBlank { account.domain } ?: "SIP"
    val callerName = contact?.name ?: entry.remoteDisplayName.ifBlank { cleanUri(entry.remoteUri) }
    val timeStr   = formatTime(entry.timestampMs)

    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCall() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
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
                        text = (callerName.firstOrNull() ?: '?').uppercaseCharCompat(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = callerName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (entry.missed)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = when {
                            entry.missed                           -> Icons.Default.CallMissed
                            entry.direction == CallDirection.INCOMING -> Icons.Default.CallReceived
                            else                                   -> Icons.Default.CallMade
                        },
                        contentDescription = null,
                        tint = when {
                            entry.missed -> MaterialTheme.colorScheme.error
                            else         -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$viaLabel • $timeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onCall) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyLogPrompt() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No recent calls",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Add a SIP account in Settings to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun FavoritesEditDialog(
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onToggleFavorite: (Contact) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredContacts = remember(contacts, searchQuery) {
        contacts
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(compareByDescending<Contact> { it.isFavorite }.thenBy { it.name })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Favourites") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn {
                        items(filteredContacts, key = { it.id }) { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleFavorite(contact) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = contact.isFavorite,
                                    onCheckedChange = { onToggleFavorite(contact) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(contact.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun RegStatusDot(status: RegStatus) {
    val color = when (status) {
        RegStatus.REGISTERED  -> DotGreen
        RegStatus.REGISTERING -> DotAmber
        RegStatus.ERROR       -> DotRed
        else                  -> DotGrey
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun startOfDay(offsetDays: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DATE, offsetDays)
    }.timeInMillis
}

private fun formatTime(ms: Long): String {
    val now = System.currentTimeMillis()
    val diffMin = (now - ms) / 60_000
    return when {
        diffMin < 1   -> "Just now"
        diffMin < 60  -> "${diffMin} min ago"
        else          -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    }
}
