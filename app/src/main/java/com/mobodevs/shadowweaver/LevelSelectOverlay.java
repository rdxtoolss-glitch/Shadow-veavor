package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * Shadow Weaver - LevelSelectOverlay
 * ==================================
 * Interactive chapter selection and level grid modal.
 * Supports 3 chapters of 5 levels each, chapter progression tabs,
 * 3-star completion indicators, best completion times, and locked trial padlocks.
 *
 * Fully responsive: dynamically centered horizontally and vertically on any aspect ratio.
 * Backed by the rich living animated celestial chamber.
 */
public final class LevelSelectOverlay {

    public enum Action {
        NONE, PICKED, CLOSE, TAB_CHANGED
    }

    private static final int CHAPTERS = 3;
    private static final int LEVELS_PER_CHAPTER = 5;

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path starPath = new Path();
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

    private static float summaryL;
    private static float summaryT;
    private static float summaryR;
    private static float summaryB;

    private static final UiButton btnBack = new UiButton();
    private static float cachedHeaderY;
    private static boolean layoutReady;
    private static float lastSceneH = 0f;

    public static void layout() {
        layout(ShadowCalculator.SCENE_H);
    }

    public static void layout(float sceneH) {
        if (layoutReady && Math.abs(lastSceneH - sceneH) < 1f) return;
        lastSceneH = sceneH;

        float cx = ShadowCalculator.SCENE_W * 0.5f;

        // Content heights
        float headerH = 110f;
        float tabH = 76f;
        float cardH = 205f;
        float gapY = 20f;
        float gridH = 3 * cardH + 2 * gapY; // 655f
        float summaryH = 42f;
        float backH = 78f;

        float totalH = headerH + 18f + tabH + 24f + gridH + 20f + summaryH + 20f + backH; // 1043f
        float remaining = Math.max(50f, sceneH - totalH);
        float topPad = remaining * 0.36f + 25f;

        cachedHeaderY = topPad + 45f;

        // 1. Chapter Tabs (Centered across 896px width)
        float tabTop = cachedHeaderY + 68f;
        float tabW = 288f;
        float tabGap = 16f;
        float totalTabsW = CHAPTERS * tabW + (CHAPTERS - 1) * tabGap; // 896f
        float tabStartX = cx - totalTabsW * 0.5f;

        for (int i = 0; i < CHAPTERS; i++) {
            float l = tabStartX + i * (tabW + tabGap);
            TAB_L[i] = l;
            TAB_T[i] = tabTop;
            TAB_R[i] = l + tabW;
            TAB_B[i] = tabTop + tabH;
        }

        // 2. 5 Level Cards (Arranged as 2 top, 2 middle, 1 centered bottom)
        float cardTop = tabTop + tabH + 24f;
        float cardW = 420f;
        float gapX = 32f;

        // Row 1 (Levels 1 & 2 in chapter)
        float row1W = cardW * 2 + gapX; // 872f
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

        // Row 3 (Level 5 Boss / Capstone Trial centered)
        float row3T = row2T + cardH + gapY;
        float card5W = 540f;
        CARD_L[4] = cx - card5W * 0.5f;
        CARD_T[4] = row3T;
        CARD_R[4] = cx + card5W * 0.5f;
        CARD_B[4] = row3T + cardH;

        // 3. Chapter Summary Bar below Row 3
        summaryT = row3T + cardH + 20f;
        summaryB = summaryT + summaryH;
        summaryL = cx - 448f;
        summaryR = cx + 448f;

        // 4. Back Button (Centered below summary bar)
        float btnW = 460f;
        float btnY = summaryB + 20f;
        btnBack.setBounds(cx - btnW * 0.5f, btnY, cx + btnW * 0.5f, btnY + backH);
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

    public static Action onTouchDown(float sx, float sy) {
        return onTouchDown(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchDown(float sx, float sy, float sceneH) {
        layout(sceneH);
        btnBack.isPressed = btnBack.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchDown(float sx, float sy, int unlocked, float sceneH) {
        return onTouchDown(sx, sy, sceneH);
    }

    public static Action onTouchUp(float sx, float sy, LevelProgress progress, int currentLevel) {
        int unlocked = (progress != null) ? progress.getHighestUnlocked() : currentLevel;
        return onTouchUp(sx, sy, unlocked, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, LevelProgress progress, int currentLevel, float sceneH) {
        int unlocked = (progress != null) ? progress.getHighestUnlocked() : currentLevel;
        return onTouchUp(sx, sy, unlocked, sceneH);
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
                if (activeChapter != i) {
                    activeChapter = i;
                    return Action.TAB_CHANGED;
                }
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
        draw(c, progress, currentLevel, 0f, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, LevelProgress progress, int currentLevel, float sceneH) {
        draw(c, progress, currentLevel, 0f, sceneH);
    }

    public static void draw(Canvas c, LevelProgress progress, int currentLevel, float time, float sceneH) {
        layout(sceneH);

        int unlocked = (progress != null) ? progress.getHighestUnlocked() : currentLevel;
        float sw = ShadowCalculator.SCENE_W;
        float cx = sw * 0.5f;

        // 1. Rich living animated celestial chamber backdrop
        TitleScreen.drawCelestialBackdrop(c, time, 1.0f, sceneH);

        // 2. Header
        float headerY = cachedHeaderY;
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(46f);

        text.setColor(0xDD000000);
        c.drawText("CHAPTER MAP", cx, headerY + 2f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("CHAPTER MAP", cx, headerY, text);

        text.setTextSize(20f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xCC000000);

        String[] chapterNames = {
                "CHAPTER I: THE APPRENTICE'S STUDY",
                "CHAPTER II: THE ALCHEMIST'S CHAMBER",
                "CHAPTER III: THE CELESTIAL OBSERVATORY"
        };
        c.drawText(chapterNames[activeChapter], cx, headerY + 42f + 2f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText(chapterNames[activeChapter], cx, headerY + 42f, text);

        // Dividing flourish
        float divY = headerY + 58f;
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

        // 3. Chapter Tabs
        String[] roman = {"CHAPTER I", "CHAPTER II", "CHAPTER III"};
        for (int i = 0; i < CHAPTERS; i++) {
            boolean isSel = (i == activeChapter);
            boolean isUnlocked = (progress == null || progress.isChapterUnlocked(i));

            fill.setColor(isSel ? 0xFFE5A836 : (isUnlocked ? 0xEE1C2234 : 0x55141A28));
            c.drawRoundRect(TAB_L[i], TAB_T[i], TAB_R[i], TAB_B[i], 16f, 16f, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isSel ? 2.6f : 1.8f);
            stroke.setColor(isSel ? 0xFFFFF4D0 : (isUnlocked ? 0xFFD4AF37 : 0x44607080));
            c.drawRoundRect(TAB_L[i], TAB_T[i], TAB_R[i], TAB_B[i], 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            float tabCx = (TAB_L[i] + TAB_R[i]) * 0.5f;
            float tabCy = (TAB_T[i] + TAB_B[i]) * 0.5f;

            text.setTextSize(19f);
            text.setTypeface(SERIF_BOLD);
            text.setColor(isSel ? 0xFF140802 : (isUnlocked ? 0xFFFFFDF8 : 0x6690A0B8));
            c.drawText(roman[i], tabCx, tabCy - 5f, text);

            int chapterStars = (progress != null) ? progress.getChapterStars(i) : 0;
            text.setTextSize(15f);
            text.setTypeface(SERIF_NORMAL);
            text.setColor(isSel ? 0xDD2A1404 : (isUnlocked ? 0xFFFFE0A8 : 0x4490A0B8));
            c.drawText("\u2605 " + chapterStars + " / 15", tabCx, tabCy + 20f, text);
        }

        // 4. 5 Level Cards for Active Chapter
        for (int i = 0; i < LEVELS_PER_CHAPTER; i++) {
            int globalLevel = activeChapter * LEVELS_PER_CHAPTER + i;
            boolean isUnlocked = (globalLevel <= unlocked);
            int stars = (progress != null) ? progress.getStars(globalLevel) : 0;
            boolean isCurrent = (globalLevel == currentLevel);
            boolean isSolved = (stars > 0);

            float l = CARD_L[i];
            float t = CARD_T[i];
            float r = CARD_R[i];
            float b = CARD_B[i];
            float cardCx = (l + r) * 0.5f;

            // Card Panel Fill
            if (isCurrent) {
                fill.setColor(0xFA262036);
            } else if (isSolved) {
                fill.setColor(0xEE1E2434);
            } else if (isUnlocked) {
                fill.setColor(0xDD181E2C);
            } else {
                fill.setColor(0x55121622);
            }
            c.drawRoundRect(l, t, r, b, 20f, 20f, fill);

            // Card Border
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isCurrent ? 3.0f : (isSolved ? 2.2f : (isUnlocked ? 1.6f : 1.2f)));
            stroke.setColor(isCurrent ? 0xFFFFD54F : (isSolved ? 0xFFD4AF37 : (isUnlocked ? 0x88D4AF37 : 0x33607080)));
            c.drawRoundRect(l, t, r, b, 20f, 20f, stroke);

            // Specular top highlight
            if (isUnlocked) {
                stroke.setStrokeWidth(1.2f);
                stroke.setColor(0x55FFFFFF);
                c.drawLine(l + 20f, t + 2f, r - 20f, t + 2f, stroke);
            }
            stroke.setStyle(Paint.Style.FILL);

            LevelData level = LevelLoader.get(globalLevel);
            String title = (level != null) ? level.title : ("Trial " + (globalLevel + 1));

            // Trial badge
            text.setTextSize(16f);
            text.setTypeface(SERIF_BOLD);
            text.setColor(isCurrent ? 0xFFFFD54F : (isSolved ? 0xFFFFE0A8 : (isUnlocked ? 0xAAFFE0A8 : 0x448090A0)));
            String badge = "TRIAL " + String.format("%02d", globalLevel + 1) + (i == 4 ? " \u2022 CAPSTONE" : "");
            c.drawText(badge, cardCx, t + 32f, text);

            if (isSolved) {
                // Relic glyph
                GameIcons.drawRelicIcon(c, globalLevel, cardCx, t + 78f, 24f, fill, stroke, true);

                // Title
                text.setTextSize(20f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText(title, cardCx, t + 132f, text);

                // 3 Stars Row
                drawCardStars(c, cardCx, t + 162f, stars);

                // Best time
                long best = (progress != null) ? progress.getBestTime(globalLevel) : 0L;
                if (best > 0L) {
                    text.setTextSize(14f);
                    text.setTypeface(SERIF_NORMAL);
                    text.setColor(0xCCFFE0A8);
                    c.drawText("Best: " + (best / 1000L) + "s", cardCx, t + 188f, text);
                }
            } else if (isUnlocked) {
                // Outline glyph
                GameIcons.drawRelicIcon(c, globalLevel, cardCx, t + 78f, 24f, fill, stroke, false);

                // Title
                text.setTextSize(20f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xEEFFFDF8);
                c.drawText(title, cardCx, t + 132f, text);

                text.setTextSize(16f);
                text.setTypeface(SERIF_NORMAL);
                text.setColor(isCurrent ? 0xFFFFD54F : 0xAAFFE0A8);
                c.drawText(isCurrent ? "\u25B6 CURRENT TRIAL" : "UNLOCKED", cardCx, t + 168f, text);
            } else {
                // Locked Padlock
                GameIcons.drawPadlock(c, cardCx, t + 82f, fill, stroke);

                text.setTextSize(19f);
                text.setTypeface(SERIF_NORMAL);
                text.setColor(0x668090A0);
                c.drawText("SEALED VAULT", cardCx, t + 145f, text);
            }
        }

        // 5. Chapter Summary Bar
        fill.setColor(0xEE141A28);
        c.drawRoundRect(summaryL, summaryT, summaryR, summaryB, 14f, 14f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.6f);
        stroke.setColor(0x88D4AF37);
        c.drawRoundRect(summaryL, summaryT, summaryR, summaryB, 14f, 14f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        int totalStars = (progress != null) ? progress.getTotalStars() : 0;
        text.setTextSize(16f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xFFFFE0A8);
        c.drawText("TOTAL STARS: " + totalStars + " / 45   \u2022   ASTRONOMER RANK: " + ((progress != null) ? progress.getPlayerRank() : "Apprentice"), cx, summaryT + 26f, text);

        // 6. Back Button
        btnBack.label = "MAIN MENU";
        btnBack.draw(c, fill, stroke, text, 1.0f, time);

        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCardStars(Canvas c, float cx, float cy, int stars) {
        float spacing = 28f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean filled = (i < stars);
            GameIcons.drawStar(c, sx, cy, 14f, filled);
        }
    }
}
