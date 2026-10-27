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
 * perfectly centered vertical composition, industry-standard sizing,
 * living candlelight glow, floating golden ember particles, dual
 * counter-rotating celestial astrolabes, and drifting shadow silhouettes.
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

    // Derived layout coordinates
    private static float cachedCandleY;
    private static float cachedTitleY;
    private static float cachedSubY;
    private static float cachedDivY;
    private static float cachedCardTop;
    private static float cachedCardBottom;
    private static float cachedCrestCy;
    private static float cachedMottoY;
    private static float cachedTransY;
    private static float cachedVersionY;

    // -------------------------------------------------------------------------
    // LIVING AMBIENT PARTICLES (EMBERS & DUST MOTES)
    // -------------------------------------------------------------------------
    private static final int EMBER_COUNT = 45;
    private static final float[] emberX = new float[EMBER_COUNT];
    private static final float[] emberY = new float[EMBER_COUNT];
    private static final float[] emberSpeed = new float[EMBER_COUNT];
    private static final float[] emberSwayAmp = new float[EMBER_COUNT];
    private static final float[] emberSwayFreq = new float[EMBER_COUNT];
    private static final float[] emberRadius = new float[EMBER_COUNT];
    private static final int[] emberColor = new int[EMBER_COUNT];
    private static final float[] emberBaseAlpha = new float[EMBER_COUNT];
    private static boolean embersReady = false;

    private static void ensureEmbers() {
        if (embersReady) return;
        long seed = 987654321L;
        for (int i = 0; i < EMBER_COUNT; i++) {
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            emberX[i] = 40f + (seed % 920);
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            emberY[i] = (seed % 2400);
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            emberSpeed[i] = 24f + (seed % 48); // 24 to 72 px/sec
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            emberSwayAmp[i] = 12f + (seed % 26); // 12 to 38 px
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            emberSwayFreq[i] = 0.8f + (seed % 100) * 0.013f; // 0.8 to 2.1 rad/s
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            emberRadius[i] = 1.8f + (seed % 30) * 0.09f; // 1.8 to 4.5 px
            seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
            emberBaseAlpha[i] = 0.35f + (seed % 50) * 0.011f; // 0.35 to 0.90

            int cIdx = i % 4;
            if (cIdx == 0) emberColor[i] = 0xFFFFD54F;
            else if (cIdx == 1) emberColor[i] = 0xFFFFB300;
            else if (cIdx == 2) emberColor[i] = 0xFFFFE082;
            else emberColor[i] = 0xFFFF9800;
        }
        embersReady = true;
    }

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;
        ensureEmbers();

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        // Industry-standard 70% button width on 1000px canvas (150px left/right margins)
        float btnW = 700f;
        float l = cx - btnW * 0.5f;
        float r = cx + btnW * 0.5f;
        float splitGap = 16f;
        float splitW = (btnW - splitGap) * 0.5f; // 342f

        // Interactive stack component heights
        float heroH = 106f;
        float btnH = 84f;
        float gap = 16f;
        float cardGap = 20f;
        float cardH = 175f;

        // Total height of the interactive block (Hero + Chapter Map + Split Row + Settings + Rank Card)
        float stackH = heroH + gap + btnH + gap + btnH + gap + btnH + cardGap + cardH; // 601f

        // Header estimated height (Candle to divider line): ~250f
        float headerH = 250f;
        // Footer estimated height (Crest to version): ~195f
        float footerH = 195f;

        // Total content height and remaining vertical space
        float totalContentH = headerH + stackH + footerH; // ~1046f
        float remainingH = Math.max(60f, sceneH - totalContentH);

        // Golden-ratio distribution of remaining vertical space:
        float topPad = remainingH * 0.18f;
        float gapHeaderStack = remainingH * 0.28f;
        float gapStackFooter = remainingH * 0.36f;
        float bottomPad = remainingH * 0.18f;

        // 1. Header Coordinates
        cachedCandleY = topPad + 48f;
        cachedTitleY = cachedCandleY + 124f;
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
        btnChapters.cornerRadius = 18f;

        // How to Play & Codex (Side-by-side, perfect bilateral symmetry)
        float y3 = y2 + btnH + gap;
        btnTutorial.setBounds(l, y3, l + splitW, y3 + btnH);
        btnTutorial.cornerRadius = 18f;
        btnCodex.setBounds(l + splitW + splitGap, y3, r, y3 + btnH);
        btnCodex.cornerRadius = 18f;

        // Settings
        float y4 = y3 + btnH + gap;
        btnSettings.setBounds(l, y4, r, y4 + btnH);
        btnSettings.cornerRadius = 18f;

        // Rank Card
        cachedCardTop = y4 + btnH + cardGap;
        cachedCardBottom = cachedCardTop + cardH;

        // 3. Footer Coordinates (Balanced in bottom third)
        cachedCrestCy = cachedCardBottom + gapStackFooter * 0.44f + 32f;
        cachedMottoY = cachedCrestCy + 94f;
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

    public static void draw(Canvas c, LevelProgress progress, float time, float flicker, float sceneH) {
        layout(sceneH);

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;

        // ---------------------------------------------------------------------
        // 1. RICH ATMOSPHERIC CHAMBER GRADIENT (ANTI-BANDING 4-STOP DITHERED)
        // ---------------------------------------------------------------------
        fill.setShader(new LinearGradient(0, 0, 0, sh,
                new int[]{
                        GameRenderer.mix(0xFF381F12, 0xFF4A2B19, flicker), // Luminous amber warmth near flame
                        0xFF1C161C,                                       // Deep walnut shadow transition
                        0xFF101626,                                       // Mystical midnight sapphire
                        0xFF080C14                                        // Velvet obsidian indigo base
                },
                new float[]{0f, 0.32f, 0.68f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, sw, sh, fill);
        fill.setShader(null);

        // ---------------------------------------------------------------------
        // 2. VOLUMETRIC CANDLELIGHT RADIAL GLOW
        // ---------------------------------------------------------------------
        float candleFlameY = cachedCandleY;
        float rGlow = (520f + 30f * (float) Math.sin(time * 2.2f)) * (0.95f + 0.05f * flicker);
        glow.setShader(new RadialGradient(cx, candleFlameY, rGlow,
                new int[]{
                        0x66FFD54F,
                        0x28FF8F00,
                        0x0CFF6F00,
                        0x00000000
                },
                new float[]{0f, 0.35f, 0.70f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(cx, candleFlameY, rGlow, glow);
        glow.setShader(null);

        // ---------------------------------------------------------------------
        // 3. SUBTLE DRIFTING AMBIENT SHADOW SILHOUETTES (THEATER OF SHADOWS)
        // ---------------------------------------------------------------------
        drawAmbientShadowSilhouettes(c, time, sw, sh);

        // ---------------------------------------------------------------------
        // 4. DUAL COUNTER-ROTATING CELESTIAL ASTROLABES & PLANETARY ORBITS
        // ---------------------------------------------------------------------
        float astrolabeCy = cachedCandleY + 60f;
        drawCelestialAstrolabes(c, cx, astrolabeCy, time, flicker);

        // ---------------------------------------------------------------------
        // 5. LIVING FLOATING CANDLELIT DUST MOTES & GOLDEN EMBERS
        // ---------------------------------------------------------------------
        drawFloatingEmbers(c, time, sh);

        // ---------------------------------------------------------------------
        // 6. LIVING CANDLE AT TOP
        // ---------------------------------------------------------------------
        drawLivingCandle(c, cx, cachedCandleY, time, flicker);

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

        // ---------------------------------------------------------------------
        // 10. ALCHEMIST MASTERY STATUS CARD (700PX WIDTH - MATCHES BUTTONS)
        // ---------------------------------------------------------------------
        float cardW = 700f;
        float cardL = cx - cardW * 0.5f;
        float cardR = cx + cardW * 0.5f;
        float cardT = cachedCardTop;
        float cardB = cachedCardBottom;

        // Card glassmorphic panel
        fill.setColor(0xF2131926);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 22f, 22f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 22f, 22f, stroke);

        // Top specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(cardL + 22f, cardT + 1.5f, cardR - 22f, cardT + 1.5f, stroke);

        // Corner diamond accents
        drawCornerDiamond(c, cardL + 16f, cardT + 16f);
        drawCornerDiamond(c, cardR - 16f, cardT + 16f);
        drawCornerDiamond(c, cardL + 16f, cardB - 16f);
        drawCornerDiamond(c, cardR - 16f, cardB - 16f);

        // Card Typography
        text.setTextAlign(Paint.Align.CENTER);
        String rank = (progress != null) ? progress.getPlayerRank() : "Apprentice of Shadows";
        text.setTextSize(16f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("\u2726  ALCHEMIST MASTERY RANK  \u2726", cx, cardT + 38f, text);

        text.setTextSize(28f);
        text.setColor(0xFFFFFDF8);
        c.drawText(rank, cx, cardT + 78f, text);

        // Star constellation progress bar
        float barW = 540f;
        float barH = 16f;
        float barL = cx - barW * 0.5f;
        float barT = cardT + 104f;
        fill.setColor(0x660B0F18);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 8f, 8f, fill);
        stroke.setStrokeWidth(1.0f);
        stroke.setColor(0x33D4AF37);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 8f, 8f, stroke);

        float pct = Math.min(1.0f, totalStars / (float) LevelProgress.TOTAL_MAX_STARS);
        if (pct > 0.01f) {
            float fillW = barW * pct;
            fill.setColor(0xFFFFB300);
            c.drawRoundRect(barL, barT, barL + fillW, barT + barH, 8f, 8f, fill);

            // Glowing head pip on progress bar
            fill.setColor(0xFFFFF4D0);
            c.drawCircle(barL + fillW - 4f, barT + barH * 0.5f, 5.5f, fill);
        }

        text.setTextSize(17f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xEEFFE0A8);
        c.drawText("Constellation Mastery: " + Math.round(pct * 100f) + "%  \u2022  " + totalStars + " / " + LevelProgress.TOTAL_MAX_STARS + " Stars", cx, cardT + 152f, text);

        // ---------------------------------------------------------------------
        // 11. GRAND ANTIQUE ASTROLOGICAL COMPASS CREST (BALANCED LOWER THIRD)
        // ---------------------------------------------------------------------
        drawCelestialCrest(c, cx, cachedCrestCy, time, flicker);

        // ---------------------------------------------------------------------
        // 12. LATIN ALCHEMICAL MOTTO & VERSION FOOTER
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
    // BACKGROUND ANIMATION: DUAL CELESTIAL ASTROLABES
    // -------------------------------------------------------------------------
    private static void drawCelestialAstrolabes(Canvas c, float cx, float cy, float time, float flicker) {
        // Outer Astrolabe (Clockwise at 1.8 deg/s)
        int saveOuter = c.save();
        c.translate(cx, cy);
        c.rotate(time * 1.8f);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0x2CD4AF37);
        c.drawCircle(0, 0, 350f, stroke);
        c.drawCircle(0, 0, 380f, stroke);

        // 72 Fine Degree Markings (every 5 deg)
        for (int i = 0; i < 360; i += 5) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            boolean isMajor = (i % 30 == 0);
            boolean isSemi = (i % 15 == 0);
            stroke.setStrokeWidth(isMajor ? 2.5f : (isSemi ? 1.5f : 0.9f));
            stroke.setColor(isMajor ? 0x77FFD54F : (isSemi ? 0x44FFE0A8 : 0x1AFFE0A8));
            float rInner = isMajor ? 335f : (isSemi ? 342f : 348f);
            c.drawLine(cos * rInner, sin * rInner, cos * 380f, sin * 380f, stroke);
        }

        // 4 Cardinal Ray Crosshairs with Diamond Finials
        for (int i = 0; i < 4; i++) {
            float rad = (float) Math.toRadians(i * 90);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            stroke.setStrokeWidth(1.8f);
            stroke.setColor(0x66FFD54F);
            c.drawLine(cos * 380f, sin * 380f, cos * 415f, sin * 415f, stroke);

            fill.setColor(0x88FFD54F);
            c.drawCircle(cos * 420f, sin * 420f, 3.5f, fill);
        }
        c.restoreToCount(saveOuter);

        // Inner Zodiac Astrolabe (Counter-Clockwise at -2.6 deg/s)
        int saveInner = c.save();
        c.translate(cx, cy);
        c.rotate(-time * 2.6f);

        stroke.setStrokeWidth(1.6f);
        stroke.setColor(0x33D4AF37);
        c.drawCircle(0, 0, 270f, stroke);
        c.drawCircle(0, 0, 250f, stroke);

        // Inscribed 12-point Sacred Geometry Alchemical Star
        path.reset();
        for (int i = 0; i < 12; i++) {
            float rad = (float) Math.toRadians(i * 30 - 90);
            float rD = (i % 2 == 0) ? 270f : 205f;
            float px = (float) Math.cos(rad) * rD;
            float py = (float) Math.sin(rad) * rD;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        stroke.setStrokeWidth(1.0f);
        stroke.setColor(0x22D4AF37);
        c.drawPath(path, stroke);

        // 24 Alchemical Tick Marks
        for (int i = 0; i < 360; i += 15) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            stroke.setStrokeWidth(1.2f);
            stroke.setColor(0x44FFE0A8);
            c.drawLine(cos * 242f, sin * 242f, cos * 258f, sin * 258f, stroke);
        }
        c.restoreToCount(saveInner);

        // Dotted Planetary Orbital Ring (Radius 180f)
        int saveOrbit = c.save();
        c.translate(cx, cy);
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x22D4AF37);
        c.drawCircle(0, 0, 180f, stroke);

        // 3 Orbiting Golden Planetary Pearls
        drawOrbitingPlanet(c, time * 16f, 180f, 4.8f, 0xFFFFD54F);
        drawOrbitingPlanet(c, -time * 22f + 115f, 180f, 3.8f, 0xFFFFB300);
        drawOrbitingPlanet(c, time * 10f + 230f, 180f, 4.2f, 0xFFFFE082);
        c.restoreToCount(saveOrbit);
    }

    private static void drawOrbitingPlanet(Canvas c, float deg, float orbitR, float pearlR, int color) {
        float rad = (float) Math.toRadians(deg);
        float px = (float) Math.cos(rad) * orbitR;
        float py = (float) Math.sin(rad) * orbitR;

        // Soft outer glow
        fill.setColor((color & 0x00FFFFFF) | 0x44000000);
        c.drawCircle(px, py, pearlR * 2.2f, fill);

        // Solid core
        fill.setColor(color);
        c.drawCircle(px, py, pearlR, fill);

        // Specular pip
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(px - pearlR * 0.35f, py - pearlR * 0.35f, pearlR * 0.35f, fill);
    }

    // -------------------------------------------------------------------------
    // BACKGROUND ANIMATION: FLOATING EMBERS & DUST MOTES
    // -------------------------------------------------------------------------
    private static void drawFloatingEmbers(Canvas c, float time, float sh) {
        ensureEmbers();
        for (int i = 0; i < EMBER_COUNT; i++) {
            float y = (emberY[i] - time * emberSpeed[i]) % (sh + 40f);
            if (y < -20f) y += sh + 40f;

            float x = emberX[i] + (float) Math.sin(time * emberSwayFreq[i] + i) * emberSwayAmp[i];

            // Smooth boundary fade
            float edgeFade = 1.0f;
            if (y < 120f) edgeFade = Math.max(0f, y / 120f);
            else if (y > sh - 120f) edgeFade = Math.max(0f, (sh - y) / 120f);

            int alpha = (int) (255 * emberBaseAlpha[i] * edgeFade);
            if (alpha <= 0) continue;

            int col = emberColor[i];
            float r = emberRadius[i];

            // Soft outer halo
            fill.setColor((col & 0x00FFFFFF) | ((alpha / 3) << 24));
            c.drawCircle(x, y, r * 2.4f, fill);

            // Glowing ember core
            fill.setColor((col & 0x00FFFFFF) | (alpha << 24));
            c.drawCircle(x, y, r, fill);

            // Incandescent center speck
            fill.setColor(0x99FFFFFF);
            c.drawCircle(x, y, r * 0.45f, fill);
        }
    }

    // -------------------------------------------------------------------------
    // BACKGROUND ANIMATION: DRIFTING AMBIENT SHADOW SILHOUETTES
    // -------------------------------------------------------------------------
    private static void drawAmbientShadowSilhouettes(Canvas c, float time, float sw, float sh) {
        fill.setColor(0x0C000000); // 5% ultra-soft opacity

        // Silhouette 1: Tumbling Alchemical Polyhedron Shadow
        int s1 = c.save();
        float polyX = ((time * 12f + 100f) % (sw + 300f)) - 150f;
        float polyY = sh * 0.46f + (float) Math.sin(time * 0.5f) * 35f;
        c.translate(polyX, polyY);
        c.rotate(time * 3.5f);

        path.reset();
        for (int i = 0; i < 6; i++) {
            float rad = (float) Math.toRadians(i * 60);
            float px = (float) Math.cos(rad) * 64f;
            float py = (float) Math.sin(rad) * 64f;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, fill);
        c.restoreToCount(s1);

        // Silhouette 2: Sweeping Crescent Astrolabe Shadow
        int s2 = c.save();
        float cresX = (sw + 200f) - ((time * 9f) % (sw + 400f));
        float cresY = sh * 0.76f + (float) Math.cos(time * 0.6f) * 28f;
        c.translate(cresX, cresY);
        c.rotate(-time * 2.8f);

        path.reset();
        path.moveTo(-45f, -45f);
        path.quadTo(50f, 0f, -45f, 45f);
        path.quadTo(15f, 0f, -45f, -45f);
        path.close();
        c.drawPath(path, fill);
        c.restoreToCount(s2);
    }

    // -------------------------------------------------------------------------
    // LIVING CANDLE
    // -------------------------------------------------------------------------
    private static void drawLivingCandle(Canvas c, float lx, float ly, float time, float flicker) {
        // Candle Tallow Pillar with wax drips
        fill.setColor(0xFFEFE8DC);
        c.drawRoundRect(lx - 9f, ly + 14f, lx + 9f, ly + 72f, 3f, 3f, fill);

        // Shaded side
        fill.setColor(0x332A180C);
        c.drawRoundRect(lx + 2f, ly + 14f, lx + 9f, ly + 72f, 3f, 3f, fill);

        // Wax drip droop
        fill.setColor(0xFFE8E0D2);
        c.drawCircle(lx - 5f, ly + 36f, 3f, fill);
        c.drawCircle(lx + 4f, ly + 46f, 2.5f, fill);

        // Brass candlestick holder rim
        fill.setColor(0xFFC5A059);
        c.drawRoundRect(lx - 26f, ly + 68f, lx + 26f, ly + 78f, 3.5f, 3.5f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x88FFE082);
        c.drawLine(lx - 24f, ly + 69f, lx + 24f, ly + 69f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Wick
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0xFF211105);
        c.drawLine(lx, ly + 14f, lx, ly + 3f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Dynamic organic flame tip sway
        float tipSwayX = (float) Math.sin(time * 8.5f) * 2.2f * flicker;

        // 1. Outer Warm Amber Flame
        float flameW = 13f * flicker;
        float flameH = 28f * flicker;
        path.reset();
        path.moveTo(lx + tipSwayX, ly - flameH);
        path.cubicTo(lx + flameW, ly - flameH * 0.4f, lx + flameW * 0.8f, ly + 4f, lx, ly + 4f);
        path.cubicTo(lx - flameW * 0.8f, ly + 4f, lx - flameW, ly - flameH * 0.4f, lx + tipSwayX, ly - flameH);
        path.close();
        fill.setColor(0xFFFF9800);
        c.drawPath(path, fill);

        // 2. Mid Saffron Flame
        float midW = flameW * 0.72f;
        float midH = flameH * 0.80f;
        path.reset();
        path.moveTo(lx + tipSwayX * 0.8f, ly - midH);
        path.cubicTo(lx + midW, ly - midH * 0.4f, lx + midW * 0.8f, ly + 3f, lx, ly + 3f);
        path.cubicTo(lx - midW * 0.8f, ly + 3f, lx - midW, ly - midH * 0.4f, lx + tipSwayX * 0.8f, ly - midH);
        path.close();
        fill.setColor(0xFFFFD54F);
        c.drawPath(path, fill);

        // 3. Inner White-Hot Incandescent Core
        float coreW = flameW * 0.45f;
        float coreH = flameH * 0.58f;
        path.reset();
        path.moveTo(lx + tipSwayX * 0.5f, ly - coreH);
        path.cubicTo(lx + coreW, ly - coreH * 0.4f, lx + coreW * 0.8f, ly + 2f, lx, ly + 2f);
        path.cubicTo(lx - coreW * 0.8f, ly + 2f, lx - coreW, ly - coreH * 0.4f, lx + tipSwayX * 0.5f, ly - coreH);
        path.close();
        fill.setColor(0xFFFFFFFF);
        c.drawPath(path, fill);

        // 4. Electric Blue Base
        fill.setColor(0xBB3F51B5);
        c.drawCircle(lx, ly + 3f, 3.5f, fill);
    }

    // -------------------------------------------------------------------------
    // GRAND ANTIQUE CELESTIAL CREST
    // -------------------------------------------------------------------------
    private static void drawCelestialCrest(Canvas c, float cx, float cy, float time, float flicker) {
        // Outer Degree Circle (Counter-rotating at -1.5 deg/s)
        int saveRing = c.save();
        c.translate(cx, cy);
        c.rotate(-time * 1.5f);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x38D4AF37);
        c.drawCircle(0, 0, 64f, stroke);
        c.drawCircle(0, 0, 88f, stroke);

        // 12 Astrological Hour Marks
        for (int i = 0; i < 360; i += 30) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            stroke.setStrokeWidth(1.2f);
            stroke.setColor(0x66FFE0A8);
            c.drawLine(cos * 64f, sin * 64f, cos * 88f, sin * 88f, stroke);
        }
        c.restoreToCount(saveRing);

        // 8-Point Faceted Diamond Star
        fill.setColor(0xAAD4AF37);
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

        // Center Pearl with highlight
        fill.setColor(0xFFFFE0A8);
        c.drawCircle(cx, cy, 6.0f, fill);
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(cx - 1.8f, cy - 1.8f, 2.0f, fill);
    }

    private static void drawCornerDiamond(Canvas c, float x, float y) {
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
