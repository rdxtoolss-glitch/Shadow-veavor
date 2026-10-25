package com.mobodevs.shadowweaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Shadow Weaver - SettingsOverlay
 * ===============================
 * Dedicated settings screen with audio, haptics, physics preferences,
 * and safety-guarded progress reset.
 *
 * Fully responsive: centers dynamically on any aspect ratio.
 */
public final class SettingsOverlay {

    public enum Action {
        NONE, CLOSE, TOGGLE_SOUND, TOGGLE_HAPTICS, RESET_CONFIRMED
    }

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static float cardL;
    private static float cardT;
    private static float cardR;
    private static float cardB;

    private static final UiButton btnSound = new UiButton();
    private static final UiButton btnHaptics = new UiButton();
    private static final UiButton btnReset = new UiButton();
    private static final UiButton btnClose = new UiButton();

    private static boolean confirmingReset = false;
    private static final UiButton btnConfirmYes = new UiButton();
    private static final UiButton btnConfirmNo = new UiButton();

    private static boolean layoutReady;
    private static float lastSceneH = 0f;

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardW = 720f;
        float cardH = 760f;
        cardL = cx - cardW * 0.5f;
        cardT = (sceneH - cardH) * 0.5f;
        cardR = cardL + cardW;
        cardB = cardT + cardH;

        float btnW = 580f;
        float btnH = 88f;
        float btnL = cx - btnW * 0.5f;
        float btnR = cx + btnW * 0.5f;

        float startY = cardT + 160f;
        float gap = 24f;

        btnSound.setBounds(btnL, startY, btnR, startY + btnH);
        btnSound.cornerRadius = 18f;

        float y2 = startY + btnH + gap;
        btnHaptics.setBounds(btnL, y2, btnR, y2 + btnH);
        btnHaptics.cornerRadius = 18f;

        float y3 = y2 + btnH + gap;
        btnReset.setBounds(btnL, y3, btnR, y3 + btnH);
        btnReset.cornerRadius = 18f;

        float yClose = cardB - 110f;
        btnClose.setBounds(cx - 180f, yClose, cx + 180f, yClose + 72f);
        btnClose.isPrimary = true;
        btnClose.cornerRadius = 18f;

        // Confirmation buttons (side-by-side)
        float halfW = (btnW - gap) * 0.5f;
        btnConfirmYes.setBounds(btnL, y3, btnL + halfW, y3 + btnH);
        btnConfirmYes.cornerRadius = 18f;

        btnConfirmNo.setBounds(btnL + halfW + gap, y3, btnR, y3 + btnH);
        btnConfirmNo.cornerRadius = 18f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        return onTouchDown(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchDown(float sx, float sy, float sceneH) {
        layout(sceneH);
        if (confirmingReset) {
            btnConfirmYes.isPressed = btnConfirmYes.hitTest(sx, sy);
            btnConfirmNo.isPressed = btnConfirmNo.hitTest(sx, sy);
        } else {
            btnSound.isPressed = btnSound.hitTest(sx, sy);
            btnHaptics.isPressed = btnHaptics.hitTest(sx, sy);
            btnReset.isPressed = btnReset.hitTest(sx, sy);
            btnClose.isPressed = btnClose.hitTest(sx, sy);
        }
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy, Context context, LevelProgress progress, AudioManager audio, HapticManager haptic) {
        return onTouchUp(sx, sy, context, progress, audio, haptic, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, Context context, LevelProgress progress, AudioManager audio, HapticManager haptic, float sceneH) {
        layout(sceneH);
        Action act = Action.NONE;

        if (confirmingReset) {
            if (btnConfirmYes.isPressed && btnConfirmYes.hitTest(sx, sy)) {
                if (progress != null) {
                    progress.resetAllProgress(context);
                }
                confirmingReset = false;
                act = Action.RESET_CONFIRMED;
            } else if (btnConfirmNo.isPressed && btnConfirmNo.hitTest(sx, sy)) {
                confirmingReset = false;
            }
        } else {
            if (btnSound.isPressed && btnSound.hitTest(sx, sy)) {
                boolean muted = (progress != null) ? !progress.isSoundMuted() : false;
                if (progress != null) {
                    progress.setSoundMuted(muted);
                    if (context != null) progress.save(context);
                }
                if (audio != null) audio.setMuted(muted);
                act = Action.TOGGLE_SOUND;
            } else if (btnHaptics.isPressed && btnHaptics.hitTest(sx, sy)) {
                boolean enabled = (progress != null) ? !progress.isHapticsEnabled() : true;
                if (progress != null) {
                    progress.setHapticsEnabled(enabled);
                    if (context != null) progress.save(context);
                }
                if (haptic != null) haptic.setEnabled(enabled);
                act = Action.TOGGLE_HAPTICS;
            } else if (btnReset.isPressed && btnReset.hitTest(sx, sy)) {
                confirmingReset = true;
            } else if (btnClose.isPressed && btnClose.hitTest(sx, sy)) {
                act = Action.CLOSE;
            }
        }

        btnSound.isPressed = false;
        btnHaptics.isPressed = false;
        btnReset.isPressed = false;
        btnClose.isPressed = false;
        btnConfirmYes.isPressed = false;
        btnConfirmNo.isPressed = false;

        return act;
    }

    public static void cancelPress() {
        btnSound.isPressed = false;
        btnHaptics.isPressed = false;
        btnReset.isPressed = false;
        btnClose.isPressed = false;
        btnConfirmYes.isPressed = false;
        btnConfirmNo.isPressed = false;
    }

    public static void draw(Canvas c, LevelProgress progress, AudioManager audio, HapticManager haptic) {
        draw(c, progress, audio, haptic, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, LevelProgress progress, AudioManager audio, HapticManager haptic, float sceneH) {
        layout(sceneH);

        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;
        float cx = sw * 0.5f;

        // 1. Dark Modal Scrim
        fill.setColor(0xCC0E121E);
        c.drawRect(0, 0, sw, sh, fill);

        // 2. Card Panel (Obsidian Alchemical Slate)
        fill.setColor(0xFA1A2234);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.6f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Specular highlight line
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x66FFFFFF);
        c.drawLine(cardL + 24f, cardT + 1.5f, cardR - 24f, cardT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 3. Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(40f);
        text.setColor(0xDD000000);
        c.drawText("ALCHEMICAL SETTINGS", cx, cardT + 66.5f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("ALCHEMICAL SETTINGS", cx, cardT + 65f, text);

        text.setTextSize(18f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xCC000000);
        c.drawText("Acoustic Chimes, Haptic Resonance & Progress", cx, cardT + 106.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("Acoustic Chimes, Haptic Resonance & Progress", cx, cardT + 105f, text);

        // Divider
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(0x44D4AF37);
        c.drawLine(cardL + 40f, cardT + 125f, cardR - 40f, cardT + 125f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 4. Update Button Labels
        boolean soundMuted = (progress != null && progress.isSoundMuted());
        btnSound.label = soundMuted ? "ACOUSTIC CHIMES: MUTED" : "ACOUSTIC CHIMES: ENABLED";
        btnSound.subLabel = soundMuted ? "Pure silence for dark contemplative play" : "Harmonic Tibetan singing bowls on rotation & snaps";

        boolean hapticsOn = (progress == null || progress.isHapticsEnabled());
        btnHaptics.label = hapticsOn ? "HAPTIC RESONANCE: ACTIVE" : "HAPTIC RESONANCE: OFF";
        btnHaptics.subLabel = hapticsOn ? "Tactile detent ticks and magnetic snap impulses" : "Vibration feedback disabled";

        if (confirmingReset) {
            text.setTextSize(18f);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(0xFFFF8A80);
            c.drawText("ARE YOU SURE? RESET ALL 15 STARS & PROGRESS?", cx, cardT + 340f, text);

            btnConfirmYes.label = "CONFIRM RESET";
            btnConfirmYes.subLabel = "Irreversible";
            btnConfirmYes.isPrimary = true;
            btnConfirmYes.draw(c, fill, stroke, text, 1.0f);

            btnConfirmNo.label = "CANCEL";
            btnConfirmNo.subLabel = "Keep Progress";
            btnConfirmNo.draw(c, fill, stroke, text, 1.0f);
        } else {
            btnReset.label = "RESET ASTRAL JOURNEY";
            btnReset.subLabel = "Clear all unlocked stars, times, and relics";
            btnReset.draw(c, fill, stroke, text, 1.0f);
        }

        btnSound.draw(c, fill, stroke, text, 1.0f);
        btnHaptics.draw(c, fill, stroke, text, 1.0f);

        btnClose.label = "CLOSE";
        btnClose.draw(c, fill, stroke, text, 1.0f);

        text.setTextAlign(Paint.Align.LEFT);
    }
}
