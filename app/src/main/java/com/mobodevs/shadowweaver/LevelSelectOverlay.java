package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Shadow Weaver - LevelSelectOverlay
 * ==================================
 * 3-Chapter navigation interface with chapter tabs, star ratings,
 * best times, lock states, and tactile press responsiveness.
 */
public final class LevelSelectOverlay {

    public enum Action {
        NONE, PICKED, CLOSE
    }

    public static final int CHAPTERS = 3;
    public static final int LEVELS_PER_CHAPTER = 5;

    private static int activeChapter = 0;
    private static int lastPickedLevel = -1;

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path starPath = new Path();
    private static final RectF rect = new RectF();

    // Chapter Tabs
    private static final float[] TAB_L = new float[CHAPTERS];
    private static final float[] TAB_T = new float[CHAPTERS];
    private static final float[] TAB_R = new float[CHAPTERS];
    private static final float[] TAB_B = new float[CHAPTERS];

    // 5 Level Cards in current chapter
    private static final float[] CARD_L = new float[LEVELS_PER_CHAPTER];
    private static final float[] CARD_T = new float[LEVELS_PER_CHAPTER];
    private static final float[] CARD_R = new float[LEVELS_PER_CHAPTER];
    private static final float[] CARD_B = new float[LEVELS_PER_CHAPTER];

    private static final UiButton btnBack = new UiButton();
    private static boolean layoutReady;

    private static void layout() {
        if (layoutReady) return;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float padX = 60f;

        // Chapter Tabs
        float tabTop = 220f;
        float tabH = 68f;
        float tabGap = 16f;
        float tabW = (ShadowCalculator.SCENE_W - padX * 2 - tabGap * (CHAPTERS - 1)) / CHAPTERS;

        for (int i = 0; i < CHAPTERS; i++) {
            float l = padX + i * (tabW + tabGap);
            TAB_L[i] = l;
            TAB_T[i] = tabTop;
            TAB_R[i] = l + tabW;
            TAB_B[i] = tabTop + tabH;
        }

        // 5 Level Cards (arranged as 3 top, 2 bottom centered)
        float cardTop = 330f;
        float cardW = 260f;
        float cardH = 210f;
        float gapX = 30f;
        float gapY = 24f;

        // Row 1 (Levels 1, 2, 3 in chapter)
        float row1W = cardW * 3 + gapX * 2;
        float row1L = cx - row1W * 0.5f;
        for (int i = 0; i < 3; i++) {
            float l = row1L + i * (cardW + gapX);
            CARD_L[i] = l;
            CARD_T[i] = cardTop;
            CARD_R[i] = l + cardW;
            CARD_B[i] = cardTop + cardH;
        }

        // Row 2 (Levels 4, 5 in chapter)
        float row2W = cardW * 2 + gapX;
        float row2L = cx - row2W * 0.5f;
        float row2T = cardTop + cardH + gapY;
        for (int i = 3; i < 5; i++) {
            float l = row2L + (i - 3) * (cardW + gapX);
            CARD_L[i] = l;
            CARD_T[i] = row2T;
            CARD_R[i] = l + cardW;
            CARD_B[i] = row2T + cardH;
        }

        // Back Button
        float btnW = 320f;
        float btnH = 68f;
        float btnY = row2T + cardH + 48f;
        btnBack.setBounds(cx - btnW * 0.5f, btnY, cx + btnW * 0.5f, btnY + btnH);
        btnBack.cornerRadius = 16f;

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
        layout();
        btnBack.isPressed = btnBack.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy, int unlocked) {
        layout();

        // 1. Back button
        if (btnBack.isPressed && btnBack.hitTest(sx, sy)) {
            btnBack.isPressed = false;
            return Action.CLOSE;
        }
        btnBack.isPressed = false;

        // 2. Chapter tabs
        for (int i = 0; i < CHAPTERS; i++) {
            if (sx >= TAB_L[i] && sx <= TAB_R[i] && sy >= TAB_T[i] && sy <= TAB_B[i]) {
                activeChapter = i;
                return Action.NONE;
            }
        }

        // 3. Level Cards
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
        layout();

        int unlocked = (progress != null) ? progress.getHighestUnlocked() : currentLevel;
        float sw = ShadowCalculator.SCENE_W;
        float sh = ShadowCalculator.SCENE_H;
        float cx = sw * 0.5f;

        // 1. Modal Scrim Backdrop
        fill.setColor(0xF6100905);
        c.drawRect(0, 0, sw, sh, fill);

        // 2. Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(44f);
        text.setColor(0xFFFFE9C4);
        c.drawText("CHAPTER MAP", cx, 110f, text);

        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x99FFE0B0);

        String[] chapterNames = {
                "CHAPTER I: THE APPRENTICE'S STUDY",
                "CHAPTER II: THE ALCHEMIST'S CHAMBER",
                "CHAPTER III: THE CELESTIAL OBSERVATORY"
        };
        c.drawText(chapterNames[activeChapter], cx, 150f, text);

        // 3. Chapter Tabs
        String[] roman = {"CHAPTER I", "CHAPTER II", "CHAPTER III"};
        for (int i = 0; i < CHAPTERS; i++) {
            boolean isSel = (i == activeChapter);
            boolean isUnlocked = (progress == null || progress.isChapterUnlocked(i));
            rect.set(TAB_L[i], TAB_T[i], TAB_R[i], TAB_B[i]);

            fill.setColor(isSel ? 0xFFC5943C : (isUnlocked ? 0x991E140C : 0x44140C07));
            c.drawRoundRect(rect, 14f, 14f, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isSel ? 2.2f : 1.2f);
            stroke.setColor(isSel ? 0xFFFFE0A8 : (isUnlocked ? 0x66C5A059 : 0x22665544));
            c.drawRoundRect(rect, 14f, 14f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            float tabCx = (TAB_L[i] + TAB_R[i]) * 0.5f;
            float tabCy = (TAB_T[i] + TAB_B[i]) * 0.5f;

            text.setTextSize(18f);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(isSel ? 0xFF1E1005 : (isUnlocked ? 0xFFF0DFC6 : 0x44FFE0A8));
            c.drawText(roman[i], tabCx, tabCy - 4f, text);

            int chapterStars = (progress != null) ? progress.getChapterStars(i) : 0;
            text.setTextSize(14f);
            text.setTypeface(Typeface.DEFAULT);
            text.setColor(isSel ? 0xCC351C08 : (isUnlocked ? 0x99D5C4A8 : 0x33FFE0A8));
            c.drawText("⭐ " + chapterStars + "/15", tabCx, tabCy + 18f, text);
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
            rect.set(l, t, r, b);

            // Card background
            if (isCurrent) {
                fill.setColor(0xDD3A2516);
            } else if (isSolved) {
                fill.setColor(0xCC26180E);
            } else if (isUnlocked) {
                fill.setColor(0xAA1C120A);
            } else {
                fill.setColor(0x55120B06);
            }
            c.drawRoundRect(rect, 18f, 18f, fill);

            // Card border
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isCurrent ? 3.0f : (isSolved ? 2.0f : 1.2f));
            stroke.setColor(isCurrent ? 0xFFFFD54F : (isSolved ? 0xAAC5A059 : (isUnlocked ? 0x55C5A059 : 0x22665544)));
            c.drawRoundRect(rect, 18f, 18f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            LevelData data = LevelLoader.get(globalLevel);
            String title = (data != null) ? data.title : ("Trial " + (globalLevel + 1));

            // Level number
            text.setTextSize(22f);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(isUnlocked ? 0xFFFFD54F : 0x44FFE0B0);
            c.drawText(String.valueOf(globalLevel + 1), cardCx, t + 36f, text);

            if (isUnlocked) {
                // Title
                text.setTextSize(24f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFF0DFC6);
                c.drawText(title, cardCx, t + 80f, text);

                // Stars row
                drawStarsRow(c, cardCx, t + 125f, stars);

                // Best time
                long best = (progress != null) ? progress.getBestTime(globalLevel) : 0L;
                text.setTextSize(17f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0x99D5C4A8);
                if (best > 0L) {
                    c.drawText("Best: " + (best / 1000L) + "s", cardCx, t + 172f, text);
                } else {
                    c.drawText(isCurrent ? "Current" : "Open", cardCx, t + 172f, text);
                }
            } else {
                // Locked Padlock
                drawPadlock(c, cardCx, t + 90f);

                text.setTextSize(18f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0x44FFE0B0);
                c.drawText("Locked", cardCx, t + 150f, text);
            }
        }

        // 5. Back Button
        btnBack.label = "MAIN MENU";
        btnBack.draw(c, fill, stroke, text, 1.0f);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawStarsRow(Canvas c, float cx, float cy, int stars) {
        float spacing = 32f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean filled = (i < stars);
            fill.setColor(filled ? 0xFFFFD54F : 0x33FFE0A8);
            drawStarPath(c, sx, cy, 11f);
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

    private static void drawPadlock(Canvas c, float cx, float cy) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0x44A09080);
        rect.set(cx - 12f, cy - 20f, cx + 12f, cy - 2f);
        c.drawArc(rect, 180, 180, false, stroke);

        fill.setColor(0x44A09080);
        rect.set(cx - 15f, cy - 4f, cx + 15f, cy + 18f);
        c.drawRoundRect(rect, 4f, 4f, fill);
        stroke.setStyle(Paint.Style.FILL);
    }
}
