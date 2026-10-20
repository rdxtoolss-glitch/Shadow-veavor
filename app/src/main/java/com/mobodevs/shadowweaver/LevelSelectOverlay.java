package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Shadow Weaver - LevelSelectOverlay
 * ==================================
 * A grid of the 15 levels, drawn over the live scene in the same way the
 * win panel is.
 *
 * ------------------------------------------------------------------
 * WHY THERE IS NO SCROLLING
 * ------------------------------------------------------------------
 * Fifteen levels fit in a 5x3 grid inside the 1000x1600 scene, so the whole
 * map is visible at once with no scrolling, no fling physics, and no risk of
 * a player being unable to reach a level they can see. A scrollable list
 * would have been more code and a worse experience for a grid this small.
 *
 * Progress is remembered as a single "highest level unlocked" integer, so
 * there is no per-level save file to keep in sync or migrate.
 */
final class LevelSelectOverlay {

    enum Action {
        NONE, PICKED, CLOSE
    }

    private static final int COLUMNS = 5;
    private static final int ROWS = 3;

    private static final RectF rect = new RectF();
    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ---- Grid geometry, in SCENE units ------------------------------------
    // Recomputed by layout() and read by both draw() and hitTest() so they
    // can never disagree.

    private static final float[] CELL_L = new float[COLUMNS * ROWS];
    private static final float[] CELL_T = new float[COLUMNS * ROWS];
    private static final float[] CELL_R = new float[COLUMNS * ROWS];
    private static final float[] CELL_B = new float[COLUMNS * ROWS];

    private static float closeL, closeT, closeR, closeB;

    private static void layout() {
        float padX = 56f;
        float top = 300f;
        float gap = 18f;
        float cellW = (ShadowCalculator.SCENE_W - padX * 2 - gap * (COLUMNS - 1)) / COLUMNS;
        float cellH = 150f;
        float gapY = 22f;

        for (int i = 0; i < COLUMNS * ROWS; i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            float l = padX + col * (cellW + gap);
            float t = top + row * (cellH + gapY);
            CELL_L[i] = l;
            CELL_T[i] = t;
            CELL_R[i] = l + cellW;
            CELL_B[i] = t + cellH;
        }

        float cw = 190f;
        closeL = (ShadowCalculator.SCENE_W - cw) * 0.5f;
        closeR = closeL + cw;
        closeT = top + ROWS * (cellH + gapY) + 26f;
        closeB = closeT + 56f;
    }

    // ======================================================================
    // HIT TESTING  (UI thread, SCENE coordinates)
    // ======================================================================

    /**
     * @param unlocked highest level index the player may open
     * @return the 0-based level picked, or -1 for none/close
     */
    static Action hitTest(float sceneX, float sceneY, int unlocked) {
        return hitTestResult(pickedLevel(sceneX, sceneY, unlocked), sceneX, sceneY);
    }

    /**
     * The 0-based level under this point, or -1 for none.
     *
     * Split out from hitTest() because "which cell" and "what did the player
     * do" are different questions, and the answer is needed in both forms:
     * the cell index to load, and the action for the Close button.
     */
    static int pickedLevel(float sceneX, float sceneY, int unlocked) {
        layout();
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            if (i > unlocked) continue;                 // locked cells ignore taps
            if (sceneX >= CELL_L[i] && sceneX <= CELL_R[i]
                    && sceneY >= CELL_T[i] && sceneY <= CELL_B[i]) {
                return i;
            }
        }
        return -1;
    }

    /** Resolve a cell hit into an action, or recognise the Close button. */
    private static Action hitTestResult(int level, float sceneX, float sceneY) {
        if (level >= 0) return Action.PICKED;
        layout();
        if (sceneX >= closeL && sceneX <= closeR
                && sceneY >= closeT && sceneY <= closeB) {
            return Action.CLOSE;
        }
        return Action.NONE;
    }

    // ======================================================================
    // DRAW  (loop thread)
    // ======================================================================

    static void draw(Canvas c, int unlocked, int current) {
        layout();

        fill.setColor(0xE6100703);
        c.drawRect(0, 0, ShadowCalculator.SCENE_W, ShadowCalculator.SCENE_H, fill);

        // Heading.
        text.setTextAlign(Paint.Align.CENTER);
        text.setColor(0xF0F0DFC6);
        text.setTextSize(48f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("Choose a level", ShadowCalculator.SCENE_W * 0.5f, 150f, text);

        text.setColor(0x8AF0DFC6);
        text.setTextSize(24f);
        text.setTypeface(android.graphics.Typeface.DEFAULT);
        c.drawText("Move the shapes until their shadows match the glow",
                ShadowCalculator.SCENE_W * 0.5f, 196f, text);

        for (int i = 0; i < COLUMNS * ROWS; i++) {
            drawCell(c, i, unlocked, current);
        }

        // Close.
        fill.setColor(0x1AFFC98A);
        rect.set(closeL, closeT, closeR, closeB);
        c.drawRoundRect(rect, 14f, 14f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(0x806B4A2A);
        c.drawRoundRect(rect, 14f, 14f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        text.setColor(0xEBF0DFC6);
        text.setTextSize(26f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("Back", ShadowCalculator.SCENE_W * 0.5f, closeT + 37f, text);

        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCell(Canvas c, int i, int unlocked, int current) {
        boolean locked = i > unlocked;
        boolean isCurrent = i == current;
        LevelData d = LevelLoader.get(i);

        if (locked) {
            fill.setColor(0x0AFF0000);
        } else if (isCurrent) {
            fill.setColor(0x4DFFC98A);
        } else {
            fill.setColor(0x26FFC98A);
        }
        rect.set(CELL_L[i], CELL_T[i], CELL_R[i], CELL_B[i]);
        c.drawRoundRect(rect, 14f, 14f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(isCurrent ? 2.4f : 1.4f);
        stroke.setColor(isCurrent ? 0xFFFFE0B0 : 0x596B4A2A);
        c.drawRoundRect(rect, 14f, 14f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        float cx = (CELL_L[i] + CELL_R[i]) * 0.5f;

        // Number.
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.setTextSize(44f);
        if (locked) {
            text.setColor(0x33F0DFC6);
            c.drawText(String.valueOf(i + 1), cx, CELL_T[i] + 62f, text);
            // A padlock, drawn rather than shipped as a drawable, so there is
            // no extra resource for one small icon.
            drawPadlock(c, cx, CELL_T[i] + 96f, 0x33F0DFC6);
        } else {
            text.setColor(0xF0F0DFC6);
            c.drawText(String.valueOf(i + 1), cx, CELL_T[i] + 62f, text);
            // Level name, ellipsised by hand: it can be long ("Butterfly")
            // and the cell is only ~150 units wide.
            text.setColor(0x8AF0DFC6);
            text.setTextSize(20f);
            text.setTypeface(android.graphics.Typeface.DEFAULT);
            c.drawText(fit(d.title, (CELL_R[i] - CELL_L[i]) * 0.86f, 20f),
                    cx, CELL_T[i] + 98f, text);
        }

        // A tick on anything already solved, so progress is visible at a
        // glance without needing a separate stats screen.
        if (!locked && i < unlocked) {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(3f);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setColor(0x99FFD79A);
            float tx = CELL_R[i] - 22f;
            float ty = CELL_T[i] + 22f;
            c.drawLine(tx - 7f, ty, tx - 2f, ty + 5f, stroke);
            c.drawLine(tx - 2f, ty + 5f, tx + 7f, ty - 6f, stroke);
            stroke.setStrokeCap(Paint.Cap.BUTT);
        }

        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawPadlock(Canvas c, float cx, float cy, int color) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.4f);
        stroke.setColor(color);
        rect.set(cx - 5.5f, cy - 5f, cx + 5.5f, cy + 1.5f);
        c.drawArc(rect, 180f, 180f, false, stroke);

        fill.setColor(color);
        rect.set(cx - 9f, cy - 1f, cx + 9f, cy + 11f);
        c.drawRoundRect(rect, 2.5f, 2.5f, fill);
        stroke.setStyle(Paint.Style.FILL);
    }

    /**
     * Ellipsis as an escape so the file stays pure ASCII and compiles
     * identically whatever encoding the build defaults to.
     */
    private static final String ELLIPSIS = "\u2026";

    /**
     * Truncate a label with an ellipsis so it fits the given width. Measured
     * rather than guessed at a character count, because the labels differ in
     * length and a fixed count would either clip or waste space.
     *
     * The shared text Paint is left at the requested size on return, so the
     * caller does not have to restore it.
     */
    private static String fit(String s, float maxWidth, float textSize) {
        if (s == null) return "";
        text.setTextSize(textSize);
        if (text.measureText(s) <= maxWidth) return s;
        String out = s;
        while (out.length() > 1 && text.measureText(out + ELLIPSIS) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + ELLIPSIS;
    }
}
