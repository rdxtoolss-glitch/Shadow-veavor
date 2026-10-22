package com.mobodevs.shadowweaver;

/**
 * JVM Verification test for Magnetic Resonance & Snap-Assist mechanics.
 */
public class SnapAssistTest {

    public static void main(String[] args) {
        System.out.println("Testing Magnetic Resonance & Snap-Assist...");

        // 1. Angular symmetry tests
        float circleDiff = ShapeObject.computeAngularDifference(ShapeObject.Kind.CIRCLE, 45f, 290f);
        if (circleDiff != 0f) {
            throw new AssertionError("Circle should have 0 angular difference, got: " + circleDiff);
        }

        float squareDiff = ShapeObject.computeAngularDifference(ShapeObject.Kind.SQUARE, 92f, 2f);
        if (Math.abs(squareDiff) > 0.001f) {
            throw new AssertionError("Square 90-deg symmetry failed: " + squareDiff);
        }

        float rectDiff = ShapeObject.computeAngularDifference(ShapeObject.Kind.RECT, 183f, 3f);
        if (Math.abs(rectDiff) > 0.001f) {
            throw new AssertionError("Rect 180-deg symmetry failed: " + rectDiff);
        }
        System.out.println("Rotational symmetry calculations PASS.");

        // 2. Lock threshold & snap test
        ShapeObject s = new ShapeObject(ShapeObject.Kind.SQUARE, 80f, 80f, 500f, 600f, 15f);
        ShapeObject sol = new ShapeObject(ShapeObject.Kind.SQUARE, 80f, 80f, 510f, 608f, 107f);

        float posDist = (float) Math.hypot(s.x - sol.x, s.y - sol.y);
        float rotDiff = ShapeObject.computeAngularDifference(s.kind, s.rotation, sol.rotation);

        boolean inLockZone = (posDist <= 25f && rotDiff <= 8f);
        if (!inLockZone) {
            throw new AssertionError("Failed to detect lock zone: posDist=" + posDist + ", rotDiff=" + rotDiff);
        }

        // Apply snap
        s.snapTo(sol.x, sol.y, sol.rotation);

        // Simulate release spring settling
        for (int i = 0; i < 40; i++) {
            s.stepPhysics(0.0166f);
        }

        float finalPosDist = (float) Math.hypot(s.x - sol.x, s.y - sol.y);
        float finalRotDiff = ShapeObject.computeAngularDifference(s.kind, s.rotation, sol.rotation);

        if (finalPosDist > 1.0f || finalRotDiff > 1.0f) {
            throw new AssertionError("Snap did not converge to solution: posDist=" + finalPosDist + ", rotDiff=" + finalRotDiff);
        }
        System.out.println("Snap converged cleanly to solution target: posDist=" + finalPosDist + ", rotDiff=" + finalRotDiff);

        // 3. Far field should not snap
        ShapeObject far = new ShapeObject(ShapeObject.Kind.SQUARE, 80f, 80f, 300f, 400f, 0f);
        float farDist = (float) Math.hypot(far.x - sol.x, far.y - sol.y);
        if (farDist <= 25f) {
            throw new AssertionError("Far shape incorrectly detected in lock zone");
        }
        System.out.println("Far-field rejection PASS.");

        System.out.println("SnapAssistTest PASS.");
    }
}
