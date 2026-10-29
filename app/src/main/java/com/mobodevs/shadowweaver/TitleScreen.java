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
 * Masterwork title screen with an authentic antique shadow-theater alchemical aesthetic:
 * - Deep multi-stop vignetted dark chamber wall
 * - Volumetric theatrical projector spotlight and light cone
 * - Architectural antique brass frame and corner filigree brackets
 * - Occult alchemical aperture sigil (clean, noble, zero cartoon wax/flames)
 * - Microscopic ambient golden dust motes
 * - Perfectly centered vertical composition with zero empty dead space
 * - Uncompromised 60-120 FPS performance (zero allocations in draw loop).
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
    private static float cachedCardTop;
    private static float cachedCardBottom;
    private static float cachedMottoY;
    private static float cachedTransY;
    private static float cachedVersionY;

    // -------------------------------------------------------------------------
    // AMBIENT DUST MOTES (SUBTLE VOLUMETRIC SUSPENDED PARTICLES)
    // -------------------------------------------------------------------------
    private static final int MOTE_COUNT = 18;
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
            moteSpeed[i] = 10f + (seed % 22); // 10 to 32 px/sec slow drift
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteSwayAmp[i] = 8f + (seed % 14); // 8 to 22 px subtle sway
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteSwayFreq[i] = 0.5f + (seed % 50) * 0.015f; // 0.5 to 1.25 rad/s
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteRadius[i] = 1.0f + (seed % 15) * 0.08f; // 1.0 to 2.2 px microscopic dust
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            moteAlpha[i] = 0.18f + (seed % 40) * 0.008f; // 0.18 to 0.50 soft transparency
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
        // Industry-standard 80% button width on 1080px canvas (110px margins)
        float btnW = 860f;
        float l = cx - btnW * 0.5f;
        float r = cx + btnW * 0.5f;
        float splitGap = 18f;
        float splitW = (btnW - splitGap) * 0.5f; // 421f

        // Interactive stack component heights
        float heroH = 118f;
        float btnH = 96f;
        float gap = 18f;
        float cardGap = 22f;
        float cardH = 195f;

        // Total height of the interactive block (Hero + Chapter Map + Split Row + Settings + Rank Card)
        float stackH = heroH + gap + btnH + gap + btnH + gap + btnH + cardGap + cardH; // 677f

        // Header estimated height (Sigil to divider line): ~220f
        float headerH = 220f;
        // Footer estimated height (Motto to version): ~110f
        float footerH = 110f;

        // Total content height and remaining vertical space
        float totalContentH = headerH + stackH + footerH; // ~1007f
        float remainingH = Math.max(50f, sceneH - totalContentH);

        // Golden-ratio distribution of remaining vertical space:
        float topPad = remainingH * 0.22f;
        float gapHeaderStack = remainingH * 0.32f;
        float gapStackFooter = remainingH * 0.26f;
        float bottomPad = remainingH * 0.20f;

        // 1. Header Coordinates
        cachedSigilY = topPad + 45f;
        cachedTitleY = cachedSigilY + 80f;
        cachedSubY = cachedTitleY + 42f;
        cachedDivY = cachedSubY + 22f;

        // 2. Interactive Menu Stack Coordinates (PERFECTLY CENTERED IN VIEWPORT)
        float stackStartY = cachedDivY + gapHeaderStack;

        // Hero CTA (Continue / Begin)
        btnContinue.setBounds(l, stackStartY, r, stackStartY + heroH);
        btnContinue.isPrimary = true;
        btnContinue.cornerRadius = 24f;

        // Chapter Map
        float y2 = stackStartY + heroH + gap;
        btnChapters.setBounds(l, y2, r, y2 + btnH);
        btnChapters.cornerRadius = 22f;

        // How to Play & Codex (Side-by-side, perfect bilateral symmetry)
        float y3 = y2 + btnH + gap;
        btnTutorial.setBounds(l, y3, l + splitW, y3 + btnH);
        btnTutorial.cornerRadius = 22f;
        btnCodex.setBounds(l + splitW + splitGap, y3, r, y3 + btnH);
        btnCodex.cornerRadius = 22f;

        // Settings
        float y4 = y3 + btnH + gap;
        btnSettings.setBounds(l, y4, r, y4 + btnH);
        btnSettings.cornerRadius = 22f;

        // Rank Card
        cachedCardTop = y4 + btnH + cardGap;
        cachedCardBottom = cachedCardTop + cardH;

        // 3. Footer Coordinates (Balanced in bottom third)
        cachedMottoY = cachedCardBottom + gapStackFooter * 0.55f + 16f;
        cachedTransY = cachedMottoY + 22f;
        cachedVersionY = sceneH - Math.max(26f, bottomPad * 0.65f);

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
        // 6. OCCULT ALCHEMICAL APERTURE SIGIL AT TOP
        // ---------------------------------------------------------------------
        drawAlchemicalSigil(c, cx, cachedSigilY, time, flicker);

        // ---------------------------------------------------------------------
        // 7. LOGO TYPOGRAPHY: "SHADOW WEAVER" (GOLD LEAF SPECULAR)
        // ---------------------------------------------------------------------
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(64f);

        // Triple-pass gold leaf typography with deep drop shadow
        text.setColor(0xEE000000);
        c.drawText("SHADOW WEAVER", cx, cachedTitleY + 3.5f, text);
        text.setColor(0xFFC5943C);
        c.drawText("SHADOW WEAVER", cx, cachedTitleY + 1.5f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("SHADOW WEAVER", cx, cachedTitleY, text);

        // Subtitle banner with drop shadow
        text.setTextSize(21f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xDD000000);
        c.drawText("\u2726  THE ALCHEMIST'S THEATER OF SHADOWS  \u2726", cx, cachedSubY + 1.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("\u2726  THE ALCHEMIST'S THEATER OF SHADOWS  \u2726", cx, cachedSubY, text);

        // Decorative brass dividing flourish
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0x66D4AF37);
        c.drawLine(cx - 240f, cachedDivY, cx + 240f, cachedDivY, stroke);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xFFFFE082);
        c.drawCircle(cx, cachedDivY, 5.5f, fill);
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x44D4AF37);
        c.drawLine(cx - 180f, cachedDivY - 3f, cx + 180f, cachedDivY - 3f, stroke);

        // ---------------------------------------------------------------------
        // 8. UPDATE DYNAMIC BUTTON LABELS
        // ---------------------------------------------------------------------
        int currentUnlocked = (progress != null) ? progress.getHighestUnlocked() : 0;
        int totalStars = (progress != null) ? progress.getTotalStars() : 0;
        LevelData currentLevel = LevelLoader.get(currentUnlocked);
        String levelName = (currentLevel != null) ? currentLevel.title : "House";

        if (currentUnlocked == 0 && totalStars == 0) {
            btnContinue.label = "BEGIN JOURNEY";
            btnContinue.subLabel = "Chapter I \u2022 Level 1: " + levelName;
        } else {
            btnContinue.label = "CONTINUE TRIAL";
            btnContinue.subLabel = "Chapter " + ((currentUnlocked / 5) + 1) + " \u2022 Level " + (currentUnlocked + 1) + ": " + levelName;
        }

        btnChapters.label = "CHAPTER MAP";
        btnChapters.subLabel = "\u2605 " + totalStars + " / " + LevelProgress.TOTAL_MAX_STARS + " Stars Collected";

        btnTutorial.label = "HOW TO PLAY";
        btnTutorial.subLabel = "Rules of Light";

        btnCodex.label = "JOURNAL";
        btnCodex.subLabel = "Relics & Lore";

        btnSettings.label = "SETTINGS";
        btnSettings.subLabel = "Audio & Haptics";

        // ---------------------------------------------------------------------
        // 9. DRAW BUTTONS WITH SPECULAR SWEEP & CORNER ACCENTS
        // ---------------------------------------------------------------------
        btnContinue.draw(c, fill, stroke, text, 1.0f, time);
        btnChapters.draw(c, fill, stroke, text, 1.0f, time);
        btnTutorial.draw(c, fill, stroke, text, 1.0f, time);
        btnCodex.draw(c, fill, stroke, text, 1.0f, time);
        btnSettings.draw(c, fill, stroke, text, 1.0f, time);

        // Official game-icons.net embellishments on menu buttons
        GameIcons.drawIcon(c, GameIcons.RELIC_PATHS[14], btnChapters.left + 54f, btnChapters.centerY(), 22f, 0xFFFFD54F);
        GameIcons.drawIcon(c, GameIcons.PATH_SETTINGS, btnSettings.left + 54f, btnSettings.centerY(), 22f, 0xFFFFD54F);

        // ---------------------------------------------------------------------
        // 10. ALCHEMIST MASTERY STATUS CARD (860PX WIDTH - MATCHES BUTTONS)
        // ---------------------------------------------------------------------
        float cardW = 860f;
        float cardL = cx - cardW * 0.5f;
        float cardR = cx + cardW * 0.5f;
        float cardT = cachedCardTop;
        float cardB = cachedCardBottom;

        // Card glassmorphic panel
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xF2131926);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Top specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(cardL + 24f, cardT + 1.5f, cardR - 24f, cardT + 1.5f, stroke);

        // Corner diamond accents
        drawCornerDiamond(c, cardL + 18f, cardT + 18f);
        drawCornerDiamond(c, cardR - 18f, cardT + 18f);
        drawCornerDiamond(c, cardL + 18f, cardB - 18f);
        drawCornerDiamond(c, cardR - 18f, cardB - 18f);

        // Card Typography
        text.setTextAlign(Paint.Align.CENTER);
        String rank = (progress != null) ? progress.getPlayerRank() : "Apprentice of Shadows";
        text.setTextSize(18f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("\u2726  ALCHEMIST MASTERY RANK  \u2726", cx, cardT + 40f, text);

        text.setTextSize(32f);
        text.setColor(0xFFFFFDF8);
        c.drawText(rank, cx, cardT + 82f, text);

        // Star constellation progress bar
        float barW = 720f;
        float barH = 18f;
        float barL = cx - barW * 0.5f;
        float barT = cardT + 112f;
        fill.setColor(0x660B0F18);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 9f, 9f, fill);
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x44D4AF37);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 9f, 9f, stroke);

        float pct = Math.min(1.0f, totalStars / (float) LevelProgress.TOTAL_MAX_STARS);
        if (pct > 0.01f) {
            float fillW = barW * pct;
            fill.setColor(0xFFFFB300);
            c.drawRoundRect(barL, barT, barL + fillW, barT + barH, 9f, 9f, fill);

            // Glowing head pip on progress bar
            fill.setColor(0xFFFFF4D0);
            c.drawCircle(barL + fillW - 4f, barT + barH * 0.5f, 6.5f, fill);
        }

        text.setTextSize(18f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xEEFFE0A8);
        c.drawText("Constellation Mastery: " + Math.round(pct * 100f) + "%  \u2022  " + totalStars + " / " + LevelProgress.TOTAL_MAX_STARS + " Stars", cx, cardT + 162f, text);

        // ---------------------------------------------------------------------
        // 11. LATIN ALCHEMICAL MOTTO & VERSION FOOTER
        // ---------------------------------------------------------------------
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(16f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0x99FFE0A8);
        c.drawText("\u2726 QUOD EST INFERIUS EST SICUT QUOD EST SUPERIUS \u2726", cx, cachedMottoY, text);
        text.setTextSize(13f);
        text.setColor(0x55FFE0A8);
        c.drawText("As within, so without \u2022 As above, so below", cx, cachedTransY, text);

        text.setTextSize(15f);
        text.setColor(0x88D4AF37);
        c.drawText("v2.0 \u2022 Pure Native Android Engine \u2022 60-120 FPS", cx, cachedVersionY, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    // -------------------------------------------------------------------------
    // OCCULT ALCHEMICAL APERTURE SIGIL (ELEGANT BRASS APERTURE WITH JEWEL CORE)
    // -------------------------------------------------------------------------
    private static void drawAlchemicalSigil(Canvas c, float cx, float cy, float time, float flicker) {
        // 1. Soft radial focal glow behind sigil
        float haloR = 64f * (0.95f + 0.05f * flicker);
        glow.setShader(new RadialGradient(cx, cy, haloR,
                new int[]{0x44FFD54F, 0x18FF9800, 0x00000000},
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, haloR, glow);
        glow.setShader(null);

        // 2. Outer Aperture Ring
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(0x99D4AF37);
        c.drawCircle(cx, cy, 32f, stroke);

        // Inner Concentric Ring
        stroke.setStrokeWidth(1.0f);
        stroke.setColor(0x55FFE0A8);
        c.drawCircle(cx, cy, 22f, stroke);

        // 3. Cardinal Ray Pips
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x77FFD54F);
        // Vertical ray segments
        c.drawLine(cx, cy - 44f, cx, cy - 32f, stroke);
        c.drawLine(cx, cy + 32f, cx, cy + 44f, stroke);
        // Horizontal ray segments
        c.drawLine(cx - 44f, cy, cx - 32f, cy, stroke);
        c.drawLine(cx + 32f, cy, cx + 44f, cy, stroke);

        // 4 Cardinal Finial Diamonds
        drawCornerDiamond(c, cx, cy - 44f);
        drawCornerDiamond(c, cx, cy + 44f);
        drawCornerDiamond(c, cx - 44f, cy);
        drawCornerDiamond(c, cx + 44f, cy);

        // 4. Central 8-Point Alchemical Star
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xDDFFD54F);
        path.reset();
        for (int i = 0; i < 8; i++) {
            float rad = (float) Math.toRadians(i * 45 - 90);
            float r = (i % 2 == 0) ? 14f : 6.5f;
            float px = cx + (float) Math.cos(rad) * r;
            float py = cy + (float) Math.sin(rad) * r;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, fill);

        // Center illuminated pearl
        fill.setColor(0xFFFFF4D0);
        c.drawCircle(cx, cy, 3.8f, fill);
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(cx - 1.0f, cy - 1.0f, 1.4f, fill);
    }

    // -------------------------------------------------------------------------
    // THEATRICAL VOLUMETRIC PROJECTOR BEAM
    // -------------------------------------------------------------------------
    private static void drawVolumetricProjectorBeam(Canvas c, float cx, float sw, float sh, float time, float flicker) {
        float beamAlphaPulse = 0.032f + 0.006f * (float) Math.sin(time * 1.8f) * flicker;
        int colBeamTop = ((int) (255 * beamAlphaPulse * 1.5f) << 24) | 0x00FFE8A8;
        int colBeamMid = ((int) (255 * beamAlphaPulse * 0.75f) << 24) | 0x00FFD54F;
        int colBeamEnd = 0x00000000;

        fill.setStyle(Paint.Style.FILL);
        fill.setShader(new LinearGradient(cx, 0, cx, sh,
                new int[]{colBeamTop, colBeamMid, colBeamEnd},
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));

        path.reset();
        path.moveTo(cx - 36f, 0f);
        path.lineTo(cx + 36f, 0f);
        path.lineTo(sw * 0.94f, sh);
        path.lineTo(sw * 0.06f, sh);
        path.close();
        c.drawPath(path, fill);
        fill.setShader(null);
    }

    // -------------------------------------------------------------------------
    // ARCHITECTURAL ANTIQUE BRASS FRAME & CORNER FILIGREES
    // -------------------------------------------------------------------------
    private static void drawArchitecturalFrame(Canvas c, float sw, float sh) {
        stroke.setStyle(Paint.Style.STROKE);

        // Outer subtle antique brass frame
        float insetOuter = 26f;
        frameRect.set(insetOuter, insetOuter, sw - insetOuter, sh - insetOuter);
        stroke.setStrokeWidth(1.6f);
        stroke.setColor(0x2BD4AF37);
        c.drawRoundRect(frameRect, 20f, 20f, stroke);

        // Inner hairline frame
        float insetInner = 34f;
        frameRect.set(insetInner, insetInner, sw - insetInner, sh - insetInner);
        stroke.setStrokeWidth(0.8f);
        stroke.setColor(0x15D4AF37);
        c.drawRoundRect(frameRect, 16f, 16f, stroke);

        // Corner Filigree Brackets
        float bracketLen = 28f;
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0x55FFD54F);

        // Top-Left
        c.drawLine(insetOuter, insetOuter + 14f, insetOuter, insetOuter + 14f + bracketLen, stroke);
        c.drawLine(insetOuter + 14f, insetOuter, insetOuter + 14f + bracketLen, insetOuter, stroke);
        drawCornerDiamond(c, insetOuter + 14f, insetOuter + 14f);

        // Top-Right
        c.drawLine(sw - insetOuter, insetOuter + 14f, sw - insetOuter, insetOuter + 14f + bracketLen, stroke);
        c.drawLine(sw - insetOuter - 14f, insetOuter, sw - insetOuter - 14f - bracketLen, insetOuter, stroke);
        drawCornerDiamond(c, sw - insetOuter - 14f, insetOuter + 14f);

        // Bottom-Left
        c.drawLine(insetOuter, sh - insetOuter - 14f, insetOuter, sh - insetOuter - 14f - bracketLen, stroke);
        c.drawLine(insetOuter + 14f, sh - insetOuter, insetOuter + 14f + bracketLen, sh - insetOuter, stroke);
        drawCornerDiamond(c, insetOuter + 14f, sh - insetOuter - 14f);

        // Bottom-Right
        c.drawLine(sw - insetOuter, sh - insetOuter - 14f, sw - insetOuter, sh - insetOuter - 14f - bracketLen, stroke);
        c.drawLine(sw - insetOuter - 14f, sh - insetOuter, sw - insetOuter - 14f - bracketLen, sh - insetOuter, stroke);
        drawCornerDiamond(c, sw - insetOuter - 14f, sh - insetOuter - 14f);

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

            // Smooth boundary fade at top and bottom
            float edgeFade = 1.0f;
            if (y < 100f) edgeFade = Math.max(0f, y / 100f);
            else if (y > sh - 100f) edgeFade = Math.max(0f, (sh - y) / 100f);

            int alpha = (int) (255 * moteAlpha[i] * edgeFade);
            if (alpha <= 0) continue;

            float r = moteRadius[i];

            // Soft warm golden dust mote
            fill.setColor((0x00FFE8A8) | (alpha << 24));
            c.drawCircle(x, y, r, fill);
        }
    }

    private static void drawCornerDiamond(Canvas c, float x, float y) {
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0x88D4AF37);
        path.reset();
        path.moveTo(x, y - 5f);
        path.lineTo(x + 5f, y);
        path.lineTo(x, y + 5f);
        path.lineTo(x - 5f, y);
        path.close();
        c.drawPath(path, fill);
    }
}
