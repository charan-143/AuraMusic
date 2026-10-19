package com.example.auramusic.ui.main
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auramusic.model.Track
import com.example.auramusic.theme.PixelTheme
import com.example.auramusic.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
internal fun CalmMainContent(vm: MainScreenViewModel, modifier: Modifier = Modifier) {
    val tab by vm.currentTab.collectAsState()
    val holder = rememberSaveableStateHolder()
    Box(modifier) { holder.SaveableStateProvider(tab.name) {
        when(tab) {
            PixelNavTab.HOME -> HomeContent(vm)
            PixelNavTab.SEARCH -> SearchContent(vm)
            PixelNavTab.LIBRARY -> LibraryContent(vm)
            PixelNavTab.SETTINGS -> SettingsContent(vm)
        }
    } }
}
@Composable
private fun PageTitle(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top=24.dp, bottom=20.dp)) {
        Text(title, fontSize=30.sp, fontWeight=FontWeight.Bold, color=PixelTheme.colors.textPrimary)
        subtitle?.let { Text(it, fontSize=13.sp, color=PixelTheme.colors.textSecondary, modifier=Modifier.padding(top=4.dp)) }
    }
}
@Composable
private fun SectionTitle(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top=20.dp, bottom=8.dp), verticalAlignment=Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontSize=20.sp, fontWeight=FontWeight.SemiBold, color=PixelTheme.colors.textPrimary)
        if(action != null) TextButton(onClick=onAction) { Text(action) }
    }
}
@Composable
private fun Song(vm: MainScreenViewModel, track: Track, queue: List<Track>) {
    val player by vm.playerState.collectAsState()
    PixelTrackTile(track, player.currentTrack?.id == track.id, player.isPlaying,
        onClick={ vm.playTrack(track, queue) }, onMoreClick={ vm.openTrackActions(track) })
}
@Composable
private fun HomeContent(vm: MainScreenViewModel) {
    val sections by vm.recommendations.collectAsState()
    val catalog by vm.allTracks.collectAsState()
    val songs = (sections.flatMap { it.tracks } + sections.flatMap { it.albums }.flatMap { it.tracks } + catalog).distinctBy { it.id }
    val albums = sections.flatMap { it.albums }.distinctBy { it.id }
    var showAll by rememberSaveable { mutableStateOf(false) }
    LazyColumn(state=rememberLazyListState(), contentPadding=PaddingValues(horizontal=20.dp, vertical=4.dp)) {
        item { PageTitle("Home", SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date())) }
        if(songs.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(PixelTheme.colors.surfaceContainer)
                .clickable { vm.playTrack(songs.first(), songs) }.padding(20.dp), verticalAlignment=Alignment.CenterVertically) {
                PixelSquircleAlbumArt(songs.first().coverArtUrl, false, Modifier.size(88.dp), 22.dp, titleFallback=songs.first().title, artist=songs.first().artist)
                Column(Modifier.weight(1f).padding(start=18.dp)) {
                    Text("MADE FOR YOU", fontSize=11.sp, color=PixelTheme.colors.textSecondary)
                    Text("Your daily mix", fontSize=22.sp, fontWeight=FontWeight.SemiBold, color=PixelTheme.colors.textPrimary)
                    Text("A little familiar. A little new.", fontSize=12.sp, color=PixelTheme.colors.textSecondary)
                    TextButton(onClick={ vm.playTrack(songs.first(), songs) }, contentPadding=PaddingValues(0.dp)) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp)); Text("Play mix")
                    }
                }
            }
        }
        item { SectionTitle("Recommended songs", if(showAll) "Show less" else "See all", { showAll=!showAll }) }
        items(if(showAll) songs.take(40) else songs.take(6), key={ "song-${it.id}" }) { Song(vm,it,songs) }
        if(songs.isEmpty()) item { Text("Your music will appear here. Search for a song or explore your library.", color=PixelTheme.colors.textSecondary, modifier=Modifier.padding(vertical=24.dp)) }
        if(albums.isNotEmpty()) {
            item { SectionTitle("Albums to explore") }
            item { LazyRow(horizontalArrangement=Arrangement.spacedBy(16.dp)) { items(albums.take(12),key={it.id}) { album ->
                PixelAlbumCard(album, {vm.openAlbum(album)}, {vm.playAlbum(album)})
            } } }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
@Composable
private fun SearchContent(vm: MainScreenViewModel) {
    val query by vm.searchQuery.collectAsState()
    val tracks by vm.filteredTracks.collectAsState()
    val albums by vm.filteredAlbums.collectAsState()
    val busy by vm.isSearchingOnline.collectAsState()
    var type by rememberSaveable { mutableStateOf("Songs") }
    LazyColumn(state=rememberLazyListState(), contentPadding=PaddingValues(horizontal=20.dp, vertical=4.dp)) {
        item { PageTitle("Search", "Find your next favorite") }
        item { OutlinedTextField(query, vm::setSearchQuery, Modifier.fillMaxWidth(), singleLine=true,
            placeholder={Text("Songs, artists or albums")}, leadingIcon={Icon(Icons.Default.Search,null)},
            trailingIcon={ if(query.isNotEmpty()) IconButton(onClick={vm.setSearchQuery("")}) { Icon(Icons.Default.Close,"Clear search") } }, shape=RoundedCornerShape(24.dp)) }
        if(query.isBlank()) {
            item { SectionTitle("Browse moods") }
            items(listOf("Chill", "Focus", "Workout", "Romance", "Tamil", "Hindi")) { genre ->
                Surface(onClick={vm.searchGenre(genre)}, shape=RoundedCornerShape(20.dp), color=PixelTheme.colors.surfaceContainer, modifier=Modifier.fillMaxWidth().padding(vertical=5.dp)) {
                    Row(Modifier.padding(20.dp)) { Text(genre,Modifier.weight(1f)); Icon(Icons.Default.ChevronRight,null) }
                }
            }
        } else {
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(vertical=12.dp)) {
                listOf("Songs","Albums").forEach { FilterChip(type==it,{type=it},label={Text(it)}) }
            } }
            if(busy) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom=12.dp)) }
            if(type=="Songs") items(tracks,key={it.id}) { Song(vm,it,tracks) }
            else items(albums.chunked(2)) { pair -> Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) { pair.forEach { album -> PixelAlbumCard(album,{vm.openAlbum(album)},{vm.playAlbum(album)},Modifier.weight(1f)) } } }
            if(!busy && (if(type=="Songs") tracks.isEmpty() else albums.isEmpty())) item { Text("No results. Try another song or artist.",Modifier.padding(vertical=24.dp),color=PixelTheme.colors.textSecondary) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
@Composable
private fun LibraryContent(vm: MainScreenViewModel) {
    val all by vm.allTracks.collectAsState()
    val albums by vm.allAlbums.collectAsState()
    val favorites by vm.favoriteTracks.collectAsState()
    val downloads by vm.downloadedTracks.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val folders by vm.folderTracks.collectAsState()
    var filter by rememberSaveable { mutableStateOf("Songs") }
    var manager by remember { mutableStateOf(false) }
    var deletePlaylistId by remember { mutableStateOf<String?>(null) }
    val songs = when(filter) { "Favorites" -> favorites; "Downloads" -> downloads; else -> all.filter { it.isLocal } }
    LazyColumn(state=rememberLazyListState(), contentPadding=PaddingValues(horizontal=20.dp, vertical=4.dp)) {
        item { PageTitle("Your library", "Music you've saved and made yours") }
        item { LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { items(listOf("Songs","Playlists","Albums","Favorites","Downloads","Folders")) { label -> FilterChip(filter==label,{filter=label},label={Text(label)}) } } }
        if(filter=="Playlists") {
            item { SectionTitle("Playlists", "New", vm::openCreatePlaylistDialog) }
            items(playlists,key={it.id}) { playlist ->
                ListItem(headlineContent={Text(playlist.name)},supportingContent={Text("${playlist.tracks.size} songs")},
                    leadingContent={Icon(Icons.Default.QueueMusic,null)}, trailingContent={IconButton(onClick={deletePlaylistId=playlist.id}) {Icon(Icons.Default.Delete,"Delete ${playlist.name}")}},
                    modifier=Modifier.clickable {vm.openPlaylistAsAlbum(playlist)}, colors=ListItemDefaults.colors(containerColor=PixelTheme.colors.background))
            }
            if(playlists.isEmpty()) item { Text("Create a playlist, then add songs from their menu.",Modifier.padding(vertical=24.dp),color=PixelTheme.colors.textSecondary) }
        } else if(filter=="Albums") {
            val savedAlbums = albums.filter { album -> album.isUserCreated || album.tracks.any { it.isLocal } }
            if(savedAlbums.isEmpty()) item { Text("Your saved and device albums will appear here.",Modifier.padding(vertical=24.dp),color=PixelTheme.colors.textSecondary) }
            items(savedAlbums.chunked(2)) { pair -> Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) { pair.forEach { album -> PixelAlbumCard(album,{vm.openAlbum(album)},{vm.playAlbum(album)},Modifier.weight(1f)) } } }
        } else if(filter=="Folders") {
            folders.forEach { (folder,tracks) -> item { SectionTitle(folder.substringAfterLast('/')) }; items(tracks,key={"$folder-${it.id}"}) {Song(vm,it,tracks)} }
        } else {
            item { SectionTitle("${songs.size} songs", if(filter=="Downloads") "Manage" else "Refresh", { if(filter=="Downloads") manager=true else vm.rescanDeviceAudio() }) }
            items(songs,key={it.id}) {Song(vm,it,songs)}
            if(songs.isEmpty()) item { Text(when(filter) { "Favorites" -> "Tap Favorite in a song's menu or player to save it here."; "Downloads" -> "Download a song from its menu for offline listening."; else -> "No device songs yet. Add music to your phone, then refresh." },Modifier.padding(vertical=24.dp),color=PixelTheme.colors.textSecondary) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if(manager) DownloadManagerDialog(vm) {manager=false}
    deletePlaylistId?.let { id -> AlertDialog(onDismissRequest={deletePlaylistId=null},title={Text("Delete playlist?")},text={Text("The playlist will be removed. Your songs stay on your device.")},confirmButton={TextButton(onClick={vm.deletePlaylist(id);deletePlaylistId=null}) {Text("Delete")}},dismissButton={TextButton(onClick={deletePlaylistId=null}) {Text("Cancel")}}) }
}
@Composable
private fun SettingsContent(vm: MainScreenViewModel) {
    val dark by vm.isDarkMode.collectAsState()
    val normalization by vm.normalizationEnabled.collectAsState()
    val quality by vm.streamingQuality.collectAsState()
    val player by vm.playerState.collectAsState()
    var chooseQuality by remember {mutableStateOf(false)}
    LazyColumn(state=rememberLazyListState(), contentPadding=PaddingValues(horizontal=20.dp, vertical=4.dp)) {
        item {PageTitle("Settings")}
        item {SectionTitle("Appearance")}
        item { Preference("Dark theme", "A quiet, true-black interface") {Switch(dark,{vm.toggleThemeMode()})} }
        item {SectionTitle("Playback")}
        item {Preference("Volume normalization", "Keep loudness more consistent") {Switch(normalization,vm::setNormalization)} }
        item {Preference("Streaming quality", quality, {chooseQuality=true})}
        item {Preference("Crossfade", "Blend tracks over ${player.crossfadeDurationSec} seconds") {Switch(player.isCrossfadeEnabled,{vm.setCrossfade(it,player.crossfadeDurationSec)})}}
        item {Preference("Travel mode", "Adapt playback to changing connections") {Switch(player.isTravelModeEnabled,{vm.toggleTravelMode()})}}
        item {Preference("Equalizer", "Adjust your sound", vm::openEqualizerSheet)}
        item {Preference("Sleep timer", "Stop playback automatically", vm::openSleepTimerDialog)}
        item {Preference("Audio output", "Choose where to listen", vm::openAudioRouteSheet)}
        item {SectionTitle("Library & storage")}
        item {LibraryTools(vm)}
        item {Preference("Refresh device music", "Find songs stored on this phone", vm::rescanDeviceAudio)}
        item {Text("Resume listening is automatic. Your last queue and position are restored paused.",Modifier.padding(vertical=24.dp),color=PixelTheme.colors.textSecondary, fontSize=12.sp)}
    }
    if(chooseQuality) AlertDialog(onDismissRequest={chooseQuality=false},title={Text("Streaming quality")},text={Column {
        listOf("Auto (Network Adaptive)", "High (up to 320kbps)", "Balanced (up to 256kbps)", "Data Saver (Roaming Auto)").forEach { option -> TextButton(onClick={vm.setStreamingQuality(option);chooseQuality=false}) {Text(option)} }
    }},confirmButton={TextButton(onClick={chooseQuality=false}) {Text("Cancel")}})
}
@Composable
private fun Preference(title: String, subtitle: String, onClick: () -> Unit = {}, control: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick=onClick).padding(vertical=14.dp,horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end=16.dp)) {Text(title,color=PixelTheme.colors.textPrimary,fontSize=16.sp); Text(subtitle,color=PixelTheme.colors.textSecondary,fontSize=12.sp)}
        if(control!=null) control() else Icon(Icons.Default.ChevronRight,null,tint=PixelTheme.colors.textSecondary)
    }
}
