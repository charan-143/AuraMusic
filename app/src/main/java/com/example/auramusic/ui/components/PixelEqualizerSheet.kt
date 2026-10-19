package com.example.auramusic.ui.components

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.auramusic.audio.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelEqualizerSheet(
    visible: Boolean, equalizerState: EqualizerState, audioSessionId: Int,
    onToggleEnabled: (Boolean) -> Unit, onToggleAiMode: (Boolean) -> Unit,
    onSelectAiTarget: (AiAudioTarget) -> Unit, onBandLevelChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit, onVirtualizerChange: (Int) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit, onResetToFlat: () -> Unit,
    onDismiss: () -> Unit, modifier: Modifier = Modifier
) {
    if (!visible) return
    val context = LocalContext.current
    val enabled = equalizerState.isEnabled
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), modifier = modifier) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 24.dp)) {
            PlayerSheetHeader("Equalizer", onDismiss)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sound effects", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = onToggleEnabled, modifier = Modifier.semantics { contentDescription = "Sound effects" })
            }
            if (!enabled) Text("Turn on sound effects to adjust the equalizer.", style = MaterialTheme.typography.bodyMedium)
            if (!equalizerState.isHardwareAttached) Text("Play a song to activate sound effects.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = equalizerState.isAiMode, enabled = enabled, onClick = { onToggleAiMode(true) }, label = { Text("Adaptive", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                FilterChip(selected = !equalizerState.isAiMode, enabled = enabled, onClick = { onToggleAiMode(false) }, label = { Text("Manual", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
            }
            if (equalizerState.isAiMode) {
                Text("Automatically adjusts the sound for your music.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PlayerChoice("Sound profile", equalizerState.activeAiTarget.displayName, AiAudioTarget.entries.map { it.displayName }, enabled) { index -> onSelectAiTarget(AiAudioTarget.entries[index]) }
            } else {
                PlayerChoice("Preset", equalizerState.currentPreset.displayName, EqualizerPreset.entries.map { it.displayName }, enabled) { index -> onSelectPreset(EqualizerPreset.entries[index]) }
                equalizerState.bands.forEach { band ->
                    PlayerSoundSlider(band.label, band.levelMilliBels / 100f, -12f..12f, enabled, { "${it.roundToInt()} dB" }) { onBandLevelChange(band.index, (it * 100).roundToInt()) }
                }
                TextButton(onClick = onResetToFlat, enabled = enabled) { Text("Reset to flat") }
            }
            HorizontalDivider()
            var advanced by remember { mutableStateOf(false) }
            TextButton(onClick = { advanced = !advanced }, modifier = Modifier.fillMaxWidth()) { Text(if (advanced) "Hide more sound controls" else "More sound controls") }
            if (advanced) {
                if (equalizerState.isAiMode) Text("Switch to Manual to adjust these effects. Adaptive mode sets them for each song.", style = MaterialTheme.typography.bodyMedium)
                PlayerSoundSlider("Bass boost", equalizerState.bassBoostStrength.toFloat(), 0f..1000f, enabled && !equalizerState.isAiMode, { "${(it / 10).roundToInt()}%" }) { onBassBoostChange(it.roundToInt()) }
                PlayerSoundSlider("Stereo width", equalizerState.virtualizerStrength.toFloat(), 0f..1000f, enabled && !equalizerState.isAiMode, { "${(it / 10).roundToInt()}%" }) { onVirtualizerChange(it.roundToInt()) }
                Text("Availability depends on your audio device.", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = {
                val panel = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId).putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName).putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                runCatching { context.startActivity(panel) }.recoverCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) }.onFailure { Toast.makeText(context, "Sound settings are unavailable", Toast.LENGTH_SHORT).show() }
            }) { Text("System sound settings") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerChoice(label: String, value: String, options: List<String>, enabled: Boolean, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, enabled = enabled,
            label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), maxLines = 2)
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelect(index) })
            }
        }
    }
}

@Composable
private fun PlayerSoundSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, enabled: Boolean, format: (Float) -> String, onChange: (Float) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.coerceIn(range)) }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(format(local), modifier = Modifier.widthIn(min = 64.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = local, onValueChange = { local = it; onChange(it) }, enabled = enabled, valueRange = range, modifier = Modifier.semantics { contentDescription = label })
    }
}
