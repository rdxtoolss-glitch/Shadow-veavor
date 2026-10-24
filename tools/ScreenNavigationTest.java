package com.mobodevs.shadowweaver;

/**
 * JVM Verification test for ScreenState, UiButton hit-testing, and LevelProgress chapter/rank logic.
 */
public class ScreenNavigationTest {

    public static void main(String[] args) {
        System.out.println("Starting ScreenNavigationTest...");

        // 1. Verify ScreenState Enum
        ScreenState[] states = ScreenState.values();
        if (states.length != 8) {
            throw new AssertionError("Expected 8 ScreenStates, got " + states.length);
        }
        System.out.println("ScreenState verified with 8 states: " + java.util.Arrays.toString(states));

        // 2. Verify LevelProgress ranks and chapter unlocking
        LevelProgress progress = new LevelProgress();
        if (!"Apprentice of Shadows".equals(progress.getPlayerRank())) {
            throw new AssertionError("Expected Apprentice rank, got: " + progress.getPlayerRank());
        }
        if (!progress.isChapterUnlocked(0)) {
            throw new AssertionError("Chapter 0 should always be unlocked");
        }
        if (progress.isChapterUnlocked(1)) {
            throw new AssertionError("Chapter 1 should be locked initially");
        }
        if (progress.isChapterUnlocked(2)) {
            throw new AssertionError("Chapter 2 should be locked initially");
        }

        // Test unlocking Chapter 1
        progress.setHighestUnlocked(5);
        if (!progress.isChapterUnlocked(1)) {
            throw new AssertionError("Chapter 1 should be unlocked at level 5");
        }
        if (progress.isChapterUnlocked(2)) {
            throw new AssertionError("Chapter 2 should still be locked at level 5");
        }

        // Test unlocking Chapter 2
        progress.setHighestUnlocked(10);
        if (!progress.isChapterUnlocked(2)) {
            throw new AssertionError("Chapter 2 should be unlocked at level 10");
        }

        System.out.println("Chapter unlock logic verified.");

        // 3. Verify UiButton hit testing
        UiButton button = new UiButton(100f, 200f, 300f, 260f, "CONTINUE", true);
        if (!button.hitTest(150f, 230f)) {
            throw new AssertionError("Hit-test failed for point inside button bounds");
        }
        if (button.hitTest(50f, 230f)) {
            throw new AssertionError("Hit-test falsely passed for point to the left");
        }
        if (button.hitTest(350f, 230f)) {
            throw new AssertionError("Hit-test falsely passed for point to the right");
        }
        if (button.hitTest(150f, 150f)) {
            throw new AssertionError("Hit-test falsely passed for point above");
        }
        if (button.hitTest(150f, 300f)) {
            throw new AssertionError("Hit-test falsely passed for point below");
        }

        // Test button disabled state
        button.enabled = false;
        if (button.hitTest(150f, 230f)) {
            throw new AssertionError("Hit-test should return false when button is disabled");
        }
        button.enabled = true;

        // Verify dimensions and center calculations
        if (Math.abs(button.width() - 200f) > 0.001f || Math.abs(button.height() - 60f) > 0.001f) {
            throw new AssertionError("Button width/height calculation incorrect: w=" + button.width() + " h=" + button.height());
        }
        if (Math.abs(button.centerX() - 200f) > 0.001f || Math.abs(button.centerY() - 230f) > 0.001f) {
            throw new AssertionError("Button center calculation incorrect: cx=" + button.centerX() + " cy=" + button.centerY());
        }

        System.out.println("UiButton hit-testing and geometry verified.");
        System.out.println("ScreenNavigationTest PASS.");
    }
}
