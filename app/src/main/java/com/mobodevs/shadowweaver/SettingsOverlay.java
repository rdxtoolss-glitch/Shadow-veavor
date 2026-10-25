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

    // Reset confirmation sub-state
    private static boolean confirmingReset;
    private static final UiButton btnConfirmYes = new UiButton();
    private static final UiButton btnConfirmNo = new UiButton();

    private static boolean layoutReady;

    private static void layout() {
        if (layoutReady) return;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardW = 720f;
        float cardH = 760f;
        cardL = cx - cardW * 0.5f;
        cardT = (ShadowCalculator.SCENE_H - cardH) * 0.5f;
        cardR = cardL + cardW;
        cardB = cardT + cardH;

        float btnW = 580f;
        float btnH = 74f;
        float startY = cardT + 170f;
        float gap = 24f;

        float btnL = cx - btnW * 0.5f;
        float btnR = cx + btnW * 0.5f;

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
        layout();
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
        layout();
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
            btnConfirmYes.isPressed = false;
            btnConfirmNo.isPressed = false;
            return act;
        }

        if (btnSound.isPressed && btnSound.hitTest(sx, sy)) {
            if (audio != null) {
                boolean muted = audio.toggleMute();
                if (progress != null) {
                    progress.setSoundMuted(muted);
                    progress.save(context);
                }
            }
            act = Action.TOGGLE_SOUND;
        } else if (btnHaptics.isPressed && btnHaptics.hitTest(sx, sy)) {
            if (haptic != null) {
                boolean next = !haptic.isEnabled();
                haptic.setEnabled(next);
                if (progress != null) {
                    progress.setHapticsEnabled(next);
                    progress.save(context);
                }
            }
            act = Action.TOGGLE_HAPTICS;
        } else if (btnReset.isPressed && btnReset.hitTest(sx, sy)) {
            confirmingReset = true;
        } else if (btnClose.isPressed && btnClose.hitTest(sx, sy)) {
            confirmingReset = false;
            act = Action.CLOSE;
        }

        btnSound.isPressed = false;
        btnHaptics.isPressed = false;
        btnReset.isPressed = false;
        btnClose.isPressed = false;

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
        layout();

        float sw = ShadowCalculator.SCENE_W;
        float sh = ShadowCalculator.SCENE_H;
        float cx = sw * 0.5f;

        // 1. Dark Modal Scrim
        fill.setColor(0xCC0E121E);
        c.drawRect(0, 0, sw, sh, fill);

        // 2. Card Body (Luminous Frosted Obsidian-Slate)
        fill.setColor(0xFA1A2234);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.8f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Top specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66FFFFFF);
        c.drawLine(cardL + 24f, cardT + 1.5f, cardR - 24f, cardT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 3. Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(42f);
        text.setColor(0xDD000000);
        c.drawText("PREFERENCES", cx, cardT + 71.5f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("PREFERENCES", cx, cardT + 70f, text);

        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xCC000000);
        c.drawText("AUDIO, HAPTICS & PROGRESSION", cx, cardT + 111f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("AUDIO, HAPTICS & PROGRESSION", cx, cardT + 110f, text);

        // 4. Update Button Labels
        boolean soundMuted = (audio != null && audio.isMuted()) || (progress != null && progress.isSoundMuted());
        btnSound.label = soundMuted ? "SOUND: MUTED" : "SOUND: ACTIVE";
        btnSound.subLabel = soundMuted ? "Procedural drone and chime audio muted" : "Harmonic procedural drone and chimes active";

        boolean hapticOn = (haptic != null && haptic.isEnabled()) && (progress == null || progress.isHapticsEnabled());
        btnHaptics.label = hapticOn ? "HAPTIC FEEDBACK: ON" : "HAPTIC FEEDBACK: OFF";
        btnHaptics.subLabel = hapticOn ? "Tactile rotary detents and magnetic snap pulses" : "Device vibrations disabled";

        if (confirmingReset) {
            btnConfirmYes.label = "YES, RESET";
            btnConfirmYes.subLabel = "Clear all stars & progress";
            btnConfirmYes.draw(c, fill, stroke, text, 1.0f);

            btnConfirmNo.label = "CANCEL";
            btnConfirmNo.subLabel = "Keep my collected stars";
            btnConfirmNo.draw(c, fill, stroke, text, 1.0f);
        } else {
            btnReset.label = "RESET ALL PROGRESS";
            btnReset.subLabel = "Clear saved constellation stars and return to Level 1";
            btnReset.draw(c, fill, stroke, text, 1.0f);
        }

        btnSound.draw(c, fill, stroke, text, 1.0f);
        btnHaptics.draw(c, fill, stroke, text, 1.0f);

        // 5. Close Button
        btnClose.label = "DONE";
        btnClose.draw(c, fill, stroke, text, 1.0f);

        // 6. Footer Credits
        text.setTextSize(16f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x88D4AF37);
        c.drawText("Shadow Weaver • Handcrafted Alchemical Puzzle Engine", cx, cardB - 24f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }
}
