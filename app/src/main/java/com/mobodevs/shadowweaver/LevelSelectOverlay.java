package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Shadow Weaver - LevelSelectOverlay
 * ==================================
 * Modern chapter grid with star ratings, lock states, and instant navigation.
 */
final class LevelSelectOverlay {

    enum Action {
        NONE, PICKED, CLOSE
    }

    private static final int COLUMNS = 5;
    private static final int ROWS = 3;

    private static final RectF rect = new RectF();
    private static final Path starPath = new Path();
    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final float[] CELL_L = new float[COLUMNS * ROWS];
    private static final float[] CELL_T = new float[COLUMNS * ROWS];
    private static final float[] CELL_R = new float[COLUMNS * ROWS];
    private static final float[] CELL_B = new float[COLUMNS * ROWS];

    private static float closeL, closeT, closeR, closeB;

    private static void layout() {
        float padX = 56f;
        float top = 310f;
        float gap = 18f;
        float cellW = (ShadowCalculator.SCENE_W - padX * 2 - gap * (COLUMNS - 1)) / COLUMNS;
        float cellH = 158f;
        float gapY = 20f;

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

        float cw = 220f;
        closeL = (ShadowCalculator.SCENE_W - cw) * 0.5f;
        closeR = closeL + cw;
        closeT = top + ROWS * (cellH + gapY) + 32f;
        closeB = closeT + 58f;
    }

    static Action hitTest(float sceneX, float sceneY, int unlocked) {
        return hitTestResult(pickedLevel(sceneX, sceneY, unlocked), sceneX, sceneY);
    }

    static int pickedLevel(float sceneX, float sceneY, int unlocked) {
        layout();
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            if (i > unlocked) continue;
            if (sceneX >= CELL_L[i] && sceneX <= CELL_R[i]
                    && sceneY >= CELL_T[i] && sceneY <= CELL_B[i]) {
                return i;
            }
        }
        return -1;
    }

    private static Action hitTestResult(int picked, float sceneX, float sceneY) {
        if (picked >= 0) return Action.PICKED;
        if (sceneX >= closeL && sceneX <= closeR
                && sceneY >= closeT && sceneY <= closeB) {
            return Action.CLOSE;
        }
        return Action.NONE;
    }

    static void draw(Canvas c, LevelProgress progress, int current) {
        int unlocked = progress != null ? progress.getHighestUnlocked() : current;
        drawInternal(c, progress, unlocked, current);
    }

    static void draw(Canvas c, int unlocked, int current) {
        drawInternal(c, null, unlocked, current);
    }

    private static void drawInternal(Canvas c, LevelProgress progress, int unlocked, int current) {
        layout();

        // Dark modal backdrop
        fill.setColor(0xF0100703);
        c.drawRect(0, 0, ShadowCalculator.SCENE_W, ShadowCalculator.SCENE_H, fill);

        float cx = ShadowCalculator.SCENE_W * 0.5f;

        // Title
        text.setTextAlign(Paint.Align.CENTER);
        text.setColor(0xFFFFE9C4);
        text.setTextSize(46f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("SHADOW WEAVER", cx, 140f, text);

        text.setColor(0xBBF0DFC6);
        text.setTextSize(22f);
        text.setTypeface(android.graphics.Typeface.DEFAULT);
        int totalStars = progress != null ? progress.getTotalStars() : 0;
        c.drawText("CHAPTER I  *  " + totalStars + " / 45 STARS", cx, 186f, text);

        // Subtitle instructions
        text.setColor(0x88F0DFC6);
        text.setTextSize(20f);
        c.drawText("Select a constellation to shape its shadow", cx, 230f, text);

        // 15 Level Cells
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            int stars = progress != null ? progress.getStars(i) : (i < unlocked ? 1 : 0);
            drawCell(c, i, unlocked, current, stars);
        }

        // Back / Close button
        rect.set(closeL, closeT, closeR, closeB);
        fill.setColor(0x33FFC98A);
        c.drawRoundRect(rect, 14f, 14f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setColor(0x88FFE0A8);
        c.drawRoundRect(rect, 14f, 14f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        text.setColor(0xFFFFFFFF);
        text.setTextSize(26f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("RESUME", cx, closeT + 38f, text);

        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCell(Canvas c, int i, int unlocked, int current, int stars) {
        boolean locked = i > unlocked;
        boolean isCurrent = i == current;
        LevelData d = LevelLoader.get(i);

        rect.set(CELL_L[i], CELL_T[i], CELL_R[i], CELL_B[i]);
        if (locked) {
            fill.setColor(0x1A20150D);
        } else if (isCurrent) {
            fill.setColor(0x55B37834);
        } else {
            fill.setColor(0x332A1C12);
        }
        c.drawRoundRect(rect, 16f, 16f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(isCurrent ? 2.6f : 1.5f);
        stroke.setColor(isCurrent ? 0xFFFFE0A8 : (locked ? 0x22FFFFFF : 0x55FFE0A8));
        c.drawRoundRect(rect, 16f, 16f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        float cx = (CELL_L[i] + CELL_R[i]) * 0.5f;

        // Level Number
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.setTextSize(42f);

        if (locked) {
            text.setColor(0x33F0DFC6);
            c.drawText(String.valueOf(i + 1), cx, CELL_T[i] + 58f, text);
            drawPadlock(c, cx, CELL_T[i] + 96f, 0x44F0DFC6);
        } else {
            text.setColor(0xFFF0DFC6);
            c.drawText(String.valueOf(i + 1), cx, CELL_T[i] + 54f, text);

            // Title
            text.setColor(0xAAF0DFC6);
            text.setTextSize(18f);
            text.setTypeface(android.graphics.Typeface.DEFAULT);
            c.drawText(fit(d.title, (CELL_R[i] - CELL_L[i]) * 0.88f, 18f),
                    cx, CELL_T[i] + 86f, text);

            // Star rating row
            drawStars(c, cx, CELL_T[i] + 124f, stars);
        }

        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawStars(Canvas c, float cx, float cy, int stars) {
        float spacing = 26f;
        float r = 9f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            drawStar(c, sx, cy, r, i < stars);
        }
    }

    private static void drawStar(Canvas c, float cx, float cy, float r, boolean filled) {
        starPath.reset();
        for (int i = 0; i < 10; i++) {
            float rad = (float) Math.toRadians(i * 36 - 90);
            float currR = (i % 2 == 0) ? r : (r * 0.45f);
            float px = cx + (float) Math.cos(rad) * currR;
            float py = cy + (float) Math.sin(rad) * currR;
            if (i == 0) starPath.moveTo(px, py);
            else starPath.lineTo(px, py);
        }
        starPath.close();

        if (filled) {
            fill.setColor(0xFFFFD54F);
            c.drawPath(starPath, fill);
        } else {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.2f);
            stroke.setColor(0x33FFE0A8);
            c.drawPath(starPath, stroke);
            stroke.setStyle(Paint.Style.FILL);
        }
    }

    private static void drawPadlock(Canvas c, float cx, float cy, int color) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(color);
        rect.set(cx - 5.5f, cy - 5f, cx + 5.5f, cy + 1.5f);
        c.drawArc(rect, 180f, 180f, false, stroke);

        fill.setColor(color);
        rect.set(cx - 9f, cy - 1f, cx + 9f, cy + 11f);
        c.drawRoundRect(rect, 2.5f, 2.5f, fill);
        stroke.setStyle(Paint.Style.FILL);
    }

    private static final String ELLIPSIS = "\u2026";

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
