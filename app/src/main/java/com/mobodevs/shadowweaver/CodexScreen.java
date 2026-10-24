package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Shadow Weaver - CodexScreen
 * ===========================
 * Astronomy Journal & Shadow Relics Gallery.
 * Displays all 15 collectible shadow constellations, player alchemist rank,
 * star mastery progress, and personal best times.
 */
public final class CodexScreen {

    public enum Action {
        NONE, BACK
    }

    private static final int COLS = 5;
    private static final int ROWS = 3;
    private static final int TOTAL_ITEMS = 15;

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path path = new Path();
    private static final RectF rect = new RectF();

    private static final float[] ITEM_L = new float[TOTAL_ITEMS];
    private static final float[] ITEM_T = new float[TOTAL_ITEMS];
    private static final float[] ITEM_R = new float[TOTAL_ITEMS];
    private static final float[] ITEM_B = new float[TOTAL_ITEMS];

    private static final UiButton btnBack = new UiButton();
    private static boolean layoutReady;

    private static void layout() {
        if (layoutReady) return;

        float padX = 54f;
        float top = 340f;
        float gapX = 18f;
        float gapY = 18f;
        float cardW = (ShadowCalculator.SCENE_W - padX * 2 - gapX * (COLS - 1)) / COLS;
        float cardH = 180f;

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

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float btnW = 320f;
        float btnH = 68f;
        float btnY = top + ROWS * (cardH + gapY) + 40f;
        btnBack.setBounds(cx - btnW * 0.5f, btnY, cx + btnW * 0.5f, btnY + btnH);
        btnBack.cornerRadius = 16f;

        layoutReady = true;
    }

    public static Action onTouchDown(float sx, float sy) {
        layout();
        btnBack.isPressed = btnBack.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        layout();
        Action act = Action.NONE;
        if (btnBack.isPressed && btnBack.hitTest(sx, sy)) {
            act = Action.BACK;
        }
        btnBack.isPressed = false;
        return act;
    }

    public static void cancelPress() {
        btnBack.isPressed = false;
    }

    public static void draw(Canvas c, LevelProgress progress, float time) {
        layout();

        float sw = ShadowCalculator.SCENE_W;
        float sh = ShadowCalculator.SCENE_H;
        float cx = sw * 0.5f;

        // 1. Midnight Background
        fill.setColor(0xF6100905);
        c.drawRect(0, 0, sw, sh, fill);

        // 2. Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(44f);
        text.setColor(0xFFFFE9C4);
        c.drawText("ASTRONOMY JOURNAL", cx, 110f, text);

        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x99FFE0B0);
        c.drawText("COLLECTED CELESTIAL SHADOW RELICS", cx, 146f, text);

        // 3. Player Alchemist Rank Banner
        float bannerL = 54f;
        float bannerR = sw - 54f;
        float bannerT = 180f;
        float bannerB = 295f;
        rect.set(bannerL, bannerT, bannerR, bannerB);

        fill.setColor(0xEE1E130B);
        c.drawRoundRect(rect, 20f, 20f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0x66C5A059);
        c.drawRoundRect(rect, 20f, 20f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        int totalStars = (progress != null) ? progress.getTotalStars() : 0;
        String rankTitle = (progress != null) ? progress.getPlayerRank() : "Apprentice of Shadows";
        String rankDesc = (progress != null) ? progress.getPlayerRankDescription() : "";

        text.setTextSize(28f);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xFFFFD54F);
        c.drawText(rankTitle, cx, bannerT + 42f, text);

        text.setTextSize(18f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xBBF0DFC6);
        c.drawText(rankDesc, cx, bannerT + 74f, text);

        // Progress bar in banner
        float barW = 460f;
        float barH = 14f;
        float barL = cx - barW * 0.5f;
        float barT = bannerT + 88f;
        rect.set(barL, barT, barL + barW, barT + barH);

        fill.setColor(0x44000000);
        c.drawRoundRect(rect, 7f, 7f, fill);

        float fillRatio = totalStars / (float) LevelProgress.TOTAL_MAX_STARS;
        if (fillRatio > 0f) {
            rect.set(barL, barT, barL + barW * fillRatio, barT + barH);
            fill.setColor(0xFFFFC46A);
            c.drawRoundRect(rect, 7f, 7f, fill);
        }

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66C5A059);
        rect.set(barL, barT, barL + barW, barT + barH);
        c.drawRoundRect(rect, 7f, 7f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 4. 15 Relic Cards
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

            rect.set(l, t, r, b);

            // Card background
            if (isSolved) {
                fill.setColor(0xDD271A10);
            } else if (isUnlocked) {
                fill.setColor(0x991C120A);
            } else {
                fill.setColor(0x55140C07);
            }
            c.drawRoundRect(rect, 14f, 14f, fill);

            // Card border
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isSolved ? 2.0f : 1.2f);
            stroke.setColor(isSolved ? 0xAAC5A059 : (isUnlocked ? 0x44C5A059 : 0x22665544));
            c.drawRoundRect(rect, 14f, 14f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            LevelData level = LevelLoader.get(i);
            String title = (level != null) ? level.title : ("#" + (i + 1));

            // Level Number badge
            text.setTextSize(18f);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(isSolved ? 0xFFFFD54F : (isUnlocked ? 0x99FFE0B0 : 0x44FFE0B0));
            c.drawText(String.valueOf(i + 1), cardCx, t + 28f, text);

            if (isSolved) {
                // Solved icon (small constellation star emblem)
                fill.setColor(0xFFFFE082);
                c.drawCircle(cardCx, t + 65f, 14f, fill);

                // Title
                text.setTextSize(18f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFF0DFC6);
                c.drawText(title, cardCx, t + 115f, text);

                // Stars row
                drawStarsMini(c, cardCx, t + 140f, stars);

                // Best time
                long best = (progress != null) ? progress.getBestTime(i) : 0L;
                if (best > 0L) {
                    text.setTextSize(14f);
                    text.setTypeface(Typeface.DEFAULT);
                    text.setColor(0x99D5C4A8);
                    c.drawText((best / 1000L) + "s", cardCx, t + 164f, text);
                }
            } else if (isUnlocked) {
                // Unsolved but reachable
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(1.6f);
                stroke.setColor(0x66FFE0A8);
                c.drawCircle(cardCx, t + 65f, 14f, stroke);
                stroke.setStyle(Paint.Style.FILL);

                text.setTextSize(17f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0x88F0DFC6);
                c.drawText(title, cardCx, t + 115f, text);

                text.setTextSize(14f);
                text.setColor(0x55FFE0A8);
                c.drawText("Trial Open", cardCx, t + 144f, text);
            } else {
                // Locked
                drawPadlock(c, cardCx, t + 65f);

                text.setTextSize(16f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0x44FFE0A8);
                c.drawText("Locked", cardCx, t + 120f, text);
            }
        }

        // 5. Back Button
        btnBack.label = "MAIN MENU";
        btnBack.draw(c, fill, stroke, text, 1.0f);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawStarsMini(Canvas c, float cx, float cy, int stars) {
        float spacing = 22f;
        for (int i = 0; i < 3; i++) {
            float sx = cx + (i - 1) * spacing;
            boolean filled = (i < stars);
            fill.setColor(filled ? 0xFFFFD54F : 0x33FFE0A8);
            drawStarPath(c, sx, cy, 7f);
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
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0x44A09080);
        rect.set(cx - 8f, cy - 14f, cx + 8f, cy - 2f);
        c.drawArc(rect, 180, 180, false, stroke);

        fill.setColor(0x44A09080);
        rect.set(cx - 10f, cy - 4f, cx + 10f, cy + 10f);
        c.drawRoundRect(rect, 3f, 3f, fill);
        stroke.setStyle(Paint.Style.FILL);
    }
}
