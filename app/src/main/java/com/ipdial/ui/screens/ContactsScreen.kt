package com.ipdial.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.unit.IntSize
import com.ipdial.data.model.Contact
import com.ipdial.ui.SipViewModel
import com.ipdial.ui.IPDialTopBar
import com.ipdial.ui.NumberPickerDialog

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(vm: SipViewModel, onOpenDrawer: () -> Unit) {
    val contacts by vm.contacts.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val context = LocalContext.current
    var activeContactForNumberPicker by remember { mutableStateOf<Contact?>(null) }

    val sortedContacts = remember(contacts) {
        contacts.sortedBy { it.name.trim().lowercase() }
    }
    
    val filteredContacts = remember(sortedContacts, searchQuery) {
        if (searchQuery.isBlank()) sortedContacts
        else sortedContacts.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.numbers.any { num -> num.contains(searchQuery) }
        }
    }

    val alphabet = remember { ('A'..'Z').toList() }
    val letterToFirstIndex = remember(filteredContacts) {
        val map = mutableMapOf<Char, Int>()
        filteredContacts.forEachIndexed { index, contact ->
            val firstChar = contact.name.trim().firstOrNull()?.uppercaseChar() ?: '#'
            val targetChar = if (firstChar in 'A'..'Z') firstChar else '#'
            if (!map.containsKey(targetChar)) {
                map[targetChar] = index
            }
        }
        map
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            IPDialTopBar(accounts = accounts, vm = vm, onOpenDrawer = onOpenDrawer)
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = CircleShape
            )
            
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactItem(
                            contact = contact,
                            onNumberClick = { num -> vm.makeCall(num) },
                            onContactClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id)
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                AlphabetIndexer(
                    alphabet = alphabet,
                    letterToFirstIndex = letterToFirstIndex,
                    onLetterSelected = { _, index ->
                        coroutineScope.launch {
                            listState.scrollToItem(index)
                        }
                    }
                )
            }
        }
    }

    activeContactForNumberPicker?.let { contact ->
        NumberPickerDialog(
            numbers = contact.numbers,
            onPick = { number -> vm.makeCall(number) },
            onDismiss = { activeContactForNumberPicker = null }
        )
    }

    val showAccountSelection by vm.showAccountSelectionDialog.collectAsState()
    val enabledAccounts = remember(accounts) {
        accounts.filter { it.isEnabled }
    }

    if (showAccountSelection && enabledAccounts.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { vm.dismissAccountSelection() },
            title = { Text("Select Account") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    enabledAccounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickableWithRipple {
                                    vm.proceedWithCallAfterAccountSelection(account.id)
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = account.label.ifBlank { account.domain },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (account.username.isNotBlank()) {
                                    Text(
                                        text = account.username,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissAccountSelection() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ContactItem(contact: Contact, onNumberClick: (String) -> Unit, onContactClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickableWithRipple { onContactClick() },
            contentAlignment = Alignment.Center
        ) {
            if (contact.photoUri != null) {
                val request = remember(contact.photoUri) {
                    ImageRequest.Builder(context)
                        .data(contact.photoUri)
                        .size(96, 96) // Precise downsampling for ~44dp
                        .crossfade(true)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Text(contact.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium)
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.clickableWithRipple { onContactClick() }
            )
            contact.numbers.forEach { number ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickableWithRipple { onNumberClick(number) }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
