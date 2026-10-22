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
 * and handles modern touch gestures (1-finger drag, 1-finger dial rotate,
 * 2-finger twist, and HUD button taps).
 */
public class GameView extends SurfaceView implements GameThread.Looper {

    private final GameThread thread;
    private final HapticManager haptic;
    private final LevelProgress progress;
    private final AudioManager audio;

    // ---- Touch tracking (UI thread only) ----------------------------------

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

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    // ======================================================================
    // LIFECYCLE
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

    // ======================================================================
    // TOUCH
    // ======================================================================

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float scale = GameRenderer.fitScale(getWidth(), getHeight());
        float ox = (getWidth() - ShadowCalculator.SCENE_W * scale) * 0.5f;
        float oy = 0f;
        final float sx = GameRenderer.sceneX(e.getX(), scale, ox);
        final float sy = GameRenderer.sceneY(e.getY(), scale, oy);

        switch (e.getActionMasked()) {

            case MotionEvent.ACTION_DOWN: {
                if (thread.showingPause()) {
                    handlePauseAction(PauseOverlay.hitTest(sx, sy));
                    return true;
                }
                if (thread.showingLevelSelect()) {
                    handleLevelSelect(sx, sy);
                    return true;
                }
                if (thread.celebrationVisible()) {
                    handleOverlayAction(LevelCompleteOverlay.hitTest(sx, sy));
                    return true;
                }

                // Check HUD Pause button (cx = 64f, cy = 82f)
                float pdx = sx - 64f;
                float pdy = sy - 82f;
                if (pdx * pdx + pdy * pdy <= 42f * 42f) {
                    thread.showPause();
                    if (haptic != null) haptic.tap();
                    return true;
                }

                // Check HUD Hint button (cx = 96f, cy = SCENE_H - 72f)
                float hdx = sx - 96f;
                float hdy = sy - (ShadowCalculator.SCENE_H - 72f);
                if (hdx * hdx + hdy * hdy <= 44f * 44f) {
                    thread.toggleHint();
                    if (haptic != null) haptic.tap();
                    return true;
                }

                // Check HUD Reset button (cx = SCENE_W - 96f, cy = SCENE_H - 72f)
                float rdx = sx - (ShadowCalculator.SCENE_W - 96f);
                float rdy = sy - (ShadowCalculator.SCENE_H - 72f);
                if (rdx * rdx + rdy * rdy <= 44f * 44f) {
                    thread.restart();
                    if (haptic != null) haptic.tap();
                    return true;
                }

                // Check rotation handle on active shape
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
                if (thread.showingLevelSelect() || thread.showingPause() || thread.celebrationVisible()) {
                    return true;
                }
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
                if (thread.showingLevelSelect() || thread.showingPause() || thread.celebrationVisible()) {
                    return true;
                }
                if (trackingRotate) {
                    if (e.getPointerCount() >= 2) {
                        thread.updateRotate(gestureAngle(e) - rotateStartAngle);
                    } else {
                        ShapeObject s = thread.getSelectedShape();
                        if (s != null) {
                            float currentAngle = (float) Math.toDegrees(Math.atan2(sy - s.y, sx - s.x));
                            thread.updateRotate(currentAngle - rotateStartAngle);
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
                if (!thread.showingLevelSelect() && !thread.showingPause() && !thread.celebrationVisible()) {
                    thread.onTouch(GameThread.EVENT_UP, sx, sy);
                }
                dragActive = false;
                trackingRotate = false;
                thread.endGesture();
                return true;
            }
        }
        return true;
    }

    private void handlePauseAction(PauseOverlay.Action action) {
        if (haptic != null) haptic.tap();
        switch (action) {
            case RESUME:
                thread.resumePlay();
                break;
            case RESTART:
                thread.restart();
                thread.resumePlay();
                break;
            case TOGGLE_SOUND:
                if (audio != null) {
                    audio.toggleMute();
                    if (progress != null) {
                        progress.setSoundMuted(audio.isMuted());
                        progress.save(getContext());
                    }
                }
                break;
            case TOGGLE_HAPTICS:
                if (haptic != null) {
                    haptic.setEnabled(!haptic.isEnabled());
                    if (progress != null) {
                        progress.setHapticsEnabled(haptic.isEnabled());
                        progress.save(getContext());
                    }
                }
                break;
            case LEVEL_SELECT:
                thread.showLevelSelect();
                break;
            case NONE:
                break;
        }
    }

    private void handleLevelSelect(float sx, float sy) {
        int unlocked = (progress != null) ? progress.getHighestUnlocked() : thread.unlockedLevel();
        int level = LevelSelectOverlay.pickedLevel(sx, sy, unlocked);
        if (level >= 0) {
            if (haptic != null) haptic.tap();
            thread.pickLevel(level);
        } else if (LevelSelectOverlay.hitTest(sx, sy, unlocked) == LevelSelectOverlay.Action.CLOSE) {
            if (haptic != null) haptic.tap();
            thread.showPlay();
        }
    }

    private void handleOverlayAction(LevelCompleteOverlay.Action action) {
        if (haptic != null) haptic.tap();
        if (action == LevelCompleteOverlay.Action.NEXT) {
            thread.advance();
        } else if (action == LevelCompleteOverlay.Action.RETRY) {
            thread.restart();
        } else if (action == LevelCompleteOverlay.Action.LEVEL_SELECT) {
            thread.showLevelSelect();
        }
    }

    /** Angle in degrees between the first two active pointers. */
    private static float gestureAngle(MotionEvent e) {
        float dx = e.getX(0) - e.getX(1);
        float dy = e.getY(0) - e.getY(1);
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }
}
