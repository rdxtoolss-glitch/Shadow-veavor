package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Shadow Weaver - TitleScreen
 * ===========================
 * Masterwork title screen with an authentic antique astrolabe theme,
 * dynamic full-height responsive layout, living candlelight glow,
 * and high-contrast alchemical UI typography.
 *
 * Guarantees zero empty dead space across any aspect ratio (16:9, 19.5:9, 20:9, 21:9).
 */
public final class TitleScreen {

    public enum Action {
        NONE, CONTINUE, CHAPTERS, TUTORIAL, CODEX, SETTINGS
    }

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
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
        float btnW = 600f;
        float l = cx - btnW * 0.5f;
        float r = cx + btnW * 0.5f;

        // Adaptive golden-ratio vertical distribution
        boolean tall = (sceneH > 1800f);
        float startY = tall ? (sceneH * 0.28f) : (sceneH * 0.32f);
        float heroH = tall ? 108f : 96f;
        float subH = tall ? 88f : 78f;
        float gap = tall ? 22f : 16f;

        // 1. Hero CTA (Continue / Begin)
        btnContinue.setBounds(l, startY, r, startY + heroH);
        btnContinue.isPrimary = true;
        btnContinue.cornerRadius = 24f;

        // 2. Chapter Map
        float y2 = startY + heroH + gap;
        btnChapters.setBounds(l, y2, r, y2 + subH);
        btnChapters.cornerRadius = 18f;

        // 3. How to Play & Codex (Side-by-side)
        float halfW = (btnW - gap) * 0.5f;
        float y3 = y2 + subH + gap;
        btnTutorial.setBounds(l, y3, l + halfW, y3 + subH);
        btnTutorial.cornerRadius = 18f;

        btnCodex.setBounds(l + halfW + gap, y3, r, y3 + subH);
        btnCodex.cornerRadius = 18f;

        // 4. Settings
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
        boolean tall = (sceneH > 1800f);

        // 1. Rich Atmospheric Chamber Backdrop (100% full-viewport coverage)
        fill.setShader(new LinearGradient(0, 0, 0, sh,
                new int[]{
                        GameRenderer.mix(0xFF6E4224, 0xFF8A532C, flicker), // Luminous amber warmth near flame
                        0xFF1B2032,                                       // Deep celestial midnight sapphire
                        0xFF0F131E                                        // Rich obsidian base vignette
                },
                new float[]{0f, 0.35f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, sw, sh, fill);
        fill.setShader(null);

        // 2. Slow Rotating Celestial Astrolabe in Background
        float astrolabeCy = tall ? (sh * 0.18f) : (sh * 0.20f);
        int saveRing = c.save();
        c.translate(cx, astrolabeCy);
        c.rotate(time * 3.0f);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0x33D4AF37);
        c.drawCircle(0, 0, 280f, stroke);
        c.drawCircle(0, 0, 360f, stroke);

        // Astrolabe degree markings
        for (int i = 0; i < 360; i += 15) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            boolean major = (i % 30 == 0);
            stroke.setStrokeWidth(major ? 2.5f : 1.2f);
            stroke.setColor(major ? 0x88FFD54F : 0x33FFE0A8);
            c.drawLine(cos * 265f, sin * 265f, cos * 290f, sin * 290f, stroke);
        }
        c.restoreToCount(saveRing);

        // 3. Living Candle Light at the top
        float lx = ShadowCalculator.LIGHT_X;
        float ly = tall ? (sh * 0.08f) : 160f;
        drawCandleGlow(c, lx, ly, flicker);

        // 4. Logo Typography: "SHADOW WEAVER"
        float titleY = tall ? (sh * 0.17f) : 340f;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(66f);

        // Triple-pass gold leaf typography
        text.setColor(0xEE000000);
        c.drawText("SHADOW WEAVER", cx, titleY + 3.5f, text);
        text.setColor(0xFFC5943C);
        c.drawText("SHADOW WEAVER", cx, titleY + 1.5f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("SHADOW WEAVER", cx, titleY, text);

        // Subtitle banner with drop shadow
        float subY = titleY + 44f;
        text.setTextSize(21f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xDD000000);
        c.drawText("✦  THE ALCHEMIST'S THEATER OF SHADOWS  ✦", cx, subY + 1.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("✦  THE ALCHEMIST'S THEATER OF SHADOWS  ✦", cx, subY, text);

        // Decorative brass dividing flourish
        float divY = subY + 24f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0x66D4AF37);
        c.drawLine(cx - 220f, divY, cx + 220f, divY, stroke);
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

        // 7. Astral Mastery Status Card (Balanced & Proportional)
        float cardW = 660f;
        float cardL = cx - cardW * 0.5f;
        float cardR = cx + cardW * 0.5f;
        float cardT = btnSettings.bottom + (tall ? 36f : 24f);
        float cardH = tall ? 220f : 180f;
        float cardB = cardT + cardH;

        fill.setColor(0xF01A2234);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.4f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Specular highlight line
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66FFFFFF);
        c.drawLine(cardL + 24f, cardT + 1.5f, cardR - 24f, cardT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Card Typography - EXPLICIT CENTER ALIGNMENT
        text.setTextAlign(Paint.Align.CENTER);
        String rank = (progress != null) ? progress.getPlayerRank() : "Apprentice of Shadows";
        text.setTextSize(18f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("CURRENT PLAYER ALCHEMIST RANK", cx, cardT + 42f, text);

        text.setTextSize(30f);
        text.setColor(0xFFFFFDF8);
        c.drawText(rank, cx, cardT + 86f, text);

        // Star constellation progress bar
        float barW = 520f;
        float barH = 18f;
        float barL = cx - barW * 0.5f;
        float barT = cardT + 120f;
        fill.setColor(0x550E131E);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 9f, 9f, fill);

        float pct = Math.min(1.0f, totalStars / (float) LevelProgress.TOTAL_MAX_STARS);
        if (pct > 0.01f) {
            fill.setColor(0xFFFFB300);
            c.drawRoundRect(barL, barT, barL + barW * pct, barT + barH, 9f, 9f, fill);
        }

        text.setTextSize(18f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xEEFFE0A8);
        c.drawText("Constellation Mastery: " + Math.round(pct * 100f) + "% (" + totalStars + " / " + LevelProgress.TOTAL_MAX_STARS + " Stars)", cx, cardT + 175f, text);

        // 8. On tall screens: Render Grand Antique Astrological Compass Crest
        if (tall && (sh - cardB) > 200f) {
            float crestCy = (cardB + sh - 60f) * 0.5f;
            drawCelestialCrest(c, cx, crestCy);
        }

        // 9. Footer Version
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(16f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0x88D4AF37);
        c.drawText("v2.0 • Pure Native Android Engine • 60-120 FPS", cx, sh - 42f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCelestialCrest(Canvas c, float cx, float cy) {
        // Delicate orbital astrolabe rings
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x33D4AF37);
        c.drawCircle(cx, cy, 64f, stroke);
        c.drawCircle(cx, cy, 88f, stroke);

        // 12 Astrological Hour Marks
        for (int i = 0; i < 360; i += 30) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            stroke.setStrokeWidth(1.2f);
            stroke.setColor(0x55FFE0A8);
            c.drawLine(cx + cos * 64f, cy + sin * 64f, cx + cos * 88f, cy + sin * 88f, stroke);
        }

        // 8-Point Diamond Star
        fill.setColor(0x99D4AF37);
        path.reset();
        for (int i = 0; i < 8; i++) {
            float rad = (float) Math.toRadians(i * 45 - 90);
            float rDist = (i % 2 == 0) ? 54f : 20f;
            float px = cx + (float) Math.cos(rad) * rDist;
            float py = cy + (float) Math.sin(rad) * rDist;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, fill);

        // Star center pearl
        fill.setColor(0xFFFFE0A8);
        c.drawCircle(cx, cy, 5.5f, fill);

        // Radiant Latin / Alchemical Motto
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(16f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0x88FFE0A8);
        c.drawText("✦ QUOD EST INFERIUS EST SICUT QUOD EST SUPERIUS ✦", cx, cy + 118f, text);
        text.setTextSize(13f);
        text.setColor(0x55FFE0A8);
        c.drawText("As within, so without • As above, so below", cx, cy + 140f, text);
    }

    private static void drawCandleGlow(Canvas c, float lx, float ly, float flicker) {
        // Multi-layered incandescent flame glow
        float r = 160f * (0.95f + 0.05f * flicker);
        glow.setShader(new RadialGradient(lx, ly, r,
                new int[]{
                        0x66FFD54F,
                        0x22FF8F00,
                        0x00000000
                },
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(lx, ly, r, glow);
        glow.setShader(null);

        // Candle Tallow Pillar
        fill.setColor(0xFFEFE8DC);
        c.drawRoundRect(lx - 9f, ly + 14f, lx + 9f, ly + 72f, 3f, 3f, fill);

        // Shaded side
        fill.setColor(0x332A180C);
        c.drawRoundRect(lx + 2f, ly + 14f, lx + 9f, ly + 72f, 3f, 3f, fill);

        // Brass candle stand rim
        fill.setColor(0xFFC5A059);
        c.drawRoundRect(lx - 24f, ly + 68f, lx + 24f, ly + 78f, 3f, 3f, fill);

        // Wick
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0xFF211105);
        c.drawLine(lx, ly + 14f, lx, ly + 3f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Outer warm golden teardrop flame
        float flameW = 12f * flicker;
        float flameH = 26f * flicker;
        path.reset();
        path.moveTo(lx, ly - flameH);
        path.cubicTo(lx + flameW, ly - flameH * 0.4f, lx + flameW * 0.8f, ly + 4f, lx, ly + 4f);
        path.cubicTo(lx - flameW * 0.8f, ly + 4f, lx - flameW, ly - flameH * 0.4f, lx, ly - flameH);
        path.close();

        fill.setColor(0xFFFF9800);
        c.drawPath(path, fill);

        // Inner incandescent white-gold core
        float coreW = flameW * 0.55f;
        float coreH = flameH * 0.65f;
        path.reset();
        path.moveTo(lx, ly - flameH * 0.2f - coreH);
        path.cubicTo(lx + coreW, ly - flameH * 0.2f - coreH * 0.4f, lx + coreW * 0.8f, ly + 3f, lx, ly + 3f);
        path.cubicTo(lx - coreW * 0.8f, ly + 3f, lx - coreW, ly - flameH * 0.2f - coreH * 0.4f, lx, ly - flameH * 0.2f - coreH);
        path.close();

        fill.setColor(0xFFFFFFFF);
        c.drawPath(path, fill);

        // Blue wick base
        fill.setColor(0xBB3F51B5);
        c.drawCircle(lx, ly + 3f, 3.5f, fill);
    }
}
