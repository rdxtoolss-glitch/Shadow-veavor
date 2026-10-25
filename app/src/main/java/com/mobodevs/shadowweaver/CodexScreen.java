package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Shadow Weaver - CodexScreen
 * ===========================
 * Astronomy Journal & Shadow Relics Gallery.
 * Displays all 15 collectible shadow constellations, player alchemist rank,
 * star mastery progress, personal best times, and inspectable relic lore.
 */
public final class CodexScreen {

    public enum Action {
        NONE, BACK
    }

    private static final int COLS = 5;
    private static final int ROWS = 3;
    private static final int TOTAL_ITEMS = 15;

    private static int selectedRelic = 0;

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path path = new Path();

    private static final float[] ITEM_L = new float[TOTAL_ITEMS];
    private static final float[] ITEM_T = new float[TOTAL_ITEMS];
    private static final float[] ITEM_R = new float[TOTAL_ITEMS];
    private static final float[] ITEM_B = new float[TOTAL_ITEMS];

    private static final UiButton btnBack = new UiButton();
    private static boolean layoutReady;

    private static final String[] RELIC_TITLES = {
            "The Apprentice's Domicile",
            "The Arbor of Wisdom",
            "The Astral Star",
            "The Vector Arrow",
            "The Golden Carp",
            "The Celestial Galleon",
            "The Sacred Feline",
            "The Phoenix Falcon",
            "The Psyche Butterfly",
            "The Alchemical Blossom",
            "The Eternal Flame",
            "The Skeleton Key",
            "The Shadow Weaver",
            "The Obsidian Peaks",
            "The Astrolabe Compass"
    };

    private static final String[] RELIC_LORE = {
            "The foundational sanctuary where light, perspective, and shadow manipulation are first mastered.",
            "An ancient cedar tree whose cast shadows whisper the secrets of rotational symmetry and branch angles.",
            "A celestial quintuple constellation guiding apprentices across the cosmic sphere.",
            "The alchemical pointer directing the seeker toward the luminous astral horizon.",
            "An ethereal carp that swims effortlessly through light rays and shadow currents.",
            "A mystical vessel designed to sail upon the boundless sea of midnight twilight.",
            "Guardian of the twilight hearth, shifting fluidly between physical form and silhouetted grace.",
            "A bird of radiant flame whose majestic silhouette stretches across the highest firmament.",
            "A creature of cosmic metamorphosis, unfolding delicate wings of dual-fold symmetry.",
            "A sacred blossom blooming exclusively under the focused incandescent glow of candlelight.",
            "The primordial candle tallow that yields illumination, casting truth upon darkness.",
            "An antique brass artifact forged to unlock the grand vault of the Celestial Observatory.",
            "The seeker of mysteries who binds geometry, distance, and illumination into living form.",
            "Twin granite summits marking the solemn threshold to the outer astral spheres.",
            "The supreme masterwork astrolabe charting navigation across all fifteen celestial realms."
    };

    private static void layout() {
        if (layoutReady) return;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float padX = 50f;
        float top = 310f;
        float gapX = 18f;
        float gapY = 16f;
        float cardW = (ShadowCalculator.SCENE_W - padX * 2 - gapX * (COLS - 1)) / COLS;
        float cardH = 175f;

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

        float btnW = 440f;
        float btnH = 76f;
        float btnY = 1330f;
        btnBack.setBounds(cx - btnW * 0.5f, btnY, cx + btnW * 0.5f, btnY + btnH);
        btnBack.cornerRadius = 18f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        layout();
        btnBack.isPressed = btnBack.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        layout();

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
        layout();

        float sw = ShadowCalculator.SCENE_W;
        float sh = ShadowCalculator.SCENE_H;
        float cx = sw * 0.5f;

        // 1. Celestial Twilight Background Gradient
        fill.setShader(new LinearGradient(0, 0, 0, sh,
                new int[]{0xFF24160E, 0xFF1B2032, 0xFF101320},
                new float[]{0f, 0.40f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, sw, sh, fill);
        fill.setShader(null);

        // 2. Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(46f);
        text.setColor(0xDD000000);
        c.drawText("ASTRONOMY JOURNAL", cx, 97f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("ASTRONOMY JOURNAL", cx, 95f, text);

        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xCC000000);
        c.drawText("TREASURY OF COLLECTED CELESTIAL SHADOW RELICS", cx, 137f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("TREASURY OF COLLECTED CELESTIAL SHADOW RELICS", cx, 135f, text);

        // 3. Player Alchemist Rank Banner
        float bannerL = 50f;
        float bannerR = sw - 50f;
        float bannerT = 165f;
        float bannerB = 285f;

        fill.setColor(0xEE1E2434);
        c.drawRoundRect(bannerL, bannerT, bannerR, bannerB, 20f, 20f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(bannerL, bannerT, bannerR, bannerB, 20f, 20f, stroke);

        // Top specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(bannerL + 20f, bannerT + 1.5f, bannerR - 20f, bannerT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        int totalStars = (progress != null) ? progress.getTotalStars() : 0;
        String rankTitle = (progress != null) ? progress.getPlayerRank() : "Apprentice of Shadows";

        text.setTextSize(16f);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("ASTRONOMER RANK", cx, bannerT + 34f, text);

        text.setTextSize(28f);
        text.setColor(0xFFFFD54F);
        c.drawText(rankTitle, cx, bannerT + 68f, text);

        // Star progress bar in banner
        float barW = 540f;
        float barH = 14f;
        float barL = cx - barW * 0.5f;
        float barT = bannerT + 84f;

        fill.setColor(0x660E131E);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 7f, 7f, fill);

        float fillRatio = totalStars / (float) LevelProgress.TOTAL_MAX_STARS;
        if (fillRatio > 0.01f) {
            fill.setColor(0xFFFFB300);
            c.drawRoundRect(barL, barT, barL + barW * fillRatio, barT + barH, 7f, 7f, fill);
        }

        // 4. 15 Relic Cards Grid
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
            text.setTextSize(16f);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(isSolved ? 0xFFFFD54F : (isUnlocked ? 0xAAFFE0A8 : 0x448090A0));
            c.drawText(String.valueOf(i + 1), cardCx, t + 26f, text);

            if (isSolved) {
                // Solved star icon
                fill.setColor(0xFFFFD54F);
                drawStarPath(c, cardCx, t + 62f, 16f);
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(cardCx, t + 60f, 4f, fill);

                // Title
                text.setTextSize(18f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText(title, cardCx, t + 112f, text);

                // Stars row
                drawStarsMini(c, cardCx, t + 138f, stars);

                // Best time
                long best = (progress != null) ? progress.getBestTime(i) : 0L;
                if (best > 0L) {
                    text.setTextSize(14f);
                    text.setTypeface(Typeface.DEFAULT);
                    text.setColor(0xDDFFE0A8);
                    c.drawText((best / 1000L) + "s", cardCx, t + 162f, text);
                }
            } else if (isUnlocked) {
                // Unsolved but reachable
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.0f);
                stroke.setColor(0x88FFE0A8);
                c.drawCircle(cardCx, t + 62f, 16f, stroke);
                stroke.setStyle(Paint.Style.FILL);

                text.setTextSize(17f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xDDFFFDF8);
                c.drawText(title, cardCx, t + 112f, text);

                text.setTextSize(14f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0x99FFE0A8);
                c.drawText("Open", cardCx, t + 144f, text);
            } else {
                // Locked
                drawPadlock(c, cardCx, t + 62f);

                text.setTextSize(16f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0x668090A0);
                c.drawText("Locked", cardCx, t + 116f, text);
            }
        }

        // 5. Selected Relic Detailed Lore Inspection Plaque (fills vertical canvas)
        float specL = 50f;
        float specR = sw - 50f;
        float specT = 905f;
        float specB = 1290f;

        fill.setColor(0xEE1E2434);
        c.drawRoundRect(specL, specT, specR, specB, 22f, 22f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(specL, specT, specR, specB, 22f, 22f, stroke);

        // Specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(specL + 22f, specT + 1.5f, specR - 22f, specT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        int starsSel = (progress != null) ? progress.getStars(selectedRelic) : 0;
        boolean isSelSolved = (starsSel > 0);
        boolean isSelUnlocked = (selectedRelic <= highestUnlocked);

        String selName = RELIC_TITLES[Math.max(0, Math.min(selectedRelic, TOTAL_ITEMS - 1))];
        String selLore = RELIC_LORE[Math.max(0, Math.min(selectedRelic, TOTAL_ITEMS - 1))];

        text.setTextSize(17f);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("ASTRAL RELIC NO. " + (selectedRelic + 1) + " • CHAPTER " + ((selectedRelic / 5) + 1), cx, specT + 42f, text);

        text.setTextSize(28f);
        text.setColor(0xFFFFFDF8);
        c.drawText(selName, cx, specT + 84f, text);

        // Status badge
        text.setTextSize(18f);
        text.setTypeface(Typeface.DEFAULT);
        if (isSelSolved) {
            long best = (progress != null) ? progress.getBestTime(selectedRelic) : 0L;
            text.setColor(0xFFFFD54F);
            String starsStr = (starsSel == 3 ? "⭐⭐⭐ 3 STARS - MASTER WEAVER" : (starsSel == 2 ? "⭐⭐ 2 STARS - ADEPT" : "⭐ 1 STAR - SOLVED"));
            c.drawText(starsStr + " (Best: " + (best / 1000L) + "s)", cx, specT + 124f, text);
        } else if (isSelUnlocked) {
            text.setColor(0xFF80D8FF);
            c.drawText("★ TRIAL UNLOCKED • READY TO WEAVE ★", cx, specT + 124f, text);
        } else {
            text.setColor(0xFF90A0B8);
            c.drawText("🔒 LOCKED • COMPLETE PREVIOUS CHAPTER TRIALS", cx, specT + 124f, text);
        }

        // Decorative separating line
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(0x44D4AF37);
        c.drawLine(cx - 220f, specT + 150f, cx + 220f, specT + 150f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Ancient Lore Excerpt
        text.setTextSize(21f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xFFE2D4BA);

        // Multi-line wrap lore display
        String[] words = selLore.split(" ");
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        int wordIdx = 0;
        while (wordIdx < words.length && line1.length() + words[wordIdx].length() < 42) {
            line1.append(words[wordIdx]).append(" ");
            wordIdx++;
        }
        while (wordIdx < words.length) {
            line2.append(words[wordIdx]).append(" ");
            wordIdx++;
        }

        c.drawText("\"" + line1.toString().trim(), cx, specT + 205f, text);
        if (line2.length() > 0) {
            c.drawText(line2.toString().trim() + "\"", cx, specT + 245f, text);
        } else {
            c.drawText("\"", cx, specT + 245f, text);
        }

        // Par benchmark stats
        long parTime = LevelProgress.getParTimeMillis(selectedRelic);
        int parMoves = LevelProgress.getParMoves(selectedRelic);
        text.setTextSize(17f);
        text.setColor(0xCCFFE0A8);
        c.drawText("Par Benchmark: Under " + (parTime / 1000L) + " seconds  •  Optimal moves: " + parMoves, cx, specT + 310f, text);

        // 6. Back Button
        btnBack.label = "MAIN MENU";
        btnBack.draw(c, fill, stroke, text, 1.0f);

        // 7. Grounding footer
        text.setTextSize(16f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x77D4AF37);
        c.drawText("Tap any relic card above to inspect its ancient lore", cx, sh - 45f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawStarsMini(Canvas c, float cx, float cy, int stars) {
        float spacing = 26f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean filled = (i < stars);
            fill.setColor(filled ? 0xFFFFD54F : 0x33445566);
            drawStarPath(c, sx, cy, 9f);
        }
    }

    private static void drawStarPath(Canvas c, float cx, float cy, float r) {
        path.reset();
        for (int i = 0; i < 10; i++) {
            float rad = (float) Math.toRadians(i * 36 - 90);
            float currR = (i % 2 == 0) ? r : (r * 0.45f);
            float px = cx + (float) Math.cos(rad) * currR;
            float py = cy + (float) Math.sin(rad) * currR;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, fill);
    }

    private static void drawPadlock(Canvas c, float cx, float cy) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0x668090A0);
        c.drawArc(cx - 10f, cy - 18f, cx + 10f, cy - 2f, 180, 180, false, stroke);

        fill.setColor(0x668090A0);
        c.drawRoundRect(cx - 13f, cy - 4f, cx + 13f, cy + 14f, 4f, 4f, fill);
        stroke.setStyle(Paint.Style.FILL);
    }
}
