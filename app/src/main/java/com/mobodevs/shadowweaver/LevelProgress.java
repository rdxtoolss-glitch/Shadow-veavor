package com.mobodevs.shadowweaver;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Shadow Weaver - LevelProgress
 * =============================
 * Manages game progression, 3-star scoring calculations, par times,
 * and persistence across player sessions.
 */
public class LevelProgress {

    private static final String PREFS = "shadow_weaver_progress";
    private static final String KEY_UNLOCKED = "highest_unlocked_level";
    private static final String KEY_STARS_PREFIX = "stars_lvl_";
    private static final String KEY_BEST_TIME_PREFIX = "best_time_lvl_";
    private static final String KEY_SOUND_MUTED = "sound_muted";
    private static final String KEY_HAPTICS_ENABLED = "haptics_enabled";

    private final int[] stars = new int[LevelLoader.count()];
    private final long[] bestTimes = new long[LevelLoader.count()];
    private int highestUnlocked;
    private boolean soundMuted;
    private boolean hapticsEnabled = true;

    public LevelProgress() {
    }

    public static long getParTimeMillis(int levelIndex) {
        if (levelIndex < 5) return 30_000L;  // 30 seconds for introductory levels
        if (levelIndex < 10) return 45_000L; // 45 seconds for intermediate levels
        return 60_000L;                      // 60 seconds for complex master levels
    }

    public static int getParMoves(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= LevelLoader.count()) return 10;
        int shapeCount = LevelLoader.get(levelIndex).solution.length;
        return shapeCount * 3; // 3 moves per shape is considered optimal efficiency
    }

    /**
     * Calculate star rating:
     *  * 1 Star:  Silhouette solved (Match >= 85%).
     *  ** 2 Stars: Solved under par time.
     *  *** 3 Stars: Solved under par time AND within par moves.
     */
    public static int calculateStars(int levelIndex, long elapsedMillis, int moves) {
        long parTime = getParTimeMillis(levelIndex);
        int parMoves = getParMoves(levelIndex);

        if (elapsedMillis <= parTime && moves <= parMoves) {
            return 3;
        } else if (elapsedMillis <= parTime) {
            return 2;
        } else {
            return 1;
        }
    }

    // ---- Instance Getters / Setters ----------------------------------------

    public int getHighestUnlocked() {
        return highestUnlocked;
    }

    public void setHighestUnlocked(int level) {
        int max = LevelLoader.count() - 1;
        this.highestUnlocked = Math.max(0, Math.min(level, max));
    }

    public int getStars(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= stars.length) return 0;
        return stars[levelIndex];
    }

    public long getBestTime(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= bestTimes.length) return 0L;
        return bestTimes[levelIndex];
    }

    public boolean isSoundMuted() {
        return soundMuted;
    }

    public void setSoundMuted(boolean muted) {
        this.soundMuted = muted;
    }

    public boolean isHapticsEnabled() {
        return hapticsEnabled;
    }

    public void setHapticsEnabled(boolean enabled) {
        this.hapticsEnabled = enabled;
    }

    public int getTotalStars() {
        int sum = 0;
        for (int s : stars) sum += s;
        return sum;
    }

    public static final int TOTAL_MAX_STARS = LevelLoader.count() * 3;

    public String getPlayerRank() {
        int total = getTotalStars();
        if (total >= 45) return "Grand Alchemist of the Cosmos";
        if (total >= 30) return "Master Shadow Weaver";
        if (total >= 15) return "Adept of the Astral Loom";
        return "Apprentice of Shadows";
    }

    public String getPlayerRankDescription() {
        int total = getTotalStars();
        if (total >= 45) return "Supreme mastery over light, perspective, and astral geometry.";
        if (total >= 30) return "Skilled at bending shadows and solving celestial mysteries.";
        if (total >= 15) return "Growing familiarity with the antique astrolabe and shadow manipulation.";
        return "Beginning your journey into the study of shadows and candlelight.";
    }

    public int getChapterStars(int chapter) {
        int start = chapter * 5;
        int end = Math.min(start + 5, stars.length);
        int sum = 0;
        for (int i = start; i < end; i++) {
            sum += stars[i];
        }
        return sum;
    }

    public boolean isChapterUnlocked(int chapter) {
        if (chapter == 0) return true;
        int startLevel = chapter * 5;
        return highestUnlocked >= startLevel;
    }

    public void resetAll() {
        highestUnlocked = 0;
        for (int i = 0; i < stars.length; i++) {
            stars[i] = 0;
            bestTimes[i] = 0L;
        }
    }

    public void resetAllProgress(Context context) {
        highestUnlocked = 0;
        for (int i = 0; i < stars.length; i++) {
            stars[i] = 0;
            bestTimes[i] = 0L;
        }
        save(context);
    }

    // ---- Persistence -------------------------------------------------------

    public void load(Context context) {
        if (context == null) return;
        try {
            SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            highestUnlocked = p.getInt(KEY_UNLOCKED, 0);
            soundMuted = p.getBoolean(KEY_SOUND_MUTED, false);
            hapticsEnabled = p.getBoolean(KEY_HAPTICS_ENABLED, true);

            for (int i = 0; i < stars.length; i++) {
                stars[i] = p.getInt(KEY_STARS_PREFIX + i, 0);
                bestTimes[i] = p.getLong(KEY_BEST_TIME_PREFIX + i, 0L);
            }
        } catch (Throwable ignored) {
        }
    }

    public void recordCompletion(Context context, int levelIndex, long elapsedMillis, int moves) {
        if (levelIndex < 0 || levelIndex >= LevelLoader.count()) return;

        int earned = calculateStars(levelIndex, elapsedMillis, moves);
        if (earned > stars[levelIndex]) {
            stars[levelIndex] = earned;
        }

        if (bestTimes[levelIndex] == 0L || elapsedMillis < bestTimes[levelIndex]) {
            bestTimes[levelIndex] = elapsedMillis;
        }

        int nextLevel = levelIndex + 1;
        if (nextLevel < LevelLoader.count() && nextLevel > highestUnlocked) {
            highestUnlocked = nextLevel;
        }

        save(context);
    }

    public void save(Context context) {
        if (context == null) return;
        try {
            SharedPreferences.Editor ed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            ed.putInt(KEY_UNLOCKED, highestUnlocked);
            ed.putBoolean(KEY_SOUND_MUTED, soundMuted);
            ed.putBoolean(KEY_HAPTICS_ENABLED, hapticsEnabled);

            for (int i = 0; i < stars.length; i++) {
                ed.putInt(KEY_STARS_PREFIX + i, stars[i]);
                ed.putLong(KEY_BEST_TIME_PREFIX + i, bestTimes[i]);
            }
            ed.commit();
        } catch (Throwable ignored) {
        }
    }
}
