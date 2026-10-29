package com.mobodevs.shadowweaver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * Shadow Weaver - SettingsOverlay
 * ===============================
 * Dedicated alchemical settings modal with independent audio preferences
 * (Music, SFX, and Master Audio toggle), haptics, and safety-guarded progress reset.
 *
 * Fully responsive: centered horizontally and vertically on any aspect ratio.
 */
public final class SettingsOverlay {

    public enum Action {
        NONE, CLOSE, TOGGLE_MUSIC, TOGGLE_SFX, TOGGLE_ALL_AUDIO, TOGGLE_HAPTICS, RESET_CONFIRMED
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

    private static final UiButton btnMusic = new UiButton();
    private static final UiButton btnSfx = new UiButton();
    private static final UiButton btnAllAudio = new UiButton();
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
        float cardW = 880f;
        float cardH = 860f;
        cardL = cx - cardW * 0.5f;
        cardT = (sceneH - cardH) * 0.5f;
        cardR = cardL + cardW;
        cardB = cardT + cardH;

        float btnW = 780f;
        float btnL = cx - btnW * 0.5f;
        float btnR = cx + btnW * 0.5f;

        float startY = cardT + 152f;
        float gap = 16f;

        // Row 1: Side-by-side Music & SFX
        float splitGap = 18f;
        float halfW = (btnW - splitGap) * 0.5f; // 381f
        float row1H = 92f;
        btnMusic.setBounds(btnL, startY, btnL + halfW, startY + row1H);
        btnMusic.cornerRadius = 20f;

        btnSfx.setBounds(btnL + halfW + splitGap, startY, btnR, startY + row1H);
        btnSfx.cornerRadius = 20f;

        // Row 2: Master Audio (Both Music & SFX)
        float y2 = startY + row1H + gap;
        float row2H = 88f;
        btnAllAudio.setBounds(btnL, y2, btnR, y2 + row2H);
        btnAllAudio.cornerRadius = 20f;

        // Row 3: Haptics
        float y3 = y2 + row2H + gap;
        float row3H = 88f;
        btnHaptics.setBounds(btnL, y3, btnR, y3 + row3H);
        btnHaptics.cornerRadius = 20f;

        // Row 4: Reset Journey
        float y4 = y3 + row3H + gap;
        float row4H = 88f;
        btnReset.setBounds(btnL, y4, btnR, y4 + row4H);
        btnReset.cornerRadius = 20f;

        // Confirmation buttons (side-by-side)
        float confirmH = 84f;
        float confirmY = cardT + 580f;
        btnConfirmYes.setBounds(btnL, confirmY, btnL + halfW, confirmY + confirmH);
        btnConfirmYes.cornerRadius = 20f;
        btnConfirmYes.isPrimary = true;

        btnConfirmNo.setBounds(btnL + halfW + splitGap, confirmY, btnR, confirmY + confirmH);
        btnConfirmNo.cornerRadius = 20f;
        btnConfirmNo.isPrimary = false;

        // Close Button (Primary gold sheen)
        float closeW = 460f;
        float closeH = 86f;
        float yClose = cardT + 724f;
        btnClose.setBounds(cx - closeW * 0.5f, yClose, cx + closeW * 0.5f, yClose + closeH);
        btnClose.isPrimary = true;
        btnClose.cornerRadius = 22f;

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
            btnMusic.isPressed = btnMusic.hitTest(sx, sy);
            btnSfx.isPressed = btnSfx.hitTest(sx, sy);
            btnAllAudio.isPressed = btnAllAudio.hitTest(sx, sy);
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
            if (btnMusic.isPressed && btnMusic.hitTest(sx, sy)) {
                boolean muted = (progress != null) ? !progress.isMusicMuted() : false;
                if (progress != null) {
                    progress.setMusicMuted(muted);
                    if (context != null) progress.save(context);
                }
                if (audio != null) {
                    audio.setMusicMuted(muted);
                    if (!muted && (progress == null || !progress.isSfxMuted())) audio.playClick();
                }
                act = Action.TOGGLE_MUSIC;
            } else if (btnSfx.isPressed && btnSfx.hitTest(sx, sy)) {
                boolean muted = (progress != null) ? !progress.isSfxMuted() : false;
                if (progress != null) {
                    progress.setSfxMuted(muted);
                    if (context != null) progress.save(context);
                }
                if (audio != null) {
                    audio.setSfxMuted(muted);
                    if (!muted) audio.playSnap();
                }
                act = Action.TOGGLE_SFX;
            } else if (btnAllAudio.isPressed && btnAllAudio.hitTest(sx, sy)) {
                boolean allMuted = (progress != null && progress.isMusicMuted() && progress.isSfxMuted());
                boolean newMutedState = !allMuted;
                if (progress != null) {
                    progress.setAllAudioMuted(newMutedState);
                    if (context != null) progress.save(context);
                }
                if (audio != null) {
                    audio.setPreferences(newMutedState, newMutedState);
                    if (!newMutedState) audio.playSnap();
                }
                act = Action.TOGGLE_ALL_AUDIO;
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

        btnMusic.isPressed = false;
        btnSfx.isPressed = false;
        btnAllAudio.isPressed = false;
        btnHaptics.isPressed = false;
        btnReset.isPressed = false;
        btnClose.isPressed = false;
        btnConfirmYes.isPressed = false;
        btnConfirmNo.isPressed = false;

        return act;
    }

    public static void cancelPress() {
        btnMusic.isPressed = false;
        btnSfx.isPressed = false;
        btnAllAudio.isPressed = false;
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
        float headY = cardT + 75f;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(44f);
        text.setColor(0xDD000000);
        c.drawText("ALCHEMICAL SETTINGS", cx, headY + 2f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("ALCHEMICAL SETTINGS", cx, headY, text);

        text.setTextSize(20f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xCC000000);
        c.drawText("Acoustic Chimes, Ambient Music & Haptics", cx, headY + 38f + 1.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("Acoustic Chimes, Ambient Music & Haptics", cx, headY + 38f, text);

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
        boolean musicMuted = (progress != null && progress.isMusicMuted());
        btnMusic.label = musicMuted ? "MUSIC: MUTED" : "MUSIC: ON";
        btnMusic.subLabel = musicMuted ? "Melody paused" : "Ambient piano";

        boolean sfxMuted = (progress != null && progress.isSfxMuted());
        btnSfx.label = sfxMuted ? "SFX: MUTED" : "SFX: ON";
        btnSfx.subLabel = sfxMuted ? "Chimes silenced" : "Clicks & chimes";

        boolean allMuted = (progress != null && progress.isMusicMuted() && progress.isSfxMuted());
        btnAllAudio.label = allMuted ? "ALL AUDIO: MUTED" : "ALL AUDIO: ACTIVE";
        btnAllAudio.subLabel = allMuted ? "Tap to restore both music & sound effects" : "One-tap toggle to mute all music and sound";

        boolean hapticsOn = (progress == null || progress.isHapticsEnabled());
        btnHaptics.label = hapticsOn ? "HAPTIC RESONANCE: ACTIVE" : "HAPTIC RESONANCE: OFF";
        btnHaptics.subLabel = hapticsOn ? "Tactile detent ticks and magnetic snap impulses" : "Vibration feedback disabled";

        btnMusic.draw(c, fill, stroke, text, 1.0f, time);
        btnSfx.draw(c, fill, stroke, text, 1.0f, time);
        btnAllAudio.draw(c, fill, stroke, text, 1.0f, time);
        btnHaptics.draw(c, fill, stroke, text, 1.0f, time);

        // Render official game-icons.net badges inside buttons
        GameIcons.drawSpeaker(c, btnMusic.left + 42f, btnMusic.centerY(), 18f, musicMuted, musicMuted ? 0x99FFE0A8 : 0xFFFFD54F);
        GameIcons.drawSpeaker(c, btnSfx.left + 42f, btnSfx.centerY(), 18f, sfxMuted, sfxMuted ? 0x99FFE0A8 : 0xFFFFD54F);
        GameIcons.drawSpeaker(c, btnAllAudio.left + 54f, btnAllAudio.centerY(), 22f, allMuted, allMuted ? 0x99FFE0A8 : 0xFFFFD54F);
        GameIcons.drawHaptics(c, btnHaptics.left + 54f, btnHaptics.centerY(), 22f, hapticsOn ? 0xFFFFD54F : 0x99FFE0A8);

        if (confirmingReset) {
            // Danger Warning Plate
            float warnT = cardT + 470f;
            float warnB = cardT + 566f;
            float warnL = cardL + 48f;
            float warnR = cardR - 48f;

            fill.setColor(0x33B71C1C);
            c.drawRoundRect(warnL, warnT, warnR, warnB, 16f, 16f, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.0f);
            stroke.setColor(0xCCFF5252);
            c.drawRoundRect(warnL, warnT, warnR, warnB, 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            text.setTextSize(22f);
            text.setTypeface(SERIF_BOLD);
            text.setColor(0xFFFF8A80);
            c.drawText("RESET ALL 45 STARS & PROGRESS?", cx, warnT + 40f, text);

            text.setTextSize(17f);
            text.setTypeface(SERIF_NORMAL);
            text.setColor(0xFFFFCDD2);
            c.drawText("All times, stars and codex relics will be permanently cleared.", cx, warnT + 72f, text);

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
            GameIcons.drawReset(c, btnReset.left + 54f, btnReset.centerY(), 22f, fill, stroke);

            // Subtle edition hallmark
            text.setTextSize(18f);
            text.setTypeface(SERIF_NORMAL);
            text.setColor(0x99FFE0A8);
            c.drawText("SHADOW WEAVER • v2.0 PRODUCTION EDITION", cx, cardT + 606f, text);
            text.setTextSize(15f);
            text.setColor(0x77FFE0A8);
            c.drawText("Atmospheric Music: The Cynic Project • Icons: game-icons.net", cx, cardT + 634f, text);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.2f);
            stroke.setColor(0x33D4AF37);
            c.drawLine(cardL + 120f, cardT + 662f, cardR - 120f, cardT + 662f, stroke);
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
