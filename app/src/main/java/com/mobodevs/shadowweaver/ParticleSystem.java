package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Shadow Weaver - ParticleSystem
 * ==============================
 * High-performance, zero-allocation particle engine for:
 *  1. Ambient dust and warm air motes drifting through the candle's glow.
 *  2. Dazzling golden celebration spark bursts upon solving a puzzle.
 *
 * All particle pools are pre-allocated at creation time. The per-frame
 * update and render methods allocate zero objects.
 */
public class ParticleSystem {

    // ---- Ambient Dust Motes ------------------------------------------------
    public static final int MAX_AMBIENT = 42;
    private final float[] ambientX = new float[MAX_AMBIENT];
    private final float[] ambientY = new float[MAX_AMBIENT];
    private final float[] ambientVx = new float[MAX_AMBIENT];
    private final float[] ambientVy = new float[MAX_AMBIENT];
    private final float[] ambientRadius = new float[MAX_AMBIENT];
    private final float[] ambientAlpha = new float[MAX_AMBIENT];
    private final float[] ambientPhase = new float[MAX_AMBIENT];

    // ---- Celebration Sparks -----------------------------------------------
    public static final int MAX_SPARKS = 72;
    private final float[] sparkX = new float[MAX_SPARKS];
    private final float[] sparkY = new float[MAX_SPARKS];
    private final float[] sparkVx = new float[MAX_SPARKS];
    private final float[] sparkVy = new float[MAX_SPARKS];
    private final float[] sparkSize = new float[MAX_SPARKS];
    private final float[] sparkLife = new float[MAX_SPARKS];     // 1.0 down to 0.0
    private final float[] sparkDecay = new float[MAX_SPARKS];
    private final int[] sparkColor = new int[MAX_SPARKS];

    private final float sceneW;
    private final float sceneH;
    private float globalTime;

    public ParticleSystem(float sceneW, float sceneH) {
        this.sceneW = sceneW;
        this.sceneH = sceneH;

        // Initialize ambient motes with varied starting positions and drifts
        long seed = 0x854329L;
        for (int i = 0; i < MAX_AMBIENT; i++) {
            seed = seed * 1664525L + 1013904223L;
            ambientX[i] = Math.abs(seed % (long) sceneW);
            seed = seed * 1664525L + 1013904223L;
            ambientY[i] = Math.abs(seed % (long) sceneH);
            seed = seed * 1664525L + 1013904223L;
            ambientVx[i] = ((seed % 1000) / 1000f - 0.5f) * 14f;
            seed = seed * 1664525L + 1013904223L;
            ambientVy[i] = -8f - ((seed % 1000) / 1000f) * 12f; // upward thermal drift
            seed = seed * 1664525L + 1013904223L;
            ambientRadius[i] = 1.8f + ((seed % 1000) / 1000f) * 2.8f;
            seed = seed * 1664525L + 1013904223L;
            ambientAlpha[i] = 0.20f + ((seed % 1000) / 1000f) * 0.35f;
            ambientPhase[i] = (float) (i * 0.45);
        }

        // Sparks start dormant
        for (int i = 0; i < MAX_SPARKS; i++) {
            sparkLife[i] = 0f;
        }
    }

    /**
     * Update simulation by dt seconds.
     */
    public void update(float dt, float candleX, float candleY, float flicker) {
        globalTime += dt;

        // Update ambient motes
        for (int i = 0; i < MAX_AMBIENT; i++) {
            // Gentle sinusoidal sway simulating thermal turbulence
            float sway = (float) Math.sin(globalTime * 1.5f + ambientPhase[i]) * 8f;
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

        // Update celebration sparks
        for (int i = 0; i < MAX_SPARKS; i++) {
            if (sparkLife[i] <= 0f) continue;

            sparkLife[i] -= sparkDecay[i] * dt;
            if (sparkLife[i] < 0f) sparkLife[i] = 0f;

            sparkX[i] += sparkVx[i] * dt;
            sparkY[i] += sparkVy[i] * dt;

            // Drag and slight gravity
            sparkVx[i] *= (1.0f - 1.8f * dt);
            sparkVy[i] = sparkVy[i] * (1.0f - 1.8f * dt) + 120f * dt;
        }
    }

    /**
     * Trigger a subtle spark burst for tactile snap assist.
     */
    public void burst(float cx, float cy, int count) {
        long seed = System.nanoTime();
        int[] palette = {0xFFFFD572, 0xFFFFE49E, 0xFFFFA834};
        int emitted = 0;
        for (int i = 0; i < MAX_SPARKS && emitted < count; i++) {
            if (sparkLife[i] > 0.3f) continue; // don't clobber active celebration sparks
            sparkX[i] = cx;
            sparkY[i] = cy;

            seed = seed * 1664525L + 1013904223L;
            double angle = (Math.abs(seed % 6283) / 1000.0);
            seed = seed * 1664525L + 1013904223L;
            float speed = 80f + (Math.abs(seed % 1000) / 1000f) * 160f;

            sparkVx[i] = (float) (Math.cos(angle) * speed);
            sparkVy[i] = (float) (Math.sin(angle) * speed) - 30f;

            sparkLife[i] = 0.8f;
            seed = seed * 1664525L + 1013904223L;
            sparkDecay[i] = 1.4f + (Math.abs(seed % 1000) / 1000f) * 0.8f; // fast fade: ~0.4s
            sparkSize[i] = 3.0f + (Math.abs(seed % 1000) / 1000f) * 3.0f;
            sparkColor[i] = palette[Math.abs((int) (seed % palette.length))];
            emitted++;
        }
    }

    /**
     * Trigger a burst of golden sparks from a central point.
     */
    public void burstCelebration(float cx, float cy) {
        long seed = System.nanoTime();
        int[] palette = {0xFFFFD572, 0xFFFFE49E, 0xFFFFA834, 0xFFFFF8E7, 0xFFFF851B};

        for (int i = 0; i < MAX_SPARKS; i++) {
            sparkX[i] = cx;
            sparkY[i] = cy;

            seed = seed * 1664525L + 1013904223L;
            double angle = (Math.abs(seed % 6283) / 1000.0);
            seed = seed * 1664525L + 1013904223L;
            float speed = 180f + (Math.abs(seed % 1000) / 1000f) * 480f;

            sparkVx[i] = (float) (Math.cos(angle) * speed);
            sparkVy[i] = (float) (Math.sin(angle) * speed) - 80f; // initial upward kick

            sparkLife[i] = 1.0f;
            seed = seed * 1664525L + 1013904223L;
            sparkDecay[i] = 0.55f + (Math.abs(seed % 1000) / 1000f) * 0.65f; // lasts 1.0 - 1.8s
            sparkSize[i] = 4.0f + (Math.abs(seed % 1000) / 1000f) * 5.0f;

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
            float proximity = 1.0f - Math.min(1.0f, distSq / (650f * 650f));
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
            int a = (int) (255 * Math.min(1.0f, life * 1.5f));
            paint.setColor((baseColor & 0x00FFFFFF) | (a << 24));

            float s = sparkSize[i] * (0.4f + 0.6f * life);
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
