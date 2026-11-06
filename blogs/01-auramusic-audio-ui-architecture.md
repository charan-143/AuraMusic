# AuraMusic: Building an Android Audio Experience Around State, Playback and Interaction Design

*Project: [AuraMusic](https://github.com/charan-143/AuraMusic) · Status: public repository · Technologies: Kotlin, Jetpack Compose, Media3/ExoPlayer, StateFlow, Navigation 3, Material 3, Coil*

Music-player interfaces make state management surprisingly visible. The same song can appear in a mini-player, an expanded player, a queue, and a search result. Those surfaces must agree about what is playing, how far playback has progressed, and what the next action will do.

## Start with one playback source of truth

The AuraMusic Android project uses Media3/ExoPlayer dependencies and documents a playback manager exposing state to the Compose UI. This is an architectural choice rather than just a library preference. A player engine owns transport behavior—play, pause, seek, shuffle, repeat—while screens observe a consistent representation of playback. StateFlow and lifecycle-aware collection can help avoid screens inventing their own playback states.

A robust implementation should model buffering, errors, and playback completion explicitly. It should also preserve state when navigating between screens, because destroying a composable is not equivalent to the user requesting music to stop.

## UI animation should reflect actual audio state

The feature log documents a custom wavy seekbar, animated album artwork, an expandable player, queue interactions, and a monochrome design system. Such animation becomes meaningful when it follows playback state: moving when audio is playing, settling when paused, and remaining responsive during seeking. The interesting Compose techniques include custom Canvas drawing, gesture handling, declarative animation, and avoiding excessive recomposition.

The project also uses Coil for cover images and Navigation 3 for screen transitions. These technologies serve different concerns: media loading, rendering, and navigation. Keeping them separate makes design iterations easier.

## Local media and streaming are different paths

The repository describes local audio discovery through Android MediaStore and support for streaming sources. Those paths differ in permissions, metadata availability, failure modes, and caching strategies. The app should distinguish a missing local file from a temporary network failure, even if both appear as a song that cannot start.

Claims such as 'lossless streaming' or 'automatic quality switching' require codec, bitrate, server-capability, and measurement evidence. The feature log records design and testing milestones, but this article does not treat every feature description as an independently verified benchmark.

## The engineering lesson

For media software, polished interactions work best when the underlying architecture is boring and predictable. Put transport in one place, expose state explicitly, and let UI components react to the same events. A good mobile portfolio case study can demonstrate both system design and user-centered motion without inventing performance results.

### Sources and scope

- [Gradle dependencies](https://github.com/charan-143/AuraMusic/blob/main/app/build.gradle.kts)
- [Version catalog](https://github.com/charan-143/AuraMusic/blob/main/gradle/libs.versions.toml)
- [Feature log](https://github.com/charan-143/AuraMusic/blob/main/FEATURE_LOG.md)

*Architecture and stack are grounded in the repository. Device-level performance and audio-quality claims would need independent measurement.*