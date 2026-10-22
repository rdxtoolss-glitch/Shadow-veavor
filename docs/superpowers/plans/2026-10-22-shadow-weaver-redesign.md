# Shadow Weaver AAA Commercial Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Shadow Weaver into an industry-grade, commercial mobile puzzle game with atmospheric lighting, 2.5D tactile puppets, procedural audio harmonics, haptic feedback, modern HUD controls, 3-star level progression, and polished modal overlays.

**Architecture:** Pure native Android architecture with hardware-accelerated SurfaceView + Canvas rendering, procedural audio synthesis via AudioTrack, device vibration via Vibrator, zero third-party dependencies, and zero-allocation per frame.

**Tech Stack:** Java 8/17/21+, Android SDK (API 21+), Canvas/SurfaceView, AudioTrack, SharedPreferences, Vibrator.

**Spec:** `docs/superpowers/specs/2026-10-22-shadow-weaver-redesign.md`

## Global Constraints

- **Platform & SDK:** Pure Android Java, `minSdkVersion 21`, `targetSdkVersion 35/36`, compile against `android-35`.
- **Zero Third-Party Dependencies:** Only `android.*` and standard Java libraries. No external Maven/Gradle libraries, no AdMob, no heavy frameworks.
- **Render Loop Integrity:** Zero per-frame object allocation (`new`) in inner render/simulation loops. Pre-allocate all paint, paths, matrices, and particle pools.
- **Level Solvability Guarantee:** All 15 existing levels must maintain 100% solvability and pass `tools/LevelDesignTest.java` and `tools/LevelRetuneTest.java`.
- **Persistence Contract:** All unlocked levels, star ratings (1-3 stars), best times, and user preferences (Audio on/off, Haptics on/off) must persist reliably in `SharedPreferences`.

## Review Focus

1. **Multi-Touch & Gesture Race Conditions:** Rapidly switching between 1-finger drag, 2-finger rotate, button taps, and pause overlay must not cause sticky drags or jumps.
2. **Display Density & Aspect Ratio Scaling:** Scene scaling (1000x1600 scene) must look crisp and centered across tablets, tall aspect ratio phones (19.5:9, 20:9), and landscape foldables.
3. **AudioTrack Lifecycle & Backgrounding:** Pausing, locking the phone, or switching apps must immediately silence synthesized audio without stutters or crashes.
4. **Haptic API Compatibility:** Older devices (API 21-25) without `VibrationEffect` must fall back gracefully to `vibrate(long)` without crashing.
5. **Level Solution Precision:** Adding visual elevation and beveled borders must not shift the underlying mathematical shadow vertices evaluated by `SilhouetteMatcher`.

---

### Task 1: Haptic & Audio Engine Enhancements

**Files:**
- Create: `app/src/main/java/com/mobodevs/shadowweaver/HapticManager.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/AudioManager.java`
- Modify: `app/src/main/AndroidManifest.xml` (add `<uses-permission android:name="android.permission.VIBRATE" />`)
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/MainActivity.java`

- [ ] **Step 1:** Add the `android.permission.VIBRATE` permission to `AndroidManifest.xml`.
- [ ] **Step 2:** Create `HapticManager.java` with safe API 21-36 support for light tap (pickup/drop), detent click (rotation notches), and victory pulse sequence, including enabled/disabled state toggling.
- [ ] **Step 3:** Update `AudioManager.java` with:
  - Procedural sound synthesis for rotation ticks, grab woosh, and celebration arpeggios.
  - Harmonic resonance milestone chimes (triggered at 25%, 50%, 75%, 85% match milestones).
  - Master mute/unmute toggle method and state persistence.
- [ ] **Step 4:** Wire `HapticManager` and updated `AudioManager` into `MainActivity.java` and initialize with saved preferences.
- [ ] **Step 5:** Verify compilation of `HapticManager` and `AudioManager` against `android.jar`.

---

### Task 2: Particle & FX Systems (Ambient Dust & Celebration Sparks)

**Files:**
- Create: `app/src/main/java/com/mobodevs/shadowweaver/ParticleSystem.java`
- Create: `tools/ParticleSystemTest.java`

- [ ] **Step 1:** Write `tools/ParticleSystemTest.java` testing particle allocation, update steps, recycling, and zero memory leaks.
- [ ] **Step 2:** Run `ParticleSystemTest` to verify the test runs.
- [ ] **Step 3:** Implement `ParticleSystem.java`:
  - Ambient dust motes pool (30-40 particles) drifting with Brownian motion and candle light illumination factor.
  - Celebration burst pool (60 golden spark particles) exploding from the solved silhouette center with velocity decay, gravity, and sparkling alpha.
- [ ] **Step 4:** Run `ParticleSystemTest` to confirm all tests pass.

---

### Task 3: 2.5D Tactile Puppets & Precision Mechanical Rotation Dial

**Files:**
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/ShapeObject.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameRenderer.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameThread.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameView.java`

- [ ] **Step 1:** Update `ShapeObject.java` with elevation factor (smooth lift on grab), drag state, and move counting.
- [ ] **Step 2:** Update `GameRenderer.java` `drawShapes()` to render:
  - Ambient stage drop shadow underneath tokens based on elevation.
  - Tactile beveled face fill with directional specular highlights facing the candle.
  - Polished mechanical rotation ring with 15° tick marks, cardinal 45°/90° badges, and a glowing rotary handle.
- [ ] **Step 3:** Update `GameView.java` and `GameThread.java` with rotation dial handle hit-testing, rotational detent detection (triggering haptic and audio tick every 15°), and elevation smoothing.
- [ ] **Step 4:** Run `tools/LevelDesignTest.java` to ensure shape physics and math stay 100% compliant and all 15 levels pass.

---

### Task 4: In-Game Controls, Modern HUD & Pause/Settings Modal

**Files:**
- Create: `app/src/main/java/com/mobodevs/shadowweaver/PauseOverlay.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameRenderer.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameThread.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameView.java`

- [ ] **Step 1:** Create `PauseOverlay.java` with frosted glass aesthetic, drawing buttons for Resume, Restart, Audio Mute toggle, Haptic toggle, and Level Select.
- [ ] **Step 2:** Upgrade `GameRenderer.java` `drawHud()`:
  - Top bar: Pause gear button (top-left), level title/chapter (center), circular match progress ring with percentage and glowing bead (top-right).
  - Bottom bar: Hint toggle button (bottom-left) and Reset level button (bottom-right).
- [ ] **Step 3:** Wire Pause overlay, Hint toggle, and Reset actions in `GameThread.java` and `GameView.java`.
- [ ] **Step 4:** Verify hit-testing logic and overlay transitions.

---

### Task 5: 3-Star Rating Progression & Modern Level Select

**Files:**
- Create: `app/src/main/java/com/mobodevs/shadowweaver/LevelProgress.java`
- Create: `tools/LevelProgressTest.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/LevelSelectOverlay.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/MainActivity.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameThread.java`

- [ ] **Step 1:** Write `tools/LevelProgressTest.java` to test star calculation formulas (1 star = solved, 2 stars = within par time, 3 stars = par time + par moves) and serialization.
- [ ] **Step 2:** Implement `LevelProgress.java` with par times, par moves, star calculation, and SharedPreferences saving/loading.
- [ ] **Step 3:** Run `LevelProgressTest` to ensure calculations pass.
- [ ] **Step 4:** Redesign `LevelSelectOverlay.java` with elegant chapter header, 15-level card grid displaying star ratings, locked padlocks, and smooth tap selection.
- [ ] **Step 5:** Connect `LevelProgress` with `MainActivity.java` and `GameThread.java`.

---

### Task 6: Victory Overlay & Golden Silhouette Celebration

**Files:**
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/LevelCompleteOverlay.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameRenderer.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameThread.java`

- [ ] **Step 1:** Redesign `LevelCompleteOverlay.java`:
  - Frosted glass modal card with gold border.
  - Animated 1-to-3 star pop-in reveal.
  - Solve time vs Par time, Moves taken vs Par moves.
  - Tactile action buttons: "Next Level" (primary glowing CTA), "Replay", "Level Select".
- [ ] **Step 2:** In `GameRenderer.java`:
  - When level solves, trigger golden silhouette flash and celebrate with `ParticleSystem` golden sparks.
  - Draw the upgraded `LevelCompleteOverlay`.
- [ ] **Step 3:** Trigger celebratory audio arpeggio and haptic vibration sequence in `GameThread.java`.
- [ ] **Step 4:** Verify overlay layout and hit-testing.

---

### Task 7: Full System Integration, Build Verification & Git Commit

**Files:**
- Modify: `build.gradle` / `app/build.gradle` (Toolchain modernization & compileSdk alignment)
- Verify: All modified and new files

- [ ] **Step 1:** Run `tools/LevelDesignTest.java` and `tools/LevelRetuneTest.java` to guarantee all 15 levels pass.
- [ ] **Step 2:** Compile the entire project against Android SDK `android.jar` to ensure zero compilation errors or missing symbols.
- [ ] **Step 3:** Test and verify build configurations.
- [ ] **Step 4:** Review git diff and commit the complete redesign with a clear descriptive message.
- [ ] **Step 5:** Push the updated codebase to GitHub remote `main`.
