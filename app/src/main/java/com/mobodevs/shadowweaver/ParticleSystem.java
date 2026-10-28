package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Shadow Weaver - ParticleSystem
 * ==============================
 * High-performance, zero-allocation particle engine for:
 *  1. Ambient dust and warm air motes drifting through the candle's glow.
 *  2. Subtle tactile pickup dust when lifting a shape.
 *  3. Dazzling golden snap starbursts when locking a shape into place.
 *  4. Grand victory fireworks upon completing a silhouette trial.
 *
 * All particle pools are pre-allocated at creation time. The per-frame
 * update and render methods allocate zero objects.
 */
public class ParticleSystem {

    // ---- Ambient Dust Motes ------------------------------------------------
    public static final int MAX_AMBIENT = 70;
    private final float[] ambientX = new float[MAX_AMBIENT];
    private final float[] ambientY = new float[MAX_AMBIENT];
    private final float[] ambientVx = new float[MAX_AMBIENT];
    private final float[] ambientVy = new float[MAX_AMBIENT];
    private final float[] ambientRadius = new float[MAX_AMBIENT];
    private final float[] ambientAlpha = new float[MAX_AMBIENT];
    private final float[] ambientPhase = new float[MAX_AMBIENT];

    // ---- Sparks & Starbursts ----------------------------------------------
    public static final int MAX_SPARKS = 128;
    private final float[] sparkX = new float[MAX_SPARKS];
    private final float[] sparkY = new float[MAX_SPARKS];
    private final float[] sparkVx = new float[MAX_SPARKS];
    private final float[] sparkVy = new float[MAX_SPARKS];
    private final float[] sparkSize = new float[MAX_SPARKS];
    private final float[] sparkLife = new float[MAX_SPARKS];     // 1.0 down to 0.0
    private final float[] sparkDecay = new float[MAX_SPARKS];
    private final int[] sparkColor = new int[MAX_SPARKS];

    private float sceneW;
    private float sceneH;
    private float globalTime;

    public ParticleSystem(float sceneW, float sceneH) {
        this.sceneW = sceneW;
        this.sceneH = sceneH;

        // Initialize ambient motes with varied starting positions and drifts
        long seed = 0x854329L;
        for (int i = 0; i < MAX_AMBIENT; i++) {
            seed = seed * 1664525L + 1013904223L;
            ambientX[i] = Math.abs(seed % (long) Math.max(1f, sceneW));
            seed = seed * 1664525L + 1013904223L;
            ambientY[i] = Math.abs(seed % (long) Math.max(1f, sceneH));
            seed = seed * 1664525L + 1013904223L;
            ambientVx[i] = ((seed % 1000) / 1000f - 0.5f) * 16f;
            seed = seed * 1664525L + 1013904223L;
            ambientVy[i] = -10f - ((seed % 1000) / 1000f) * 16f; // upward thermal drift
            seed = seed * 1664525L + 1013904223L;
            // Larger, visible particles on high-DPI smartphone displays
            ambientRadius[i] = 3.2f + ((seed % 1000) / 1000f) * 4.4f;
            seed = seed * 1664525L + 1013904223L;
            ambientAlpha[i] = 0.35f + ((seed % 1000) / 1000f) * 0.40f;
            ambientPhase[i] = (float) (i * 0.45);
        }

        // Sparks start dormant
        for (int i = 0; i < MAX_SPARKS; i++) {
            sparkLife[i] = 0f;
        }
    }

    public void updateDimensions(float sceneW, float sceneH) {
        if (sceneW > 0f) this.sceneW = sceneW;
        if (sceneH > 0f) this.sceneH = sceneH;
    }

    /**
     * Update simulation by dt seconds.
     */
    public void update(float dt, float candleX, float candleY, float flicker) {
        globalTime += dt;

        // Update ambient motes
        for (int i = 0; i < MAX_AMBIENT; i++) {
            // Gentle sinusoidal sway simulating thermal air currents
            float sway = (float) Math.sin(globalTime * 1.6f + ambientPhase[i]) * 10f;
            ambientX[i] += (ambientVx[i] + sway) * dt;
            ambientY[i] += ambientVy[i] * dt;

            // Wrap edges gracefully
            if (ambientY[i] < -20f) {
                ambientY[i] = sceneH + 10f;
            } else if (ambientY[i] > sceneH + 20f) {
                ambientY[i] = -10f;
            }
            if (ambientX[i] < -20f) {
                ambientX[i] = sceneW + 10f;
            } else if (ambientX[i] > sceneW + 20f) {
                ambientX[i] = -10f;
            }
        }

        // Update celebration and interaction sparks
        for (int i = 0; i < MAX_SPARKS; i++) {
            if (sparkLife[i] <= 0f) continue;

            sparkLife[i] -= sparkDecay[i] * dt;
            if (sparkLife[i] < 0f) sparkLife[i] = 0f;

            sparkX[i] += sparkVx[i] * dt;
            sparkY[i] += sparkVy[i] * dt;

            // Drag and gravity
            sparkVx[i] *= (1.0f - 1.6f * dt);
            sparkVy[i] = sparkVy[i] * (1.0f - 1.6f * dt) + 110f * dt;
        }
    }

    /**
     * Trigger a subtle spark burst for shape pickup.
     */
    public void burstPickup(float cx, float cy) {
        burst(cx, cy, 10);
    }

    /**
     * Trigger a burst of sparks with specified count.
     */
    public void burst(float cx, float cy, int count) {
        long seed = System.nanoTime();
        int[] palette = {0xFFFFD572, 0xFFFFE49E, 0xFFFFA834, 0xFFFFF8E7};
        int emitted = 0;
        for (int i = 0; i < MAX_SPARKS && emitted < count; i++) {
            if (sparkLife[i] > 0.35f) continue; // don't clobber active celebration sparks
            sparkX[i] = cx;
            sparkY[i] = cy;

            seed = seed * 1664525L + 1013904223L;
            double angle = (Math.abs(seed % 6283) / 1000.0);
            seed = seed * 1664525L + 1013904223L;
            float speed = 70f + (Math.abs(seed % 1000) / 1000f) * 160f;

            sparkVx[i] = (float) (Math.cos(angle) * speed);
            sparkVy[i] = (float) (Math.sin(angle) * speed) - 40f; // slight upward lift

            sparkLife[i] = 0.85f;
            seed = seed * 1664525L + 1013904223L;
            sparkDecay[i] = 1.2f + (Math.abs(seed % 1000) / 1000f) * 0.8f; // ~0.5s fade
            sparkSize[i] = 4.0f + (Math.abs(seed % 1000) / 1000f) * 4.0f;
            sparkColor[i] = palette[Math.abs((int) (seed % palette.length))];
            emitted++;
        }
    }

    /**
     * Trigger a radiant starburst when locking into solution.
     */
    public void burstSnap(float cx, float cy) {
        long seed = System.nanoTime();
        int[] palette = {0xFFFFD54F, 0xFFFFE082, 0xFFFFA000, 0xFFFFFFFF};
        int emitted = 0;
        int count = 28;
        for (int i = 0; i < MAX_SPARKS && emitted < count; i++) {
            if (sparkLife[i] > 0.5f) continue;
            sparkX[i] = cx;
            sparkY[i] = cy;

            seed = seed * 1664525L + 1013904223L;
            double angle = (Math.abs(seed % 6283) / 1000.0);
            seed = seed * 1664525L + 1013904223L;
            float speed = 120f + (Math.abs(seed % 1000) / 1000f) * 280f;

            sparkVx[i] = (float) (Math.cos(angle) * speed);
            sparkVy[i] = (float) (Math.sin(angle) * speed) - 60f;

            sparkLife[i] = 1.0f;
            seed = seed * 1664525L + 1013904223L;
            sparkDecay[i] = 0.85f + (Math.abs(seed % 1000) / 1000f) * 0.60f; // ~1.0s lifespan
            sparkSize[i] = 5.0f + (Math.abs(seed % 1000) / 1000f) * 5.0f;
            sparkColor[i] = palette[Math.abs((int) (seed % palette.length))];
            emitted++;
        }
    }

    /**
     * Trigger a grand burst of golden sparks from a central point on puzzle completion.
     */
    public void burstCelebration(float cx, float cy) {
        long seed = System.nanoTime();
        int[] palette = {0xFFFFD572, 0xFFFFE49E, 0xFFFFA834, 0xFFFFF8E7, 0xFFFF851B, 0xFFFFFFFF};

        for (int i = 0; i < MAX_SPARKS; i++) {
            sparkX[i] = cx;
            sparkY[i] = cy;

            seed = seed * 1664525L + 1013904223L;
            double angle = (Math.abs(seed % 6283) / 1000.0);
            seed = seed * 1664525L + 1013904223L;
            float speed = 160f + (Math.abs(seed % 1000) / 1000f) * 450f;

            sparkVx[i] = (float) (Math.cos(angle) * speed);
            sparkVy[i] = (float) (Math.sin(angle) * speed) - 90f; // initial upward kick

            sparkLife[i] = 1.0f;
            seed = seed * 1664525L + 1013904223L;
            sparkDecay[i] = 0.50f + (Math.abs(seed % 1000) / 1000f) * 0.60f; // lasts 1.2 - 2.0s
            sparkSize[i] = 4.5f + (Math.abs(seed % 1000) / 1000f) * 5.5f;

            sparkColor[i] = palette[Math.abs((int) (seed % palette.length))];
        }
    }

    // ---- Render methods ----------------------------------------------------

    public void drawAmbient(Canvas c, Paint paint, float candleX, float candleY, float flicker) {
        for (int i = 0; i < MAX_AMBIENT; i++) {
            float x = ambientX[i];
            float y = ambientY[i];

            // Dust particles illuminate warmer and brighter when near the candle
            float dx = x - candleX;
            float dy = y - candleY;
            float distSq = dx * dx + dy * dy;
            float proximity = 1.0f - Math.min(1.0f, distSq / (700f * 700f));
            float alpha = (ambientAlpha[i] + proximity * 0.45f) * flicker;
            if (alpha > 1.0f) alpha = 1.0f;

            int a = (int) (255 * alpha);
            paint.setColor((a << 24) | 0x00FFE3B0);
            c.drawCircle(x, y, ambientRadius[i], paint);
        }
    }

    public void drawSparks(Canvas c, Paint paint) {
        for (int i = 0; i < MAX_SPARKS; i++) {
            if (sparkLife[i] <= 0f) continue;

            float life = sparkLife[i];
            int baseColor = sparkColor[i];
            int a = (int) (255 * Math.min(1.0f, life * 1.4f));

            // Soft outer halo
            paint.setColor((baseColor & 0x00FFFFFF) | ((a / 3) << 24));
            float halo = sparkSize[i] * (0.6f + 0.8f * life) * 1.8f;
            c.drawCircle(sparkX[i], sparkY[i], halo, paint);

            // Radiant core
            paint.setColor((baseColor & 0x00FFFFFF) | (a << 24));
            float s = sparkSize[i] * (0.5f + 0.5f * life);
            c.drawCircle(sparkX[i], sparkY[i], s, paint);
        }
    }

    // ---- Harness Inspection Methods ----------------------------------------

    public int ambientCount() {
        return MAX_AMBIENT;
    }

    public float getAmbientX(int i) {
        return ambientX[i];
    }

    public float getAmbientY(int i) {
        return ambientY[i];
    }

    public int activeSparksCount() {
        int count = 0;
        for (int i = 0; i < MAX_SPARKS; i++) {
            if (sparkLife[i] > 0f) count++;
        }
        return count;
    }
}
