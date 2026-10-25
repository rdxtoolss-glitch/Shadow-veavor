package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Shadow Weaver - TitleScreen
 * ===========================
 * Main menu and entrance to the game.
 * Features glowing celestial typography, animated astrolabe background rings,
 * hero "Continue" CTA with level badge, and quick access to Chapters, How to Play,
 * Journal, and Settings.
 *
 * Fully responsive: adapts gracefully to any aspect ratio (from 16:10 tablets up to 21:9 phones)
 * with zero letterboxing and seamless edge-to-edge celestial rendering.
 */
public final class TitleScreen {

    public enum Action {
        NONE, CONTINUE, CHAPTERS, TUTORIAL, CODEX, SETTINGS
    }

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path path = new Path();

    private static final UiButton btnContinue = new UiButton();
    private static final UiButton btnChapters = new UiButton();
    private static final UiButton btnTutorial = new UiButton();
    private static final UiButton btnCodex = new UiButton();
    private static final UiButton btnSettings = new UiButton();

    private static boolean layoutReady;
    private static float lastSceneH = 0f;

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float btnW = 580f;
        float l = cx - btnW * 0.5f;
        float r = cx + btnW * 0.5f;

        boolean tall = (sceneH > 1750f);
        float startY = tall ? 640f : 570f;
        float heroH = tall ? 104f : 96f;
        float subH = tall ? 86f : 78f;
        float gap = tall ? 22f : 18f;

        // Hero CTA
        btnContinue.setBounds(l, startY, r, startY + heroH);
        btnContinue.isPrimary = true;
        btnContinue.cornerRadius = 22f;

        // Chapters
        float y2 = startY + heroH + gap;
        btnChapters.setBounds(l, y2, r, y2 + subH);
        btnChapters.cornerRadius = 18f;

        // How to Play & Codex (side-by-side)
        float halfW = (btnW - gap) * 0.5f;
        float y3 = y2 + subH + gap;
        btnTutorial.setBounds(l, y3, l + halfW, y3 + subH);
        btnTutorial.cornerRadius = 18f;

        btnCodex.setBounds(l + halfW + gap, y3, r, y3 + subH);
        btnCodex.cornerRadius = 18f;

        // Settings
        float y4 = y3 + subH + gap;
        btnSettings.setBounds(l, y4, r, y4 + subH);
        btnSettings.cornerRadius = 18f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        return onTouchDown(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchDown(float sx, float sy, float sceneH) {
        layout(sceneH);
        btnContinue.isPressed = btnContinue.hitTest(sx, sy);
        btnChapters.isPressed = btnChapters.hitTest(sx, sy);
        btnTutorial.isPressed = btnTutorial.hitTest(sx, sy);
        btnCodex.isPressed = btnCodex.hitTest(sx, sy);
        btnSettings.isPressed = btnSettings.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        return onTouchUp(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, float sceneH) {
        layout(sceneH);
        Action act = Action.NONE;

        if (btnContinue.isPressed && btnContinue.hitTest(sx, sy)) act = Action.CONTINUE;
        else if (btnChapters.isPressed && btnChapters.hitTest(sx, sy)) act = Action.CHAPTERS;
        else if (btnTutorial.isPressed && btnTutorial.hitTest(sx, sy)) act = Action.TUTORIAL;
        else if (btnCodex.isPressed && btnCodex.hitTest(sx, sy)) act = Action.CODEX;
        else if (btnSettings.isPressed && btnSettings.hitTest(sx, sy)) act = Action.SETTINGS;

        btnContinue.isPressed = false;
        btnChapters.isPressed = false;
        btnTutorial.isPressed = false;
        btnCodex.isPressed = false;
        btnSettings.isPressed = false;

        return act;
    }

    public static void cancelPress() {
        btnContinue.isPressed = false;
        btnChapters.isPressed = false;
        btnTutorial.isPressed = false;
        btnCodex.isPressed = false;
        btnSettings.isPressed = false;
    }

    public static void draw(Canvas c, LevelProgress progress, float time, float flicker) {
        draw(c, progress, time, flicker, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, LevelProgress progress, float time, float flicker, float sceneH) {
        layout(sceneH);

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;
        boolean tall = (sceneH > 1750f);

        // 1. Rich Celestial Twilight & Warm Radiance Gradient (Fills 100% of the screen)
        fill.setShader(new LinearGradient(0, 0, 0, sh,
                new int[]{
                        GameRenderer.mix(0xFF56341C, 0xFF6E4224, flicker), // Luminous amber warmth near candle
                        0xFF1B2032,                                       // Deep celestial midnight indigo
                        0xFF101320                                        // Dark obsidian base vignette
                },
                new float[]{0f, 0.38f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, sw, sh, fill);
        fill.setShader(null);

        // 2. Slow Rotating Celestial Astrolabe Ring in Background
        float ringCy = tall ? 440f : 390f;
        int saveRing = c.save();
        c.translate(cx, ringCy);
        c.rotate(time * 3.5f);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0x44D4AF37);
        c.drawCircle(0, 0, 260f, stroke);
        c.drawCircle(0, 0, 340f, stroke);

        // Ticks around background ring
        for (int i = 0; i < 360; i += 15) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            boolean major = (i % 30 == 0);
            stroke.setStrokeWidth(major ? 2.5f : 1.2f);
            stroke.setColor(major ? 0x99FFD54F : 0x44FFE0A8);
            c.drawLine(cos * 248f, sin * 248f, cos * 272f, sin * 272f, stroke);
        }
        c.restoreToCount(saveRing);

        // 3. Living Candle Light at the top (safe clearance below status bar/notch)
        float lx = ShadowCalculator.LIGHT_X;
        float ly = tall ? 210f : 170f;
        drawCandleGlow(c, lx, ly, flicker);

        // 4. Logo Typography: "SHADOW WEAVER"
        float titleY = tall ? 390f : 350f;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(64f);

        // Deep drop-shadow for title
        text.setColor(0xEE000000);
        c.drawText("SHADOW WEAVER", cx, titleY + 3.5f, text);

        // Warm gold foil bevel
        text.setColor(0xFFC5943C);
        c.drawText("SHADOW WEAVER", cx, titleY + 1.5f, text);

        // Main luminous gold title
        text.setColor(0xFFFFFDF8);
        c.drawText("SHADOW WEAVER", cx, titleY, text);

        // Subtitle banner with drop shadow
        float subY = titleY + 44f;
        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xDD000000);
        c.drawText("*  THE ALCHEMIST'S THEATER OF SHADOWS  *", cx, subY + 1.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("*  THE ALCHEMIST'S THEATER OF SHADOWS  *", cx, subY, text);

        // Decorative brass dividing flourish
        float divY = subY + 24f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawLine(cx - 200f, divY, cx + 200f, divY, stroke);
        fill.setColor(0xFFFFE082);
        c.drawCircle(cx, divY, 5.5f, fill);

        // 5. Update Dynamic Button Labels
        int currentUnlocked = (progress != null) ? progress.getHighestUnlocked() : 0;
        int totalStars = (progress != null) ? progress.getTotalStars() : 0;
        LevelData currentLevel = LevelLoader.get(currentUnlocked);
        String levelName = (currentLevel != null) ? currentLevel.title : "House";

        if (currentUnlocked == 0 && totalStars == 0) {
            btnContinue.label = "BEGIN JOURNEY";
            btnContinue.subLabel = "Chapter I • Level 1: " + levelName;
        } else {
            btnContinue.label = "CONTINUE TRIAL";
            btnContinue.subLabel = "Chapter " + ((currentUnlocked / 5) + 1) + " • Level " + (currentUnlocked + 1) + ": " + levelName;
        }

        btnChapters.label = "CHAPTER MAP";
        btnChapters.subLabel = "⭐ " + totalStars + " / " + LevelProgress.TOTAL_MAX_STARS + " Stars Collected";

        btnTutorial.label = "HOW TO PLAY";
        btnTutorial.subLabel = "Rules of Light";

        btnCodex.label = "JOURNAL";
        btnCodex.subLabel = "Relics & Lore";

        btnSettings.label = "SETTINGS";
        btnSettings.subLabel = "Audio & Haptics";

        // 6. Draw Buttons
        btnContinue.draw(c, fill, stroke, text, 1.0f);
        btnChapters.draw(c, fill, stroke, text, 1.0f);
        btnTutorial.draw(c, fill, stroke, text, 1.0f);
        btnCodex.draw(c, fill, stroke, text, 1.0f);
        btnSettings.draw(c, fill, stroke, text, 1.0f);

        // 7. Astral Mastery Status Card (centered, perfectly formatted)
        float cardW = 600f;
        float cardL = cx - cardW * 0.5f;
        float cardR = cx + cardW * 0.5f;
        float cardT = btnSettings.bottom + (tall ? 36f : 24f);
        float cardB = cardT + (tall ? 200f : 176f);

        fill.setColor(0xEE1A2234);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 20f, 20f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 20f, 20f, stroke);

        // Specular highlight line
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66FFFFFF);
        c.drawLine(cardL + 20f, cardT + 1f, cardR - 20f, cardT + 1f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Card Typography - EXPLICIT CENTER ALIGNMENT FIX
        text.setTextAlign(Paint.Align.CENTER);
        String rank = (progress != null) ? progress.getPlayerRank() : "Apprentice of Shadows";
        text.setTextSize(17f);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("CURRENT PLAYER ALCHEMIST RANK", cx, cardT + 42f, text);

        text.setTextSize(28f);
        text.setColor(0xFFFFFDF8);
        c.drawText(rank, cx, cardT + 84f, text);

        // Star constellation progress bar
        float barW = 480f;
        float barH = 16f;
        float barL = cx - barW * 0.5f;
        float barT = cardT + 116f;
        fill.setColor(0x550E131E);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 8f, 8f, fill);

        float pct = Math.min(1.0f, totalStars / (float) LevelProgress.TOTAL_MAX_STARS);
        if (pct > 0.01f) {
            fill.setColor(0xFFFFB300);
            c.drawRoundRect(barL, barT, barL + barW * pct, barT + barH, 8f, 8f, fill);
        }

        text.setTextSize(17f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xDDFFE0A8);
        c.drawText("Constellation Mastery: " + Math.round(pct * 100f) + "% (" + totalStars + " / " + LevelProgress.TOTAL_MAX_STARS + " Stars)", cx, cardT + 168f, text);

        // 8. On tall screens: Render an authentic antique Astrological Compass Crest
        if (tall && (sh - cardB) > 220f) {
            float crestCy = (cardB + sh - 70f) * 0.5f;
            drawCelestialCrest(c, cx, crestCy);
        }

        // 9. Footer Version
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(16f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x88D4AF37);
        c.drawText("v2.0 • Pure Native Android Engine • 60-120 FPS", cx, sh - 46f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCelestialCrest(Canvas c, float cx, float cy) {
        // Delicate orbital astrolabe rings
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x33D4AF37);
        c.drawCircle(cx, cy, 54f, stroke);
        c.drawCircle(cx, cy, 74f, stroke);

        // 8-Point Diamond Star
        fill.setColor(0x88D4AF37);
        path.reset();
        for (int i = 0; i < 8; i++) {
            float rad = (float) Math.toRadians(i * 45 - 90);
            float rDist = (i % 2 == 0) ? 46f : 18f;
            float px = cx + (float) Math.cos(rad) * rDist;
            float py = cy + (float) Math.sin(rad) * rDist;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, fill);

        // Star center pearl
        fill.setColor(0xFFFFE0A8);
        c.drawCircle(cx, cy, 4.5f, fill);

        // Radiant Latin / Alchemical Motto
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(15f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x66FFE0A8);
        c.drawText("• ALIGN THE SHADOWS • REVEAL THE LIGHT •", cx, cy + 92f, text);
    }

    private static void drawCandleGlow(Canvas c, float lx, float ly, float flicker) {
        // Candlestick saucer & pillar
        fill.setColor(0xFFD4AF37);
        c.drawRoundRect(lx - 22f, ly + 68f, lx + 22f, ly + 76f, 4f, 4f, fill);
        c.drawCircle(lx, ly + 72f, 5f, fill);

        // Ivory Tallow Pillar
        fill.setColor(0xFFDFD4C4);
        c.drawRoundRect(lx - 8f, ly + 14f, lx + 8f, ly + 68f, 4f, 4f, fill);

        // Candle Wick
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0xFF2E1C14);
        c.drawLine(lx, ly + 14f, lx, ly + 4f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Multi-lobe incandescent flame
        float flameH = 36f * (0.88f + 0.12f * flicker);
        float flameW = 13f * (0.92f + 0.08f * flicker);

        path.reset();
        path.moveTo(lx, ly - flameH);
        path.cubicTo(lx + flameW * 1.2f * flicker, ly - flameH * 0.45f,
                lx + flameW, ly + 5f, lx, ly + 6f);
        path.cubicTo(lx - flameW, ly + 5f,
                lx - flameW * 1.2f * flicker, ly - flameH * 0.45f, lx, ly - flameH);
        path.close();

        fill.setColor(0xFFFFB74D);
        c.drawPath(path, fill);

        // White incandescent core
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(lx, ly - flameH * 0.35f, 6.5f, fill);

        // Blue combustion base
        fill.setColor(0xBB3F51B5);
        c.drawCircle(lx, ly + 4f, 4f, fill);
    }
}
