package com.example.auramusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.TrackLyrics
import com.example.auramusic.player.PlaybackProgress
import com.example.auramusic.player.PlayerState
import com.example.auramusic.theme.PixelTheme
import kotlinx.coroutines.flow.StateFlow
@Composable
fun PixelExpandedPlayer(
    playerState: PlayerState,
    playbackProgressFlow: StateFlow<PlaybackProgress>? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onSkipPreviousClick: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onCollapseClick: () -> Unit,
    onQueueClick: () -> Unit,
    onMoreOptionsClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    lyrics: TrackLyrics? = null,
    isLyricsActive: Boolean = false,
    onToggleLyrics: () -> Unit = {},
    onSeekToTimestamp: (Long) -> Unit = {},
    isDownloaded: Boolean = false,
    downloadProgress: Float? = null,
    downloadError: String? = null,
    onDownloadClick: () -> Unit = {},
    onOpenSpeedSheet: () -> Unit = {},
    onAudioOutputClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = PixelTheme.colors
    val track = playerState.currentTrack ?: return
    var menu by remember { mutableStateOf(false) }
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier.fillMaxWidth().fillMaxHeight(0.96f).clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(c.background).navigationBarsPadding()) {
        val compact = maxHeight < 620.dp || fontScale > 1.3f
        val artworkSize = minOf(maxWidth - 48.dp, 180.dp)
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCollapseClick) { Icon(Icons.Default.KeyboardArrowDown, "Collapse player", tint = c.textPrimary) }
                Text(if (isLyricsActive) "Lyrics" else "Now playing", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = c.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "Song options", tint = c.textPrimary) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Audio output") }, onClick = { menu = false; onAudioOutputClick() })
                        DropdownMenuItem(text = { Text(if (isDownloaded) "Remove download" else "Download") }, onClick = { menu = false; onDownloadClick() })
                        DropdownMenuItem(text = { Text("More song actions") }, onClick = { menu = false; onMoreOptionsClick() })
                    }
                }
            }
            if (isLyricsActive) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                PixelExpandedLyricsSection(playbackProgressFlow, playerState.currentPositionMs, lyrics, onSeekToTimestamp, onToggleLyrics, Modifier.weight(1f).padding(top = 12.dp))
            }
            Column(Modifier.fillMaxWidth().then(if (isLyricsActive) Modifier else Modifier.weight(1f)).then(if (compact || isLyricsActive) Modifier.verticalScroll(rememberScrollState()) else Modifier), horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isLyricsActive) {
                    BoxWithConstraints(
                        Modifier.fillMaxWidth().then(if (compact) Modifier.height(artworkSize + 32.dp) else Modifier.weight(1f)).padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val side = minOf(maxWidth, maxHeight)
                        PixelSquircleAlbumArt(coverArtUrl = track.coverArtUrl, isPlaying = playerState.isPlaying,
                            modifier = Modifier.size(side).testTag("player-artwork"), cornerRadius = 28.dp,
                            titleFallback = track.title, artist = track.artist, audioUri = track.localFilePath ?: track.audioUrl)
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(track.title, fontSize = 22.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = c.textPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text(track.artist, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = c.textSecondary)
                        }
                        IconButton(onClick = onToggleFavorite) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (isFavorite) "Remove from favorites" else "Add to favorites", tint = c.textPrimary) }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                PixelExpandedSeekbarSection(playbackProgressFlow, playerState, playerState.isPlaying, onSeek)
                Row(Modifier.fillMaxWidth().padding(vertical = 16.dp).testTag("player-transport"), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, if (playerState.isShuffle) "Shuffle on" else "Shuffle off", tint = if (playerState.isShuffle) c.textPrimary else c.textTertiary) }
                    IconButton(onClick = onSkipPreviousClick) { Icon(Icons.Default.SkipPrevious, "Previous song", tint = c.textPrimary, modifier = Modifier.size(32.dp)) }
                    FilledIconButton(onClick = onPlayPauseClick, modifier = Modifier.size(72.dp), shape = CircleShape) { Icon(if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (playerState.isPlaying) "Pause" else "Play", modifier = Modifier.size(36.dp)) }
                    IconButton(onClick = onSkipNextClick) { Icon(Icons.Default.SkipNext, "Next song", tint = c.textPrimary, modifier = Modifier.size(32.dp)) }
                    IconButton(onClick = onToggleRepeat) { Icon(if (playerState.isRepeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat, if (playerState.isRepeatOne) "Repeat one" else "Repeat off", tint = if (playerState.isRepeatOne) c.textPrimary else c.textTertiary) }
                }
                Row(Modifier.fillMaxWidth().testTag("player-tools").clip(RoundedCornerShape(24.dp)).background(c.surfaceContainer).padding(vertical = 4.dp)) {
                    PlayerTool(Icons.Default.QueueMusic, "Queue", false, onQueueClick, Modifier.weight(1f))
                    PlayerTool(Icons.Default.Mic, "Lyrics", isLyricsActive, onToggleLyrics, Modifier.weight(1f))
                    PlayerTool(Icons.Default.GraphicEq, "Sound", false, onEqualizerClick, Modifier.weight(1f))
                    PlayerTool(Icons.Default.Speed, "Speed", playerState.playbackSpeed != 1f, onOpenSpeedSheet, Modifier.weight(1f))
                }
                downloadProgress?.let { LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) }
                downloadError?.let { Text(it, color = c.textSecondary, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PlayerTool(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = PixelTheme.colors
    Column(modifier.semantics { this.selected = selected }.clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = if (selected) c.textPrimary else c.textSecondary, modifier = Modifier.size(22.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = c.textPrimary)
    }
}



@Composable
private fun PixelExpandedSeekbarSection(playbackProgressFlow: StateFlow<PlaybackProgress>?, fallbackPlayerState: PlayerState, isPlaying: Boolean, onSeek: (Float) -> Unit) {
    val progress = playbackProgressFlow?.collectAsState()?.value
    val c = PixelTheme.colors
    Column(Modifier.fillMaxWidth()) {
        PixelSquigglySeekbar(progress = progress?.progress ?: fallbackPlayerState.progress, bufferedProgress = progress?.bufferedProgress ?: fallbackPlayerState.bufferedProgress, isPlaying = isPlaying, onSeek = onSeek, activeColor = c.textPrimary, inactiveColor = c.outline)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(progress?.formattedCurrentPosition ?: fallbackPlayerState.formattedCurrentPosition, fontSize = 12.sp, color = c.textSecondary)
            Text(progress?.formattedDuration ?: fallbackPlayerState.formattedDuration, fontSize = 12.sp, color = c.textSecondary)
        }
    }
}

@Composable
private fun PixelExpandedLyricsSection(playbackProgressFlow: StateFlow<PlaybackProgress>?, fallbackPositionMs: Long, lyrics: TrackLyrics?, onSeekToTimestamp: (Long) -> Unit, onSwitchToAlbumArt: () -> Unit, modifier: Modifier) {
    val position = playbackProgressFlow?.collectAsState()?.value?.currentPositionMs ?: fallbackPositionMs
    PixelSyncedLyricsView(lyrics, position, onSeekToTimestamp, onSwitchToAlbumArt, modifier)
}
