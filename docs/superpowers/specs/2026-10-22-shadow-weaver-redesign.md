# Shadow Weaver: AAA Commercial Mobile Redesign Specification

**Date:** 2026-10-22  
**Target:** Android (API 21+)  
**Architecture:** Pure Native Android (Hardware-accelerated SurfaceView + Canvas + AudioTrack + Vibrator), Zero External Dependencies  
**Inspiration:** *Shadowmatic*, *Monument Valley*, *The Room*, *Prune*

---

## 1. Executive Summary & Vision

Shadow Weaver is an atmospheric physics-and-light shadow puzzle game where players manipulate geometric cast tokens suspended between a flickering candle and a projection wall to forge intricate shadow silhouettes. 

This redesign elevates Shadow Weaver from a minimalist prototype into a commercial, industry-level title with:
1. **Atmospheric Visual Polish:** Floating ambient dust embers, dynamic flame lighting, volumetric god rays, tactile 2.5D mechanical puppets with beveled specular highlights, procedural canvas textures, and a dazzling golden celebration burst.
2. **Tactile "Game Juice" & Haptics:** Directional inertia, rotational notch detents with micro-haptic clicks, proximity resonance (warmth cue when nearing alignment), and particle physics.
3. **Adaptive Audio Architecture:** Real-time synthesized harmonic resonance that climbs in pitch as match percentage increases, tactile wood-block detent clicks, and lush celebratory arpeggios.
4. **Professional UI/UX & Flow:** Sleek modern HUD with Pause/Settings, Hint toggle, Reset button, 3-Star scoring system with time/move tracking, interactive Level Select grid with progress badges, and an elegant frosted-glass Level Complete celebration modal.
5. **Modern Android Standards:** Edge-to-edge support, high-refresh-rate (90/120Hz) display compatibility, zero memory allocations in the render loop, and 100% offline self-containment.

---

## 2. Visual Systems & Art Direction

### 2.1 Lighting & Chamber Atmosphere
- **Multi-layered Ambient Vignette:** Deep warm charcoal and ember gradient with subtle vignette darkening at screen edges.
- **Floating Ambient Dust Motes / Embers:** A continuous particle system simulating 30-40 microscopic motes drifting in the candle's heat with Brownian motion, softly illuminated when passing through the light rays.
- **Dynamic Flame & Volumetric Glow:**
  - Multi-lobe flame with luminous core, amber mantle, and breathing radius.
  - Flame micro-flicker smoothly shifts the scene's color temperature and wall illumination.
  - Subtle volumetric god rays streaming from the candle toward the stage and wall.

### 2.2 Tactile 2.5D Stage Puppets (Tokens)
- **Physical Substance:** Shapes are no longer flat wireframe outlines. They have:
  - Deep lacquered obsidian/brass base fill with directional bevel lighting (light-facing edges gleam with warm highlights; away edges fall into dark shade).
  - Ambient contact drop shadow cast on the stage plane directly below each shape.
  - Elevation on touch: When grabbed, a shape smoothly scales up (+5%), lifts off the stage (drop shadow expands and blurs), and gains a soft ambient glow.
- **Precision Mechanical Rotation Dial:**
  - When selected, a circular dial appears around the shape with discrete 15° tick marks, primary 45°/90° cardinal indicators, and a tactile rotatable handle.
  - Dragging the handle or performing a two-finger twist turns the shape with smooth damped momentum and subtle detent snapping.

### 2.3 Wall Projection & Silhouette Reveal
- **Multi-Band Penumbra Shadows:** Retained and polished multi-band Minkowski dilation for soft, realistic shadow penumbras with warm ambient occlusion stack.
- **Proximity Resonance:**
  - At <50% match: Target ghost is a subtle blueprint watermark.
  - At 50%-84% match: The target edges begin pulsing gently in sync with candle flickers; light rays intensify.
  - At >=85% (Level Complete): The shadow snaps into the target, a golden particle burst explodes from the silhouette, and the shape turns into an illuminated golden emblem.

---

## 3. Audio & Haptics ("Game Juice")

### 3.1 Adaptive Procedural Audio Engine (`AudioManager`)
- **Binaural Bed & Fire Crackle:** Continuous warm drone with low harmonic richness and organic randomized wood-flicker pops.
- **Harmonic Resonance Ladder:** As match percentage crosses milestones (25%, 50%, 75%, 85%), a procedurally synthesized bell/chime note sounds in an ascending pentatonic harmony, giving intuitive acoustic "warmer/colder" feedback.
- **Tactile Sound Effects:**
  - Soft air whoosh on token pickup / release.
  - Crisp micro-click on rotation detents (every 15°).
  - Multi-voice celebratory chime chord on level solve.
- **Master Audio Settings:** Full mute/unmute control accessible from both HUD and Pause menu.

### 3.2 Haptic Feedback Engine (`HapticManager`)
- Safe Android API 21-36 vibration handling (`Vibrator` with `VibrationEffect` fallback):
  - **Pickup / Drop:** Light 10ms tap.
  - **Rotation Detent:** Subtle 8ms micro-pulse on passing 15° notch intervals.
  - **Level Solved:** Rhythmic 3-pulse celebration vibration (40ms, pause 60ms, 80ms).
- Toggleable in Settings (default enabled).

---

## 4. Game Progression, Scoring & UI/UX

### 4.1 Scoring & 3-Star System
Each level records:
- **Solve Status:** Unlocked / Completed.
- **Star Rating (1 to 3 stars):**
  - ⭐ **1 Star:** Complete the silhouette (Match >= 85%).
  - ⭐⭐ **2 Stars:** Solve within Par Time (e.g., Level 1-5: 30s; Level 6-10: 45s; Level 11-15: 60s).
  - ⭐⭐⭐ **3 Stars:** Master Weaver (Solve within Par Time AND under Par Moves).
- **Best Time & Move Count:** Preserved across sessions in `SharedPreferences`.

### 4.2 HUD (Heads-Up Display)
- **Top Bar:**
  - Left: Pause/Settings button (Gear icon).
  - Center: Level Title & Category ("Level 3: Star").
  - Right: Circular Match Meter with progress percentage ring and glowing bead.
- **Bottom Bar:**
  - Left: Hint button (Lightbulb icon) — toggling on displays gentle directional arrows pointing toward solution alignment.
  - Right: Reset button (Restart icon) — quickly resets level shapes to initial scatter.

### 4.3 Menus & Overlays
- **Level Select Screen:**
  - Elegant modal grid of 15 levels.
  - Shows level number, title, earned stars (0-3), and locked/unlocked state.
  - Quick tap to jump to any unlocked level; close button to return to current puzzle.
- **Level Complete (Victory Modal):**
  - Frosted dark glass card with gold filigree border.
  - Animated star reveal (stars pop in with scale and chime).
  - Stats: Solved time vs Par time, Moves taken vs Par moves.
  - Buttons: "Next Level" (primary glowing CTA), "Replay", "Level Select".
- **Pause / Settings Modal:**
  - Resume button.
  - Restart Level button.
  - Sound Volume / Mute toggle.
  - Haptics On / Off toggle.
  - Level Select button.

---

## 5. Performance, Architecture & Reliability

- **Zero Allocation in Render Loop:** All particle pools, matrices, paths, and paint objects are pre-allocated and reused.
- **Display Refresh Rate Support:** Smooth 60Hz / 90Hz / 120Hz frame pacing with capped delta-time.
- **JVM Verification Harness:** All 15 levels remain 100% verified by `tools/LevelDesignTest.java` and `tools/LevelRetuneTest.java`.
