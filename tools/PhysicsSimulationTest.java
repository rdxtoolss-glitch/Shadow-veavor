package com.mobodevs.shadowweaver;

/**
 * JVM Verification test for physics dynamics:
 *  - Spring-mass drag damping
 *  - Inertial momentum glide and viscous decay
 *  - Stage boundary bounce restitution
 *  - Soft inter-token repulsion
 */
public class PhysicsSimulationTest {

    public static void main(String[] args) {
        System.out.println("Testing Physics Simulation dynamics...");

        ShapeObject s1 = new ShapeObject(ShapeObject.Kind.RECT, 120f, 60f, 500f, 700f, 0f);
        ShapeObject s2 = new ShapeObject(ShapeObject.Kind.CIRCLE, 80f, 80f, 520f, 710f, 0f);

        // 1. Test Spring-Mass Drag Damping
        s1.targetX = 600f;
        s1.targetY = 800f;
        s1.isDragging = true;

        for (int i = 0; i < 60; i++) {
            s1.stepPhysics(0.0166f);
        }

        float distToTarget = (float) Math.hypot(s1.x - s1.targetX, s1.y - s1.targetY);
        if (distToTarget > 2.0f) {
            throw new AssertionError("Spring drag failed to settle at target: dist=" + distToTarget);
        }
        System.out.println("Spring drag smoothly converged to target within " + distToTarget + " units.");

        // 2. Test Inertial Momentum & Viscous Decay
        s1.isDragging = false;
        s1.vx = 400f; // initial flick velocity
        s1.vy = 0f;
        float initialVx = s1.vx;

        for (int i = 0; i < 40; i++) {
            s1.stepPhysics(0.0166f);
        }

        if (s1.vx >= initialVx || s1.vx <= 0f) {
            throw new AssertionError("Inertial velocity did not decay properly: vx=" + s1.vx);
        }
        System.out.println("Inertial momentum decayed smoothly from 400 to " + s1.vx);

        // 3. Test Boundary Restitution
        s1.x = ShadowCalculator.STAGE_RIGHT - 1f;
        s1.vx = 200f; // moving toward right wall
        s1.stepPhysics(0.0166f);

        if (s1.vx >= 0f || s1.x > ShadowCalculator.STAGE_RIGHT) {
            throw new AssertionError("Boundary bounce failed: x=" + s1.x + ", vx=" + s1.vx);
        }
        System.out.println("Boundary collision correctly bounced token with restitution: vx=" + s1.vx);

        // 4. Test Token Non-Interference (Tokens must NOT push each other in tangram puzzles)
        s1.vx = 0f; s1.vy = 0f;
        s2.vx = 0f; s2.vy = 0f;
        s1.x = 500f; s1.y = 700f;
        s2.x = 520f; s2.y = 710f;
        float initialOverlapDist = (float) Math.hypot(s1.x - s2.x, s1.y - s2.y);
        for (int i = 0; i < 40; i++) {
            ShapeObject.applySoftRepulsion(s1, s2, 0.0166f);
            s1.stepPhysics(0.0166f);
            s2.stepPhysics(0.0166f);
        }
        float finalOverlapDist = (float) Math.hypot(s1.x - s2.x, s1.y - s2.y);
        if (Math.abs(finalOverlapDist - initialOverlapDist) > 0.001f) {
            throw new AssertionError("Tokens unexpectedly pushed each other: initial=" + initialOverlapDist + ", final=" + finalOverlapDist);
        }
        System.out.println("Tokens remain completely independent and non-pushing: dist=" + finalOverlapDist);

        System.out.println("Physics Simulation test PASS.");
    }
}
