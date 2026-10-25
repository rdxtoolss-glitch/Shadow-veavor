package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
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

    private static void layout() {
        if (layoutReady) return;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardW = 680f;
        float cardH = 720f;
        cardL = cx - cardW * 0.5f;
        cardT = (ShadowCalculator.SCENE_H - cardH) * 0.5f;
        cardR = cardL + cardW;
        cardB = cardT + cardH;

        float btnW = 540f;
        float btnH = 72f;
        float btnL = cx - btnW * 0.5f;
        float btnR = cx + btnW * 0.5f;

        float startY = cardT + 160f;
        float gap = 20f;

        btnResume.setBounds(btnL, startY, btnR, startY + btnH);
        btnResume.isPrimary = true;
        btnResume.cornerRadius = 18f;

        float y2 = startY + btnH + gap;
        btnRestart.setBounds(btnL, y2, btnR, y2 + btnH);
        btnRestart.cornerRadius = 18f;

        float y3 = y2 + btnH + gap;
        btnChapters.setBounds(btnL, y3, btnR, y3 + btnH);
        btnChapters.cornerRadius = 18f;

        float y4 = y3 + btnH + gap;
        btnSettings.setBounds(btnL, y4, btnR, y4 + btnH);
        btnSettings.cornerRadius = 18f;

        float y5 = y4 + btnH + gap;
        btnMainMenu.setBounds(btnL, y5, btnR, y5 + btnH);
        btnMainMenu.cornerRadius = 18f;

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
        fill.setColor(0xCC0E121E);
        c.drawRect(0, 0, sw, sh, fill);

        // 2. Frosted Obsidian-Slate Panel
        fill.setColor(0xFA1A2234);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.8f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Specular top highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66FFFFFF);
        c.drawLine(cardL + 24f, cardT + 1.5f, cardR - 24f, cardT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 3. Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(42f);
        text.setColor(0xDD000000);
        c.drawText("PAUSED", cx, cardT + 71.5f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("PAUSED", cx, cardT + 70f, text);

        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xCC000000);
        c.drawText(g.levelTitle() + " • CHAPTER " + ((g.levelIndex() / 5) + 1), cx, cardT + 111f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText(g.levelTitle() + " • CHAPTER " + ((g.levelIndex() / 5) + 1), cx, cardT + 110f, text);

        // 4. Update Button Labels
        btnResume.label = "RESUME WEAVING";
        btnResume.subLabel = "Continue this trial";

        btnRestart.label = "RESTART TRIAL";
        btnRestart.subLabel = "Reset piece coordinates";

        btnChapters.label = "CHAPTER MAP";
        btnChapters.subLabel = "Select another trial";

        btnSettings.label = "SETTINGS";
        btnSettings.subLabel = "Audio & Haptic preferences";

        btnMainMenu.label = "TITLE SCREEN";
        btnMainMenu.subLabel = "Return to main entrance";

        // 5. Draw Buttons
        btnResume.draw(c, fill, stroke, text, 1.0f);
        btnRestart.draw(c, fill, stroke, text, 1.0f);
        btnChapters.draw(c, fill, stroke, text, 1.0f);
        btnSettings.draw(c, fill, stroke, text, 1.0f);
        btnMainMenu.draw(c, fill, stroke, text, 1.0f);

        text.setTextAlign(Paint.Align.LEFT);
    }
}
