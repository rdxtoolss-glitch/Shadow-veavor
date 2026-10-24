package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * Shadow Weaver - GameThread
 * ==========================
 * The game loop.
 *
 * Owns: the per-level state, the 60Hz simulation (idle drift, candle
 * flicker, easing), the match evaluation, and the drawing of one frame.
 *
 * ------------------------------------------------------------------
 * THREADING
 * ------------------------------------------------------------------
 * Rendering must happen on its own thread because SurfaceHolder
 * lockCanvas() can block for a whole frame waiting for the display. Doing
 * that on the UI thread is the single most common cause of jank in a
 * SurfaceView game.
 *
 * That means the game state IS shared with the UI thread, so:
 *
 *   - {@link #lockState()} / {@link #unlockState()} guard every field below.
 *     The UI thread only ever holds it for the handful of instructions it
 *     needs to read a touch result, never across a draw.
 *   - the SurfaceHolder callbacks are all forwarded to the UI thread via
 *     post() so the handler state is always touched from one thread.
 *   - {@code running} is volatile, because that is the flag that decides
 *     whether the loop is alive and the UI thread must see it immediately.
 *
 * The important consequence of putting rendering here rather than in
 * onDraw(): onDraw() is not called on a SurfaceView at all. A SurfaceView has
 * no window of its own; you draw into the holder's Surface, and the system's
 * compositor does the rest. So this class implements Runnable and owns a
 * draw() method that the loop calls.
 */
public class GameThread implements Runnable, SurfaceHolder.Callback {

    private static final String TAG = "ShadowWeaverLoop";

    /** Target frame period. 60fps. */
    private static final long FRAME_NS = 1_000_000_000L / 60L;

    /**
     * Guard against unbounded catch-up. If a frame took longer than this, the
     * device is struggling and we should simply skip time rather than try to
     * simulate 200 frames in one go, which would make the game get slower the
     * more badly it is already going.
     */
    private static final long MAX_FRAME_NS = FRAME_NS * 5L;

    // ---- Callback to the view/activity -------------------------------------

    /** Notified on the loop thread whenever a level is solved. */
    public interface LevelListener {
        void onLevelComplete(int levelIndex, long elapsedMillis, int moves);
    }

    // ---- Surface / thread --------------------------------------------------

    private final SurfaceHolder holder;
    private final LevelListener listener;

    private Thread thread;
    private volatile boolean running;
    private volatile boolean surfaceReady;
    private int surfaceW;
    private int surfaceH;
    private boolean surfaceIsPortrait;

    // ---- Shared state (guarded by stateLock) ------------------------------

    private final Object stateLock = new Object();

    private boolean paused;
    private SurfaceHolder surface;

    private int levelIndex;
    private LevelData level;

    private ShapeObject[] shapes = new ShapeObject[0];
    private ShapeObject[] solution = new ShapeObject[0];
    private float[][] targetPolygons = new float[0][];

    private final SilhouetteMatcher matcher = new SilhouetteMatcher();

    private int selected = -1;
    private float matchPercent;
    private float rawIoU;
    private boolean solved;

    private long levelStartNanos;
    private long levelElapsedMillis;

    private boolean showCelebration;

    private final ParticleSystem particles = new ParticleSystem(ShadowCalculator.SCENE_W, ShadowCalculator.SCENE_H);
    private HapticManager hapticSink;
    private LevelProgress progress;
    private int moveCount;
    private boolean hintActive;

    // ---- Simulation (loop thread only) ------------------------------------

    private float time;
    private float candleFlicker = 1f;

    private float selectPulse;

    // ---- Input queue (UI thread writes, loop thread drains) ---------------

    private static final int MAX_PENDING_EVENTS = 64;
    private final int[] pendingX = new int[MAX_PENDING_EVENTS];
    private final int[] pendingY = new int[MAX_PENDING_EVENTS];
    private final int[] pendingKind = new int[MAX_PENDING_EVENTS];
    private int pendingCount;

    /** Where a drag that started is anchored, in SCENE units. */
    private float dragGrabDX;
    private float dragGrabDY;
    private float dragStartRotation;
    private float dragStartAngle;
    private boolean dragging;

    /** Two-finger rotate/zoom gesture state. */
    private boolean pinching;
    private float pinchStartDistance;
    private float pinchStartAngle;
    private float pinchStartRotation;
    private float pinchStartScale;

    public static final int EVENT_DOWN = 0;
    public static final int EVENT_MOVE = 1;
    public static final int EVENT_UP = 2;

    // ======================================================================
    // CONSTRUCTION
    // ======================================================================

    public GameThread(SurfaceHolder holder, LevelListener listener) {
        this.holder = holder;
        this.listener = listener;
    }

    // ======================================================================
    // SURFACE LIFECYCLE  (all forwarded to the UI thread)
    // ======================================================================

    @Override
    public void surfaceCreated(final SurfaceHolder h) {
        post(new Runnable() {
            @Override
            public void run() {
                // Validate the Surface, but keep the HOLDER as the thing we
                // draw through: lockCanvas() is a SurfaceHolder API, and
                // holding the Surface also means we are explicitly responsible
                // for releasing it, which is one more way to get this wrong.
                Surface s = h.getSurface();
                if (s == null || !s.isValid()) return;
                surface = h;
                surfaceReady = true;
                resumeLoop();
            }
        });
    }

    @Override
    public void surfaceChanged(final SurfaceHolder h, final int format,
                               final int width, final int height) {
        post(new Runnable() {
            @Override
            public void run() {
                if (width <= 0 || height <= 0) return;
                surfaceW = width;
                surfaceH = height;
                surfaceIsPortrait = height >= width;
                // The scene is drawn through a single affine transform, so a
                // resize is just a recompute - there is no relayout of state.
                ShadowCalculator.refreshDerived();
            }
        });
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder h) {
        post(new Runnable() {
            @Override
            public void run() {
                surfaceReady = false;
                pauseLoop();
                releaseSurface();
            }
        });
    }

    private void releaseSurface() {
        synchronized (stateLock) {
            // Nothing to release: the Surface is owned by the view system and
            // is invalidated for us in surfaceDestroyed. We only drop our
            // reference so a queued draw cannot use a dead surface.
            surface = null;
        }
    }

    // ======================================================================
    // START / PAUSE / RESUME / STOP
    // ======================================================================

    /** Begin the loop. Safe to call more than once. */
    public void start() {
        synchronized (stateLock) {
            if (paused) paused = false;
        }
        resumeLoop();
    }

    public void pause() {
        synchronized (stateLock) {
            paused = true;
        }
    }

    public void resume() {
        synchronized (stateLock) {
            paused = false;
        }
        resumeLoop();
    }

    /** Stop the loop and join the thread. */
    public void shutdown() {
        pauseLoop();
    }

    private void resumeLoop() {
        if (!surfaceReady) return;
        if (running) return;
        running = true;
        thread = new Thread(this, "sw-loop");
        thread.start();
    }

    private void pauseLoop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t == null) return;
        // The loop can be parked in lockCanvas() for up to a frame, so a bare
        // interrupt is not enough on its own; we join with a short budget and
        // the loop checks running between every operation.
        try {
            t.join(500);
            if (t.isAlive()) {
                Log.w(TAG, "loop thread still alive after join; interrupting");
                t.interrupt();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ======================================================================
    // LEVEL CONTROL
    // ======================================================================

    /** Load a level (0-based index) and restart its timer. */
    public void loadLevel(final int index) {
        post(new Runnable() {
            @Override
            public void run() {
                synchronized (stateLock) {
                    if (index < 0 || index >= LevelLoader.count()) return;
                    mode = MODE_PLAY;
                    levelIndex = index;
                    level = LevelLoader.get(index);
                    shapes = level.createStartingShapes();
                    solution = LevelLoader.createSolutionShapes(index);
                    // One forge, two consumers: the matcher scores against it
                    // and the renderer ghosts it, so they can never drift.
                    targetPolygons = LevelLoader.forgeTarget(index);
                    matcher.setTarget(targetPolygons);
                    matcher.resetCelebration();
                    selected = -1;
                    matchPercent = 0f;
                    rawIoU = 0f;
                    solved = false;
                    showCelebration = false;
                    celebrationReveal = 0f;
                    levelBannerTimer = 1.8f;
                    dragging = false;
                    pinching = false;
                    pendingCount = 0;
                    moveCount = 0;
                    hintActive = false;
                    levelStartNanos = System.nanoTime();
                    levelElapsedMillis = 0L;
                }
            }
        });
    }

    public ShapeObject findSolutionFor(ShapeObject s) {
        if (solution == null || s == null) return null;
        ShapeObject bestSol = null;
        float bestDistSq = Float.MAX_VALUE;
        for (ShapeObject sol : solution) {
            if (sol.kind == s.kind && Math.abs(sol.width - s.width) < 1f && Math.abs(sol.height - s.height) < 1f) {
                float dx = sol.x - s.x;
                float dy = sol.y - s.y;
                float d2 = dx * dx + dy * dy;
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    bestSol = sol;
                }
            }
        }
        return bestSol;
    }

    public ParticleSystem particles() {
        return particles;
    }

    public void attachHaptics(HapticManager haptic) {
        this.hapticSink = haptic;
    }

    public void attachProgress(LevelProgress progress) {
        this.progress = progress;
    }

    public LevelProgress progress() {
        return progress;
    }

    public boolean isSoundEnabled() {
        return chimeSink != null && !chimeSink.isMuted();
    }

    public boolean isHapticsEnabled() {
        return hapticSink != null && hapticSink.isEnabled();
    }

    public int moveCount() {
        synchronized (stateLock) {
            return moveCount;
        }
    }

    public boolean isHintActive() {
        synchronized (stateLock) {
            return hintActive;
        }
    }

    public void toggleHint() {
        synchronized (stateLock) {
            hintActive = !hintActive;
        }
    }

    public void deselect() {
        synchronized (stateLock) {
            if (selected >= 0 && selected < shapes.length) {
                shapes[selected].targetElevation = 0f;
            }
            selected = -1;
            dragging = false;
            pinching = false;
        }
    }

    public ShapeObject getSelectedShape() {
        synchronized (stateLock) {
            if (selected >= 0 && selected < shapes.length) {
                return shapes[selected];
            }
            return null;
        }
    }

    public boolean hitTestRotationHandle(float sceneX, float sceneY) {
        synchronized (stateLock) {
            if (selected < 0 || selected >= shapes.length) return false;
            ShapeObject s = shapes[selected];
            float dialR = s.maxRadius() + 24f;
            float handleAngleRad = (float) Math.toRadians(s.rotation);
            float hrx = s.x + dialR * (float) Math.cos(handleAngleRad);
            float hry = s.y + dialR * (float) Math.sin(handleAngleRad);

            // Distance to knob
            float dx = sceneX - hrx;
            float dy = sceneY - hry;
            if (dx * dx + dy * dy <= 44f * 44f) return true;

            // Distance to ring band
            float cdx = sceneX - s.x;
            float cdy = sceneY - s.y;
            float dist = (float) Math.hypot(cdx, cdy);
            return Math.abs(dist - dialR) <= 24f;
        }
    }

    public int levelIndex() {
        synchronized (stateLock) {
            return levelIndex;
        }
    }

    public int levelCount() {
        return LevelLoader.count();
    }

    public String levelTitle() {
        synchronized (stateLock) {
            return level == null ? "" : level.title;
        }
    }

    public String levelHint() {
        synchronized (stateLock) {
            return level == null ? "" : level.hint;
        }
    }

    public float matchPercent() {
        synchronized (stateLock) {
            return matchPercent;
        }
    }

    public boolean isSolved() {
        synchronized (stateLock) {
            return solved;
        }
    }

    public boolean celebrationVisible() {
        synchronized (stateLock) {
            return showCelebration;
        }
    }

    public int selectedIndex() {
        synchronized (stateLock) {
            return selected;
        }
    }

    /** Ask the loop to redraw once without advancing the simulation much. */
    public void requestFrame() {
        // Nothing to do: the loop runs continuously while the surface is
        // valid. This exists so callers do not have to know that.
    }

    // ======================================================================
    // INPUT  (called on the UI thread)
    // ======================================================================

    private void post(Runnable r) {
        // Route through the holder's owning view so all of this is serialised
        // on the main thread. GameView calls attachLoopOwner for this.
        if (loopOwner != null) {
            loopOwner.post(r);
        } else {
            r.run();
        }
    }

    /** A View that can post to the main looper. Set once by GameView. */
    public interface Looper {
        boolean post(Runnable r);
    }

    private Looper loopOwner;

    public void attachLoopOwner(Looper owner) {
        this.loopOwner = owner;
    }

    /**
     * Feed one touch event in SCENE coordinates (already un-scaled by GameView).
     *
     * Events are queued rather than applied immediately, because the loop owns
     * the shape list. Queueing also means a fast drag is sampled at frame rate
     * instead of at touch rate, which is what makes dragging feel smooth rather
     * than jumpy.
     */
    public void onTouch(int kind, float sceneX, float sceneY) {
        synchronized (stateLock) {
            if (pendingCount >= MAX_PENDING_EVENTS) {
                // Overflow: drop the oldest MOVE rather than the newest, so
                // the shape keeps following the finger.
                System.arraycopy(pendingKind, 1, pendingKind, 0, pendingCount - 1);
                System.arraycopy(pendingX, 1, pendingX, 0, pendingCount - 1);
                System.arraycopy(pendingY, 1, pendingY, 0, pendingCount - 1);
                pendingCount--;
            }
            pendingKind[pendingCount] = kind;
            pendingX[pendingCount] = Math.round(sceneX);
            pendingY[pendingCount] = Math.round(sceneY);
            pendingCount++;
        }
    }

    /** Public so GameView can hit-test without duplicating the logic. */
    public int hitTestAt(float sceneX, float sceneY) {
        return hitTest(sceneX, sceneY);
    }

    /** Public entry point for starting a drag from the UI thread. */
    public void beginDrag(int index, float sceneX, float sceneY) {
        if (index < 0 || index >= shapes.length) return;
        selected = index;
        shapes[index].targetElevation = 1.0f;
        moveCount++;
        beginDragInternal(index, sceneX, sceneY);
    }

    /** Exposed for the overlay: advance to the next level. */
    public void advance() {
        if (levelIndex + 1 < LevelLoader.count()) {
            loadLevel(levelIndex + 1);
        }
    }

    public void restart() {
        loadLevel(levelIndex);
    }

    // ======================================================================
    // SCREEN STATE & NAVIGATION
    // ======================================================================

    public static final int MODE_PLAY = 0;
    public static final int MODE_SELECT = 1;
    public static final int MODE_PAUSE = 2;

    private int mode = MODE_PLAY;
    private ScreenState screenState = ScreenState.TITLE;
    private ScreenState previousScreenState = ScreenState.TITLE;
    private int unlocked;

    private float celebrationReveal = 0f;
    private float levelBannerTimer = 0f;

    public ScreenState getScreenState() {
        synchronized (stateLock) {
            return screenState;
        }
    }

    public void setScreenState(ScreenState state) {
        synchronized (stateLock) {
            if (this.screenState != state) {
                this.previousScreenState = this.screenState;
            }
            this.screenState = state;
            if (state == ScreenState.PAUSE) {
                mode = MODE_PAUSE;
            } else if (state == ScreenState.LEVEL_SELECT) {
                mode = MODE_SELECT;
            } else if (state == ScreenState.PLAY) {
                mode = MODE_PLAY;
            }
        }
    }

    public ScreenState getPreviousScreenState() {
        synchronized (stateLock) {
            return previousScreenState;
        }
    }

    public float celebrationReveal() {
        synchronized (stateLock) {
            return celebrationReveal;
        }
    }

    public float levelBannerAlpha() {
        synchronized (stateLock) {
            if (levelBannerTimer <= 0f) return 0f;
            if (levelBannerTimer > 1.3f) {
                return Math.max(0f, Math.min(1f, (1.8f - levelBannerTimer) / 0.5f));
            } else if (levelBannerTimer < 0.5f) {
                return Math.max(0f, Math.min(1f, levelBannerTimer / 0.5f));
            }
            return 1.0f;
        }
    }

    public AudioManager audio() {
        return chimeSink;
    }

    public HapticManager haptics() {
        return hapticSink;
    }

    public void setUnlocked(int level) {
        synchronized (stateLock) {
            unlocked = Math.max(0, Math.min(level, LevelLoader.count() - 1));
        }
    }

    public int unlockedLevel() {
        synchronized (stateLock) {
            return unlocked;
        }
    }

    public void showTitle() {
        setScreenState(ScreenState.TITLE);
    }

    public void showLevelSelect() {
        setScreenState(ScreenState.LEVEL_SELECT);
    }

    public void showPause() {
        setScreenState(ScreenState.PAUSE);
    }

    public void resumePlay() {
        setScreenState(ScreenState.PLAY);
    }

    public void showPlay() {
        setScreenState(ScreenState.PLAY);
    }

    public void showTutorial() {
        setScreenState(ScreenState.HOW_TO_PLAY);
    }

    public void showCodex() {
        setScreenState(ScreenState.CODEX);
    }

    public void showSettings() {
        setScreenState(ScreenState.SETTINGS);
    }

    public int mode() {
        synchronized (stateLock) {
            return mode;
        }
    }

    public boolean showingLevelSelect() {
        return getScreenState() == ScreenState.LEVEL_SELECT;
    }

    public boolean showingPause() {
        return getScreenState() == ScreenState.PAUSE;
    }

    /** Select a level from the grid and return to play. */
    public void pickLevel(int index) {
        loadLevel(index);
        setScreenState(ScreenState.PLAY);
    }

    // ======================================================================
    // THE LOOP
    // ======================================================================

    @Override
    public void run() {
        long previous = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            long delta = now - previous;
            if (delta < 0) delta = 0;                 // clock went backwards
            if (delta > MAX_FRAME_NS) delta = MAX_FRAME_NS;
            previous = now;

            boolean doPause;
            SurfaceHolder target;
            int w;
            int h;
            synchronized (stateLock) {
                // surfaceReady goes true in surfaceCreated, but the size only
                // arrives in surfaceChanged. Drawing at size 0 would compute a
                // degenerate transform and post a frame of garbage, so wait
                // for a real size before the first draw.
                doPause = paused || !surfaceReady || surfaceW <= 0 || surfaceH <= 0;
                target = surface;
                w = surfaceW;
                h = surfaceH;
            }

            if (doPause || target == null) {
                sleepQuietly(8);
                continue;
            }

            float dt = delta / 1_000_000_000f;
            time += dt;

            update(dt);
            drawFrame(target, w, h);

            // Sleep out the remainder of the frame. Done AFTER the work so
            // the loop is not capped at half rate on a device where drawing
            // is fast.
            long remaining = FRAME_NS - (System.nanoTime() - now);
            if (remaining > 0) sleepQuietly(remaining / 1_000_000L);
        }
    }

    private static void sleepQuietly(long ms) {
        if (ms < 1) ms = 1;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ======================================================================
    // SIMULATION
    // ======================================================================

    private void update(float dt) {
        drainInput();
        ScreenState state = getScreenState();
        if (state == ScreenState.PLAY || state == ScreenState.PAUSE || state == ScreenState.LEVEL_COMPLETE) {
            stepIdleDrift(dt);
            stepCandle(dt);
            stepSelection(dt);
            updateResonance(dt);
            evaluateMatch();
        } else {
            stepCandle(dt);
            particles.update(dt, ShadowCalculator.LIGHT_X, ShadowCalculator.LIGHT_Y, candleFlicker);
        }

        synchronized (stateLock) {
            if (screenState == ScreenState.LEVEL_COMPLETE) {
                celebrationReveal = Math.min(1.0f, celebrationReveal + dt * 1.4f);
            }
            if (levelBannerTimer > 0f) {
                levelBannerTimer = Math.max(0f, levelBannerTimer - dt);
            }
        }
    }

    /** Apply queued touches. */
    private void drainInput() {
        while (true) {
            int kind;
            float sx;
            float sy;
            synchronized (stateLock) {
                if (pendingCount == 0) return;
                kind = pendingKind[0];
                sx = pendingX[0];
                sy = pendingY[0];
                System.arraycopy(pendingKind, 1, pendingKind, 0, pendingCount - 1);
                System.arraycopy(pendingX, 1, pendingX, 0, pendingCount - 1);
                System.arraycopy(pendingY, 1, pendingY, 0, pendingCount - 1);
                pendingCount--;
            }
            handleTouch(kind, sx, sy);
        }
    }

    private void handleTouch(int kind, float x, float y) {
        switch (kind) {
            case EVENT_DOWN: {
                if (solved) {
                    // Taps on a solved level are for the celebration overlay,
                    // which is drawn on top; let it through.
                    return;
                }
                int hit = hitTest(x, y);
                if (hit >= 0) {
                    selected = hit;
                    beginDrag(hit, x, y);
                } else {
                    selected = -1;
                    dragging = false;
                }
                break;
            }
            case EVENT_MOVE: {
                if (dragging && selected >= 0) {
                    moveDrag(x, y);
                }
                break;
            }
            case EVENT_UP: {
                endGesture();
                break;
            }
        }
    }

    private void beginDragInternal(int index, float x, float y) {
        ShapeObject s = shapes[index];
        dragGrabDX = s.x - x;
        dragGrabDY = s.y - y;
        s.targetX = s.x;
        s.targetY = s.y;
        s.isDragging = true;
        s.isSnapping = false;
        dragging = true;
    }

    private void moveDrag(float x, float y) {
        ShapeObject s = shapes[selected];
        float nx = ShadowCalculator.clampStageX(x + dragGrabDX);
        float ny = ShadowCalculator.clampStageY(y + dragGrabDY);
        s.targetX = nx;
        s.targetY = ny;
        s.isDragging = true;
        s.isSnapping = false;
        s.spin = 0f;
    }

    /**
     * Rotation gesture. Driven by GameView, which owns the multi-touch
     * bookkeeping and calls in with the absolute gesture angle.
     */
    public void beginRotate(int index, float gestureAngle) {
        if (index < 0 || index >= shapes.length) return;
        selected = index;
        ShapeObject s = shapes[index];
        s.targetElevation = 1.0f;
        s.isRotating = true;
        s.isSnapping = false;
        moveCount++;
        dragging = true;
        dragStartRotation = s.rotation;
        dragStartAngle = gestureAngle;
        s.targetRotation = s.rotation;
        pinching = true;
    }

    public void updateRotate(float gestureAngle) {
        if (!pinching || selected < 0 || selected >= shapes.length) return;
        ShapeObject s = shapes[selected];
        float delta = ShapeObject.normalizeDeltaAngle(gestureAngle - dragStartAngle);
        float newRot = ShapeObject.normalizeAngle(dragStartRotation + delta);
        s.targetRotation = newRot;
        s.isRotating = true;
        s.isSnapping = false;

        int detent = (int) Math.floor((newRot + 7.5f) / 15f);
        if (s.lastDetentIndex != -1 && s.lastDetentIndex != detent) {
            if (hapticSink != null) hapticSink.detent();
            if (chimeSink != null) chimeSink.playTick();
        }
        s.lastDetentIndex = detent;
    }

    public void endGesture() {
        if (selected >= 0 && selected < shapes.length) {
            ShapeObject s = shapes[selected];
            s.targetElevation = 0f;
            s.isDragging = false;
            s.isRotating = false;
            checkAndApplySnap(s);
        }
        for (ShapeObject s : shapes) {
            s.isDragging = false;
            s.isRotating = false;
        }
        dragging = false;
        pinching = false;
    }

    private void checkAndApplySnap(ShapeObject s) {
        if (solution == null || solved) return;
        ShapeObject bestSol = null;
        float bestDistSq = Float.MAX_VALUE;

        for (ShapeObject sol : solution) {
            if (sol.kind == s.kind && Math.abs(sol.width - s.width) < 1f && Math.abs(sol.height - s.height) < 1f) {
                float dx = sol.x - s.x;
                float dy = sol.y - s.y;
                float d2 = dx * dx + dy * dy;
                if (d2 < bestDistSq) {
                    bestDistSq = d2;
                    bestSol = sol;
                }
            }
        }

        if (bestSol != null) {
            float posDist = (float) Math.sqrt(bestDistSq);
            float rotDiff = ShapeObject.computeAngularDifference(s.kind, s.rotation, bestSol.rotation);

            // Lock field threshold: posDist <= 32f and rotDiff <= 10f
            if (posDist <= 32f && rotDiff <= 10f) {
                s.snapTo(bestSol.x, bestSol.y, bestSol.rotation);
                if (hapticSink != null) hapticSink.tap();
                if (chimeSink != null) chimeSink.playSnap();
                particles.burst(s.x, s.y, 10);
            }
        }
    }

    private void updateResonance(float dt) {
        if (solution == null || shapes == null) return;
        for (int i = 0; i < shapes.length; i++) {
            ShapeObject s = shapes[i];
            ShapeObject bestSol = null;
            float bestDistSq = Float.MAX_VALUE;

            for (ShapeObject sol : solution) {
                if (sol.kind == s.kind && Math.abs(sol.width - s.width) < 1f && Math.abs(sol.height - s.height) < 1f) {
                    float dx = sol.x - s.x;
                    float dy = sol.y - s.y;
                    float d2 = dx * dx + dy * dy;
                    if (d2 < bestDistSq) {
                        bestDistSq = d2;
                        bestSol = sol;
                    }
                }
            }

            if (bestSol != null) {
                float posDist = (float) Math.sqrt(bestDistSq);
                float rotDiff = ShapeObject.computeAngularDifference(s.kind, s.rotation, bestSol.rotation);

                float posFactor = Math.max(0f, 1f - posDist / 90f);
                float rotFactor = Math.max(0f, 1f - rotDiff / 18f);
                float targetResonance = posFactor * rotFactor;

                s.proximityResonance += (targetResonance - s.proximityResonance) * Math.min(1.0f, dt * 10f);
            } else {
                s.proximityResonance = 0f;
            }
        }
    }

    /** Which shape is under this point, if any. Front-most (highest index) wins. */
    private int hitTest(float x, float y) {
        // Iterate backwards so the shape drawn on top is picked first.
        for (int i = shapes.length - 1; i >= 0; i--) {
            ShapeObject s = shapes[i];
            // A generous circle is much kinder to a finger than the true
            // silhouette, and matches what is drawn as the grab area.
            float r = s.maxRadius() * 0.75f + 24f;
            float dx = x - s.x;
            float dy = y - s.y;
            if (dx * dx + dy * dy <= r * r) return i;
        }
        return -1;
    }

    private void stepIdleDrift(float dt) {
        synchronized (stateLock) {
            if (level == null) return;
        }
        particles.update(dt, ShadowCalculator.LIGHT_X, ShadowCalculator.LIGHT_Y, candleFlicker);
        for (ShapeObject s : shapes) {
            s.stepPhysics(dt);
        }
        for (int i = 0; i < shapes.length; i++) {
            for (int j = i + 1; j < shapes.length; j++) {
                ShapeObject.applySoftRepulsion(shapes[i], shapes[j], dt);
            }
        }
    }

    private void stepCandle(float dt) {
        // Layered sine "flicker". Real candlelight is a fast small jitter on
        // top of a slow breathing swell; one sine alone reads as a pulse, and
        // random alone reads as noise. This sums three incommensurate
        // frequencies so it never visibly repeats.
        float t = time;
        candleFlicker = 1f
                + 0.055f * (float) Math.sin(t * 5.3f)
                + 0.030f * (float) Math.sin(t * 11.7f + 1.3f)
                + 0.018f * (float) Math.sin(t * 23.1f + 2.7f);
    }

    private void stepSelection(float dt) {
        float target = selected >= 0 ? 1f : 0f;
        // Exponential ease, framerate independent.
        selectPulse += (target - selectPulse) * Math.min(1f, dt * 12f);
    }

    /**
     * Project every shape and score it against the target.
     *
     * The score is only recomputed when something actually moved. Idle drift
     * moves things constantly, so this guard is what stops a drifting shape
     * from firing chimes by accident.
     */
    private void evaluateMatch() {
        if (level == null) return;

        float[][] polys = new float[shapes.length][];
        for (int i = 0; i < shapes.length; i++) {
            polys[i] = ShadowCalculator.project(shapes[i]);
        }

        float iou = matcher.evaluate(polys);
        float percent = matcher.reportMatch(iou);
        boolean complete = matcher.isComplete(iou);

        int chimes = 0;
        synchronized (stateLock) {
            // Only celebrate a mark on a rising crossing, and never while the
            // level is already solved.
            if (!solved) {
                chimes = matcher.newlyCrossedChimes(iou);
            }
            rawIoU = iou;
            matchPercent = percent;

            if (complete && !solved) {
                solved = true;
                levelElapsedMillis = (System.nanoTime() - levelStartNanos) / 1_000_000L;
                showCelebration = true;
                celebrateNow = true;
                celebrationReveal = 0f;
                screenState = ScreenState.LEVEL_COMPLETE;
                // Unlock the next level immediately rather than waiting for the
                // player to press Next: if they close the app on the win
                // screen, the progress should still be kept.
                if (levelIndex + 1 < LevelLoader.count() && levelIndex + 1 > unlocked) {
                    unlocked = levelIndex + 1;
                }
            }
        }

        if (chimes != 0 && chimeSink != null) {
            chimeSink.playChimes(chimes);
        }

        // Notify outside the lock: the listener bounces to the UI thread and
        // may call back in, which would deadlock a non-reentrant lock.
        if (celebrateNow) {
            celebrateNow = false;
            particles.burstCelebration(500f, 380f);
            if (hapticSink != null) hapticSink.victory();
            if (chimeSink != null) chimeSink.playCelebration();
            if (listener != null) listener.onLevelComplete(levelIndex, levelElapsedMillis, moveCount);
        }
    }

    private boolean celebrateNow;
    private AudioManager chimeSink;

    public void attachAudio(AudioManager audio) {
        this.chimeSink = audio;
    }

    // ======================================================================
    // ACCESSORS FOR THE RENDERER  (loop thread only, no lock needed)
    // ======================================================================

    ShapeObject[] shapes() {
        return shapes;
    }

    ShapeObject[] solution() {
        return solution;
    }

    /**
     * The level's target silhouette, in wall space, for drawing the ghost.
     *
     * Exposed rather than re-derived so the renderer and the matcher are
     * guaranteed to be looking at literally the same polygons. If they ever
     * disagreed, the ghost would not line up with the thing being scored.
     *
     * Loop thread only.
     */
    float[][] targetPolygons() {
        return targetPolygons;
    }

    SilhouetteMatcher matcher() {
        return matcher;
    }

    float candleFlicker() {
        return candleFlicker;
    }

    float timeSeconds() {
        return time;
    }

    float selectPulse() {
        return selectPulse;
    }

    boolean celebration() {
        synchronized (stateLock) {
            return showCelebration;
        }
    }

    long elapsedMillis() {
        synchronized (stateLock) {
            return solved ? levelElapsedMillis
                    : (levelStartNanos == 0 ? 0
                    : (System.nanoTime() - levelStartNanos) / 1_000_000L);
        }
    }

    // ======================================================================
    // DRAW
    // ======================================================================

    private void drawFrame(SurfaceHolder target, int w, int h) {
        Canvas canvas = null;
        try {
            canvas = target.lockCanvas();
            if (canvas == null) return;
            canvas.drawColor(0xFF140D08);
            GameRenderer.drawScene(canvas, w, h, this, surfaceIsPortrait);
        } catch (Throwable t) {
            // A throw from one frame (e.g. the surface went away mid-lock)
            // must not kill the loop.
            Log.w(TAG, "frame failed", t);
        } finally {
            if (canvas != null) {
                try {
                    target.unlockCanvasAndPost(canvas);
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
