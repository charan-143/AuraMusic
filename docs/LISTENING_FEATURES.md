# Listening and library features

## Resume listening

AuraMusic saves the selected song, queue, position, speed, shuffle, repeat, and explicit Play Next choice. Reopening after the app process exits restores the session paused. Playback checkpoints run every two seconds; pausing, seeking, and leaving the activity also save a checkpoint. Background playback continues when the activity closes.

## Queue

Open Up Next and long-press a drag handle to move a song. Handles also expose accessible Move up and Move down actions. Removing a song shows Undo removal. Song menus provide Play Next and Add to queue. Play Next takes precedence over shuffle.

## Song editor

Open a song's menu and choose Edit song and artwork. Title, artist, album, and cover changes are stored in AuraMusic without rewriting the audio file. Choose a photo or search album covers and select a result. Selected photos are copied into app storage and downsampled to at most 1024 pixels per side.

## Downloads

Settings → Listening & library → Manage downloads provides pause/resume, a Wi-Fi-only switch, storage usage including partial transfers, selection, and confirmed bulk deletion. Paused requests survive process restarts. Wi-Fi-waiting requests start when the policy allows them. Partial HTTP transfers resume with Range/If-Range when the server supplied a stable ETag; unsupported servers restart the transfer. After process termination, unfinished requests remain available to resume in the manager. Download deletion only removes AuraMusic's copies.

## Playlist backup

Settings → Listening & library → Export/Import uses Android's document picker. Versioned JSON backups contain playlist order, song metadata, favorite IDs, and custom cover images. Imports merge copies of playlists and preserve existing ones. Files are limited to 10 MB. Audio files are not bundled: copy local music separately and rescan it on the destination phone. Playlist entries can relink by title/artist or original filename.

## Volume normalization

The Settings switch controls a PCM audio processor. It uses linked-channel RMS gain control, a silence gate, a maximum 2× boost, and peak protection. This is adaptive leveling, not a precomputed LUFS/ReplayGain library scan. Disabling the processor passes decoded PCM samples through unchanged.

## Verification

Unit coverage includes queue ordering and duplication, RMS convergence, silence and peak handling, audio format labels, and interrupted/cancelled transfers. Isolated device tests cover session restoration and actual resume, metadata persistence, portable custom covers, download pause/resume, Wi-Fi policy, storage/deletion, and queue gestures/undo.
