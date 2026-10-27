package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * Shadow Weaver - CodexScreen
 * ============================
 * Astronomy Journal & Treasury of Collected Celestial Relics.
 * Displays all 15 collectible astral shadow artifacts, inspection lore,
 * and completion statistics.
 *
 * Fully responsive: dynamically centered horizontally and vertically on any aspect ratio.
 * Backed by the rich living animated celestial chamber.
 */
public final class CodexScreen {

    public enum Action {
        NONE, BACK
    }

    private static final int TOTAL_ITEMS = 15;
    private static final int COLS = 5;
    private static final int ROWS = 3;

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

    private static final float[] ITEM_L = new float[TOTAL_ITEMS];
    private static final float[] ITEM_T = new float[TOTAL_ITEMS];
    private static final float[] ITEM_R = new float[TOTAL_ITEMS];
    private static final float[] ITEM_B = new float[TOTAL_ITEMS];

    private static float cachedHeaderY;
    private static float cachedBannerT;
    private static float cachedBannerB;
    private static float cachedSpecT;
    private static float cachedSpecB;

    private static final UiButton btnBack = new UiButton();
    private static int selectedRelic = 0;
    private static boolean layoutReady;
    private static float lastSceneH = 0f;

    private static final String[] RELIC_TITLES = {
            "The Apprentice's Domicile",
            "The Arbor of Ancient Wisdom",
            "The Radiant Astral Beacon",
            "The Vector of Illumination",
            "The Sacred Golden Carp",
            "The Celestial Astrolabe Galleon",
            "The Feline Guardian of Shadows",
            "The Soaring Solar Phoenix",
            "The Psyche Chrysalis Butterfly",
            "The Blossom of Philosophers",
            "The Eternal Vesper Candle",
            "The Key of Celestial Gates",
            "The Master Shadow Weaver",
            "The Obsidian Astral Peaks",
            "The Great Nautical Chronometer"
    };

    private static final String[] RELIC_LORE = {
            "The humble timber dwelling where the alchemical study of light and shade first commenced.",
            "Deep in the ancient groves, branches filter celestial rays to cast geometric shadow tapestries upon the moss.",
            "An octagonal star forged from celestial bronze, aligning midnight horizons with cardinal rays.",
            "The true trajectory of illumination, demonstrating that every ray cast into darkness must find its origin in flame.",
            "Swims through currents of pure starlight, guiding lost alchemists across the ethereal celestial sea.",
            "Carries celestial voyagers across the boundary between candlelight and obsidian night.",
            "Worshiped in ancient temples for its keen sight through darkest umbra and deepest midnight.",
            "Born from eternal solar embers, ascending with outstretched wings of pure golden silhouette.",
            "Emblem of transformative illumination, folding delicate wings of umbra into radiant daylight.",
            "Unfolds concentric petals of golden brass only when touched by the exact focal convergence of light.",
            "The prime luminary of the alchemist's sanctum, whose living flame casts every trial into existence.",
            "Unlocks the hidden vaults beneath the Observatory, where the primordial tablets of optics remain preserved.",
            "The sacred silhouette of the practitioner who has mastered the delicate art of weaving light into form.",
            "High pinnacles of obsidian where the twin moons cast double penumbras across the celestial horizon.",
            "The ultimate navigational relic of the ancients, combining azimuth rings, sundial rays, and magnetic star finials."
    };

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;

        float cx = ShadowCalculator.SCENE_W * 0.5f;

        // Content sizes
        float headerH = 100f;
        float bannerH = 100f;
        float cardH = 152f;
        float gapY = 12f;
        float gridH = ROWS * cardH + (ROWS - 1) * gapY; // 480f

        float backH = 78f;

        // Dynamic vertical centering algorithm
        float baseH = headerH + 16f + bannerH + 18f + gridH + 16f + 320f + 18f + backH; // ~1142f
        float remaining = Math.max(40f, sceneH - baseH);
        float topPad = remaining * 0.32f + 25f;

        cachedHeaderY = topPad + 42f;

        // Player Alchemist Rank Banner
        cachedBannerT = cachedHeaderY + 58f;
        cachedBannerB = cachedBannerT + bannerH;

        // Grid of 15 Collectible Relic Cards
        float padX = 52f;
        float gapX = 14f;
        float cardW = (ShadowCalculator.SCENE_W - padX * 2 - gapX * (COLS - 1)) / COLS; // 168f
        float top = cachedBannerB + 16f;

        for (int i = 0; i < TOTAL_ITEMS; i++) {
            int col = i % COLS;
            int row = i / COLS;
            float l = padX + col * (cardW + gapX);
            float t = top + row * (cardH + gapY);
            ITEM_L[i] = l;
            ITEM_T[i] = t;
            ITEM_R[i] = l + cardW;
            ITEM_B[i] = t + cardH;
        }

        float gridB = top + gridH;

        // Selected Relic Detailed Lore Inspection Plaque
        cachedSpecT = gridB + 16f;
        float availH = sceneH - cachedSpecT - backH - 36f;
        float specH = Math.max(300f, Math.min(460f, availH));
        cachedSpecB = cachedSpecT + specH;

        // Back Button
        float btnW = 460f;
        float btnY = cachedSpecB + 18f;
        btnBack.setBounds(cx - btnW * 0.5f, btnY, cx + btnW * 0.5f, btnY + backH);
        btnBack.cornerRadius = 20f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        return onTouchDown(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchDown(float sx, float sy, float sceneH) {
        layout(sceneH);
        btnBack.isPressed = btnBack.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        return onTouchUp(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, float sceneH) {
        layout(sceneH);

        if (btnBack.isPressed && btnBack.hitTest(sx, sy)) {
            btnBack.isPressed = false;
            return Action.BACK;
        }
        btnBack.isPressed = false;

        // Check relic card selection
        for (int i = 0; i < TOTAL_ITEMS; i++) {
            if (sx >= ITEM_L[i] && sx <= ITEM_R[i] && sy >= ITEM_T[i] && sy <= ITEM_B[i]) {
                selectedRelic = i;
                return Action.NONE;
            }
        }

        return Action.NONE;
    }

    public static void cancelPress() {
        btnBack.isPressed = false;
    }

    public static void draw(Canvas c, LevelProgress progress, float time) {
        draw(c, progress, time, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, LevelProgress progress, float time, float sceneH) {
        layout(sceneH);

        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;
        float cx = sw * 0.5f;

        // 1. Rich living animated celestial chamber backdrop
        TitleScreen.drawCelestialBackdrop(c, time, 1.0f, sceneH);

        // 2. Header
        float headerY = cachedHeaderY;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(46f);
        text.setColor(0xDD000000);
        c.drawText("ASTRONOMY JOURNAL", cx, headerY + 2f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("ASTRONOMY JOURNAL", cx, headerY, text);

        text.setTextSize(20f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xCC000000);
        c.drawText("TREASURY OF COLLECTED CELESTIAL SHADOW RELICS", cx, headerY + 40f + 2f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("TREASURY OF COLLECTED CELESTIAL SHADOW RELICS", cx, headerY + 40f, text);

        // Header flourish
        float divY = headerY + 54f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(0x66D4AF37);
        c.drawLine(cx - 240f, divY, cx - 18f, divY, stroke);
        c.drawLine(cx + 18f, divY, cx + 240f, divY, stroke);
        stroke.setStyle(Paint.Style.FILL);

        fill.setColor(0xFFFFD54F);
        path.reset();
        path.moveTo(cx, divY - 5f);
        path.lineTo(cx + 5f, divY);
        path.lineTo(cx, divY + 5f);
        path.lineTo(cx - 5f, divY);
        path.close();
        c.drawPath(path, fill);

        // 3. Player Alchemist Rank Banner
        float bannerL = 52f;
        float bannerR = sw - 52f;
        float bannerT = cachedBannerT;
        float bannerB = cachedBannerB;

        fill.setColor(0xEE141A28);
        c.drawRoundRect(bannerL, bannerT, bannerR, bannerB, 20f, 20f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(bannerL, bannerT, bannerR, bannerB, 20f, 20f, stroke);

        // Top specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(bannerL + 20f, bannerT + 2f, bannerR - 20f, bannerT + 2f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        int totalStars = (progress != null) ? progress.getTotalStars() : 0;
        String rankTitle = (progress != null) ? progress.getPlayerRank() : "Apprentice of Shadows";

        text.setTextSize(15f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("ASTRONOMER RANK", cx, bannerT + 28f, text);

        text.setTextSize(26f);
        text.setColor(0xFFFFD54F);
        c.drawText(rankTitle + "  \u2022  " + totalStars + " / " + LevelProgress.TOTAL_MAX_STARS + " \u2605", cx, bannerT + 58f, text);

        // Star progress bar in banner
        float barW = 540f;
        float barH = 12f;
        float barL = cx - barW * 0.5f;
        float barT = bannerT + 72f;

        fill.setColor(0x66080C14);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 6f, 6f, fill);

        float fillRatio = totalStars / (float) LevelProgress.TOTAL_MAX_STARS;
        if (fillRatio > 0.01f) {
            fill.setColor(0xFFFFB300);
            c.drawRoundRect(barL, barT, barL + barW * fillRatio, barT + barH, 6f, 6f, fill);
        }

        // 4. Grid of 15 Collectible Relic Cards
        int highestUnlocked = (progress != null) ? progress.getHighestUnlocked() : 0;

        for (int i = 0; i < TOTAL_ITEMS; i++) {
            float l = ITEM_L[i];
            float t = ITEM_T[i];
            float r = ITEM_R[i];
            float b = ITEM_B[i];
            float cardCx = (l + r) * 0.5f;

            int stars = (progress != null) ? progress.getStars(i) : 0;
            boolean isSolved = (stars > 0);
            boolean isUnlocked = (i <= highestUnlocked);
            boolean isSel = (i == selectedRelic);

            // Card background
            if (isSel) {
                fill.setColor(0xF02A2438);
            } else if (isSolved) {
                fill.setColor(0xEE1E2434);
            } else if (isUnlocked) {
                fill.setColor(0xDD181E2C);
            } else {
                fill.setColor(0x55121622);
            }
            c.drawRoundRect(l, t, r, b, 16f, 16f, fill);

            // Card border
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isSel ? 3.0f : (isSolved ? 2.2f : 1.4f));
            stroke.setColor(isSel ? 0xFFFFD54F : (isSolved ? 0xFFD4AF37 : (isUnlocked ? 0x88D4AF37 : 0x33607080)));
            c.drawRoundRect(l, t, r, b, 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            LevelData level = LevelLoader.get(i);
            String title = (level != null) ? level.title : ("#" + (i + 1));

            // Level Number badge
            text.setTextSize(15f);
            text.setTypeface(SERIF_BOLD);
            text.setColor(isSolved ? 0xFFFFD54F : (isUnlocked ? 0xAAFFE0A8 : 0x448090A0));
            c.drawText(String.valueOf(i + 1), cardCx, t + 22f, text);

            if (isSolved) {
                // Bespoke Celestial Relic Icon
                GameIcons.drawRelicIcon(c, i, cardCx, t + 54f, 18f, fill, stroke, true);

                // Title
                text.setTextSize(16f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText(title, cardCx, t + 98f, text);

                // Stars row
                drawStarsMini(c, cardCx, t + 120f, stars);

                // Best time
                long best = (progress != null) ? progress.getBestTime(i) : 0L;
                if (best > 0L) {
                    text.setTextSize(13f);
                    text.setTypeface(SERIF_NORMAL);
                    text.setColor(0xDDFFE0A8);
                    c.drawText((best / 1000L) + "s", cardCx, t + 140f, text);
                }
            } else if (isUnlocked) {
                // Unsolved but reachable: outline relic glyph
                GameIcons.drawRelicIcon(c, i, cardCx, t + 54f, 18f, fill, stroke, false);

                text.setTextSize(16f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xDDFFFDF8);
                c.drawText(title, cardCx, t + 98f, text);

                text.setTextSize(13f);
                text.setTypeface(SERIF_NORMAL);
                text.setColor(0x99FFE0A8);
                c.drawText("Open", cardCx, t + 126f, text);
            } else {
                // Locked Padlock
                GameIcons.drawPadlock(c, cardCx, t + 54f, fill, stroke);

                text.setTextSize(15f);
                text.setTypeface(SERIF_NORMAL);
                text.setColor(0x668090A0);
                c.drawText("Locked", cardCx, t + 102f, text);
            }
        }

        // 5. Selected Relic Detailed Lore Inspection Plaque
        float specL = 52f;
        float specR = sw - 52f;
        float specT = cachedSpecT;
        float specB = cachedSpecB;

        fill.setColor(0xEE141A28);
        c.drawRoundRect(specL, specT, specR, specB, 22f, 22f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(specL, specT, specR, specB, 22f, 22f, stroke);

        // Specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(specL + 22f, specT + 2f, specR - 22f, specT + 2f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        int starsSel = (progress != null) ? progress.getStars(selectedRelic) : 0;
        boolean isSelSolved = (starsSel > 0);
        boolean isSelUnlocked = (selectedRelic <= highestUnlocked);

        String selName = RELIC_TITLES[Math.max(0, Math.min(selectedRelic, TOTAL_ITEMS - 1))];
        String selLore = RELIC_LORE[Math.max(0, Math.min(selectedRelic, TOTAL_ITEMS - 1))];

        text.setTextSize(16f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("ASTRAL RELIC NO. " + (selectedRelic + 1) + " \u2022 CHAPTER " + ((selectedRelic / 5) + 1), cx, specT + 34f, text);

        text.setTextSize(26f);
        text.setColor(0xFFFFFDF8);
        c.drawText(selName, cx, specT + 68f, text);

        // Status badge
        text.setTextSize(16f);
        text.setTypeface(SERIF_NORMAL);
        if (isSelSolved) {
            long best = (progress != null) ? progress.getBestTime(selectedRelic) : 0L;
            text.setColor(0xFFFFD54F);
            String starsStr = (starsSel == 3 ? "\u2605\u2605\u2605 3 STARS - MASTER WEAVER" : (starsSel == 2 ? "\u2605\u2605 2 STARS - ADEPT" : "\u2605 1 STAR - SOLVED"));
            c.drawText(starsStr + " (Best: " + (best / 1000L) + "s)", cx, specT + 98f, text);
        } else if (isSelUnlocked) {
            text.setColor(0xFF80D8FF);
            c.drawText("\u2726 TRIAL UNLOCKED \u2022 READY TO WEAVE \u2726", cx, specT + 98f, text);
        } else {
            text.setColor(0xFF90A0B8);
            c.drawText("SEALED VAULT \u2022 COMPLETE PREVIOUS CHAPTER TRIALS", cx, specT + 98f, text);
        }

        // Large Bespoke Relic Emblem Showcase
        GameIcons.drawRelicIcon(c, selectedRelic, cx, specT + 150f, 30f, fill, stroke, isSelSolved);

        // Decorative separating line
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.6f);
        stroke.setColor(0x44D4AF37);
        c.drawLine(cx - 220f, specT + 195f, cx + 220f, specT + 195f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Ancient Lore Excerpt
        text.setTextSize(19f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xFFE2D4BA);

        // Multi-line wrap lore display
        String[] words = selLore.split(" ");
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        int wordIdx = 0;
        while (wordIdx < words.length && line1.length() + words[wordIdx].length() < 46) {
            line1.append(words[wordIdx]).append(" ");
            wordIdx++;
        }
        while (wordIdx < words.length) {
            line2.append(words[wordIdx]).append(" ");
            wordIdx++;
        }

        c.drawText("\"" + line1.toString().trim(), cx, specT + 228f, text);
        if (line2.length() > 0) {
            c.drawText(line2.toString().trim() + "\"", cx, specT + 258f, text);
        } else {
            c.drawText("\"", cx, specT + 258f, text);
        }

        // Par benchmark stats
        long parTime = LevelProgress.getParTimeMillis(selectedRelic);
        int parMoves = LevelProgress.getParMoves(selectedRelic);
        text.setTextSize(15f);
        text.setColor(0xCCFFE0A8);
        c.drawText("Par Benchmark: Under " + (parTime / 1000L) + "s  \u2022  Optimal moves: " + parMoves, cx, specT + 296f, text);

        // 6. Back Button
        btnBack.label = "MAIN MENU";
        btnBack.draw(c, fill, stroke, text, 1.0f, time);

        // 7. Grounding footer
        text.setTextSize(15f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0x77D4AF37);
        c.drawText("Tap any relic card above to inspect ancient celestial lore", cx, sh - 36f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawStarsMini(Canvas c, float cx, float cy, int stars) {
        float spacing = 24f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean filled = (i < stars);
            GameIcons.drawStar(c, sx, cy, 10f, filled);
        }
    }
}
