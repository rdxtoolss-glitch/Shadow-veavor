package com.mobodevs.shadowweaver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Shadow Weaver - GameView
 * ========================
 * The SurfaceView: owns the surface, hands the loop a looper, and converts
 * touches into SCENE coordinates.
 *
 * ------------------------------------------------------------------
 * WHY THERE IS NO onDraw
 * ------------------------------------------------------------------
 * A SurfaceView has no window of its own. It is a hole punched through the
 * view hierarchy that the app draws into directly, which is exactly why it
 * can be driven from a background thread and why it never sees the normal
 * invalidate/draw cycle. So all drawing lives in GameThread via the holder,
 * and this class is the plumbing plus the input handling - input being the one
 * thing that genuinely has to run on the UI thread.
 *
 * ------------------------------------------------------------------
 * WHY GameView KNOWS ALMOST NOTHING
 * ------------------------------------------------------------------
 * Screen mode, unlocked level, current level, and selection all live in
 * GameThread, not here. There is exactly one owner for that state, so there
 * is no possibility of the view and the loop disagreeing about which screen
 * is showing - which is the failure mode that produces "I tapped a button
 * and the wrong thing happened" bugs in a two-owner design.
 *
 * ------------------------------------------------------------------
 * COORDINATES
 * ------------------------------------------------------------------
 * The game is authored in a fixed 1000x1600 scene; the surface is whatever
 * the device gives us. Every touch is inverted through the same transform the
 * renderer uses, so the game only ever speaks scene units. This is the single
 * place that conversion happens.
 *
 * ------------------------------------------------------------------
 * GESTURES
 * ------------------------------------------------------------------
 * One finger drags the selected shape. Two fingers rotate it, by tracking the
 * angle between the fingers against the angle when the second finger landed.
 * A second finger cancels an in-progress drag rather than compounding with
 * it, so a player who starts a drag and then adds a finger does not get a
 * shape that jumps.
 */
public class GameView extends SurfaceView implements GameThread.Looper {

    private final GameThread thread;

    // ---- Touch tracking (UI thread only) ----------------------------------

    private boolean dragActive;
    private boolean trackingRotate;
    private float rotateStartAngle;

    // ---- Persistence hook -------------------------------------------------

    /** Notified on the UI thread when the unlocked level advances. */
    public interface Progress {
        void report(int highestUnlocked);
    }

    private Progress onProgress;
    private int lastReportedUnlocked = -1;

    public GameView(Context context, int startLevel, int unlocked, AudioManager audio) {
        super(context);

        SurfaceHolder holder = getHolder();

        thread = new GameThread(holder, new GameThread.LevelListener() {
            @Override
            public void onLevelComplete(final int levelIndex, final long elapsedMillis) {
                // Bounce to the UI thread: the listener is invoked on the loop
                // thread, and anything that touches views or preferences
                // belongs on the main thread.
                post(new Runnable() {
                    @Override
                    public void run() {
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

        // Registered last, once the loop is fully constructed: the surface may
        // already exist, and surfaceCreated can therefore arrive
        // synchronously from inside addCallback.
        holder.addCallback(thread);

        thread.setUnlocked(unlocked);
        lastReportedUnlocked = unlocked;
        thread.loadLevel(startLevel);

        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    // ======================================================================
    // LIFECYCLE  (all called by the activity)
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
                if (thread.showingLevelSelect()) {
                    handleLevelSelect(sx, sy);
                    return true;
                }
                if (thread.celebrationVisible()) {
                    // The win panel is modal: it owns every tap while it is up,
                    // so a tap cannot fall through and grab a shape behind it.
                    handleOverlayAction(
                            LevelCompleteOverlay.hitTest(sx, sy));
                    return true;
                }

                int hit = thread.hitTestAt(sx, sy);
                if (hit >= 0) {
                    dragActive = true;
                    thread.beginDrag(hit, sx, sy);
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                if (thread.showingLevelSelect() || thread.celebrationVisible()) {
                    return true;
                }
                if (e.getPointerCount() >= 2) {
                    // Second finger down: stop any drag and switch to rotate.
                    // Cancelling the drag is what prevents the shape from
                    // snapping under the first finger.
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
                if (thread.showingLevelSelect() || thread.celebrationVisible()) {
                    return true;
                }
                if (trackingRotate && e.getPointerCount() >= 2) {
                    thread.updateRotate(gestureAngle(e) - rotateStartAngle);
                } else if (dragActive) {
                    thread.onTouch(GameThread.EVENT_MOVE, sx, sy);
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                // When a finger leaves a two-finger gesture, end the rotation
                // rather than falling back to a drag, otherwise the remaining
                // finger would drag the shape somewhere unexpected.
                if (e.getPointerCount() <= 2) {
                    trackingRotate = false;
                    thread.endGesture();
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (!thread.showingLevelSelect() && !thread.celebrationVisible()) {
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

    private void handleLevelSelect(float sx, float sy) {
        int level = LevelSelectOverlay.pickedLevel(sx, sy, thread.unlockedLevel());
        if (level >= 0) {
            thread.pickLevel(level);
        } else if (LevelSelectOverlay.hitTest(sx, sy, thread.unlockedLevel())
                == LevelSelectOverlay.Action.CLOSE) {
            thread.showPlay();
        }
    }

    private void handleOverlayAction(LevelCompleteOverlay.Action action) {
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
