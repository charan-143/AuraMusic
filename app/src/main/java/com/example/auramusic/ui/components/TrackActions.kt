package com.example.auramusic.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auramusic.model.Track
import com.example.auramusic.ui.main.MainScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActions(viewModel: MainScreenViewModel) {
    val track by viewModel.actionTrack.collectAsState()
    val selected = track ?: return
    var editing by remember(selected.id) { mutableStateOf(false) }
    if (editing) {
        TrackEditor(selected, viewModel, onDismiss = { viewModel.closeTrackActions() })
    } else {
        ModalBottomSheet(onDismissRequest = viewModel::closeTrackActions) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=8.dp)) {
                Text(selected.title,style=MaterialTheme.typography.titleLarge)
                Text(selected.artist,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=12.dp))
                val actions = listOf<Pair<String, () -> Unit>>(
                    "Play Next" to { viewModel.playNext(selected); viewModel.closeTrackActions() },
                    "Add to queue" to { viewModel.addToQueue(selected); viewModel.closeTrackActions() },
                    (if(viewModel.isFavorite(selected.id)) "Remove from favorites" else "Add to favorites") to { viewModel.toggleFavorite(selected); viewModel.closeTrackActions() },
                    "Add to playlist" to { viewModel.openAddToPlaylist(selected); viewModel.closeTrackActions() },
                    "Edit song and artwork" to { editing=true },
                    "Download" to { viewModel.downloadTrack(selected); viewModel.closeTrackActions() },
                    "Start song radio" to { viewModel.startTrackRadio(selected); viewModel.closeTrackActions() },
                    "Sleep timer" to { viewModel.openSleepTimerDialog(); viewModel.closeTrackActions() }
                )
                actions.forEach { (label,action) -> TextButton(onClick=action,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {Text(label,Modifier.fillMaxWidth())} }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TrackEditor(track: Track, viewModel: MainScreenViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    var cover by remember(track.id) { mutableStateOf(track.coverArtUrl) }
    var candidates by remember { mutableStateOf(emptyList<Track>()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                cover = withContext(Dispatchers.IO) {
                    val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { it.readBytesLimited(8 * 1024 * 1024) }
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                    require(options.outWidth > 0 && options.outHeight > 0) { "Choose a valid image" }
                    var scale = 1
                    while (maxOf(options.outWidth, options.outHeight) / scale > 1024) scale *= 2
                    val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = scale }))
                    val file = File(context.filesDir, "custom_artwork/${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                    bitmap.recycle()
                    Uri.fromFile(file).toString()
                }
            } catch (error: Exception) { message = error.message ?: "Could not open this image" }
            finally { busy = false }
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit song") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(cover, "Selected artwork", modifier = Modifier.size(128.dp))
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
            OutlinedTextField(artist, { artist = it }, label = { Text("Artist") }, singleLine = true)
            OutlinedTextField(album, { album = it }, label = { Text("Album") }, singleLine = true)
            TextButton(enabled = !busy, onClick = { picker.launch(arrayOf("image/*")) }) { Text("Choose a photo") }
            TextButton(enabled = !busy && title.isNotBlank() && artist.isNotBlank(), onClick = {
                scope.launch {
                    busy = true; message = null
                    try {
                        candidates = viewModel.searchCoverCandidates(title, artist)
                        if (candidates.isEmpty()) message = "No matching covers found. You can choose a photo instead."
                    } catch (_: Exception) { message = "Could not search for covers" }
                    finally { busy = false }
                }
            }) { Text("Find album covers") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates) { candidate ->
                    Column(Modifier.width(96.dp).clickable { cover = candidate.coverArtUrl }) {
                        AsyncImage(candidate.coverArtUrl, "Use cover for ${candidate.title}", modifier = Modifier.size(88.dp))
                        Text(candidate.title, maxLines = 2, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Changes are saved in AuraMusic. Original audio files stay unchanged.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        TextButton(enabled = !busy && title.isNotBlank() && artist.isNotBlank(), onClick = {
            viewModel.saveTrackEdits(track.copy(title = title.trim(), artist = artist.trim(), album = album.trim(), coverArtUrl = cover))
            Toast.makeText(context, "Song updated", Toast.LENGTH_SHORT).show(); onDismiss()
        }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

internal fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= limit) { "File is too large" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
