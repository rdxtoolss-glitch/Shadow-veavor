package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;

/**
 * Shadow Weaver - LevelSelectOverlay
 * ==================================
 * Interactive chapter selection and level grid modal.
 * Supports 3 chapters of 5 levels each, chapter progression tabs,
 * 3-star completion indicators, best completion times, and locked trial padlocks.
 *
 * Fully responsive: expands seamlessly across tall phone aspect ratios with zero black bars.
 */
public final class LevelSelectOverlay {

    public enum Action {
        NONE, PICKED, CLOSE
    }

    private static final int CHAPTERS = 3;
    private static final int LEVELS_PER_CHAPTER = 5;

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path starPath = new Path();

    private static int activeChapter = 0;
    private static int lastPickedLevel = -1;

    // Layout Bounds
    private static final float[] TAB_L = new float[CHAPTERS];
    private static final float[] TAB_T = new float[CHAPTERS];
    private static final float[] TAB_R = new float[CHAPTERS];
    private static final float[] TAB_B = new float[CHAPTERS];

    private static final float[] CARD_L = new float[LEVELS_PER_CHAPTER];
    private static final float[] CARD_T = new float[LEVELS_PER_CHAPTER];
    private static final float[] CARD_R = new float[LEVELS_PER_CHAPTER];
    private static final float[] CARD_B = new float[LEVELS_PER_CHAPTER];

    private static final UiButton btnBack = new UiButton();
    private static boolean layoutReady;
    private static float lastSceneH = 0f;

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float padX = 54f;

        // Chapter Tabs
        float tabTop = 190f;
        float tabH = 74f;
        float tabGap = 16f;
        float tabW = (ShadowCalculator.SCENE_W - padX * 2 - tabGap * (CHAPTERS - 1)) / CHAPTERS;

        for (int i = 0; i < CHAPTERS; i++) {
            float l = padX + i * (tabW + tabGap);
            TAB_L[i] = l;
            TAB_T[i] = tabTop;
            TAB_R[i] = l + tabW;
            TAB_B[i] = tabTop + tabH;
        }

        // 5 Level Cards (arranged as 2 top, 2 middle, 1 centered bottom)
        float cardTop = 300f;
        float cardW = 415f;
        float cardH = 210f;
        float gapX = 30f;
        float gapY = 24f;

        // Row 1 (Levels 1 & 2 in chapter)
        float row1W = cardW * 2 + gapX;
        float row1L = cx - row1W * 0.5f;
        CARD_L[0] = row1L;
        CARD_T[0] = cardTop;
        CARD_R[0] = row1L + cardW;
        CARD_B[0] = cardTop + cardH;

        CARD_L[1] = row1L + cardW + gapX;
        CARD_T[1] = cardTop;
        CARD_R[1] = row1L + cardW * 2 + gapX;
        CARD_B[1] = cardTop + cardH;

        // Row 2 (Levels 3 & 4 in chapter)
        float row2T = cardTop + cardH + gapY;
        CARD_L[2] = row1L;
        CARD_T[2] = row2T;
        CARD_R[2] = row1L + cardW;
        CARD_B[2] = row2T + cardH;

        CARD_L[3] = row1L + cardW + gapX;
        CARD_T[3] = row2T;
        CARD_R[3] = row1L + cardW * 2 + gapX;
        CARD_B[3] = row2T + cardH;

        // Row 3 (Level 5 centered)
        float row3T = row2T + cardH + gapY;
        float card5W = 460f;
        CARD_L[4] = cx - card5W * 0.5f;
        CARD_T[4] = row3T;
        CARD_R[4] = cx + card5W * 0.5f;
        CARD_B[4] = row3T + cardH;

        // Back Button (Anchored to bottom of viewport)
        float btnW = 460f;
        float btnH = 80f;
        float btnY = (sceneH > 1800f) ? (sceneH - 125f) : (sceneH - 95f);
        btnBack.setBounds(cx - btnW * 0.5f, btnY, cx + btnW * 0.5f, btnY + btnH);
        btnBack.cornerRadius = 20f;

        layoutReady = true;
    }

    public static int getPickedLevel() {
        return lastPickedLevel;
    }

    public static int getActiveChapter() {
        return activeChapter;
    }

    public static void setActiveChapterForLevel(int levelIndex) {
        if (levelIndex >= 0 && levelIndex < LevelLoader.count()) {
            activeChapter = levelIndex / LEVELS_PER_CHAPTER;
        }
    }

    public static Action onTouchDown(float sx, float sy, int unlocked) {
        return onTouchDown(sx, sy, unlocked, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchDown(float sx, float sy, int unlocked, float sceneH) {
        layout(sceneH);
        btnBack.isPressed = btnBack.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy, int unlocked) {
        return onTouchUp(sx, sy, unlocked, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, int unlocked, float sceneH) {
        layout(sceneH);
        if (btnBack.isPressed && btnBack.hitTest(sx, sy)) {
            btnBack.isPressed = false;
            return Action.CLOSE;
        }
        btnBack.isPressed = false;

        // Check Chapter Tab selection
        for (int i = 0; i < CHAPTERS; i++) {
            if (sx >= TAB_L[i] && sx <= TAB_R[i] && sy >= TAB_T[i] && sy <= TAB_B[i]) {
                activeChapter = i;
                return Action.NONE;
            }
        }

        // Check Level Card selection
        for (int i = 0; i < LEVELS_PER_CHAPTER; i++) {
            int globalLevel = activeChapter * LEVELS_PER_CHAPTER + i;
            if (globalLevel > unlocked) continue;

            if (sx >= CARD_L[i] && sx <= CARD_R[i] && sy >= CARD_T[i] && sy <= CARD_B[i]) {
                lastPickedLevel = globalLevel;
                return Action.PICKED;
            }
        }

        return Action.NONE;
    }

    public static void cancelPress() {
        btnBack.isPressed = false;
    }

    public static void draw(Canvas c, LevelProgress progress, int currentLevel) {
        draw(c, progress, currentLevel, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, LevelProgress progress, int currentLevel, float sceneH) {
        layout(sceneH);

        int unlocked = (progress != null) ? progress.getHighestUnlocked() : currentLevel;
        float sw = ShadowCalculator.SCENE_W;
        float sh = sceneH;
        float cx = sw * 0.5f;

        // 1. Modal Scrim Backdrop with Celestial Twilight Gradient (Fills 100% of screen)
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
        c.drawText("CHAPTER MAP", cx, 97f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("CHAPTER MAP", cx, 95f, text);

        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xCC000000);

        String[] chapterNames = {
                "CHAPTER I: THE APPRENTICE'S STUDY",
                "CHAPTER II: THE ALCHEMIST'S CHAMBER",
                "CHAPTER III: THE CELESTIAL OBSERVATORY"
        };
        c.drawText(chapterNames[activeChapter], cx, 139f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText(chapterNames[activeChapter], cx, 137f, text);

        // Dividing flourish
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0xFFD4AF37);
        c.drawLine(cx - 180f, 155f, cx + 180f, 155f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 3. Chapter Tabs
        String[] roman = {"CHAPTER I", "CHAPTER II", "CHAPTER III"};
        for (int i = 0; i < CHAPTERS; i++) {
            boolean isSel = (i == activeChapter);
            boolean isUnlocked = (progress == null || progress.isChapterUnlocked(i));

            fill.setColor(isSel ? 0xFFE5A836 : (isUnlocked ? 0xEE1E2434 : 0x55141A28));
            c.drawRoundRect(TAB_L[i], TAB_T[i], TAB_R[i], TAB_B[i], 16f, 16f, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isSel ? 2.5f : 1.8f);
            stroke.setColor(isSel ? 0xFFFFF4D0 : (isUnlocked ? 0xFFD4AF37 : 0x44607080));
            c.drawRoundRect(TAB_L[i], TAB_T[i], TAB_R[i], TAB_B[i], 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            float tabCx = (TAB_L[i] + TAB_R[i]) * 0.5f;
            float tabCy = (TAB_T[i] + TAB_B[i]) * 0.5f;

            text.setTextSize(19f);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(isSel ? 0xFF140802 : (isUnlocked ? 0xFFFFFDF8 : 0x6690A0B8));
            c.drawText(roman[i], tabCx, tabCy - 5f, text);

            int chapterStars = (progress != null) ? progress.getChapterStars(i) : 0;
            text.setTextSize(15f);
            text.setTypeface(Typeface.DEFAULT);
            text.setColor(isSel ? 0xDD2A1404 : (isUnlocked ? 0xFFFFE0A8 : 0x4490A0B8));
            c.drawText("⭐ " + chapterStars + " / 15", tabCx, tabCy + 20f, text);
        }

        // 4. 5 Level Cards for Active Chapter
        for (int i = 0; i < LEVELS_PER_CHAPTER; i++) {
            int globalLevel = activeChapter * LEVELS_PER_CHAPTER + i;
            boolean isUnlocked = (globalLevel <= unlocked);
            boolean isCurrent = (globalLevel == currentLevel);
            int stars = (progress != null) ? progress.getStars(globalLevel) : 0;
            boolean isSolved = (stars > 0);

            float l = CARD_L[i];
            float t = CARD_T[i];
            float r = CARD_R[i];
            float b = CARD_B[i];
            float cardCx = (l + r) * 0.5f;

            // Card background
            if (isCurrent) {
                fill.setColor(0xF02A2438);
            } else if (isSolved) {
                fill.setColor(0xEE1E2434);
            } else if (isUnlocked) {
                fill.setColor(0xDD181E2C);
            } else {
                fill.setColor(0x55121622);
            }
            c.drawRoundRect(l, t, r, b, 20f, 20f, fill);

            // Card border
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isCurrent ? 3.5f : (isSolved ? 2.4f : 1.5f));
            stroke.setColor(isCurrent ? 0xFFFFD54F : (isSolved ? 0xFFD4AF37 : (isUnlocked ? 0x88D4AF37 : 0x33607080)));
            c.drawRoundRect(l, t, r, b, 20f, 20f, stroke);

            // Specular top highlight
            if (isUnlocked) {
                stroke.setStrokeWidth(1.2f);
                stroke.setColor(0x44FFFFFF);
                c.drawLine(l + 20f, t + 1.5f, r - 20f, t + 1.5f, stroke);
            }
            stroke.setStyle(Paint.Style.FILL);

            LevelData data = LevelLoader.get(globalLevel);
            String title = (data != null) ? data.title : ("Trial " + (globalLevel + 1));

            // Level number badge
            text.setTextSize(18f);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(isUnlocked ? 0xFFFFD54F : 0x5590A0B8);
            c.drawText("LEVEL " + (globalLevel + 1), cardCx, t + 36f, text);

            if (isUnlocked) {
                // Title
                text.setTextSize(26f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xCC000000);
                c.drawText(title, cardCx, t + 79f, text);
                text.setColor(0xFFFFFDF8);
                c.drawText(title, cardCx, t + 77f, text);

                // Stars row
                drawStarsRow(c, cardCx, t + 125f, stars);

                // Best time
                long best = (progress != null) ? progress.getBestTime(globalLevel) : 0L;
                long parTime = LevelProgress.getParTimeMillis(globalLevel);
                text.setTextSize(16f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xDDFFE0A8);
                if (best > 0L) {
                    c.drawText("Best: " + (best / 1000L) + "s (Par " + (parTime / 1000L) + "s)", cardCx, t + 176f, text);
                } else {
                    c.drawText(isCurrent ? "★ CURRENT TRIAL ★" : "Ready to Solve", cardCx, t + 176f, text);
                }
            } else {
                // Locked Padlock
                GameIcons.drawPadlock(c, cardCx, t + 95f, fill, stroke);

                text.setTextSize(18f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0x6690A0B8);
                c.drawText("Locked", cardCx, t + 160f, text);
            }
        }

        // 5. Chapter Mastery Summary Card (Proportionally sized and positioned)
        float sumL = cx - 380f;
        float sumR = cx + 380f;
        float sumT = CARD_B[4] + (sceneH > 1800f ? 44f : 30f);
        float sumH = (sceneH > 1800f) ? 320f : 230f;
        float sumB = sumT + sumH;

        fill.setColor(0xEE1E2434);
        c.drawRoundRect(sumL, sumT, sumR, sumB, 22f, 22f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.4f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(sumL, sumT, sumR, sumB, 22f, 22f, stroke);

        // Specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(sumL + 22f, sumT + 1.5f, sumR - 22f, sumT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        int chStars = (progress != null) ? progress.getChapterStars(activeChapter) : 0;
        text.setTextSize(17f);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xCCFFE0A8);
        c.drawText("CHAPTER STARS & MASTERY PROGRESS", cx, sumT + 42f, text);

        text.setTextSize(28f);
        text.setColor(0xFFFFFDF8);
        c.drawText("⭐ " + chStars + " of 15 Constellation Stars Earned", cx, sumT + 86f, text);

        // Progress bar
        float barW = 560f;
        float barH = 18f;
        float barL = cx - barW * 0.5f;
        float barT = sumT + 120f;
        fill.setColor(0x660E131E);
        c.drawRoundRect(barL, barT, barL + barW, barT + barH, 9f, 9f, fill);

        float chPct = chStars / 15f;
        if (chPct > 0.01f) {
            fill.setColor(0xFFFFB300);
            c.drawRoundRect(barL, barT, barL + barW * chPct, barT + barH, 9f, 9f, fill);
        }

        text.setTextSize(17f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xDDFFE0A8);
        String appraisal = (chStars == 15) ? "Complete Astral Perfection Achieved!"
                : (chStars >= 10 ? "Great progress! Complete trials under par for all stars."
                : "Solve silhouettes under par time and move limits to earn 3 stars.");
        c.drawText(appraisal, cx, sumT + 175f, text);

        if (sceneH > 1800f) {
            String[] lore = {
                    "✦ The Apprentice's Study: Learn to bend candlelight and master single-plane shadows.",
                    "✦ The Alchemist's Chamber: Shape compound silhouettes and rotational symmetries.",
                    "✦ The Celestial Observatory: Weave complex constellations and celestial archetypes."
            };
            text.setTextSize(15f);
            text.setColor(0x88FFE0A8);
            c.drawText(lore[activeChapter], cx, sumT + 225f, text);
        }

        // 6. Back Button
        btnBack.label = "MAIN MENU";
        btnBack.draw(c, fill, stroke, text, 1.0f);

        // 7. Grounding footer
        text.setTextSize(16f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x77D4AF37);
        c.drawText("Select any unlocked trial to begin weaving", cx, sh - 46f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawStarsRow(Canvas c, float cx, float cy, int stars) {
        float spacing = 38f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean filled = (i < stars);
            fill.setColor(filled ? 0xFFFFD54F : 0x33445566);
            drawStarPath(c, sx, cy, 14f);

            if (filled) {
                // White specular pearl at star center
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(sx, cy - 2f, 3.5f, fill);
            }
        }
    }

    private static void drawStarPath(Canvas c, float cx, float cy, float r) {
        starPath.reset();
        for (int i = 0; i < 10; i++) {
            float rad = (float) Math.toRadians(i * 36 - 90);
            float currR = (i % 2 == 0) ? r : (r * 0.45f);
            float px = cx + (float) Math.cos(rad) * currR;
            float py = cy + (float) Math.sin(rad) * currR;
            if (i == 0) starPath.moveTo(px, py);
            else starPath.lineTo(px, py);
        }
        starPath.close();
        c.drawPath(starPath, fill);
    }
}
