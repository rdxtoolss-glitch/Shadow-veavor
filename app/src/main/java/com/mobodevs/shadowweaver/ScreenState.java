package com.mobodevs.shadowweaver;

/**
 * Shadow Weaver - ScreenState
 * ===========================
 * Central screen and navigation state machine.
 */
public enum ScreenState {
    /** Main menu with logo, continue, chapters, guide, journal, settings. */
    TITLE,
    /** Chapter & level map with 3-star ratings and completion records. */
    LEVEL_SELECT,
    /** In-game puzzle solving stage. */
    PLAY,
    /** In-game pause modal dialog. */
    PAUSE,
    /** Victory 3-star reveal with par time/moves breakdown. */
    LEVEL_COMPLETE,
    /** 4-card interactive illustrated guide explaining core mechanics. */
    HOW_TO_PLAY,
    /** Astral relic journal showing unlocked silhouettes and player rank. */
    CODEX,
    /** Dedicated settings modal (sound, haptics, physics, progress reset). */
    SETTINGS
}
