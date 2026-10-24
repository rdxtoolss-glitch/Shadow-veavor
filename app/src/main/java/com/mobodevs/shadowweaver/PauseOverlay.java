package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Shadow Weaver - PauseOverlay
 * ============================
 * Frosted modal dialog for game pause, restart, settings, and navigation.
 */
public final class PauseOverlay {

    public enum Action {
        NONE, RESUME, RESTART, LEVEL_SELECT, SETTINGS, MAIN_MENU
    }

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF cardRect = new RectF();

    private static final UiButton btnResume = new UiButton();
    private static final UiButton btnRestart = new UiButton();
    private static final UiButton btnChapters = new UiButton();
    private static final UiButton btnSettings = new UiButton();
    private static final UiButton btnMainMenu = new UiButton();

    private static boolean layoutReady;

    private static void layout() {
        if (layoutReady) return;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardW = 640f;
        float cardH = 680f;
        float cardL = cx - cardW * 0.5f;
        float cardT = (ShadowCalculator.SCENE_H - cardH) * 0.5f;
        cardRect.set(cardL, cardT, cardL + cardW, cardT + cardH);

        float btnW = 500f;
        float btnH = 68f;
        float btnL = cx - btnW * 0.5f;
        float btnR = cx + btnW * 0.5f;

        float startY = cardT + 160f;
        float gap = 20f;

        btnResume.setBounds(btnL, startY, btnR, startY + btnH);
        btnResume.isPrimary = true;
        btnResume.cornerRadius = 16f;

        float y2 = startY + btnH + gap;
        btnRestart.setBounds(btnL, y2, btnR, y2 + btnH);
        btnRestart.cornerRadius = 16f;

        float y3 = y2 + btnH + gap;
        btnChapters.setBounds(btnL, y3, btnR, y3 + btnH);
        btnChapters.cornerRadius = 16f;

        float y4 = y3 + btnH + gap;
        btnSettings.setBounds(btnL, y4, btnR, y4 + btnH);
        btnSettings.cornerRadius = 16f;

        float y5 = y4 + btnH + gap;
        btnMainMenu.setBounds(btnL, y5, btnR, y5 + btnH);
        btnMainMenu.cornerRadius = 16f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        layout();
        btnResume.isPressed = btnResume.hitTest(sx, sy);
        btnRestart.isPressed = btnRestart.hitTest(sx, sy);
        btnChapters.isPressed = btnChapters.hitTest(sx, sy);
        btnSettings.isPressed = btnSettings.hitTest(sx, sy);
        btnMainMenu.isPressed = btnMainMenu.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        layout();
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
        layout();

        float sw = ShadowCalculator.SCENE_W;
        float sh = ShadowCalculator.SCENE_H;
        float cx = sw * 0.5f;

        // 1. Dark Modal Scrim
        fill.setColor(0xB80E0A06);
        c.drawRect(0, 0, sw, sh, fill);

        // 2. Frosted Obsidian Panel
        fill.setColor(0xF020150D);
        c.drawRoundRect(cardRect, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0x88C5A059);
        c.drawRoundRect(cardRect, 24f, 24f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 3. Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(40f);
        text.setColor(0xFFFFE9C4);
        c.drawText("PAUSED", cx, cardRect.top + 68f, text);

        text.setTextSize(22f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x99FFE0B0);
        c.drawText("Chapter " + ((g.levelIndex() / 5) + 1) + " • " + g.levelTitle(), cx, cardRect.top + 106f, text);

        // 4. Buttons
        btnResume.label = "RESUME TRIAL";
        btnRestart.label = "RESTART LEVEL";
        btnChapters.label = "CHAPTER MAP";
        btnSettings.label = "SETTINGS & AUDIO";
        btnMainMenu.label = "RETURN TO TITLE";

        btnResume.draw(c, fill, stroke, text, 1.0f);
        btnRestart.draw(c, fill, stroke, text, 1.0f);
        btnChapters.draw(c, fill, stroke, text, 1.0f);
        btnSettings.draw(c, fill, stroke, text, 1.0f);
        btnMainMenu.draw(c, fill, stroke, text, 1.0f);

        text.setTextAlign(Paint.Align.LEFT);
    }
}
