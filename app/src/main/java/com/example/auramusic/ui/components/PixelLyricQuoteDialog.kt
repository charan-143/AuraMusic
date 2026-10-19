package com.example.auramusic.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

@Composable
fun PixelLyricQuoteDialog(lyricText: String, trackTitle: String, trackArtist: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val quote = "\"$lyricText\"\n$trackTitle — $trackArtist"
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Share lyric") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { Text(lyricText, style = MaterialTheme.typography.titleLarge); Text("$trackTitle\n$trackArtist", style = MaterialTheme.typography.bodyMedium) }
    }, confirmButton = { TextButton(onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, quote) }, "Share lyric")); onDismiss() }) { Text("Share") } }, dismissButton = {
        Row { TextButton(onClick = { clipboard.setText(AnnotatedString(quote)); onDismiss() }) { Text("Copy") }; TextButton(onClick = onDismiss) { Text("Cancel") } }
    })
}
