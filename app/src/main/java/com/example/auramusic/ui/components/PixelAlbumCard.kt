package com.example.auramusic.ui.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.Album
import com.example.auramusic.theme.PixelTheme
@Composable
fun PixelAlbumCard(album: Album, onClick: () -> Unit, onPlayClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PixelTheme.colors
    Column(modifier.width(152.dp).clickable(onClick=onClick).padding(bottom=8.dp)) {
        PixelSquircleAlbumArt(album.coverArtUrl, false, Modifier.size(152.dp), 24.dp, titleFallback=album.title, artist=album.artist)
        Spacer(Modifier.height(10.dp))
        Text(album.title, color=c.textPrimary, fontSize=15.sp, fontWeight=FontWeight.Medium, maxLines=1, overflow=TextOverflow.Ellipsis)
        Text(album.artist, color=c.textSecondary, fontSize=12.sp, maxLines=1, overflow=TextOverflow.Ellipsis)
    }
}
