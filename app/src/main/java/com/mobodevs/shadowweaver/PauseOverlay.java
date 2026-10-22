package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * Shadow Weaver - PauseOverlay
 * ============================
 * Frosted modal dialog for game pause, settings, and level navigation.
 */
public final class PauseOverlay {

    public enum Action {
        NONE, RESUME, RESTART, TOGGLE_SOUND, TOGGLE_HAPTICS, LEVEL_SELECT
    }

    private static final RectF rect = new RectF();
    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final float CARD_W = 600f;
    private static final float CARD_H = 680f;

    private static float cardLeft;
    private static float cardTop;

    private static float resumeTop, resumeBottom;
    private static float restartTop, restartBottom;
    private static float soundTop, soundBottom;
    private static float hapticTop, hapticBottom;
    private static float selectTop, selectBottom;
    private static float btnLeft, btnRight;

    private static void layout() {
        float cx = ShadowCalculator.SCENE_W * 0.5f;
        cardLeft = cx - CARD_W * 0.5f;
        cardTop = (ShadowCalculator.SCENE_H - CARD_H) * 0.5f;

        btnLeft = cardLeft + 48f;
        btnRight = cardLeft + CARD_W - 48f;

        float startY = cardTop + 160f;
        float btnH = 64f;
        float gap = 20f;

        resumeTop = startY;
        resumeBottom = resumeTop + btnH;

        restartTop = resumeBottom + gap;
        restartBottom = restartTop + btnH;

        soundTop = restartBottom + gap;
        soundBottom = soundTop + btnH;

        hapticTop = soundBottom + gap;
        hapticBottom = hapticTop + btnH;

        selectTop = hapticBottom + gap;
        selectBottom = selectTop + btnH;
    }

    public static void ensureLayout() {
        layout();
    }

    public static Action hitTest(float sx, float sy) {
        ensureLayout();
        if (sx < cardLeft || sx > cardLeft + CARD_W || sy < cardTop || sy > cardTop + CARD_H) {
            // Tapping outside closes pause menu
            return Action.RESUME;
        }

        if (sx >= btnLeft && sx <= btnRight) {
            if (sy >= resumeTop && sy <= resumeBottom) return Action.RESUME;
            if (sy >= restartTop && sy <= restartBottom) return Action.RESTART;
            if (sy >= soundTop && sy <= soundBottom) return Action.TOGGLE_SOUND;
            if (sy >= hapticTop && sy <= hapticBottom) return Action.TOGGLE_HAPTICS;
            if (sy >= selectTop && sy <= selectBottom) return Action.LEVEL_SELECT;
        }

        return Action.NONE;
    }

    public static void draw(Canvas c, GameThread g, boolean soundEnabled, boolean hapticsEnabled) {
        ensureLayout();

        // 1. Scrim over the game
        fill.setColor(0xB00E0A06);
        c.drawRect(0, 0, ShadowCalculator.SCENE_W, ShadowCalculator.SCENE_H, fill);

        // 2. Card background (frosted obsidian panel)
        rect.set(cardLeft, cardTop, cardLeft + CARD_W, cardTop + CARD_H);
        fill.setColor(0xEE20150D);
        c.drawRoundRect(rect, 24f, 24f, fill);

        // Gold filigree border
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0x66FFE0A8);
        c.drawRoundRect(rect, 24f, 24f, stroke);

        // 3. Header
        float cx = ShadowCalculator.SCENE_W * 0.5f;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.setColor(0xFFFFE0A8);
        text.setTextSize(38f);
        c.drawText("PAUSED", cx, cardTop + 72f, text);

        text.setColor(0x99F0DFC6);
        text.setTextSize(22f);
        text.setTypeface(android.graphics.Typeface.DEFAULT);
        String sub = "Level " + (g.levelIndex() + 1) + " - " + g.levelTitle();
        c.drawText(sub, cx, cardTop + 112f, text);

        // 4. Buttons
        drawButton(c, cx, resumeTop, resumeBottom, "RESUME", true);
        drawButton(c, cx, restartTop, restartBottom, "RESTART LEVEL", false);
        drawButton(c, cx, soundTop, soundBottom, soundEnabled ? "SOUND: ON" : "SOUND: MUTED", false);
        drawButton(c, cx, hapticTop, hapticBottom, hapticsEnabled ? "HAPTICS: ON" : "HAPTICS: OFF", false);
        drawButton(c, cx, selectTop, selectBottom, "LEVEL SELECT", false);

        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawButton(Canvas c, float cx, float top, float bottom, String label, boolean primary) {
        rect.set(btnLeft, top, btnRight, bottom);
        if (primary) {
            fill.setColor(0xCCB37834);
            c.drawRoundRect(rect, 14f, 14f, fill);
            stroke.setStrokeWidth(2f);
            stroke.setColor(0xFFFFE9C4);
            c.drawRoundRect(rect, 14f, 14f, stroke);
            text.setColor(0xFFFFFFFF);
        } else {
            fill.setColor(0x33F0DFC6);
            c.drawRoundRect(rect, 14f, 14f, fill);
            stroke.setStrokeWidth(1.5f);
            stroke.setColor(0x44FFE0A8);
            c.drawRoundRect(rect, 14f, 14f, stroke);
            text.setColor(0xEEF0DFC6);
        }

        text.setTextSize(24f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        float textY = (top + bottom) * 0.5f + 8f;
        c.drawText(label, cx, textY, text);
    }
}
