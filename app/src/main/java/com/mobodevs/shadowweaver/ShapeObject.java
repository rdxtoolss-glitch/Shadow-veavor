package com.mobodevs.shadowweaver;

/**
 * Shadow Weaver - ShapeObject
 * ===========================
 * ROLE IN THE GAME
 * A single manipulable "puppet" that floats on the stage between the candle
 * (light source) and the wall. Every level is made of 3-6 of these.
 *
 * DESIGN NOTE - WHY THIS CLASS HAS NO android.* IMPORTS
 * ShapeObject is the *data model* of a shape, and deliberately it is written
 * as pure Java (plain float[] arrays instead of PointF/Path). The silhouette
 * maths is therefore unit-testable on a plain JVM (see tools/LevelForgeTest),
 * which is how the 15 shipped levels were verified to be solvable. The
 * android.graphics.Path is only built later, in GameView, when it is time to
 * actually draw.
 *
 * A shape knows:
 *   - its kind   : CIRCLE / TRIANGLE / SQUARE / RECT / LINE (a thin rod)
 *   - its size   : full width + full height in scene units, measured on its
 *                  own local axes (before rotation)
 *   - where it is: x/y in SCENE space (see ShadowCalculator for the space
 *                  definitions) and its rotation in degrees, clockwise
 *
 * It does NOT know anything about shadows; that is ShadowCalculator's job.
 */
public class ShapeObject {

    public enum Kind {
        CIRCLE,
        TRIANGLE,
        SQUARE,
        RECT,
        LINE
    }

    /** How many points approximate a circle. 48 is smooth but still cheap. */
    public static final int CIRCLE_SEGMENTS = 48;

    /** Wrap any angle into [0,360) so rotation can never drift over a long session. */
    public static float normalizeAngle(float deg) {
        float a = deg % 360f;
        if (a < 0f) a += 360f;
        return a;
    }

    public Kind kind;
    /** Full width in scene units, measured on the shape's un-rotated x axis. */
    public float width;
    /** Full height in scene units, measured on the shape's un-rotated y axis. */
    public float height;
    /** Centre of the shape in scene space. */
    public float x;
    public float y;
    /** Clockwise rotation in degrees, around the shape centre. */
    public float rotation;

    /**
     * Tangential drag velocity, in scene units per second. Used only to give
     * the shapes a gentle "floating" drift when they are released. Purely
     * cosmetic - it never affects the match outcome.
     */
    public float vx;
    public float vy;
    /** Rotational drift, degrees per second. Cosmetic only, same as above. */
    public float spin;

    /**
     * Visual elevation factor (0.0 = resting on stage, 1.0 = lifted on touch).
     * Drives drop shadow offset/blur, scale lift, and tactile lighting.
     */
    public float elevation;
    public float targetElevation;
    public boolean isDragging;
    public int lastDetentIndex = -1;

    public void updateElevation(float dt) {
        float speed = 16f;
        elevation += (targetElevation - elevation) * Math.min(1.0f, dt * speed);
    }

    public ShapeObject(Kind kind, float width, float height, float x, float y, float rotation) {
        this.kind = kind;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    /** Corner-to-corner radius of the un-rotated shape; handy for hit tests. */
    public float baseRadius() {
        float hw = width * 0.5f;
        float hh = height * 0.5f;
        return (float) Math.sqrt(hw * hw + hh * hh);
    }

    /** Largest distance from the centre to any silhouette vertex, rotated. */
    public float maxRadius() {
        float[] local = localSilhouette();
        float best = 0f;
        for (int i = 0; i < local.length; i += 2) {
            float r = (float) Math.sqrt(local[i] * local[i] + local[i + 1] * local[i + 1]);
            if (r > best) best = r;
        }
        return best;
    }

    /**
     * STEP 1 OF THE SHADOW PIPELINE - the shape's own silhouette.
     *
     * Returns the outline as a flat [x0,y0, x1,y1, ...] array, centred on the
     * origin, pointing UP (negative y is up, matching screen coordinates), and
     * NOT yet rotated. Every shape kind is approximated by a convex polygon;
     * the circle uses enough points that it looks perfectly round once drawn.
     *
     * The rest of the pipeline is: localSilhouette() -> applyTransform() ->
     * ShadowCalculator.project(). Nothing else touches the geometry, which
     * keeps the maths in one place.
     */
    public float[] localSilhouette() {
        switch (kind) {
            case CIRCLE: {
                int n = CIRCLE_SEGMENTS;
                float[] p = new float[n * 2];
                float r = width * 0.5f;
                for (int i = 0; i < n; i++) {
                    double a = 2.0 * Math.PI * i / n;
                    p[i * 2] = (float) (r * Math.cos(a));
                    p[i * 2 + 1] = (float) (-r * Math.sin(a));
                }
                return p;
            }
            case TRIANGLE:
                // Upright, isosceles: apex on top, flat base at the bottom.
                return new float[]{
                        0f, -height * 0.5f,
                        width * 0.5f, height * 0.5f,
                        -width * 0.5f, height * 0.5f
                };
            case SQUARE: {
                float hw = width * 0.5f;
                return new float[]{
                        -hw, -hw, hw, -hw, hw, hw, -hw, hw
                };
            }
            case RECT: {
                float hw = width * 0.5f;
                float hh = height * 0.5f;
                return new float[]{
                        -hw, -hh, hw, -hh, hw, hh, -hw, hh
                };
            }
            case LINE:
            default: {
                // A rod is a very thin, long rectangle so that it still has a
                // measurable area when rasterised for the match test.
                float hw = width * 0.5f;
                float hh = height * 0.5f;
                return new float[]{
                        -hw, -hh, hw, -hh, hw, hh, -hw, hh
                };
            }
        }
    }

    /**
     * STEP 2 OF THE PIPELINE - rotate + move the silhouette into scene space.
     * Rotation is clockwise, identical to Canvas.rotate(), so the shape the
     * player sees and the shape the maths uses can never drift apart.
     */
    public float[] applyTransform() {
        float[] local = localSilhouette();
        double rad = Math.toRadians(rotation);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float[] out = new float[local.length];
        for (int i = 0; i < local.length; i += 2) {
            float lx = local[i];
            float ly = local[i + 1];
            out[i] = lx * cos - ly * sin + x;
            out[i + 1] = lx * sin + ly * cos + y;
        }
        return out;
    }

    public float copyRotation() {
        return rotation;
    }

    /** Normalise rotation into [0,360) so long sessions cannot drift. */
    public void normalizeRotation() {
        rotation = normalizeAngle(rotation);
    }

    /** Deep-ish copy, used when re-seeding a level's starting layout. */
    public ShapeObject copy() {
        ShapeObject s = new ShapeObject(kind, width, height, x, y, rotation);
        s.vx = vx;
        s.vy = vy;
        s.spin = spin;
        return s;
    }

    @Override
    public String toString() {
        return kind + "(" + Math.round(width) + "x" + Math.round(height)
                + ") @(" + Math.round(x) + "," + Math.round(y) + ") r" + Math.round(rotation);
    }
}
