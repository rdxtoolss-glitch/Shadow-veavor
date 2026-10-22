package com.mobodevs.shadowweaver;

/**
 * JVM Verification test for LevelProgress star calculations and par ratings.
 */
public class LevelProgressTest {

    public static void main(String[] args) {
        System.out.println("Testing LevelProgress star logic...");

        // Level 0: 3 shapes -> parTime = 30s, parMoves = 9
        int stars1 = LevelProgress.calculateStars(0, 45_000L, 15); // slow, many moves
        if (stars1 != 1) {
            throw new AssertionError("Expected 1 star for slow completion, got: " + stars1);
        }

        int stars2 = LevelProgress.calculateStars(0, 20_000L, 14); // fast, but many moves
        if (stars2 != 2) {
            throw new AssertionError("Expected 2 stars for fast completion with extra moves, got: " + stars2);
        }

        int stars3 = LevelProgress.calculateStars(0, 18_000L, 6); // fast AND low moves
        if (stars3 != 3) {
            throw new AssertionError("Expected 3 stars for master completion, got: " + stars3);
        }

        System.out.println("Stars test passed: 1 star=" + stars1 + ", 2 stars=" + stars2 + ", 3 stars=" + stars3);

        // Verify par times are defined for all 15 levels
        for (int i = 0; i < LevelLoader.count(); i++) {
            long parTime = LevelProgress.getParTimeMillis(i);
            int parMoves = LevelProgress.getParMoves(i);
            if (parTime <= 0 || parMoves <= 0) {
                throw new AssertionError("Invalid par values for level " + i);
            }
        }
        System.out.println("All 15 level pars verified.");
        System.out.println("LevelProgress test PASS.");
    }
}
