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
        NONE, BACK, INSPECT
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
            "Timber Cabin",
            "Old Pine",
            "Compass Star",
            "Iron Arrow",
            "Golden Carp",
            "Galleon",
            "Shadow Cat",
            "Phoenix",
            "Butterfly",
            "Lotus Bloom",
            "Studio Candle",
            "Brass Key",
            "Shadow Weaver",
            "Twin Peaks",
            "Astrolabe"
    };

    private static final String[] RELIC_LORE = {
            "A timber shelter crafted with notched pine beams, standing firmly against the mountain wind.",
            "An ancient evergreen that casts long geometric shadow needles across morning snow.",
            "A four-point navigational compass forged from maritime bronze.",
            "A balanced iron arrow showing the exact trajectory of projected light.",
            "A stream fish carved from willow wood, swimming through quiet mountain waters.",
            "A three-masted wooden vessel built for crossing the open ocean under starlight.",
            "A silent domestic companion that moves soundlessly through cast shadows.",
            "A mythic bird with outspread wings, carved to catch the warm glow of candlelight.",
            "A delicate winged insect that rests upon the studio sill in early spring.",
            "A water lily whose symmetrical petals open under direct optical illumination.",
            "The primary wax luminary of the studio, casting every silhouette into existence.",
            "An antique skeleton key cut to open the mahogany observation cabinet.",
            "The silhouette of the artisan who weaves light and shade into recognizable form.",
            "Granite mountain summits that cast deep umbra across the valley at sunset.",
            "A precision navigational instrument combining degree rings, sights, and celestial coordinates."
    };

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;

        float cx = ShadowCalculator.SCENE_W * 0.5f;

        // Content sizes
        float headerH = 80f;
        float cardH = 152f;
        float gapY = 12f;
        float gridH = ROWS * cardH + (ROWS - 1) * gapY; // 480f
        float backH = 88f;

        // Dynamic vertical centering algorithm
        float baseH = headerH + 20f + gridH + 20f + 320f + 20f + backH; // ~948f
        float remaining = Math.max(40f, sceneH - baseH);
        float topPad = remaining * 0.35f + 30f;

        cachedHeaderY = topPad + 40f;

        // Grid of 15 Collectible Relic Cards
        float padX = 52f;
        float gapX = 14f;
        float cardW = (ShadowCalculator.SCENE_W - padX * 2 - gapX * (COLS - 1)) / COLS; // 168f
        float top = cachedHeaderY + 68f;

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
        float btnW = 540f;
        float btnY = cachedSpecB + 18f;
        btnBack.setBounds(cx - btnW * 0.5f, btnY, cx + btnW * 0.5f, btnY + backH);
        btnBack.cornerRadius = 22f;

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
                if (selectedRelic != i) {
                    selectedRelic = i;
                    return Action.INSPECT;
                }
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
        text.setTextSize(44f);
        text.setColor(0xDD000000);
        c.drawText("JOURNAL", cx, headerY + 2f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("JOURNAL", cx, headerY, text);

        text.setTextSize(19f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xAA000000);
        c.drawText("FOLIO OF SHADOW RELICS", cx, headerY + 38f + 1.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("FOLIO OF SHADOW RELICS", cx, headerY + 38f, text);

        // Header flourish
        float divY = headerY + 52f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x55D4AF37);
        c.drawLine(cx - 200f, divY, cx + 200f, divY, stroke);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(0xFFFFE082);
        c.drawCircle(cx, divY, 3.5f, fill);

        // 3. Grid of 15 Collectible Relic Cards
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
            String starsStr = (starsSel == 3 ? "★★★ MASTER WEAVER" : (starsSel == 2 ? "★★ ADEPT" : "★ SOLVED"));
            c.drawText(starsStr + " (Best: " + (best / 1000L) + "s)", cx, specT + 98f, text);
        } else if (isSelUnlocked) {
            text.setColor(0xFF80D8FF);
            c.drawText("UNLOCKED • READY TO PLAY", cx, specT + 98f, text);
        } else {
            text.setColor(0xFF90A0B8);
            c.drawText("LOCKED • COMPLETE PREVIOUS CHAPTERS", cx, specT + 98f, text);
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
        c.drawText("Tap any relic above to view its folio notes", cx, sh - 36f, text);
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
