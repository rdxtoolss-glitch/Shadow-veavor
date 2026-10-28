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

    /** Smallest signed angle difference in [-180, 180) for shortest-arc rotation. */
    public static float normalizeDeltaAngle(float diff) {
        float a = diff % 360f;
        if (a > 180f) a -= 360f;
        if (a < -180f) a += 360f;
        return a;
    }

    /**
     * Compute minimum angular difference taking rotational symmetry into account.
     */
    public static float computeAngularDifference(Kind kind, float rotA, float rotB) {
        if (kind == Kind.CIRCLE) {
            return 0f;
        }
        float diff = Math.abs(normalizeDeltaAngle(rotA - rotB));
        if (kind == Kind.SQUARE) {
            diff = diff % 90f;
            if (diff > 45f) diff = 90f - diff;
        } else if (kind == Kind.RECT || kind == Kind.LINE) {
            diff = diff % 180f;
            if (diff > 90f) diff = 180f - diff;
        }
        return diff;
    }

    /**
     * Find the closest symmetric target angle matching the solution rotation.
     */
    public static float closestSymmetricAngle(Kind kind, float currentRot, float solRot) {
        if (kind == Kind.CIRCLE) return currentRot;
        if (kind == Kind.SQUARE) {
            float best = solRot;
            float minDiff = 360f;
            for (int k = 0; k < 4; k++) {
                float cand = normalizeAngle(solRot + k * 90f);
                float diff = Math.abs(normalizeDeltaAngle(currentRot - cand));
                if (diff < minDiff) {
                    minDiff = diff;
                    best = cand;
                }
            }
            return best;
        }
        if (kind == Kind.RECT || kind == Kind.LINE) {
            float cand0 = normalizeAngle(solRot);
            float cand1 = normalizeAngle(solRot + 180f);
            float diff0 = Math.abs(normalizeDeltaAngle(currentRot - cand0));
            float diff1 = Math.abs(normalizeDeltaAngle(currentRot - cand1));
            return diff0 <= diff1 ? cand0 : cand1;
        }
        return solRot;
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

    /** Physical drag targets and velocities. */
    public float targetX;
    public float targetY;
    public float targetRotation;
    public float vx;
    public float vy;
    public float angularVelocity;
    public float spin;
    public float mass = 1.0f;

    /** Dynamic physical tilt angles (in degrees) for subtle 3D perspective cues. */
    public float tiltX;
    public float tiltY;

    /**
     * Visual elevation factor (0.0 = resting on stage, 1.0 = lifted on touch).
     * Drives drop shadow offset/blur, scale lift, and tactile lighting.
     */
    public float elevation;
    public float targetElevation;
    public boolean isDragging;
    public boolean isRotating;
    public boolean isSnapping;
    public float proximityResonance;
    public int lastDetentIndex = -1;

    public void updateElevation(float dt) {
        float speed = 16f;
        elevation += (targetElevation - elevation) * Math.min(1.0f, dt * speed);
    }

    public void snapTo(float solX, float solY, float solRot) {
        this.targetX = solX;
        this.targetY = solY;
        this.targetRotation = closestSymmetricAngle(this.kind, this.rotation, solRot);
        this.isSnapping = true;
        this.isDragging = false;
        this.isRotating = false;
    }

    /**
     * Step physical dynamics: spring-mass drag, viscous decay, boundary bounce, and rotation.
     */
    public void stepPhysics(float dt) {
        if (dt <= 0f) return;
        if (dt > 0.05f) dt = 0.05f; // cap physics dt to prevent tunnelling

        if (isDragging) {
            // Silky-smooth responsive tracking towards finger position (zero jitter, zero lag)
            float smooth = 1f - (float) Math.exp(-dt * 45f);
            x += (targetX - x) * smooth;
            y += (targetY - y) * smooth;
            vx *= (float) Math.pow(0.01f, dt);
            vy *= (float) Math.pow(0.01f, dt);
            tiltX = 0f;
            tiltY = 0f;
        } else if (isSnapping) {
            // Smooth magnetic snap convergence to target
            float dx = targetX - x;
            float dy = targetY - y;
            float k = 320f;
            float c = 32f;
            float fx = k * dx - c * vx;
            float fy = k * dy - c * vy;
            vx += (fx / mass) * dt;
            vy += (fy / mass) * dt;
            x += vx * dt;
            y += vy * dt;

            if (Math.abs(dx) < 0.1f && Math.abs(dy) < 0.1f && Math.abs(vx) < 0.5f && Math.abs(vy) < 0.5f) {
                x = targetX;
                y = targetY;
                vx = 0f;
                vy = 0f;
                isSnapping = false;
            }
            tiltX = 0f;
            tiltY = 0f;
        } else {
            // Inertial glide with exponential surface friction
            x += vx * dt;
            y += vy * dt;
            float friction = (float) Math.pow(0.005f, dt);
            vx *= friction;
            vy *= friction;
            if (Math.abs(vx) < 0.2f) vx = 0f;
            if (Math.abs(vy) < 0.2f) vy = 0f;
            tiltX = 0f;
            tiltY = 0f;

            if (spin != 0f) {
                rotation = normalizeAngle(rotation + spin * dt);
                spin *= (float) Math.pow(0.01f, dt);
                if (Math.abs(spin) < 0.2f) spin = 0f;
            }
        }

        // Boundary collision with soft restitution bounce
        float minX = ShadowCalculator.STAGE_LEFT;
        float maxX = ShadowCalculator.STAGE_RIGHT;
        float minY = ShadowCalculator.STAGE_TOP;
        float maxY = ShadowCalculator.STAGE_BOTTOM;
        float restitution = 0.28f;

        if (x < minX) {
            x = minX;
            vx = Math.abs(vx) * restitution;
        } else if (x > maxX) {
            x = maxX;
            vx = -Math.abs(vx) * restitution;
        }

        if (y < minY) {
            y = minY;
            vy = Math.abs(vy) * restitution;
        } else if (y > maxY) {
            y = maxY;
            vy = -Math.abs(vy) * restitution;
        }

        // Rotation physics
        if (isRotating) {
            // Silky-smooth rotation tracking towards target angle with shortest-arc lerp
            float dRot = normalizeDeltaAngle(targetRotation - rotation);
            float rotSmooth = 1f - (float) Math.exp(-dt * 45f);
            rotation = normalizeAngle(rotation + dRot * rotSmooth);
            angularVelocity = 0f;
        } else if (isSnapping) {
            float dRot = normalizeDeltaAngle(targetRotation - rotation);
            float kRot = 240f;
            float cRot = 24f;
            float torque = kRot * dRot - cRot * angularVelocity;
            angularVelocity += torque * dt;
            rotation = normalizeAngle(rotation + angularVelocity * dt);
            if (Math.abs(dRot) < 0.2f && Math.abs(angularVelocity) < 0.5f) {
                rotation = targetRotation;
                angularVelocity = 0f;
                isSnapping = false;
            }
        } else if (angularVelocity != 0f) {
            rotation = normalizeAngle(rotation + angularVelocity * dt);
            angularVelocity *= (float) Math.pow(0.12f, dt);
            if (Math.abs(angularVelocity) < 0.4f) angularVelocity = 0f;
        }

        updateElevation(dt);
    }

    /**
     * Soft repulsion disabled: in tangram silhouette puzzles, shapes must NEVER push each other.
     * Tokens can touch, align edge-to-edge, and overlap freely without displacing other pieces.
     */
    public static void applySoftRepulsion(ShapeObject a, ShapeObject b, float dt) {
        // No-op: pieces are completely independent on the workbench and do not push each other.
    }

    public ShapeObject(Kind kind, float width, float height, float x, float y, float rotation) {
        this.kind = kind;
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.targetX = x;
        this.targetY = y;
        this.targetRotation = rotation;
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
