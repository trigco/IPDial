package com.ipdial.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.os.Environment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ipdial.ui.IPDialTopBar
import com.ipdial.ui.SipViewModel
import com.ipdial.ui.theme.glass
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(vm: SipViewModel, onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val accounts by vm.accounts.collectAsState()
    
    val internalDir = File(context.filesDir, "recordings")
    val externalDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "IPDialRecordings")

    var recordings by remember { mutableStateOf<List<File>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val iList = if (internalDir.exists()) internalDir.listFiles()?.toList() ?: emptyList() else emptyList()
        val eList = if (externalDir.exists()) externalDir.listFiles()?.toList() ?: emptyList() else emptyList()
        recordings = (iList + eList).sortedByDescending { it.lastModified() }
    }

    var playingFile by remember { mutableStateOf<File?>(null) }
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (e: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            IPDialTopBar(accounts = accounts, vm = vm, onOpenDrawer = onOpenDrawer)
        },
        bottomBar = {}
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (recordings.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No recordings found", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(recordings) { file ->
                        RecordingItem(
                            file = file,
                            isPlaying = playingFile == file,
                            onPlay = {
                                vm.incrementRecordingAction {
                                    if (playingFile == file) {
                                        try {
                                            mediaPlayer.stop()
                                            mediaPlayer.reset()
                                        } catch (e: Exception) {}
                                        playingFile = null
                                    } else {
                                        try {
                                            mediaPlayer.reset()
                                            java.io.FileInputStream(file).use { fis ->
                                                mediaPlayer.setDataSource(fis.fd)
                                            }
                                            mediaPlayer.prepare()
                                            mediaPlayer.start()
                                            playingFile = file
                                            mediaPlayer.setOnCompletionListener { 
                                                playingFile = null
                                            }
                                        } catch (e: Exception) {
                                            playingFile = null
                                            android.widget.Toast.makeText(context, "Playback failed", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                if (playingFile == file) {
                                    try {
                                        mediaPlayer.stop()
                                        mediaPlayer.reset()
                                    } catch (e: Exception) {}
                                    playingFile = null
                                }
                                file.delete()
                                val iList = if (internalDir.exists()) internalDir.listFiles()?.toList() ?: emptyList() else emptyList()
                                val eList = if (externalDir.exists()) externalDir.listFiles()?.toList() ?: emptyList() else emptyList()
                                recordings = (iList + eList).sortedByDescending { it.lastModified() }
                            },
                            onShare = {
                                vm.incrementRecordingAction {
                                    try {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "audio/*"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share/Export Recording"))
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingItem(file: File, isPlaying: Boolean, onPlay: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    val dateStr = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(file.lastModified()))
    val sizeStr = "%.2f MB".format(file.length().toDouble() / (1024 * 1024))
    val isGlass = com.ipdial.ui.theme.LocalGlassMode.current != com.ipdial.ui.theme.GlassMode.None

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isGlass) Modifier.glass(RoundedCornerShape(16.dp)) else Modifier),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isGlass) 0.dp else 2.dp,
        color = if (isGlass) Color.Transparent else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .clickableWithRipple { onPlay() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("$dateStr • $sizeStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
