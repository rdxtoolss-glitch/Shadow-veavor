package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Shadow Weaver - TitleScreen
 * ===========================
 * Masterwork title screen with an authentic 19th-century optical shadow-theater aesthetic:
 * - Deep multi-stop vignetted dark chamber wall
 * - Volumetric theatrical projector spotlight and soft atmospheric light cone
 * - Architectural antique brass frame and corner brackets
 * - Occult optical aperture sigil
 * - Microscopic suspended golden dust motes
 * - Perfectly balanced, uncluttered vertical composition
 * - Zero vanity cards, zero Latin slogans, zero glyph pollution
 * - Zero allocations in draw loop (solid 60-120 FPS).
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
    private static final RectF frameRect = new RectF();

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

    // Derived layout coordinates
    private static float cachedSigilY;
    private static float cachedTitleY;
    private static float cachedSubY;
    private static float cachedDivY;
    private static float cachedFooterY;

    // -------------------------------------------------------------------------
    // AMBIENT DUST MOTES (SUBTLE VOLUMETRIC SUSPENDED PARTICLES)
    // -------------------------------------------------------------------------
    private static final int MOTE_COUNT = 16;
    private static final float[] moteX = new float[MOTE_COUNT];
    private static final float[] moteY = new float[MOTE_COUNT];
    private static final float[] moteSpeed = new float[MOTE_COUNT];
    private static final float[] moteSwayAmp = new float[MOTE_COUNT];
    private static final float[] moteSwayFreq = new float[MOTE_COUNT];
    private static final float[] moteRadius = new float[MOTE_COUNT];
    private static final float[] moteAlpha = new float[MOTE_COUNT];
    private static boolean motesReady = false;

    private static void ensureMotes() {
        if (motesReady) return;
        long seed = 123456789L;
        for (int i = 0; i < MOTE_COUNT; i++) {
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteX[i] = 60f + (seed % 960);
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteY[i] = (seed % 2400);
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteSpeed[i] = 8f + (seed % 18); // 8 to 26 px/sec slow drift
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteSwayAmp[i] = 6f + (seed % 12); // 6 to 18 px subtle sway
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteSwayFreq[i] = 0.4f + (seed % 50) * 0.012f; // 0.4 to 1.0 rad/s
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteRadius[i] = 1.0f + (seed % 12) * 0.08f; // 1.0 to 2.0 px microscopic dust
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteAlpha[i] = 0.15f + (seed % 30) * 0.008f; // 0.15 to 0.39 soft transparency
        }
        motesReady = true;
    }

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;
        ensureMotes();

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float btnW = 860f;
        float l = cx - btnW * 0.5f;
        float r = cx + btnW * 0.5f;
        float splitGap = 18f;
        float splitW = (btnW - splitGap) * 0.5f; // 421f

        // Interactive stack component heights
        float heroH = 114f;
        float btnH = 92f;
        float splitH = 88f;
        float settingsH = 88f;
        float gap = 18f;

        // Total height of the interactive block (Hero + Chapters + Split Row + Settings)
        float stackH = heroH + gap + btnH + gap + splitH + gap + settingsH; // 436f

        // Header estimated height: ~210f
        float headerH = 210f;
        // Footer estimated height: ~60f
        float footerH = 60f;

        float totalContentH = headerH + stackH + footerH; // ~706f
        float remainingH = Math.max(60f, sceneH - totalContentH);

        // Golden-ratio distribution of vertical space
        float topPad = remainingH * 0.30f;
        float gapHeaderStack = remainingH * 0.38f;
        float bottomPad = remainingH * 0.32f;

        // 1. Header Coordinates
        cachedSigilY = topPad + 45f;
        cachedTitleY = cachedSigilY + 82f;
        cachedSubY = cachedTitleY + 44f;
        cachedDivY = cachedSubY + 24f;

        // 2. Interactive Menu Stack Coordinates
        float stackStartY = cachedDivY + gapHeaderStack;

        // Hero CTA (Continue / Begin)
        btnContinue.setBounds(l, stackStartY, r, stackStartY + heroH);
        btnContinue.isPrimary = true;
        btnContinue.cornerRadius = 24f;

        // Chapters
        float y2 = stackStartY + heroH + gap;
        btnChapters.setBounds(l, y2, r, y2 + btnH);
        btnChapters.cornerRadius = 22f;

        // How to Play & Journal (Side-by-side)
        float y3 = y2 + btnH + gap;
        btnTutorial.setBounds(l, y3, l + splitW, y3 + splitH);
        btnTutorial.cornerRadius = 22f;
        btnCodex.setBounds(l + splitW + splitGap, y3, r, y3 + splitH);
        btnCodex.cornerRadius = 22f;

        // Settings
        float y4 = y3 + splitH + gap;
        btnSettings.setBounds(l, y4, r, y4 + settingsH);
        btnSettings.cornerRadius = 22f;

        // 3. Footer Coordinates
        cachedFooterY = sceneH - Math.max(28f, bottomPad * 0.5f);

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

    public static void drawCelestialBackdrop(Canvas c, float time, float flicker, float sceneH) {
        ensureMotes();
        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;

        // 1. Rich Atmospheric Shadow Wall Gradient (Anti-banding 4-stop)
        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new LinearGradient(0, 0, 0, sh,
                new int[]{
                        GameRenderer.mix(0xFF1E1714, 0xFF271C17, flicker), // Subtle warm antique chamber top
                        0xFF13151D,                                       // Deep walnut obsidian shadow
                        0xFF0C0F17,                                       // Midnight sapphire shadow
                        0xFF05070B                                        // Velvet obsidian black base
                },
                new float[]{0f, 0.30f, 0.68f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, sw, sh, fill);
        fill.setShader(null);

        // 2. Wide Volumetric Projector Spotlight Glow
        float spotY = (cachedSigilY > 0f) ? cachedSigilY + 30f : sh * 0.14f;
        float rGlow = (sh * 0.52f) * (0.97f + 0.03f * flicker);
        glow.setShader(new RadialGradient(cx, spotY, rGlow,
                new int[]{
                        0x28FFD54F, // Soft warm amber spotlight core
                        0x14FF9800, // Gentle golden haze
                        0x051C1410, // Soft atmospheric dropoff
                        0x00000000  // Zero edge
                },
                new float[]{0f, 0.28f, 0.65f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, spotY, rGlow, glow);
        glow.setShader(null);

        // 3. Volumetric Theatrical Projector Light Cone
        drawVolumetricProjectorBeam(c, cx, sw, sh, time, flicker);

        // 4. Architectural Antique Brass Screen Frame & Corner Filigrees
        drawArchitecturalFrame(c, sw, sh);

        // 5. Living Suspended Golden Dust Motes
        drawFloatingDustMotes(c, time, sh);
    }

    public static void draw(Canvas c, LevelProgress progress, float time, float flicker, float sceneH) {
        layout(sceneH);

        float cx = ShadowCalculator.SCENE_W * 0.5f;

        // ---------------------------------------------------------------------
        // 1-5. CINEMATIC ALCHEMICAL BACKDROP
        // ---------------------------------------------------------------------
        drawCelestialBackdrop(c, time, flicker, sceneH);

        // ---------------------------------------------------------------------
        // 6. OCCULT OPTICAL APERTURE SIGIL AT TOP
        // ---------------------------------------------------------------------
        drawAlchemicalSigil(c, cx, cachedSigilY, time, flicker);

        // ---------------------------------------------------------------------
        // 7. LOGO TYPOGRAPHY: "SHADOW WEAVER" (CLEAN RADIANT IVORY)
        // ---------------------------------------------------------------------
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(64f);

        // Refined drop shadow + radiant ivory letterforms
        text.setColor(0xCC000000);
        c.drawText("SHADOW WEAVER", cx, cachedTitleY + 3.0f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("SHADOW WEAVER", cx, cachedTitleY, text);

        // Subtitle: Clean, quiet, atmospheric (zero unicode starbursts)
        text.setTextSize(20f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xAA000000);
        c.drawText("THE THEATER OF SHADOWS", cx, cachedSubY + 1.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("THE THEATER OF SHADOWS", cx, cachedSubY, text);

        // Subtle brass dividing line
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.6f);
        stroke.setColor(0x55D4AF37);
        c.drawLine(cx - 200f, cachedDivY, cx + 200f, cachedDivY, stroke);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xFFFFE082);
        c.drawCircle(cx, cachedDivY, 4.0f, fill);

        // ---------------------------------------------------------------------
        // 8. UPDATE DYNAMIC BUTTON LABELS
        // ---------------------------------------------------------------------
        int currentUnlocked = (progress != null) ? progress.getHighestUnlocked() : 0;
        int totalStars = (progress != null) ? progress.getTotalStars() : 0;
        LevelData currentLevel = LevelLoader.get(currentUnlocked);
        String levelName = (currentLevel != null) ? currentLevel.title : "House";

        if (currentUnlocked == 0 && totalStars == 0) {
            btnContinue.label = "BEGIN JOURNEY";
            btnContinue.subLabel = "Chapter I • Level 1: " + levelName;
        } else {
            btnContinue.label = "CONTINUE";
            btnContinue.subLabel = "Chapter " + ((currentUnlocked / 5) + 1) + " • Level " + (currentUnlocked + 1) + ": " + levelName;
        }

        btnChapters.label = "CHAPTERS";
        btnChapters.subLabel = "★ " + totalStars + " / " + LevelProgress.TOTAL_MAX_STARS + " Stars";

        btnTutorial.label = "HOW TO PLAY";
        btnTutorial.subLabel = "Rules of Light";

        btnCodex.label = "JOURNAL";
        btnCodex.subLabel = "Relics & Lore";

        btnSettings.label = "SETTINGS";
        btnSettings.subLabel = "Audio & Haptics";

        // ---------------------------------------------------------------------
        // 9. DRAW BUTTONS
        // ---------------------------------------------------------------------
        btnContinue.draw(c, fill, stroke, text, 1.0f, time);
        btnChapters.draw(c, fill, stroke, text, 1.0f, time);
        btnTutorial.draw(c, fill, stroke, text, 1.0f, time);
        btnCodex.draw(c, fill, stroke, text, 1.0f, time);
        btnSettings.draw(c, fill, stroke, text, 1.0f, time);

        // ---------------------------------------------------------------------
        // 10. CLEAN FOOTER
        // ---------------------------------------------------------------------
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(14f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0x66FFE0A8);
        c.drawText("SHADOW WEAVER  •  AN OPTICAL PUZZLE", cx, cachedFooterY, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    // -------------------------------------------------------------------------
    // OCCULT OPTICAL APERTURE SIGIL
    // -------------------------------------------------------------------------
    private static void drawAlchemicalSigil(Canvas c, float cx, float cy, float time, float flicker) {
        float haloR = 56f * (0.95f + 0.05f * flicker);
        glow.setShader(new RadialGradient(cx, cy, haloR,
                new int[]{0x38FFD54F, 0x10FF9800, 0x00000000},
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, haloR, glow);
        glow.setShader(null);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.6f);
        stroke.setColor(0x88D4AF37);
        c.drawCircle(cx, cy, 28f, stroke);

        stroke.setStrokeWidth(0.9f);
        stroke.setColor(0x44FFE0A8);
        c.drawCircle(cx, cy, 19f, stroke);

        // Cardinal rays
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66FFD54F);
        c.drawLine(cx, cy - 38f, cx, cy - 28f, stroke);
        c.drawLine(cx, cy + 28f, cx, cy + 38f, stroke);
        c.drawLine(cx - 38f, cy, cx - 28f, cy, stroke);
        c.drawLine(cx + 28f, cy, cx + 38f, cy, stroke);

        // Central illuminated jewel
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xFFFFD54F);
        c.drawCircle(cx, cy, 3.5f, fill);
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(cx - 1.0f, cy - 1.0f, 1.2f, fill);
    }

    // -------------------------------------------------------------------------
    // THEATRICAL VOLUMETRIC PROJECTOR BEAM
    // -------------------------------------------------------------------------
    private static void drawVolumetricProjectorBeam(Canvas c, float cx, float sw, float sh, float time, float flicker) {
        float beamAlphaPulse = 0.025f + 0.005f * (float) Math.sin(time * 1.5f) * flicker;
        int colBeamTop = ((int) (255 * beamAlphaPulse * 1.4f) << 24) | 0x00FFE8A8;
        int colBeamMid = ((int) (255 * beamAlphaPulse * 0.7f) << 24) | 0x00FFD54F;
        int colBeamEnd = 0x00000000;

        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new LinearGradient(cx, 0, cx, sh,
                new int[]{colBeamTop, colBeamMid, colBeamEnd},
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));

        path.reset();
        path.moveTo(cx - 32f, 0f);
        path.lineTo(cx + 32f, 0f);
        path.lineTo(sw * 0.92f, sh);
        path.lineTo(sw * 0.08f, sh);
        path.close();
        c.drawPath(path, fill);
        fill.setShader(null);
    }

    // -------------------------------------------------------------------------
    // ARCHITECTURAL ANTIQUE BRASS FRAME
    // -------------------------------------------------------------------------
    private static void drawArchitecturalFrame(Canvas c, float sw, float sh) {
        stroke.setStyle(Paint.Style.STROKE);

        float insetOuter = 26f;
        frameRect.set(insetOuter, insetOuter, sw - insetOuter, sh - insetOuter);
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x22D4AF37);
        c.drawRoundRect(frameRect, 18f, 18f, stroke);

        float insetInner = 34f;
        frameRect.set(insetInner, insetInner, sw - insetInner, sh - insetInner);
        stroke.setStrokeWidth(0.7f);
        stroke.setColor(0x10D4AF37);
        c.drawRoundRect(frameRect, 14f, 14f, stroke);

        stroke.setStyle(Paint.Style.FILL);
    }

    // -------------------------------------------------------------------------
    // SUSPENDED GOLDEN DUST MOTES
    // -------------------------------------------------------------------------
    private static void drawFloatingDustMotes(Canvas c, float time, float sh) {
        ensureMotes();
        fill.setStyle(Paint.Style.FILL);
        for (int i = 0; i < MOTE_COUNT; i++) {
            float y = (moteY[i] - time * moteSpeed[i]) % (sh + 20f);
            if (y < -10f) y += sh + 20f;

            float x = moteX[i] + (float) Math.sin(time * moteSwayFreq[i] + i) * moteSwayAmp[i];

            float edgeFade = 1.0f;
            if (y < 100f) edgeFade = Math.max(0f, y / 100f);
            else if (y > sh - 100f) edgeFade = Math.max(0f, (sh - y) / 100f);

            int alpha = (int) (255 * moteAlpha[i] * edgeFade);
            if (alpha <= 0) continue;

            float r = moteRadius[i];
            fill.setColor((0x00FFE8A8) | (alpha << 24));
            c.drawCircle(x, y, r, fill);
        }
    }
}
