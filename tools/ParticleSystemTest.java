package com.mobodevs.shadowweaver;

/**
 * JVM Verification test for ParticleSystem.
 * Tests allocation pool stability, update cycles, and celebration bursts.
 */
public class ParticleSystemTest {

    public static void main(String[] args) {
        System.out.println("Testing ParticleSystem...");

        ParticleSystem ps = new ParticleSystem(1000f, 1600f);

        // 1. Verify ambient particle pool initialization
        if (ps.ambientCount() <= 0) {
            throw new AssertionError("Ambient particles not initialized: count = " + ps.ambientCount());
        }
        System.out.println("Ambient particles initialized: " + ps.ambientCount());

        // 2. Simulate 120 frames of ambient drift (2 seconds at 60fps)
        for (int i = 0; i < 120; i++) {
            ps.update(0.0166f, 500f, 260f, 1.0f);
        }

        // Verify bounds remain within scene
        for (int i = 0; i < ps.ambientCount(); i++) {
            float x = ps.getAmbientX(i);
            float y = ps.getAmbientY(i);
            if (x < -50f || x > 1050f || y < -50f || y > 1650f) {
                throw new AssertionError("Ambient particle drifted out of bounds: x=" + x + ", y=" + y);
            }
        }
        System.out.println("Ambient particles stayed within bounds over 120 frames.");

        // 3. Trigger celebration burst and simulate 60 frames
        ps.burstCelebration(500f, 400f);
        if (ps.activeSparksCount() <= 0) {
            throw new AssertionError("No active sparks after celebration burst!");
        }
        System.out.println("Active celebration sparks: " + ps.activeSparksCount());

        for (int i = 0; i < 120; i++) {
            ps.update(0.0166f, 500f, 260f, 1.0f);
        }

        // After 2 seconds (120 frames), sparks should naturally decay
        System.out.println("Sparks after decay: " + ps.activeSparksCount());
        System.out.println("ParticleSystem test PASS.");
    }
}
