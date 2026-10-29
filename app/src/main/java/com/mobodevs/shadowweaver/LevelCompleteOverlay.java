package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * Shadow Weaver - LevelCompleteOverlay
 * =====================================
 * Victory celebration modal with sequential 3-star reveal,
 * par time and move efficiency breakdown, and tactile buttons.
 * Fully responsive across all aspect ratios using dynamic sceneH.
 */
public final class LevelCompleteOverlay {

    public enum Action {
        NONE, NEXT, RETRY, LEVEL_SELECT
    }

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path starPath = new Path();

    private static Typeface safeTypeface(Typeface family, int style) {
        try {
            return Typeface.create(family, style);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final Typeface SERIF_BOLD = safeTypeface(Typeface.SERIF, Typeface.BOLD);
    private static final Typeface SERIF_NORMAL = safeTypeface(Typeface.SERIF, Typeface.NORMAL);

    private static float cardL;
    private static float cardT;
    private static float cardR;
    private static float cardB;

    private static final UiButton btnNext = new UiButton();
    private static final UiButton btnRetry = new UiButton();
    private static final UiButton btnSelect = new UiButton();

    private static boolean layoutReady;
    private static float lastSceneH = 0f;

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardW = 880f;
        float cardH = 880f;
        cardL = cx - cardW * 0.5f;
        cardT = (sceneH - cardH) * 0.5f;
        cardR = cardL + cardW;
        cardB = cardT + cardH;

        float splitGap = 24f;
        float btnW = 370f;
        float btnH = 94f;
        float btnY = cardT + 620f;

        btnNext.setBounds(cx - btnW - splitGap * 0.5f, btnY, cx - splitGap * 0.5f, btnY + btnH);
        btnNext.isPrimary = true;
        btnNext.cornerRadius = 22f;

        btnRetry.setBounds(cx + splitGap * 0.5f, btnY, cx + splitGap * 0.5f + btnW, btnY + btnH);
        btnRetry.cornerRadius = 22f;

        float selectW = 600f;
        float selectH = 88f;
        float selectY = btnY + btnH + 20f;
        btnSelect.setBounds(cx - selectW * 0.5f, selectY, cx + selectW * 0.5f, selectY + selectH);
        btnSelect.cornerRadius = 22f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        return onTouchDown(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchDown(float sx, float sy, float sceneH) {
        layout(sceneH);
        btnNext.isPressed = btnNext.hitTest(sx, sy);
        btnRetry.isPressed = btnRetry.hitTest(sx, sy);
        btnSelect.isPressed = btnSelect.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        return onTouchUp(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, float sceneH) {
        layout(sceneH);
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
        draw(c, g, 1.0f, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, GameThread g, float reveal) {
        draw(c, g, reveal, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, GameThread g, float reveal, float sceneH) {
        layout(sceneH);
        if (reveal <= 0f) return;
        if (reveal > 1f) reveal = 1f;

        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;
        float cx = sw * 0.5f;

        // 1. Dark Modal Scrim covering entire viewport
        fill.setColor(withAlpha(0xFF0E121E, (int) (225 * reveal)));
        c.drawRect(0, 0, sw, sh, fill);

        int save = c.save();
        // Emil Kowalski entrance: cubic ease-out, scale from 0.95, slide subtly
        float ease = 1.0f - (float) Math.pow(1.0f - reveal, 3.0);
        float cardScale = 0.95f + 0.05f * ease;
        float slideY = (1.0f - ease) * 36f;
        c.translate(0, slideY);
        c.scale(cardScale, cardScale, cx, (cardT + cardB) * 0.5f);

        // 2. Card Body (Luminous Frosted Obsidian-Slate Plate)
        fill.setColor(withAlpha(0xFF1A2234, (int) (252 * reveal)));
        c.drawRoundRect(cardL, cardT, cardR, cardB, 28f, 28f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(withAlpha(0xFFD4AF37, (int) (240 * reveal)));
        c.drawRoundRect(cardL, cardT, cardR, cardB, 28f, 28f, stroke);

        // Specular top highlight
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(withAlpha(0x66FFFFFF, (int) (200 * reveal)));
        c.drawLine(cardL + 28f, cardT + 1.5f, cardR - 28f, cardT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Corner filigree brackets
        drawCornerBrackets(c, cardL, cardT, cardR, cardB, reveal);

        // 3. Victory Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(52f);
        text.setColor(withAlpha(0xEE000000, (int) (255 * reveal)));
        c.drawText("SHADOW WOVEN", cx, cardT + 81.5f, text);
        text.setColor(withAlpha(0xFFFFFDF8, (int) (255 * reveal)));
        c.drawText("SHADOW WOVEN", cx, cardT + 80f, text);

        text.setTextSize(28f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(withAlpha(0xCC000000, (int) (255 * reveal)));
        c.drawText(g.levelTitle(), cx, cardT + 126.5f, text);
        text.setColor(withAlpha(0xFFFFE0A8, (int) (255 * reveal)));
        c.drawText(g.levelTitle(), cx, cardT + 125f, text);

        // 4. Calculate Earned Stars
        long elapsed = g.elapsedMillis();
        int moves = g.moveCount();
        int earnedStars = LevelProgress.calculateStars(g.levelIndex(), elapsed, moves);

        // 5. Draw Sequential 3 Stars
        drawSequentialStars(c, cx, cardT + 215f, earnedStars, reveal);

        // 6. Par Performance Breakdown Table
        long parTime = LevelProgress.getParTimeMillis(g.levelIndex());
        int parMoves = LevelProgress.getParMoves(g.levelIndex());

        boolean timeStar = (elapsed <= parTime);
        boolean moveStar = (elapsed <= parTime && moves <= parMoves);

        float tableT = cardT + 325f;
        // Inlaid card plate for stat rows
        fill.setColor(withAlpha(0x440E131E, (int) (255 * reveal)));
        c.drawRoundRect(cx - 360f, tableT - 22f, cx + 360f, tableT + 155f, 18f, 18f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.6f);
        stroke.setColor(withAlpha(0x44D4AF37, (int) (180 * reveal)));
        c.drawRoundRect(cx - 360f, tableT - 22f, cx + 360f, tableT + 155f, 18f, 18f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        drawStatRow(c, cx, tableT + 18f, "Silhouette Completed", "90%+ Match", true, reveal);
        drawStatRow(c, cx, tableT + 72f, "Time Efficiency", formatTime(elapsed) + " (Par " + (parTime / 1000L) + "s)", timeStar, reveal);
        drawStatRow(c, cx, tableT + 126f, "Move Precision", moves + " (Par " + parMoves + ")", moveStar, reveal);

        // 7. Navigation Buttons
        btnNext.label = "NEXT TRIAL";
        btnRetry.label = "RETRY";
        btnSelect.label = "CHAPTER MAP";

        float t = g.timeSeconds();
        btnNext.draw(c, fill, stroke, text, reveal, t);
        btnRetry.draw(c, fill, stroke, text, reveal, t);
        btnSelect.draw(c, fill, stroke, text, reveal, t);

        GameIcons.drawReset(c, btnRetry.left + 44f, btnRetry.centerY(), 20f, fill, stroke);
        GameIcons.drawIcon(c, GameIcons.RELIC_PATHS[14], btnSelect.left + 52f, btnSelect.centerY(), 22f, 0xFFFFD54F);

        c.restoreToCount(save);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawSequentialStars(Canvas c, float cx, float cy, int stars, float reveal) {
        float spacing = 110f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean active = (i < stars);

            // Stagger reveal: star 0 at 0.15, star 1 at 0.40, star 2 at 0.65
            float threshold = 0.15f + i * 0.25f;
            float starProgress = Math.max(0f, Math.min(1f, (reveal - threshold) / 0.25f));

            float r = 44f;
            if (active && starProgress > 0f) {
                // Bounce scale on entrance
                float bounceScale = 1.0f + 0.35f * (float) Math.sin(starProgress * Math.PI);
                r *= bounceScale;
            }

            drawStar(c, sx, cy, r, active && starProgress > 0f, reveal);
        }
    }

    private static void drawStar(Canvas c, float cx, float cy, float r, boolean filled, float alpha) {
        GameIcons.drawStar(c, cx, cy, r, filled);
        if (filled) {
            fill.setColor(withAlpha(0xFFFFFFFF, (int) (255 * alpha)));
            c.drawCircle(cx, cy, r * 0.18f, fill);
        }
    }

    private static void drawStatRow(Canvas c, float cx, float y, String label, String value, boolean achieved, float alpha) {
        float rowW = 660f;
        float l = cx - rowW * 0.5f;
        float r = cx + rowW * 0.5f;

        text.setTextSize(24f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(withAlpha(0xFFFFFDF8, (int) (255 * alpha)));
        text.setTextAlign(Paint.Align.LEFT);
        c.drawText(label, l, y, text);

        text.setTextAlign(Paint.Align.RIGHT);
        text.setTypeface(SERIF_BOLD);
        text.setColor(withAlpha(achieved ? 0xFFFFD54F : 0x778090A0, (int) (255 * alpha)));
        String mark = achieved ? " [\u2605 OK]" : " [--]";
        c.drawText(value + mark, r, y, text);
    }

    private static void drawCornerBrackets(Canvas c, float l, float t, float r, float b, float reveal) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(withAlpha(0xFFD4AF37, (int) (240 * reveal)));

        float len = 22f;
        c.drawLine(l + 12f, t + 12f, l + 12f + len, t + 12f, stroke);
        c.drawLine(l + 12f, t + 12f, l + 12f, t + 12f + len, stroke);

        c.drawLine(r - 12f, t + 12f, r - 12f - len, t + 12f, stroke);
        c.drawLine(r - 12f, t + 12f, r - 12f, t + 12f + len, stroke);

        c.drawLine(l + 12f, b - 12f, l + 12f + len, b - 12f, stroke);
        c.drawLine(l + 12f, b - 12f, l + 12f, b - 12f - len, stroke);

        c.drawLine(r - 12f, b - 12f, r - 12f - len, b - 12f, stroke);
        c.drawLine(r - 12f, b - 12f, r - 12f, b - 12f - len, stroke);

        stroke.setStyle(Paint.Style.FILL);
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
