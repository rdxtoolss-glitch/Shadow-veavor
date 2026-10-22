package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Shadow Weaver - LevelCompleteOverlay
 * =====================================
 * Modern, frosted glass victory modal with 3-star rating reveal,
 * par stats, and tactile action buttons.
 */
final class LevelCompleteOverlay {

    enum Action {
        NONE, NEXT, RETRY, LEVEL_SELECT
    }

    private static final RectF rect = new RectF();
    private static final Path starPath = new Path();

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final float CARD_W = 660f;
    private static final float CARD_H = 500f;

    private static float cardLeft;
    private static float cardTop;
    private static float nextLeft, nextRight, nextTop, nextBottom;
    private static float retryLeft, retryRight, retryTop, retryBottom;
    private static float selectLeft, selectRight, selectTop, selectBottom;

    private static void layout() {
        float cx = ShadowCalculator.SCENE_W * 0.5f;
        cardLeft = cx - CARD_W * 0.5f;
        cardTop = (ShadowCalculator.SCENE_H - CARD_H) * 0.5f;

        float pad = 24f;
        float btnY = cardTop + 330f;
        float btnH = 68f;
        float btnW = 284f;

        nextLeft = cx - btnW - pad * 0.5f;
        nextRight = nextLeft + btnW;
        nextTop = btnY;
        nextBottom = btnY + btnH;

        retryLeft = cx + pad * 0.5f;
        retryRight = retryLeft + btnW;
        retryTop = btnY;
        retryBottom = btnY + btnH;

        selectLeft = cardLeft + 52f;
        selectRight = cardLeft + CARD_W - 52f;
        selectTop = nextBottom + 20f;
        selectBottom = selectTop + 56f;
    }

    static void ensureLayout() {
        layout();
    }

    static Action hitTest(float sceneX, float sceneY) {
        ensureLayout();

        if (inRect(nextLeft, nextTop, nextRight, nextBottom, sceneX, sceneY)) {
            return Action.NEXT;
        }
        if (inRect(retryLeft, retryTop, retryRight, retryBottom, sceneX, sceneY)) {
            return Action.RETRY;
        }
        if (inRect(selectLeft, selectTop, selectRight, selectBottom, sceneX, sceneY)) {
            return Action.LEVEL_SELECT;
        }
        if (inRect(cardLeft, cardTop, cardLeft + CARD_W, cardTop + CARD_H, sceneX, sceneY)) {
            return Action.NONE;
        }
        return Action.NONE;
    }

    private static boolean inRect(float l, float t, float r, float b, float x, float y) {
        return x >= l && x <= r && y >= t && y <= b;
    }

    static void draw(Canvas c, GameThread g) {
        draw(c, g, 1f);
    }

    static void draw(Canvas c, GameThread g, float reveal) {
        ensureLayout();
        if (reveal <= 0f) return;
        if (reveal > 1f) reveal = 1f;

        float e = 1f - (1f - reveal) * (1f - reveal);

        // Dark modal scrim
        fill.setColor(withAlpha(0xFF0E0A06, (int) (210 * e)));
        c.drawRect(0, 0, ShadowCalculator.SCENE_W, ShadowCalculator.SCENE_H, fill);

        int save = c.save();
        c.translate(0f, (1f - e) * 70f);

        // Card body (frosted dark obsidian panel)
        rect.set(cardLeft, cardTop, cardLeft + CARD_W, cardTop + CARD_H);
        fill.setColor(withAlpha(0xFF24170E, (int) (250 * e)));
        c.drawRoundRect(rect, 26f, 26f, fill);

        // Gold border
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(withAlpha(0xFFFFE0A8, (int) (230 * e)));
        c.drawRoundRect(rect, 26f, 26f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        float cx = ShadowCalculator.SCENE_W * 0.5f;

        // Title
        text.setTextAlign(Paint.Align.CENTER);
        text.setColor(withAlpha(0xFFFFE9C4, (int) (255 * e)));
        text.setTextSize(46f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("SHADOW WOVEN", cx, cardTop + 72f, text);

        // Level Title
        text.setColor(withAlpha(0xBBF0DFC6, (int) (255 * e)));
        text.setTextSize(26f);
        text.setTypeface(android.graphics.Typeface.DEFAULT);
        c.drawText(g.levelTitle(), cx, cardTop + 112f, text);

        // Calculate stars
        long elapsed = g.elapsedMillis();
        int moves = g.moveCount();
        int earnedStars = LevelProgress.calculateStars(g.levelIndex(), elapsed, moves);

        // Draw 3 Rating Stars
        drawStarsRow(c, cx, cardTop + 172f, earnedStars, e);

        // Stats line: Time & Moves vs Par
        long parTime = LevelProgress.getParTimeMillis(g.levelIndex());
        int parMoves = LevelProgress.getParMoves(g.levelIndex());
        String timeStr = "Time: " + formatTime(elapsed) + " (Par: " + (parTime / 1000L) + "s)";
        String moveStr = "Moves: " + moves + " (Par: " + parMoves + ")";

        text.setColor(withAlpha(0xEEF0DFC6, (int) (220 * e)));
        text.setTextSize(22f);
        c.drawText(timeStr + "   |   " + moveStr, cx, cardTop + 248f, text);

        // Buttons
        drawButton(c, "NEXT LEVEL", nextLeft, nextTop, nextRight, nextBottom, e, true);
        drawButton(c, "REPLAY", retryLeft, retryTop, retryRight, retryBottom, e, false);
        drawButton(c, "LEVEL SELECT", selectLeft, selectTop, selectRight, selectBottom, e, false);

        c.restoreToCount(save);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawStarsRow(Canvas c, float cx, float cy, int stars, float e) {
        float starR = 32f;
        float spacing = 76f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean active = (i < stars);
            drawStar(c, sx, cy, starR, active, e);
        }
    }

    private static void drawStar(Canvas c, float cx, float cy, float r, boolean filled, float e) {
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
            // Golden star with glow
            fill.setColor(withAlpha(0xFFFFD54F, (int) (255 * e)));
            c.drawPath(starPath, fill);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2f);
            stroke.setColor(withAlpha(0xFFFFF8E1, (int) (255 * e)));
            c.drawPath(starPath, stroke);
            stroke.setStyle(Paint.Style.FILL);
        } else {
            // Faint outline of empty star
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2f);
            stroke.setColor(withAlpha(0x44FFE0A8, (int) (180 * e)));
            c.drawPath(starPath, stroke);
            stroke.setStyle(Paint.Style.FILL);
        }
    }

    private static void drawButton(Canvas c, String label,
                                   float l, float t, float r, float b,
                                   float e, boolean primary) {
        rect.set(l, t, r, b);
        if (primary) {
            fill.setColor(withAlpha(0xEEB37834, (int) (255 * e)));
            c.drawRoundRect(rect, 16f, 16f, fill);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.5f);
            stroke.setColor(withAlpha(0xFFFFE0A8, (int) (255 * e)));
            c.drawRoundRect(rect, 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);
            text.setColor(withAlpha(0xFFFFFFFF, (int) (255 * e)));
        } else {
            fill.setColor(withAlpha(0x33F0DFC6, (int) (255 * e)));
            c.drawRoundRect(rect, 16f, 16f, fill);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.8f);
            stroke.setColor(withAlpha(0x66FFE0A8, (int) (200 * e)));
            c.drawRoundRect(rect, 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);
            text.setColor(withAlpha(0xEEF0DFC6, (int) (240 * e)));
        }

        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(24f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText(label, (l + r) * 0.5f, (t + b) * 0.5f + 9f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static String formatTime(long ms) {
        long total = ms / 1000L;
        long m = total / 60L;
        long s = total % 60L;
        if (m > 0) return m + "m " + (s < 10 ? "0" : "") + s + "s";
        return s + "." + ((ms % 1000L) / 100L) + "s";
    }

    private static int withAlpha(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
