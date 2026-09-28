---
name: pixel-monochrome-ui
description: Design and redesign mobile applications to Google Pixel UI (Material 3 Expressive Monochrome), featuring squircle geometry, OLED pitch black & high-contrast grayscale tonal steps, fluid spring physics, dynamic wavy seekbars, At-a-Glance widgets, and tactile Pixel motion.
---

# Google Pixel Monochrome UI Design System

This skill guides the design, redesign, and implementation of Android and cross-platform mobile user interfaces matching the **Google Pixel Material 3 Expressive Monochrome** design language.

---

## 1. Core Visual Foundations

### A. Monochrome Color Palette (True OLED Dark Mode)
Google Pixel's monochrome dynamic theming eliminates chromatic hue while maximizing depth through deliberate luminance contrast:

| Token | Hex Value | Purpose |
| :--- | :--- | :--- |
| `Background` | `#000000` | True Pitch Black for OLED energy savings and maximum contrast |
| `SurfaceContainerLow` | `#0E0E0E` | Subtle elevation backdrop for nested lists |
| `SurfaceContainer` | `#161616` | Standard card and sheet background |
| `SurfaceContainerHigh`| `#202020` | Elevated cards, dialogs, and floating controls |
| `SurfaceContainerHighest`| `#2C2C2C` | Pill chips, unselected track tiles, slider tracks |
| `OutlineVariant` | `#333333` | 1dp subtle border strokes for crisp card borders |
| `TextPrimary` | `#FFFFFF` | Stark white for song titles, hero headers, active states |
| `TextSecondary` | `#A0A0A0` | Light graphite for artist names, timestamps, metadata |
| `TextTertiary` | `#666666` | Muted slate for queue numbers, subtle labels |
| `AccentMonochrome` | `#EEEEEE` | High-luminance accent for primary FAB / play button |

### B. Pixel Geometry (Squircle Architecture)
- **Extreme Rounded Squircles**: Google Pixel UI avoids sharp corners in favor of deep superellipses:
  - Album Art / Media Cards: `RoundedCornerShape(32.dp)`
  - Elevated Containers: `RoundedCornerShape(28.dp)`
  - Bottom Sheets: `RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)`
  - Action Chips / Pill Buttons: `CircleShape` or `RoundedCornerShape(percent = 50)`
  - Mini Player Bar: `RoundedCornerShape(24.dp)`

---

## 2. Motion & Physics Principles

Pixel UI is defined by **tactile, spring-loaded physicality**:

### Spring Specs
```kotlin
// Bouncy primary action (Play/Pause button, heart toggle)
val PixelBouncySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

// Smooth sheet expansion and morphing
val PixelSheetSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)
```

### Motion Signatures
1. **Dynamic Squiggly/Wave Progress Indicator**:
   - The playback scrubber transforms into a live undulating sine wave while music is actively playing, flattening into a straight line when paused.
2. **Vinyl / Disc Spin Micro-interaction**:
   - Circular album art or vinyl groove overlay smoothly spins with continuous angular velocity during playback, gracefully decelerating on pause (`animateFloatAsState` or infinite transition).
3. **Squircle Morphing Play/Pause FAB**:
   - Morphing between a rounded squircle (Play) and an elongated capsule or circle (Pause) with scale-bounce feedback on tap (`0.92f` on press to `1.0f` on release).
4. **Haptic Cues**:
   - Light tick haptics on slider drag and spring feedback on track selection using `HapticFeedbackType.LongPress` or `TextHandleMove`.

---

## 3. Essential Pixel UI Components

### A. Pixel "At-a-Glance" Header
Place at the top of the Home / Library screen:
- Left: Current weekday and date (`"Saturday, Sep 26"`) with ambient status indicator (`"Monochrome Soundscape"`).
- Right: Discrete pill badge displaying active audio output (e.g., `"Pixel Buds Pro"` or `"Phone Speaker"`) with an audio wave icon.

### B. Pixel Squiggly Seekbar
Draw a custom Canvas path with an oscillating sine wave for the elapsed duration:
```kotlin
// Wave formula for playing state:
// y = baseline + amplitude * sin((x / waveLength) + phase)
```

### C. Pixel Bottom Floating Mini Player
- Floats 16dp above the navigation bar.
- Background: `#161616` with a 1dp `#2A2A2A` stroke and 24dp corner radius.
- Left: 48dp squircle album art thumbnail.
- Center: Marquee text for track title and artist.
- Right: Bouncy Play/Pause button + Skip button.

---

## 4. Redesign Checklist for Any Mobile App
When redesigning any screen to Pixel Monochrome:
1. [ ] Remove all saturated color accents, gradients, and colored tints.
2. [ ] Map primary surfaces to `#000000`, cards to `#161616`, and elevated containers to `#202020`.
3. [ ] Increase corner radiuses to 28dp-32dp.
4. [ ] Replace standard progress bars with squiggly/wavy animated scrubbers.
5. [ ] Add physics-based springs (`Spring.DampingRatioMediumBouncy`) to all click and toggle states.
6. [ ] Include an At-a-Glance top widget and squircle pill controls.
