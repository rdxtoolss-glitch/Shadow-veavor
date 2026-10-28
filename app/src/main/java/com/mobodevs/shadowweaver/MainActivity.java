package com.mobodevs.shadowweaver;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/**
 * Shadow Weaver - MainActivity
 * ============================
 * Hosts GameView, initializes subsystems (AudioManager, HapticManager, LevelProgress),
 * and maintains window and lifecycle states.
 */
public class MainActivity extends Activity {

    private GameView gameView;
    private AudioManager audio;
    private HapticManager haptic;
    private LevelProgress progress;

    // ======================================================================
    // LIFECYCLE
    // ======================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        goFullscreen();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progress = new LevelProgress();
        progress.load(this);

        audio = new AudioManager();
        audio.initSystemAudio(this);
        audio.setMuted(progress.isSoundMuted());
        audio.start(); // Begin async synthesis immediately on app creation

        haptic = new HapticManager(this);
        haptic.setEnabled(progress.isHapticsEnabled());

        gameView = new GameView(this,
                /* startLevel */ progress.getHighestUnlocked(),
                progress,
                audio,
                haptic);

        gameView.setProgressListener(new GameView.Progress() {
            @Override
            public void report(int level) {
                if (progress != null) {
                    progress.setHighestUnlocked(level);
                    progress.save(MainActivity.this);
                }
            }
        });

        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (audio != null) {
            audio.resume(); // Resume audio and ambient loop smoothly
        }
        if (gameView != null) {
            gameView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (gameView != null) {
            gameView.onPause();
        }
        if (audio != null) {
            audio.pause();
        }
        if (progress != null) {
            progress.save(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (gameView != null) {
            gameView.onDestroy();
        }
        if (audio != null) {
            audio.release();
        }
        super.onDestroy();
    }

    // ======================================================================
    // WINDOW & IMMERSIVE DISPLAY
    // ======================================================================

    private void goFullscreen() {
        Window w = getWindow();
        if (w == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            w.getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = w.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            @SuppressWarnings("deprecation")
            View decor = w.getDecorView();
            if (decor != null) {
                decor.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (gameView != null && gameView.handleBackPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            goFullscreen();
        }
    }
}
