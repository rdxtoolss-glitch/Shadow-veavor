package com.mobodevs.shadowweaver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Shadow Weaver - GameView
 * ========================
 * The SurfaceView: owns the surface, forwards lifecycle to GameThread,
 * routes input across all application screens, and handles gestures
 * (1-finger drag, dial rotation, 2-finger twist, and HUD interactions).
 */
public class GameView extends SurfaceView implements GameThread.Looper {

    private final GameThread thread;
    private final HapticManager haptic;
    private final LevelProgress progress;
    private final AudioManager audio;

    // ---- Touch tracking for In-Game Stage ---------------------------------
    private boolean dragActive;
    private boolean trackingRotate;
    private float rotateStartAngle;

    // ---- Persistence hook -------------------------------------------------
    public interface Progress {
        void report(int highestUnlocked);
    }

    private Progress onProgress;
    private int lastReportedUnlocked = -1;

    public GameView(Context context, int startLevel, int unlocked, AudioManager audio) {
        this(context, startLevel, new LevelProgress(), audio, new HapticManager(context));
    }

    public GameView(Context context, int startLevel, LevelProgress progress, AudioManager audio, HapticManager haptic) {
        super(context);
        this.progress = progress;
        this.audio = audio;
        this.haptic = haptic;

        SurfaceHolder holder = getHolder();

        thread = new GameThread(holder, new GameThread.LevelListener() {
            @Override
            public void onLevelComplete(final int levelIndex, final long elapsedMillis, final int moves) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (GameView.this.progress != null) {
                            GameView.this.progress.recordCompletion(getContext(), levelIndex, elapsedMillis, moves);
                        }
                        if (onProgress != null) {
                            int u = thread.unlockedLevel();
                            if (u != lastReportedUnlocked) {
                                lastReportedUnlocked = u;
                                onProgress.report(u);
                            }
                        }
                    }
                });
            }
        });

        thread.attachLoopOwner(this);
        thread.attachAudio(audio);
        thread.attachHaptics(haptic);
        thread.attachProgress(progress);

        holder.addCallback(thread);

        int unlockedLevel = (progress != null) ? progress.getHighestUnlocked() : 0;
        thread.setUnlocked(unlockedLevel);
        lastReportedUnlocked = unlockedLevel;
        thread.loadLevel(startLevel);

        // App starts on Title Screen
        thread.setScreenState(ScreenState.TITLE);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    // ======================================================================
    // LIFECYCLE & NAVIGATION
    // ======================================================================

    public void onResume() {
        thread.resume();
    }

    public void onPause() {
        thread.pause();
    }

    public void onDestroy() {
        thread.shutdown();
    }

    @Override
    public boolean post(Runnable r) {
        return super.post(r);
    }

    public void setProgressListener(Progress p) {
        this.onProgress = p;
    }

    /**
     * Handle hardware / gesture back button navigation hierarchically.
     * @return true if handled internally, false to exit application.
     */
    public boolean handleBackPress() {
        ScreenState state = thread.getScreenState();
        if (state == ScreenState.PLAY) {
            thread.showPause();
            if (haptic != null) haptic.tap();
            return true;
        } else if (state == ScreenState.PAUSE) {
            thread.resumePlay();
            if (haptic != null) haptic.tap();
            return true;
        } else if (state == ScreenState.LEVEL_SELECT || state == ScreenState.HOW_TO_PLAY || state == ScreenState.CODEX) {
            thread.showTitle();
            if (haptic != null) haptic.tap();
            return true;
        } else if (state == ScreenState.SETTINGS) {
            thread.setScreenState(thread.getPreviousScreenState());
            if (haptic != null) haptic.tap();
            return true;
        } else if (state == ScreenState.LEVEL_COMPLETE) {
            thread.showLevelSelect();
            if (haptic != null) haptic.tap();
            return true;
        }
        return false; // Exit app if already in TITLE
    }

    // ======================================================================
    // TOUCH ROUTING
    // ======================================================================

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float scale = GameRenderer.fitScale(getWidth(), getHeight());
        float sceneH = GameRenderer.sceneHeight(getWidth(), getHeight());
        float ox = (getWidth() - ShadowCalculator.SCENE_W * scale) * 0.5f;
        float oy = 0f;
        final float sx = GameRenderer.sceneX(e.getX(), scale, ox);
        final float sy = GameRenderer.sceneY(e.getY(), scale, oy);

        ScreenState state = thread.getScreenState();

        // 1. Title Screen Routing
        if (state == ScreenState.TITLE) {
            return handleTitleTouch(e.getActionMasked(), sx, sy, sceneH);
        }

        // 2. How to Play Routing
        if (state == ScreenState.HOW_TO_PLAY) {
            return handleTutorialTouch(e.getActionMasked(), sx, sy, sceneH);
        }

        // 3. Codex Gallery Routing
        if (state == ScreenState.CODEX) {
            return handleCodexTouch(e.getActionMasked(), sx, sy, sceneH);
        }

        // 4. Chapter / Level Select Routing
        if (state == ScreenState.LEVEL_SELECT) {
            return handleLevelSelectTouch(e.getActionMasked(), sx, sy, sceneH);
        }

        // 5. Settings Modal Routing
        if (state == ScreenState.SETTINGS) {
            return handleSettingsTouch(e.getActionMasked(), sx, sy, sceneH);
        }

        // 6. Pause Modal Routing
        if (state == ScreenState.PAUSE) {
            return handlePauseTouch(e.getActionMasked(), sx, sy, sceneH);
        }

        // 7. Victory Modal Routing
        if (state == ScreenState.LEVEL_COMPLETE) {
            return handleVictoryTouch(e.getActionMasked(), sx, sy, sceneH);
        }

        // 8. In-Game Stage Routing
        return handlePlayTouch(e, sx, sy, sceneH);
    }

    private boolean handleTitleTouch(int action, float sx, float sy, float sceneH) {
        if (action == MotionEvent.ACTION_DOWN) {
            TitleScreen.onTouchDown(sx, sy, sceneH);
            if (haptic != null) haptic.detent();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            TitleScreen.Action act = TitleScreen.onTouchUp(sx, sy, sceneH);
            if (act == TitleScreen.Action.CONTINUE) {
                int lvl = (progress != null) ? progress.getHighestUnlocked() : 0;
                thread.pickLevel(lvl);
                if (haptic != null) haptic.tap();
            } else if (act == TitleScreen.Action.CHAPTERS) {
                thread.showLevelSelect();
                if (haptic != null) haptic.tap();
            } else if (act == TitleScreen.Action.TUTORIAL) {
                TutorialScreen.setPage(0);
                thread.showTutorial();
                if (haptic != null) haptic.tap();
            } else if (act == TitleScreen.Action.CODEX) {
                thread.showCodex();
                if (haptic != null) haptic.tap();
            } else if (act == TitleScreen.Action.SETTINGS) {
                thread.showSettings();
                if (haptic != null) haptic.tap();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            TitleScreen.cancelPress();
            return true;
        }
        return true;
    }

    private boolean handleTutorialTouch(int action, float sx, float sy, float sceneH) {
        if (action == MotionEvent.ACTION_DOWN) {
            TutorialScreen.onTouchDown(sx, sy, sceneH);
            if (haptic != null) haptic.detent();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            TutorialScreen.Action act = TutorialScreen.onTouchUp(sx, sy, sceneH);
            if (act == TutorialScreen.Action.BACK) {
                thread.showTitle();
                if (haptic != null) haptic.tap();
            } else if (act == TutorialScreen.Action.PLAY) {
                int lvl = (progress != null) ? progress.getHighestUnlocked() : 0;
                thread.pickLevel(lvl);
                if (haptic != null) haptic.tap();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            TutorialScreen.cancelPress();
            return true;
        }
        return true;
    }

    private boolean handleCodexTouch(int action, float sx, float sy, float sceneH) {
        if (action == MotionEvent.ACTION_DOWN) {
            CodexScreen.onTouchDown(sx, sy, sceneH);
            if (haptic != null) haptic.detent();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            CodexScreen.Action act = CodexScreen.onTouchUp(sx, sy, sceneH);
            if (act == CodexScreen.Action.BACK) {
                thread.showTitle();
                if (haptic != null) haptic.tap();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            CodexScreen.cancelPress();
            return true;
        }
        return true;
    }

    private boolean handleLevelSelectTouch(int action, float sx, float sy, float sceneH) {
        int unlocked = (progress != null) ? progress.getHighestUnlocked() : thread.unlockedLevel();
        if (action == MotionEvent.ACTION_DOWN) {
            LevelSelectOverlay.onTouchDown(sx, sy, unlocked, sceneH);
            if (haptic != null) haptic.detent();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            LevelSelectOverlay.Action act = LevelSelectOverlay.onTouchUp(sx, sy, unlocked, sceneH);
            if (act == LevelSelectOverlay.Action.PICKED) {
                thread.pickLevel(LevelSelectOverlay.getPickedLevel());
                if (haptic != null) haptic.tap();
            } else if (act == LevelSelectOverlay.Action.CLOSE) {
                thread.setScreenState(thread.getPreviousScreenState() == ScreenState.PAUSE ? ScreenState.PAUSE : ScreenState.TITLE);
                if (haptic != null) haptic.tap();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            LevelSelectOverlay.cancelPress();
            return true;
        }
        return true;
    }

    private boolean handleSettingsTouch(int action, float sx, float sy, float sceneH) {
        if (action == MotionEvent.ACTION_DOWN) {
            SettingsOverlay.onTouchDown(sx, sy, sceneH);
            if (haptic != null) haptic.detent();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            SettingsOverlay.Action act = SettingsOverlay.onTouchUp(sx, sy, getContext(), progress, audio, haptic, sceneH);
            if (act == SettingsOverlay.Action.CLOSE) {
                thread.setScreenState(thread.getPreviousScreenState());
                if (haptic != null) haptic.tap();
            } else if (act == SettingsOverlay.Action.RESET_CONFIRMED) {
                thread.setUnlocked(0);
                thread.loadLevel(0);
                thread.setScreenState(ScreenState.TITLE);
                if (haptic != null) haptic.victory();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            SettingsOverlay.cancelPress();
            return true;
        }
        return true;
    }

    private boolean handlePauseTouch(int action, float sx, float sy, float sceneH) {
        if (action == MotionEvent.ACTION_DOWN) {
            PauseOverlay.onTouchDown(sx, sy, sceneH);
            if (haptic != null) haptic.detent();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            PauseOverlay.Action act = PauseOverlay.onTouchUp(sx, sy, sceneH);
            if (act == PauseOverlay.Action.RESUME) {
                thread.resumePlay();
                if (haptic != null) haptic.tap();
            } else if (act == PauseOverlay.Action.RESTART) {
                thread.restart();
                thread.resumePlay();
                if (haptic != null) haptic.tap();
            } else if (act == PauseOverlay.Action.LEVEL_SELECT) {
                LevelSelectOverlay.setActiveChapterForLevel(thread.levelIndex());
                thread.showLevelSelect();
                if (haptic != null) haptic.tap();
            } else if (act == PauseOverlay.Action.SETTINGS) {
                thread.showSettings();
                if (haptic != null) haptic.tap();
            } else if (act == PauseOverlay.Action.MAIN_MENU) {
                thread.showTitle();
                if (haptic != null) haptic.tap();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            PauseOverlay.cancelPress();
            return true;
        }
        return true;
    }

    private boolean handleVictoryTouch(int action, float sx, float sy, float sceneH) {
        if (action == MotionEvent.ACTION_DOWN) {
            LevelCompleteOverlay.onTouchDown(sx, sy, sceneH);
            if (haptic != null) haptic.detent();
            return true;
        } else if (action == MotionEvent.ACTION_UP) {
            LevelCompleteOverlay.Action act = LevelCompleteOverlay.onTouchUp(sx, sy, sceneH);
            if (act == LevelCompleteOverlay.Action.NEXT) {
                thread.advance();
                thread.resumePlay();
                if (haptic != null) haptic.tap();
            } else if (act == LevelCompleteOverlay.Action.RETRY) {
                thread.restart();
                thread.resumePlay();
                if (haptic != null) haptic.tap();
            } else if (act == LevelCompleteOverlay.Action.LEVEL_SELECT) {
                LevelSelectOverlay.setActiveChapterForLevel(thread.levelIndex());
                thread.showLevelSelect();
                if (haptic != null) haptic.tap();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            LevelCompleteOverlay.cancelPress();
            return true;
        }
        return true;
    }

    private boolean handlePlayTouch(MotionEvent e, float sx, float sy, float sceneH) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                // 1. Check HUD Pause button (safe radius 48f)
                float px = GameRenderer.getPauseX();
                float py = GameRenderer.getPauseY(sceneH);
                float pdx = sx - px;
                float pdy = sy - py;
                if (pdx * pdx + pdy * pdy <= 48f * 48f) {
                    thread.showPause();
                    if (haptic != null) haptic.tap();
                    return true;
                }

                // 2. Check HUD Hint button (generous radius 64f)
                float hx = GameRenderer.getHintX();
                float hy = GameRenderer.getHintY(sceneH);
                float hdx = sx - hx;
                float hdy = sy - hy;
                if (hdx * hdx + hdy * hdy <= 64f * 64f) {
                    thread.toggleHint();
                    if (haptic != null) haptic.tap();
                    return true;
                }

                // 3. Check HUD Reset button (generous radius 64f)
                float rx = GameRenderer.getResetX();
                float ry = GameRenderer.getResetY(sceneH);
                float rdx = sx - rx;
                float rdy = sy - ry;
                if (rdx * rdx + rdy * rdy <= 64f * 64f) {
                    thread.restart();
                    if (haptic != null) haptic.tap();
                    return true;
                }

                // 4. Check rotation handle on active shape
                if (thread.hitTestRotationHandle(sx, sy)) {
                    int sel = thread.selectedIndex();
                    if (sel >= 0) {
                        trackingRotate = true;
                        dragActive = false;
                        ShapeObject s = thread.getSelectedShape();
                        float centerAngle = (s != null)
                                ? (float) Math.toDegrees(Math.atan2(sy - s.y, sx - s.x))
                                : 0f;
                        rotateStartAngle = centerAngle;
                        thread.beginRotate(sel, rotateStartAngle);
                        return true;
                    }
                }

                int hit = thread.hitTestAt(sx, sy);
                if (hit >= 0) {
                    dragActive = true;
                    thread.beginDrag(hit, sx, sy);
                    if (haptic != null) haptic.tap();
                } else {
                    thread.deselect();
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                if (e.getPointerCount() >= 2) {
                    dragActive = false;
                    int idx = thread.selectedIndex();
                    if (idx >= 0) {
                        trackingRotate = true;
                        rotateStartAngle = gestureAngle(e);
                        thread.beginRotate(idx, rotateStartAngle);
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                if (trackingRotate) {
                    if (e.getPointerCount() >= 2) {
                        thread.updateRotate(gestureAngle(e));
                    } else {
                        ShapeObject s = thread.getSelectedShape();
                        if (s != null) {
                            float currentAngle = (float) Math.toDegrees(Math.atan2(sy - s.y, sx - s.x));
                            thread.updateRotate(currentAngle);
                        }
                    }
                } else if (dragActive) {
                    thread.onTouch(GameThread.EVENT_MOVE, sx, sy);
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                if (e.getPointerCount() <= 2) {
                    trackingRotate = false;
                    thread.endGesture();
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                thread.onTouch(GameThread.EVENT_UP, sx, sy);
                dragActive = false;
                trackingRotate = false;
                thread.endGesture();
                return true;
            }
        }
        return true;
    }

    private static float gestureAngle(MotionEvent e) {
        float dx = e.getX(0) - e.getX(1);
        float dy = e.getY(0) - e.getY(1);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }
}
