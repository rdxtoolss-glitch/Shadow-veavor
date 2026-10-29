package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * Shadow Weaver - PauseOverlay
 * ============================
 * Frosted modal dialog for game pause, restart, settings, and navigation.
 * Perfectly centered horizontally and vertically with golden-ratio button sizing.
 */
public final class PauseOverlay {

    public enum Action {
        NONE, RESUME, RESTART, LEVEL_SELECT, SETTINGS, MAIN_MENU
    }

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path path = new Path();

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

    private static final UiButton btnResume = new UiButton();
    private static final UiButton btnRestart = new UiButton();
    private static final UiButton btnChapters = new UiButton();
    private static final UiButton btnSettings = new UiButton();
    private static final UiButton btnMainMenu = new UiButton();

    private static boolean layoutReady;
    private static float lastSceneH = 0f;

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardW = 860f;
        float cardH = 780f;
        cardL = cx - cardW * 0.5f;
        cardT = (sceneH - cardH) * 0.5f;
        cardR = cardL + cardW;
        cardB = cardT + cardH;

        float btnW = 760f;
        float btnH = 92f;
        float btnL = cx - btnW * 0.5f;
        float btnR = cx + btnW * 0.5f;

        float startY = cardT + 172f;
        float gap = 18f;

        btnResume.setBounds(btnL, startY, btnR, startY + btnH);
        btnResume.isPrimary = true;
        btnResume.cornerRadius = 22f;

        float y2 = startY + btnH + gap;
        btnRestart.setBounds(btnL, y2, btnR, y2 + btnH);
        btnRestart.cornerRadius = 22f;

        float y3 = y2 + btnH + gap;
        btnChapters.setBounds(btnL, y3, btnR, y3 + btnH);
        btnChapters.cornerRadius = 22f;

        float y4 = y3 + btnH + gap;
        btnSettings.setBounds(btnL, y4, btnR, y4 + btnH);
        btnSettings.cornerRadius = 22f;

        float y5 = y4 + btnH + gap;
        btnMainMenu.setBounds(btnL, y5, btnR, y5 + btnH);
        btnMainMenu.cornerRadius = 22f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        return onTouchDown(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchDown(float sx, float sy, float sceneH) {
        layout(sceneH);
        btnResume.isPressed = btnResume.hitTest(sx, sy);
        btnRestart.isPressed = btnRestart.hitTest(sx, sy);
        btnChapters.isPressed = btnChapters.hitTest(sx, sy);
        btnSettings.isPressed = btnSettings.hitTest(sx, sy);
        btnMainMenu.isPressed = btnMainMenu.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        return onTouchUp(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, float sceneH) {
        layout(sceneH);
        Action act = Action.NONE;

        if (btnResume.isPressed && btnResume.hitTest(sx, sy)) act = Action.RESUME;
        else if (btnRestart.isPressed && btnRestart.hitTest(sx, sy)) act = Action.RESTART;
        else if (btnChapters.isPressed && btnChapters.hitTest(sx, sy)) act = Action.LEVEL_SELECT;
        else if (btnSettings.isPressed && btnSettings.hitTest(sx, sy)) act = Action.SETTINGS;
        else if (btnMainMenu.isPressed && btnMainMenu.hitTest(sx, sy)) act = Action.MAIN_MENU;

        btnResume.isPressed = false;
        btnRestart.isPressed = false;
        btnChapters.isPressed = false;
        btnSettings.isPressed = false;
        btnMainMenu.isPressed = false;

        return act;
    }

    public static void cancelPress() {
        btnResume.isPressed = false;
        btnRestart.isPressed = false;
        btnChapters.isPressed = false;
        btnSettings.isPressed = false;
        btnMainMenu.isPressed = false;
    }

    public static void draw(Canvas c, GameThread g) {
        draw(c, g, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, GameThread g, float sceneH) {
        layout(sceneH);

        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;
        float cx = sw * 0.5f;

        // 1. Dark Modal Scrim
        fill.setColor(0xDD080C14);
        c.drawRect(0, 0, sw, sh, fill);

        // Emil Kowalski modal entrance: start from scale(0.95) with cubic ease-out
        float transAlpha = g.screenTransitionAlpha();
        float ease = 1.0f - (float) Math.pow(1.0f - transAlpha, 3.0);
        float cardScale = 0.95f + 0.05f * ease;
        float slideY = (1.0f - ease) * 30f;

        int saveCard = c.save();
        c.translate(0, slideY);
        c.scale(cardScale, cardScale, cx, (cardT + cardB) * 0.5f);

        // 2. Frosted Obsidian-Slate Panel
        fill.setColor(0xFA141A28);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.8f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Specular top highlight
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(cardL + 24f, cardT + 2f, cardR - 24f, cardT + 2f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Corner filigree brackets
        drawCornerBrackets(c, cardL, cardT, cardR, cardB);

        // 3. Header
        float headY = cardT + 75f;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(44f);
        text.setColor(0xDD000000);
        c.drawText("SANCTUM PAUSED", cx, headY + 2f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("SANCTUM PAUSED", cx, headY, text);

        text.setTextSize(20f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xCC000000);
        c.drawText(g.levelTitle() + " \u2022 CHAPTER " + ((g.levelIndex() / 5) + 1), cx, headY + 36f + 1.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText(g.levelTitle() + " \u2022 CHAPTER " + ((g.levelIndex() / 5) + 1), cx, headY + 36f, text);

        // Ornamental dividing flourish
        float divY = cardT + 144f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(0x66D4AF37);
        c.drawLine(cardL + 48f, divY, cx - 18f, divY, stroke);
        c.drawLine(cx + 18f, divY, cardR - 48f, divY, stroke);
        stroke.setStyle(Paint.Style.FILL);

        fill.setColor(0xFFFFD54F);
        path.reset();
        path.moveTo(cx, divY - 5f);
        path.lineTo(cx + 5f, divY);
        path.lineTo(cx, divY + 5f);
        path.lineTo(cx - 5f, divY);
        path.close();
        c.drawPath(path, fill);

        // 4. Update Button Labels
        btnResume.label = "RESUME WEAVING";
        btnResume.subLabel = "Continue this trial";

        btnRestart.label = "RESTART TRIAL";
        btnRestart.subLabel = "Reset piece coordinates";

        btnChapters.label = "CHAPTER MAP";
        btnChapters.subLabel = "Select another trial";

        btnSettings.label = "ALCHEMICAL SETTINGS";
        btnSettings.subLabel = "Audio & Haptic preferences";

        btnMainMenu.label = "MAIN MENU";
        btnMainMenu.subLabel = "Return to main entrance";

        // 5. Draw Buttons
        float t = g.timeSeconds();
        btnResume.draw(c, fill, stroke, text, 1.0f, t);
        btnRestart.draw(c, fill, stroke, text, 1.0f, t);
        btnChapters.draw(c, fill, stroke, text, 1.0f, t);
        btnSettings.draw(c, fill, stroke, text, 1.0f, t);
        btnMainMenu.draw(c, fill, stroke, text, 1.0f, t);

        // Official game-icons.net embellishments on pause buttons
        GameIcons.drawPause(c, btnResume.left + 54f, btnResume.centerY(), 22f, fill, stroke, false);
        GameIcons.drawReset(c, btnRestart.left + 54f, btnRestart.centerY(), 22f, fill, stroke);
        GameIcons.drawIcon(c, GameIcons.RELIC_PATHS[14], btnChapters.left + 54f, btnChapters.centerY(), 22f, 0xFFFFD54F);
        GameIcons.drawIcon(c, GameIcons.PATH_SETTINGS, btnSettings.left + 54f, btnSettings.centerY(), 22f, 0xFFFFD54F);

        c.restoreToCount(saveCard);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCornerBrackets(Canvas c, float l, float t, float r, float b) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);

        float len = 20f;
        c.drawLine(l + 10f, t + 10f, l + 10f + len, t + 10f, stroke);
        c.drawLine(l + 10f, t + 10f, l + 10f, t + 10f + len, stroke);

        c.drawLine(r - 10f, t + 10f, r - 10f - len, t + 10f, stroke);
        c.drawLine(r - 10f, t + 10f, r - 10f, t + 10f + len, stroke);

        c.drawLine(l + 10f, b - 10f, l + 10f + len, b - 10f, stroke);
        c.drawLine(l + 10f, b - 10f, l + 10f, b - 10f - len, stroke);

        c.drawLine(r - 10f, b - 10f, r - 10f - len, b - 10f, stroke);
        c.drawLine(r - 10f, b - 10f, r - 10f, b - 10f - len, stroke);

        stroke.setStyle(Paint.Style.FILL);
    }
}
