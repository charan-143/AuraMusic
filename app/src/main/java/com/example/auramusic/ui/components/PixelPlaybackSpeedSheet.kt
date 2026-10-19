package com.example.auramusic.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelPlaybackSpeedSheet(
    currentSpeed: Float, isCrossfadeEnabled: Boolean, crossfadeDurationSec: Int,
    onSpeedSelected: (Float) -> Unit, onCrossfadeToggled: (Boolean) -> Unit,
    onCrossfadeDurationChanged: (Int) -> Unit, onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), modifier = modifier) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayerSheetHeader("Playback speed", onDismiss)
            var speed by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }
            Text("${"%.2f".format(speed)}×", style = MaterialTheme.typography.headlineLarge)
            Text("1× is the original speed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = speed, onValueChange = { speed = it }, onValueChangeFinished = { onSpeedSelected(speed) }, valueRange = 0.5f..2f, modifier = Modifier.semantics { contentDescription = "Playback speed" })
            listOf(listOf(0.5f, 0.75f, 1f), listOf(1.25f, 1.5f, 2f)).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { value ->
                        FilterChip(selected = kotlin.math.abs(currentSpeed - value) < 0.01f, onClick = { onSpeedSelected(value) }, label = { Text("${value}×") }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                    }
                }
            }
            HorizontalDivider()
            var transitions by remember { mutableStateOf(false) }
            TextButton(onClick = { transitions = !transitions }, modifier = Modifier.fillMaxWidth()) { Text(if (transitions) "Hide track transitions" else "Track transitions") }
            if (transitions) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Crossfade", style = MaterialTheme.typography.titleMedium)
                        Text("Blend the end of one song into the next.", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = isCrossfadeEnabled, onCheckedChange = onCrossfadeToggled, modifier = Modifier.semantics { contentDescription = "Crossfade" })
                }
                if (isCrossfadeEnabled) {
                    var seconds by remember(crossfadeDurationSec) { mutableFloatStateOf(crossfadeDurationSec.toFloat().coerceIn(1f, 12f)) }
                    Text("Duration · ${seconds.roundToInt()} seconds")
                    Slider(value = seconds, onValueChange = { seconds = it }, onValueChangeFinished = { onCrossfadeDurationChanged(seconds.roundToInt()) }, valueRange = 1f..12f, steps = 10, modifier = Modifier.semantics { contentDescription = "Crossfade duration" })
                }
            }
        }
    }
}

@Composable
internal fun PlayerSheetHeader(title: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close $title") }
    }
}
