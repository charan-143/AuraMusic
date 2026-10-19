package com.example.auramusic.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.auramusic.cache.DownloadState
import com.example.auramusic.ui.main.MainScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LibraryTools(viewModel: MainScreenViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val normalization by viewModel.normalizationEnabled.collectAsState()
    var showDownloads by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    val backup = viewModel.exportPlaylistBackup()
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(backup.toByteArray(Charsets.UTF_8)) }
                }
                Toast.makeText(context, "Playlist backup saved", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) { Toast.makeText(context, error.message ?: "Export failed", Toast.LENGTH_LONG).show() }
            finally { busy = false }
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val count = withContext(Dispatchers.IO) {
                    val text = requireNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytesLimited(10 * 1024 * 1024).toString(Charsets.UTF_8) }
                    viewModel.importPlaylistBackup(text)
                }
                Toast.makeText(context, "Imported $count playlists", Toast.LENGTH_LONG).show()
            } catch (error: Exception) { Toast.makeText(context, error.message ?: "Import failed", Toast.LENGTH_LONG).show() }
            finally { busy = false }
        }
    }
    var showBackup by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { showDownloads = true }, modifier = Modifier.fillMaxWidth()) { Text("Manage downloads", Modifier.fillMaxWidth()) }
        TextButton(onClick = { showBackup = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Playlist backup & import", Modifier.fillMaxWidth()) }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (showBackup) AlertDialog(onDismissRequest = { showBackup = false }, title = { Text("Playlist backup") },
        text = { Text("Save playlists, song details and custom covers. Audio files must be copied separately.") },
        confirmButton = { TextButton(onClick = { showBackup = false; exportPicker.launch("AuraMusic-playlists.json") }) { Text("Export") } },
        dismissButton = { TextButton(onClick = { showBackup = false; importPicker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Import") } })
    if (showDownloads) DownloadManagerDialog(viewModel) { showDownloads = false }
}

@Composable
fun DownloadManagerDialog(viewModel: MainScreenViewModel, onDismiss: () -> Unit) {
    val items by viewModel.downloadItems.collectAsState()
    val states by viewModel.downloadStates.collectAsState()
    val bytes by viewModel.downloadStorageBytes.collectAsState()
    val wifiOnly by viewModel.wifiOnlyDownloads.collectAsState()
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Downloads") }, text = {
        Column(Modifier.fillMaxWidth()) {
            Text("${formatStorage(bytes)} used, including partial downloads")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Wi-Fi-only downloads", Modifier.weight(1f))
                Switch(wifiOnly, onCheckedChange = viewModel::setWifiOnlyDownloads)
            }
            Text("Paused downloads are kept when you close the app. Resume them here.", style = MaterialTheme.typography.bodySmall)
            Row {
                TextButton(onClick = { selected = if (selected.size == items.size) emptySet() else items.map { it.id }.toSet() }) { Text("Select all") }
                TextButton(enabled = selected.isNotEmpty(), onClick = { confirmDelete = true }) { Text("Delete (${selected.size})") }
            }
            if (items.isEmpty()) Text("No downloads yet. Use a song's menu to download it.")
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(items, key = { it.id }) { track ->
                    val state = states[track.id]
                    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(track.id in selected, onCheckedChange = { checked -> selected = if (checked) selected + track.id else selected - track.id })
                            Column(Modifier.weight(1f)) {
                                Text(track.title, maxLines = 1)
                                Text(track.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                Text(when (state) {
                                    is DownloadState.Downloaded -> "Downloaded"
                                    is DownloadState.Downloading -> "${(state.progress * 100).toInt()}%"
                                    is DownloadState.Paused -> if (state.waitingForWifi) "Waiting for Wi-Fi" else "Paused"
                                    is DownloadState.Failed -> "Failed — retry available"
                                    else -> "Ready"
                                }, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (state is DownloadState.Downloading) {
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                            TextButton(onClick = { viewModel.pauseDownload(track.id) }) { Text("Pause") }
                        } else if (state !is DownloadState.Downloaded) {
                            TextButton(onClick = { viewModel.resumeDownload(track.id) }) { Text(if (state is DownloadState.Failed) "Retry" else "Resume") }
                        }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete selected downloads?") },
        text = { Text("Remove ${selected.size} downloads from AuraMusic storage? Original device music files stay untouched.") },
        confirmButton = { TextButton(onClick = {
            val ids = selected
            scope.launch(Dispatchers.IO) { viewModel.deleteDownloads(ids) }
            selected = emptySet(); confirmDelete = false
        }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
}

internal fun formatStorage(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024L * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024))
    else -> "${bytes / 1024} KB"
}
