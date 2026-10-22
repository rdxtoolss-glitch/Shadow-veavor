# Deep Physics, Mechanics & Celestial Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a comprehensive physics simulation (spring-mass-damper drag, momentum, angular inertia, soft repulsion, magnetic snap-assist) and transform the visual theme into an antique celestial astrolabe theater with volumetric lighting and constellation reveals.

**Architecture:** Pure native Android SurfaceView + Canvas with zero third-party dependencies, 60-120fps symplectic physical integration, zero garbage per frame, and regression-tested mathematical solvability.

**Tech Stack:** Java, Android SDK (API 21+), Canvas/SurfaceView, AudioTrack, Vibrator.

**Spec:** `docs/superpowers/specs/2026-10-22-deep-physics-theme-redesign.md`

## Global Constraints

- **Platform & SDK:** Pure Android Java, `minSdkVersion 21`, `compileSdkVersion 35`.
- **Zero Third-Party Dependencies:** Only `android.*` and standard Java libraries.
- **Render Loop Integrity:** Zero per-frame memory allocation (`new`) in inner render and physics loops.
- **Level Solvability Guarantee:** All 15 existing levels must remain 100% solvable and pass all regression tests.

## Review Focus

1. **Physics Numerical Stability:** Fast erratic finger movements must not cause runaway velocities, NaN coordinates, or shape tunnelling through boundaries.
2. **Magnetic Snap Precision:** Snapping must strictly aid intended placement and never override player control or snap to incorrect orientations.
3. **Multi-Touch Conflicts:** Seamless transition between single-finger drag, dial knob spin, and two-finger pinch.
4. **Performance Pacing:** 60-120fps lockless rendering on mobile hardware.
5. **Acoustic Harmony:** Dynamic resonance tones must blend smoothly into the ambient bed without digital clipping.

---

### Task 1: Physics Engine Simulation

**Files:**
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/ShapeObject.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameThread.java`
- Create: `tools/PhysicsSimulationTest.java`

- [x] **Step 1:** Write `tools/PhysicsSimulationTest.java` to test spring-mass drag damping, inertial glide decay, boundary bounce restitution, and inter-token repulsion.
- [x] **Step 2:** Run `PhysicsSimulationTest` to verify it runs and catches missing physics parameters.
- [x] **Step 3:** Implement physical dynamics in `ShapeObject.java`:
  - Target tracking point `(targetX, targetY)`
  - Velocity `(vx, vy)` and acceleration `(ax, ay)`
  - Angular velocity `angularVelocity` and angular target
  - Mass, drag damping factor, and tilt angles `(tiltX, tiltY)`
- [x] **Step 4:** Implement physics update loop in `GameThread.java`:
  - Symplectic Euler integration for spring drag
  - Boundary collision with restitution bounce
  - Exponential surface friction decay
  - Inter-token soft repulsion
- [x] **Step 5:** Run `PhysicsSimulationTest` and confirm all tests pass.

---

### Task 2: Magnetic Resonance & Snap-Assist Mechanic

**Files:**
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/ShapeObject.java`
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameThread.java`
- Create: `tools/SnapAssistTest.java`

- [x] **Step 1:** Write `tools/SnapAssistTest.java` testing proximity calculation to solution targets and snap locking.
- [x] **Step 2:** Implement per-shape solution tracking and alignment calculation in `GameThread.java`.
- [x] **Step 3:** Implement magnetic resonance zone and release-to-snap assist logic in `GameThread.java`.
- [x] **Step 4:** Run `SnapAssistTest` and confirm tests pass.

---

### Task 3: Celestial Astrolabe Theme & Visual Rendering

**Files:**
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/GameRenderer.java`

- [x] **Step 1:** Render the Astrolabe Workbench stage with inlaid concentric celestial coordinate rings, radiant compass rose, and degree markings.
- [x] **Step 2:** Render volumetric golden god rays radiating downward from the candle flame.
- [x] **Step 3:** Render the living candle flame with realistic multi-lobe teardrop (blue inner wick, white core, warm amber halo).
- [x] **Step 4:** Render 2.5D crafted obsidian and brass tokens with directional bevel shading, specular light reflections, and center astrolabe pivot rivets.
- [x] **Step 5:** Render magnetic resonance tethers, proximity halos, and glowing segmented silhouette illumination on puzzle solve.

---

### Task 4: Interactive Adaptive Audio Enhancements

**Files:**
- Modify: `app/src/main/java/com/mobodevs/shadowweaver/AudioManager.java`

- [x] **Step 1:** Add procedural magnetic snap "thunk" sound effect.
- [x] **Step 2:** Add proximity resonance tone feedback.
- [x] **Step 3:** Verify audio buffer synthesis against `android.jar`.

---

### Task 5: End-to-End Verification, APK Assembly, and Push

**Files:**
- Verify: All files and test suites

- [x] **Step 1:** Run all JVM test suites (`LevelDesignTest`, `LevelRetuneTest`, `ParticleSystemTest`, `LevelProgressTest`, `PhysicsSimulationTest`, `SnapAssistTest`).
- [x] **Step 2:** Assemble debug APK with `./gradlew.bat assembleDebug` and update root `ShadowWeaver.apk`.
- [x] **Step 3:** Commit all changes with a comprehensive descriptive commit message and push to GitHub `origin/main`.
