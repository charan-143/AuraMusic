package com.example.auramusic.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.TrackLyrics

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PixelSyncedLyricsView(lyrics: TrackLyrics?, currentPositionMs: Long, onSeekToTimestamp: (Long) -> Unit, onSwitchToAlbumArt: () -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    var follow by remember(lyrics?.trackId) { mutableStateOf(true) }
    var quote by remember(lyrics?.trackId) { mutableStateOf<String?>(null) }
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(dragged) { if (dragged) follow = false }
    val active = lyrics?.lines?.indexOfLast { it.timestampMs <= currentPositionMs } ?: -1
    LaunchedEffect(active, follow, lyrics?.trackId) {
        if (follow && lyrics?.isSynced == true && active >= 0) listState.animateScrollToItem((active - 1).coerceAtLeast(0))
    }
    Column(modifier.fillMaxSize()) {
        if (lyrics == null || lyrics.isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(Modifier.size(24.dp)); Text("Finding lyrics…") }
            }
        } else if (!lyrics.hasLines && lyrics.plainLyrics.isBlank()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Lyrics aren’t available for this song yet.", style = MaterialTheme.typography.titleMedium) }
            TextButton(onClick = onSwitchToAlbumArt) { Text("Back to artwork") }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (lyrics.isSynced && lyrics.hasLines) "Synced lyrics" else "Lyrics", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (lyrics.isSynced && lyrics.hasLines) FilterChip(selected = follow, onClick = { follow = !follow }, label = { Text(if (follow) "Following" else "Follow song") })
            }
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                if (lyrics.hasLines) {
                    itemsIndexed(lyrics.lines) { index, line ->
                        Text(line.text.ifBlank { "♪" }, fontSize = 24.sp, lineHeight = 34.sp, fontWeight = if (index == active && lyrics.isSynced) FontWeight.Bold else FontWeight.Medium,
                            color = if (index == active && lyrics.isSynced) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).combinedClickable(onClickLabel = if (lyrics.isSynced) "Play from this line" else null, onClick = { if (lyrics.isSynced) onSeekToTimestamp(line.timestampMs) }, onLongClickLabel = "Share lyric", onLongClick = { quote = line.text }))
                    }
                } else {
                    item { Text(lyrics.plainLyrics, fontSize = 22.sp, lineHeight = 34.sp) }
                }
            }
            if (lyrics.source.isNotBlank()) Text(lyrics.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
    quote?.let { PixelLyricQuoteDialog(it, lyrics?.title.orEmpty(), lyrics?.artist.orEmpty()) { quote = null } }
}
