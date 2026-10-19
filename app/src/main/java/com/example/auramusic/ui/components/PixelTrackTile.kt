package com.example.auramusic.ui.components
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.Track
import com.example.auramusic.theme.PixelTheme
@Composable
fun PixelTrackTile(track: Track, isSelected: Boolean, isPlaying: Boolean, onClick: () -> Unit,
    onMoreClick: () -> Unit = {}, isFavorite: Boolean = false, isDownloaded: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val c = PixelTheme.colors
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(if(isSelected) c.surfaceContainer else Color.Transparent).clickable(onClick=onClick)
        .padding(start=8.dp, top=8.dp, bottom=8.dp), verticalAlignment=Alignment.CenterVertically) {
        PixelSquircleAlbumArt(track.coverArtUrl, isSelected && isPlaying, Modifier.size(52.dp), 14.dp,
            titleFallback=track.title, artist=track.artist, audioUri=track.localFilePath ?: track.audioUrl)
        Column(Modifier.weight(1f).padding(horizontal=14.dp)) {
            Text(track.title, color=c.textPrimary, fontSize=15.sp, fontWeight=FontWeight.Medium, maxLines=1, overflow=TextOverflow.Ellipsis)
            Text(track.artist, color=c.textSecondary, fontSize=13.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
        }
        if(onToggleFavorite != null) IconButton(onClick=onToggleFavorite) {
            Icon(if(isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite", tint=c.textSecondary, modifier=Modifier.size(20.dp))
        }
        IconButton(onClick=onMoreClick) { Icon(Icons.Default.MoreVert, "More options for ${track.title}", tint=c.textSecondary) }
    }
}
