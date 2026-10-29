package com.mobodevs.shadowweaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * Shadow Weaver - SettingsOverlay
 * ===============================
 * Dedicated alchemical settings modal with audio, haptics preferences,
 * and safety-guarded progress reset.
 *
 * Fully responsive: centered horizontally and vertically on any aspect ratio.
 * Industry-standard 720px card width with balanced golden-ratio spacing.
 */
public final class SettingsOverlay {

    public enum Action {
        NONE, CLOSE, TOGGLE_SOUND, TOGGLE_HAPTICS, RESET_CONFIRMED
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
        float cardH = 680f;
        cardL = cx - cardW * 0.5f;
        cardT = (sceneH - cardH) * 0.5f;
        cardR = cardL + cardW;
        cardB = cardT + cardH;

        float btnW = 640f;
        float btnH = 84f;
        float btnL = cx - btnW * 0.5f;
        float btnR = cx + btnW * 0.5f;

        float startY = cardT + 160f;
        float gap = 18f;

        // Button 1: Sound
        btnSound.setBounds(btnL, startY, btnR, startY + btnH);
        btnSound.cornerRadius = 18f;

        // Button 2: Haptics
        float y2 = startY + btnH + gap;
        btnHaptics.setBounds(btnL, y2, btnR, y2 + btnH);
        btnHaptics.cornerRadius = 18f;

        // Button 3: Reset
        float y3 = y2 + btnH + gap;
        btnReset.setBounds(btnL, y3, btnR, y3 + btnH);
        btnReset.cornerRadius = 18f;

        // Confirmation buttons (side-by-side)
        float confirmH = 74f;
        float confirmY = cardT + 466f;
        float splitGap = 16f;
        float halfW = (btnW - splitGap) * 0.5f;
        btnConfirmYes.setBounds(btnL, confirmY, btnL + halfW, confirmY + confirmH);
        btnConfirmYes.cornerRadius = 18f;
        btnConfirmYes.isPrimary = true;

        btnConfirmNo.setBounds(btnL + halfW + splitGap, confirmY, btnR, confirmY + confirmH);
        btnConfirmNo.cornerRadius = 18f;
        btnConfirmNo.isPrimary = false;

        // Close Button (Primary gold sheen)
        float closeW = 360f;
        float closeH = 74f;
        float yClose = cardT + 574f;
        btnClose.setBounds(cx - closeW * 0.5f, yClose, cx + closeW * 0.5f, yClose + closeH);
        btnClose.isPrimary = true;
        btnClose.cornerRadius = 20f;

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
        }
        btnClose.isPressed = btnClose.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy, Context context, LevelProgress progress, AudioManager audio, HapticManager haptic) {
        return onTouchUp(sx, sy, context, progress, audio, haptic, 0f, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, Context context, LevelProgress progress, AudioManager audio, HapticManager haptic, float sceneH) {
        return onTouchUp(sx, sy, context, progress, audio, haptic, 0f, sceneH);
    }

    public static Action onTouchUp(float sx, float sy, Context context, LevelProgress progress, AudioManager audio, HapticManager haptic, float time, float sceneH) {
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
                if (audio != null) { audio.setMuted(muted); if (!muted) { audio.playSnap(); } }
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
            }
        }

        if (btnClose.isPressed && btnClose.hitTest(sx, sy)) {
            confirmingReset = false;
            act = Action.CLOSE;
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
        draw(c, progress, audio, haptic, 0f, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, LevelProgress progress, AudioManager audio, HapticManager haptic, float sceneH) {
        draw(c, progress, audio, haptic, 0f, sceneH);
    }

    public static void draw(Canvas c, LevelProgress progress, AudioManager audio, HapticManager haptic, float time, float sceneH) {
        layout(sceneH);

        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;
        float cx = sw * 0.5f;

        // 1. Dark Modal Scrim
        fill.setColor(0xDD080C14);
        c.drawRect(0, 0, sw, sh, fill);

        int saveCard = c.save();

        // 2. Card Panel (Obsidian Alchemical Slate)
        fill.setColor(0xFA141A28);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.8f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Specular highlight line
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(cardL + 24f, cardT + 2f, cardR - 24f, cardT + 2f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 4 Corner Brass Filigree Brackets
        drawCornerBrackets(c, cardL, cardT, cardR, cardB);

        // 3. Header
        float headY = cardT + 68f;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(38f);
        text.setColor(0xDD000000);
        c.drawText("ALCHEMICAL SETTINGS", cx, headY + 2f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("ALCHEMICAL SETTINGS", cx, headY, text);

        text.setTextSize(17f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xCC000000);
        c.drawText("Acoustic Chimes, Haptic Resonance & Progress", cx, headY + 36f + 1.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("Acoustic Chimes, Haptic Resonance & Progress", cx, headY + 36f, text);

        // Ornamental divider with center diamond pip
        float divY = cardT + 138f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(0x66D4AF37);
        c.drawLine(cardL + 48f, divY, cx - 18f, divY, stroke);
        c.drawLine(cx + 18f, divY, cardR - 48f, divY, stroke);
        stroke.setStyle(Paint.Style.FILL);

        fill.setColor(0xFFFFD54F);
        path.reset();
        path.moveTo(cx, divY - 6f);
        path.lineTo(cx + 6f, divY);
        path.lineTo(cx, divY + 6f);
        path.lineTo(cx - 6f, divY);
        path.close();
        c.drawPath(path, fill);

        // 4. Update Button Labels
        boolean soundMuted = (progress != null && progress.isSoundMuted());
        btnSound.label = soundMuted ? "MUSIC & CHIMES: MUTED" : "MUSIC & CHIMES: ENABLED";
        btnSound.subLabel = soundMuted ? "Pure silence for dark contemplative play" : "Atmospheric piano & celestial Tibetan singing bowls";

        boolean hapticsOn = (progress == null || progress.isHapticsEnabled());
        btnHaptics.label = hapticsOn ? "HAPTIC RESONANCE: ACTIVE" : "HAPTIC RESONANCE: OFF";
        btnHaptics.subLabel = hapticsOn ? "Tactile detent ticks and magnetic snap impulses" : "Vibration feedback disabled";

        btnSound.draw(c, fill, stroke, text, 1.0f, time);
        btnHaptics.draw(c, fill, stroke, text, 1.0f, time);

        // Render official game-icons.net badges inside settings buttons
        GameIcons.drawSpeaker(c, btnSound.left + 44f, btnSound.centerY(), 20f, soundMuted, soundMuted ? 0x99FFE0A8 : 0xFFFFD54F);
        GameIcons.drawHaptics(c, btnHaptics.left + 44f, btnHaptics.centerY(), 20f, hapticsOn ? 0xFFFFD54F : 0x99FFE0A8);

        if (confirmingReset) {
            // Danger Warning Plate
            float warnT = cardT + 364f;
            float warnB = cardT + 452f;
            float warnL = cardL + 40f;
            float warnR = cardR - 40f;

            fill.setColor(0x33B71C1C);
            c.drawRoundRect(warnL, warnT, warnR, warnB, 16f, 16f, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.0f);
            stroke.setColor(0xCCFF5252);
            c.drawRoundRect(warnL, warnT, warnR, warnB, 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            text.setTextSize(19f);
            text.setTypeface(SERIF_BOLD);
            text.setColor(0xFFFF8A80);
            c.drawText("RESET ALL 15 STARS & PROGRESS?", cx, warnT + 36f, text);

            text.setTextSize(15f);
            text.setTypeface(SERIF_NORMAL);
            text.setColor(0xFFFFCDD2);
            c.drawText("All times, stars and codex relics will be permanently cleared.", cx, warnT + 64f, text);

            btnConfirmYes.label = "CONFIRM RESET";
            btnConfirmYes.subLabel = "Irreversible";
            btnConfirmYes.draw(c, fill, stroke, text, 1.0f, time);

            btnConfirmNo.label = "CANCEL";
            btnConfirmNo.subLabel = "Keep Progress";
            btnConfirmNo.draw(c, fill, stroke, text, 1.0f, time);
        } else {
            btnReset.label = "RESET ASTRAL JOURNEY";
            btnReset.subLabel = "Clear all unlocked stars, times, and relics";
            btnReset.draw(c, fill, stroke, text, 1.0f, time);
            GameIcons.drawReset(c, btnReset.left + 44f, btnReset.centerY(), 20f, fill, stroke);

            // Subtle edition hallmark
            text.setTextSize(15f);
            text.setTypeface(SERIF_NORMAL);
            text.setColor(0x88FFE0A8);
            c.drawText("SHADOW WEAVER \u2022 v2.0 PRODUCTION EDITION", cx, cardT + 486f, text);
            text.setTextSize(13f);
            text.setColor(0x77FFE0A8);
            c.drawText("Atmospheric Music: The Cynic Project \u2022 Icons: game-icons.net", cx, cardT + 510f, text);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.2f);
            stroke.setColor(0x33D4AF37);
            c.drawLine(cardL + 120f, cardT + 524f, cardR - 120f, cardT + 524f, stroke);
            stroke.setStyle(Paint.Style.FILL);
        }

        // 5. Close Button
        btnClose.label = "RETURN";
        btnClose.draw(c, fill, stroke, text, 1.0f, time);

        c.restoreToCount(saveCard);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCornerBrackets(Canvas c, float l, float t, float r, float b) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);

        float len = 20f;
        // Top-Left
        c.drawLine(l + 10f, t + 10f, l + 10f + len, t + 10f, stroke);
        c.drawLine(l + 10f, t + 10f, l + 10f, t + 10f + len, stroke);

        // Top-Right
        c.drawLine(r - 10f, t + 10f, r - 10f - len, t + 10f, stroke);
        c.drawLine(r - 10f, t + 10f, r - 10f, t + 10f + len, stroke);

        // Bottom-Left
        c.drawLine(l + 10f, b - 10f, l + 10f + len, b - 10f, stroke);
        c.drawLine(l + 10f, b - 10f, l + 10f, b - 10f - len, stroke);

        // Bottom-Right
        c.drawLine(r - 10f, b - 10f, r - 10f - len, b - 10f, stroke);
        c.drawLine(r - 10f, b - 10f, r - 10f, b - 10f - len, stroke);

        stroke.setStyle(Paint.Style.FILL);
    }
}
