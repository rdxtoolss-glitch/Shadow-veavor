package com.mobodevs.shadowweaver;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Shadow Weaver - MainActivity
 * ============================
 * The only Activity. It does exactly three things: host the GameView, forward
 * the lifecycle, and remember how far the player has got.
 *
 * ------------------------------------------------------------------
 * WHY A PLAIN android.app.Activity AND NOT AppCompatActivity
 * ------------------------------------------------------------------
 * The original scaffold extended AppCompatActivity and used
 * Theme.AppCompat, which dragged in the AppCompat and Material artifact
 * dependencies. The game has no widgets, no fragments and no menu, so those
 * were pure weight - and pure risk, since a dependency that fails to resolve
 * fails the whole build. Extending the framework Activity and using a
 * platform theme gives an identical-looking result with zero dependencies.
 *
 * ------------------------------------------------------------------
 * LIFECYCLE
 * ------------------------------------------------------------------
 * The subtle part is audio versus loop. On pause the loop stops drawing
 * immediately but the ambience is paused rather than released, because
 * rebuilding the drone buffer takes a moment and doing that on every
 * trip through the app switcher would be audible as a stutter. The AudioManager
 * is only released in onDestroy, when the process is genuinely going away.
 */
public class MainActivity extends Activity {

    private static final String PREFS = "shadow_weaver_progress";
    private static final String KEY_UNLOCKED = "highest_unlocked_level";

    private GameView gameView;
    private AudioManager audio;

    private int highestUnlocked;

    // ======================================================================
    // LIFECYCLE
    // ======================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        goFullscreen();

        // Keep the screen on: this is a game you look at and think, often with
        // one hand on the shape and no touch events for several seconds while
        // considering a move. Letting it time out mid-puzzle is maddening.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        highestUnlocked = loadProgress();

        audio = new AudioManager();

        gameView = new GameView(this,
                /* startLevel   */ 0,
                /* unlocked     */ highestUnlocked,
                audio);

        gameView.setProgressListener(new GameView.Progress() {
            @Override
            public void report(int level) {
                saveProgress(level);
            }
        });

        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        audio.start();
        gameView.onResume();
    }

    @Override
    protected void onPause() {
        // Loop first, then audio: stop drawing before changing audio state so
        // no frame is ever drawn against a half-torn-down loop.
        gameView.onPause();
        audio.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        gameView.onDestroy();
        audio.release();
        super.onDestroy();
    }

    // ======================================================================
    // WINDOW
    // ======================================================================

    /**
     * Hide the system bars and let the game draw into the whole screen.
     *
     * Re-applied in onWindowFocusChanged as well as onCreate: the system
     * restores the bars transiently whenever the user swipes from an edge, so
     * a one-time call in onCreate would leave the bars permanently visible
     * after the first accidental swipe.
     *
     * The SYSTEM_UI_FLAG_* constants are deprecated as of API 30 in favour of
     * WindowInsetsController, but they remain the only mechanism that both
     * compiles at this project's compileSdkVersion and works back to
     * minSdk 21, and they still function on current Android versions. The
     * suppression is deliberate and local, not a blanket "ignore warnings".
     */
    @SuppressWarnings("deprecation")
    private void goFullscreen() {
        Window w = getWindow();
        if (w == null) return;

        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);

        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            goFullscreen();
        }
    }

    // ======================================================================
    // PROGRESS
    // ======================================================================

    private int loadProgress() {
        try {
            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            int v = p.getInt(KEY_UNLOCKED, 0);
            int max = LevelLoader.count() - 1;
            if (v < 0) v = 0;
            if (v > max) v = max;
            return v;
        } catch (Throwable t) {
            // If preferences are unavailable for any reason, start at level 1
            // rather than refusing to launch. Progress is a convenience, never
            // a reason the game cannot start.
            return 0;
        }
    }

    private void saveProgress(int level) {
        if (level <= highestUnlocked) return;
        highestUnlocked = level;
        try {
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_UNLOCKED, level)
                    // commit(), not apply(): this is a tiny single int and it
                    // is written at a moment the player is about to leave the
                    // app. Async could be lost if the process dies first, which
                    // would silently lose a level unlock.
                    .commit();
        } catch (Throwable ignored) {
        }
    }
}
