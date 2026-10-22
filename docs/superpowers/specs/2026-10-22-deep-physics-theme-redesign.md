# Shadow Weaver: Deep Physics, Mechanics & Celestial Theme Redesign Specification

**Date:** 2026-10-22  
**Target:** Android (API 21+)  
**Architecture:** Pure Native Android (Hardware-accelerated SurfaceView + Canvas + AudioTrack + Vibrator), Zero External Dependencies  
**Theme:** *The Alchemist's Celestial Shadow Theater* (Antique Brass Astrolabes, Inlaid Walnut, Warm Amber Candlelight, Constellation Stained-Glass)  

---

## 1. Executive Summary & Design Pillars

This deep redesign transforms Shadow Weaver from a geometric manipulation tool into an atmospheric, tactile physics simulation and celestial puzzle masterpiece.

The transformation rests on three core pillars:

1. **Rigorous Physical Dynamics (Physics & Game Feel):**
   - **Spring-Mass-Damper Elastic Drag:** Moving tokens feel tethered by a damped harmonic spring with physical mass, velocity, acceleration, and micro-tilt.
   - **Inertial Momentum & Surface Friction:** Flicks glide with exponential viscous drag and bounce softly against stage boundaries.
   - **Angular Momentum & Rotational Inertia:** Rotating tokens have physical rotational mass and spin smoothly, clicking across magnetic detents with dampening.
   - **Soft-Body Token Repulsion:** Multiple tokens on the stage gently repel each other to prevent chaotic overlap and preserve tactile separation.

2. **Engaging Puzzle Mechanics (Playability & Rewarding Feedback):**
   - **Magnetic Alignment Resonance & Snap-Assist:** As a shadow nears its target silhouette (< 6% tolerance), it enters an acoustic and visual magnetic field. Releasing within the resonance threshold snaps it cleanly into position with a satisfying physical "thunk" and golden chime.
   - **Segmented Constellation Assembly:** Each aligned shape illuminates its portion of the target silhouette into glowing golden stained-glass filigree, giving players distinct "1 of N", "2 of N" progress milestones.
   - **Volumetric Light Cone & Depth Rays:** Real-time projected light cones from the candle through the token to the wall, visually demonstrating the physical ray optics.

3. **Celestial Alchemical Theme & Visual Splendor:**
   - **Inlaid Walnut & Brass Astrolabe Stage:** The stage is rendered as an antique astronomical workbench with engraved concentric coordinate rings, degree markings, and warm ambient occlusion.
   - **Crafted Obsidian & Brass Relics:** Tokens feature multi-pass beveled metallic rims, directionally shaded faces with specular light highlights, central brass pivot rivets, and illuminated constellation markers.
   - **Living Candle & Volumetric God Rays:** Multi-layer flame with realistic blue base, white mantle, amber halo, and rising smoky particles.
   - **Golden Constellation Solve Transformation:** Solved silhouettes ignite into glowing constellations with animated star nodes and harmonic choral arpeggios.

---

## 2. Physics & Motion Architecture

### 2.1 Spring-Mass-Damper Drag Model
When the player touches a token, instead of hard-clamping its position to the finger coordinates:
$$\vec{F}_{\text{spring}} = -k \cdot (\vec{x} - \vec{x}_{\text{finger}}) - c \cdot \vec{v}$$
$$\vec{a} = \frac{\vec{F}}{m}$$
$$\vec{v} \leftarrow \vec{v} + \vec{a} \cdot \Delta t$$
$$\vec{x} \leftarrow \vec{x} + \vec{v} \cdot \Delta t$$
- **Spring Stiffness $k$:** `180.0f` (tight, responsive, yet buttery and organic)
- **Damping Coefficient $c$:** `18.0f` (critically damped, zero wild overshoot)
- **Token Mass $m$:** `1.0f` (scaled by shape area)

### 2.2 Rotational Inertia & Detents
When rotating:
$$\tau = -k_{\theta} \cdot (\theta - \theta_{\text{gesture}}) - c_{\theta} \cdot \omega$$
- Released with angular velocity $\omega$: spins freely with exponential decay $\omega \leftarrow \omega \cdot (1 - \mu_{\theta} \cdot \Delta t)$.
- When passing $15^\circ$ detents: triggers micro-haptic pulse and acoustic wooden click. If $\omega$ is below a low threshold near a detent, soft magnetic detent attraction draws $\theta$ smoothly to the nearest notch.

### 2.3 Boundary Restitution
When a token glides into the stage boundary:
$$v_{\perp} \leftarrow -e \cdot v_{\perp}$$
Where $e = 0.28f$ (soft wooden rim bounce).

### 2.4 Soft Inter-Token Repulsion
Between any two tokens $i$ and $j$:
$$\text{dist} = \|\vec{x}_i - \vec{x}_j\|$$
$$\text{minDist} = r_i + r_j$$
If $\text{dist} < \text{minDist}$, a gentle repulsive force separates them:
$$\vec{F}_{\text{repel}} = \hat{n} \cdot (\text{minDist} - \text{dist}) \cdot k_{\text{repel}}$$

---

## 3. Game Mechanics & Player Feedback

### 3.1 Proximity Resonance & Snap-Assist
For each token $i$, calculate alignment error to its solution:
$$\Delta x = |x_i - x_{\text{sol}}|$$
$$\Delta y = |y_i - y_{\text{sol}}|$$
$$\Delta \theta = \min(|\theta_i - \theta_{\text{sol}}|, 360^\circ - |\theta_i - \theta_{\text{sol}}|)$$
- **Near Field ($\text{error} < 12\%$):**
  - Token emits subtle golden aura.
  - Shadow outline begins pulsating in sync with flame flicker.
  - Procedural harmonic drone pitch shifts upward.
- **Lock Field ($\text{error} < 4\%$):**
  - Golden magnetic tether connects token to solution.
  - On finger release: token gently snaps into exact solution coordinates!
  - Plays resonant chime and triggers haptic satisfaction pulse.

### 3.2 Segmented Silhouette Illumination
- The silhouette is partitioned into the contributions of each shape.
- As each individual shape enters alignment, its corresponding silhouette portion lights up into warm golden stained glass with a glowing perimeter.
- Solved state activates full constellation star map reveal!

---

## 4. Visual Art Direction & Thematic Overhaul

1. **Backdrop & Atmosphere:**
   - Deep rich midnight-umber vignette (`#1A1009` to `#0B0604`).
   - Floating ember/dust particle field with thermal updraft.
   - Faint astrolabe celestial star chart etched onto the background.
2. **Candle & Volumetric Lighting:**
   - Volumetric golden god rays radiating downward across the chamber.
   - Dynamic candle with flame core, flickering amber corona, and candle wax drips.
3. **The Shadow Wall (The Shoji / Parchment Screen):**
   - Rich parchment texture with illuminated wooden frame.
   - Multi-layer Minkowski dilation shadows with warm penumbra falloff.
4. **The Celestial Tokens:**
   - Rich obsidian base with brushed brass borders.
   - Center brass astrolabe pivot with etched sun/moon glyphs.
   - Rotary dial with fine $15^\circ$ etched ticks, $45^\circ/90^\circ$ cardinal emblems, and an illuminated amber knob.
5. **HUD & Menus:**
   - Clean antique brass framing, radial match meter with celestial sun icon, and minimalist pause/hint/reset controls.
