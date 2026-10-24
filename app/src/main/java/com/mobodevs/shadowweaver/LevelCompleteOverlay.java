package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Shadow Weaver - LevelCompleteOverlay
 * =====================================
 * Victory celebration modal with sequential 3-star reveal,
 * par time and move efficiency breakdown, and tactile buttons.
 */
public final class LevelCompleteOverlay {

    public enum Action {
        NONE, NEXT, RETRY, LEVEL_SELECT
    }

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path starPath = new Path();
    private static final RectF cardRect = new RectF();

    private static final UiButton btnNext = new UiButton();
    private static final UiButton btnRetry = new UiButton();
    private static final UiButton btnSelect = new UiButton();

    private static boolean layoutReady;

    private static void layout() {
        if (layoutReady) return;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardW = 720f;
        float cardH = 680f;
        float cardL = cx - cardW * 0.5f;
        float cardT = (ShadowCalculator.SCENE_H - cardH) * 0.5f;
        cardRect.set(cardL, cardT, cardL + cardW, cardT + cardH);

        float pad = 24f;
        float btnY = cardT + 470f;
        float btnH = 74f;
        float btnW = 300f;

        btnNext.setBounds(cx - btnW - pad * 0.5f, btnY, cx - pad * 0.5f, btnY + btnH);
        btnNext.isPrimary = true;
        btnNext.cornerRadius = 18f;

        btnRetry.setBounds(cx + pad * 0.5f, btnY, cx + pad * 0.5f + btnW, btnY + btnH);
        btnRetry.cornerRadius = 18f;

        float selectW = 420f;
        float selectH = 60f;
        float selectY = btnY + btnH + 20f;
        btnSelect.setBounds(cx - selectW * 0.5f, selectY, cx + selectW * 0.5f, selectY + selectH);
        btnSelect.cornerRadius = 16f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        layout();
        btnNext.isPressed = btnNext.hitTest(sx, sy);
        btnRetry.isPressed = btnRetry.hitTest(sx, sy);
        btnSelect.isPressed = btnSelect.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        layout();
        Action act = Action.NONE;

        if (btnNext.isPressed && btnNext.hitTest(sx, sy)) act = Action.NEXT;
        else if (btnRetry.isPressed && btnRetry.hitTest(sx, sy)) act = Action.RETRY;
        else if (btnSelect.isPressed && btnSelect.hitTest(sx, sy)) act = Action.LEVEL_SELECT;

        btnNext.isPressed = false;
        btnRetry.isPressed = false;
        btnSelect.isPressed = false;

        return act;
    }

    public static void cancelPress() {
        btnNext.isPressed = false;
        btnRetry.isPressed = false;
        btnSelect.isPressed = false;
    }

    public static void draw(Canvas c, GameThread g) {
        draw(c, g, 1.0f);
    }

    public static void draw(Canvas c, GameThread g, float reveal) {
        layout();
        if (reveal <= 0f) return;
        if (reveal > 1f) reveal = 1f;

        float sw = ShadowCalculator.SCENE_W;
        float sh = ShadowCalculator.SCENE_H;
        float cx = sw * 0.5f;

        // 1. Dark Modal Scrim
        fill.setColor(withAlpha(0xFF0E0A06, (int) (220 * reveal)));
        c.drawRect(0, 0, sw, sh, fill);

        int save = c.save();
        // Subtle drop-in scale/slide
        float slideY = (1f - reveal) * 60f;
        c.translate(0, slideY);

        // 2. Card Body (Frosted Obsidian Plate)
        fill.setColor(withAlpha(0xFF22160E, (int) (250 * reveal)));
        c.drawRoundRect(cardRect, 28f, 28f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.8f);
        stroke.setColor(withAlpha(0xFFFFE0A8, (int) (230 * reveal)));
        c.drawRoundRect(cardRect, 28f, 28f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 3. Victory Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(46f);
        text.setColor(withAlpha(0xFFFFE9C4, (int) (255 * reveal)));
        c.drawText("SHADOW WOVEN", cx, cardRect.top + 75f, text);

        text.setTextSize(26f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(withAlpha(0xBBF0DFC6, (int) (255 * reveal)));
        c.drawText(g.levelTitle(), cx, cardRect.top + 118f, text);

        // 4. Calculate Earned Stars
        long elapsed = g.elapsedMillis();
        int moves = g.moveCount();
        int earnedStars = LevelProgress.calculateStars(g.levelIndex(), elapsed, moves);

        // 5. Draw Sequential 3 Stars
        drawSequentialStars(c, cx, cardRect.top + 200f, earnedStars, reveal);

        // 6. Par Performance Breakdown Table
        long parTime = LevelProgress.getParTimeMillis(g.levelIndex());
        int parMoves = LevelProgress.getParMoves(g.levelIndex());

        boolean timeStar = (elapsed <= parTime);
        boolean moveStar = (elapsed <= parTime && moves <= parMoves);

        float tableT = cardRect.top + 285f;
        drawStatRow(c, cx, tableT, "Trial Completion", "Solved", true, reveal);
        drawStatRow(c, cx, tableT + 48f, "Time Efficiency", formatTime(elapsed) + " / " + (parTime / 1000L) + "s", timeStar, reveal);
        drawStatRow(c, cx, tableT + 96f, "Move Precision", moves + " / " + parMoves + " moves", moveStar, reveal);

        // 7. Navigation Buttons
        btnNext.label = "NEXT TRIAL";
        btnRetry.label = "RETRY";
        btnSelect.label = "CHAPTER MAP";

        btnNext.draw(c, fill, stroke, text, reveal);
        btnRetry.draw(c, fill, stroke, text, reveal);
        btnSelect.draw(c, fill, stroke, text, reveal);

        c.restoreToCount(save);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawSequentialStars(Canvas c, float cx, float cy, int stars, float reveal) {
        float spacing = 86f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean active = (i < stars);

            // Stagger reveal: star 0 at 0.2, star 1 at 0.45, star 2 at 0.70
            float threshold = 0.20f + i * 0.25f;
            float starProgress = Math.max(0f, Math.min(1f, (reveal - threshold) / 0.25f));

            float r = 36f;
            if (active && starProgress > 0f) {
                // Bounce scale on entrance
                float bounceScale = 1.0f + 0.35f * (float) Math.sin(starProgress * Math.PI);
                r *= bounceScale;
            }

            drawStar(c, sx, cy, r, active && starProgress > 0f, reveal);
        }
    }

    private static void drawStar(Canvas c, float cx, float cy, float r, boolean filled, float alpha) {
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
            fill.setColor(withAlpha(0xFFFFD54F, (int) (255 * alpha)));
            c.drawPath(starPath, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.5f);
            stroke.setColor(withAlpha(0xFFFFF9C4, (int) (240 * alpha)));
            c.drawPath(starPath, stroke);
            stroke.setStyle(Paint.Style.FILL);
        } else {
            fill.setColor(withAlpha(0x33FFE0A8, (int) (200 * alpha)));
            c.drawPath(starPath, fill);
        }
    }

    private static void drawStatRow(Canvas c, float cx, float y, String label, String value, boolean achieved, float alpha) {
        float rowW = 540f;
        float l = cx - rowW * 0.5f;
        float r = cx + rowW * 0.5f;

        text.setTextSize(22f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(withAlpha(0xBBF0DFC6, (int) (255 * alpha)));
        text.setTextAlign(Paint.Align.LEFT);
        c.drawText(label, l, y, text);

        text.setTextAlign(Paint.Align.RIGHT);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(withAlpha(achieved ? 0xFFFFD54F : 0x77F0DFC6, (int) (255 * alpha)));
        String mark = achieved ? " [⭐ OK]" : " [--]";
        c.drawText(value + mark, r, y, text);
    }

    private static String formatTime(long ms) {
        long s = ms / 1000L;
        long tenths = (ms % 1000L) / 100L;
        return s + "." + tenths + "s";
    }

    private static int withAlpha(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
