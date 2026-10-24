package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * Shadow Weaver - TutorialScreen
 * ==============================
 * 4-page interactive illustrated guide explaining:
 *  1. Distance to candle = scale
 *  2. Dial & two-finger rotation
 *  3. Magnetic resonance & snap-assist
 *  4. Constellation completion & 3-star mastery
 */
public final class TutorialScreen {

    public enum Action {
        NONE, BACK, PLAY
    }

    private static final int TOTAL_PAGES = 4;
    private static int currentPage = 0;

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path path = new Path();
    private static final RectF cardRect = new RectF();

    private static final UiButton btnPrev = new UiButton();
    private static final UiButton btnNext = new UiButton();
    private static final UiButton btnBack = new UiButton();

    private static boolean layoutReady;

    private static void layout() {
        if (layoutReady) return;

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardW = 760f;
        float cardH = 920f;
        float cardL = cx - cardW * 0.5f;
        float cardT = 240f;
        cardRect.set(cardL, cardT, cardL + cardW, cardT + cardH);

        float btnW = 220f;
        float btnH = 68f;
        float btnY = cardT + cardH - 100f;

        btnPrev.setBounds(cardL + 40f, btnY, cardL + 40f + btnW, btnY + btnH);
        btnPrev.cornerRadius = 16f;

        btnNext.setBounds(cardL + cardW - 40f - btnW, btnY, cardL + cardW - 40f, btnY + btnH);
        btnNext.isPrimary = true;
        btnNext.cornerRadius = 16f;

        btnBack.setBounds(cx - 160f, cardT + cardH + 40f, cx + 160f, cardT + cardH + 40f + 64f);
        btnBack.cornerRadius = 16f;

        layoutReady = true;
    }

    public static void setPage(int page) {
        currentPage = Math.max(0, Math.min(page, TOTAL_PAGES - 1));
    }

    public static Action onTouchDown(float sx, float sy) {
        layout();
        btnPrev.isPressed = btnPrev.hitTest(sx, sy);
        btnNext.isPressed = btnNext.hitTest(sx, sy);
        btnBack.isPressed = btnBack.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        layout();
        Action act = Action.NONE;

        if (btnPrev.isPressed && btnPrev.hitTest(sx, sy)) {
            if (currentPage > 0) currentPage--;
        } else if (btnNext.isPressed && btnNext.hitTest(sx, sy)) {
            if (currentPage < TOTAL_PAGES - 1) {
                currentPage++;
            } else {
                act = Action.PLAY; // "BEGIN WEAVING" on last page
            }
        } else if (btnBack.isPressed && btnBack.hitTest(sx, sy)) {
            act = Action.BACK;
        }

        btnPrev.isPressed = false;
        btnNext.isPressed = false;
        btnBack.isPressed = false;

        return act;
    }

    public static void cancelPress() {
        btnPrev.isPressed = false;
        btnNext.isPressed = false;
        btnBack.isPressed = false;
    }

    public static void draw(Canvas c, float time) {
        layout();

        float sw = ShadowCalculator.SCENE_W;
        float sh = ShadowCalculator.SCENE_H;
        float cx = sw * 0.5f;

        // 1. Dark Backdrop
        fill.setColor(0xF4120A05);
        c.drawRect(0, 0, sw, sh, fill);

        // 2. Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setTextSize(44f);
        text.setColor(0xFFFFE9C4);
        c.drawText("HOW TO PLAY", cx, 140f, text);

        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x99FFE0B0);
        c.drawText("THE PRINCIPLES OF ASTRAL PERSPECTIVE", cx, 178f, text);

        // 3. Frosted Obsidian Guide Card
        fill.setColor(0xEE1E130B);
        c.drawRoundRect(cardRect, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0x66C5A059);
        c.drawRoundRect(cardRect, 24f, 24f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 4. Page Content
        drawCardContent(c, cx, cardRect.top + 50f, currentPage, time);

        // 5. Page Dots Indicator
        float dotSpacing = 32f;
        float dotStartX = cx - (TOTAL_PAGES - 1) * dotSpacing * 0.5f;
        float dotY = cardRect.bottom - 135f;

        for (int i = 0; i < TOTAL_PAGES; i++) {
            float dx = dotStartX + i * dotSpacing;
            if (i == currentPage) {
                fill.setColor(0xFFFFD54F);
                c.drawCircle(dx, dotY, 6.5f, fill);
            } else {
                fill.setColor(0x44FFE0A8);
                c.drawCircle(dx, dotY, 4.5f, fill);
            }
        }

        // 6. Navigation Buttons
        btnPrev.label = "PREV";
        btnPrev.enabled = (currentPage > 0);

        if (currentPage == TOTAL_PAGES - 1) {
            btnNext.label = "LET'S PLAY";
        } else {
            btnNext.label = "NEXT";
        }

        btnBack.label = "MAIN MENU";

        btnPrev.draw(c, fill, stroke, text, 1.0f);
        btnNext.draw(c, fill, stroke, text, 1.0f);
        btnBack.draw(c, fill, stroke, text, 1.0f);
    }

    private static void drawCardContent(Canvas c, float cx, float topY, int page, float time) {
        text.setTextAlign(Paint.Align.CENTER);

        switch (page) {
            case 0: { // The Candle & Scale
                text.setTextSize(32f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFD572);
                c.drawText("I. DISTANCE & SCALE", cx, topY + 20f, text);

                // Illustration Box
                float boxY = topY + 70f;
                drawIllustrationBox(c, cx, boxY, 680f, 320f);

                // Draw Candle at top
                fill.setColor(0xFFFFB74D);
                c.drawCircle(cx, boxY + 50f, 16f, fill);
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(cx, boxY + 48f, 6f, fill);

                // Arrow moving shape up/down
                float bounce = (float) Math.sin(time * 3f) * 25f;
                float tokenY = boxY + 160f + bounce;

                // Token
                fill.setColor(0xFF352416);
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.5f);
                stroke.setColor(0xFFD4AF37);
                RectF r = new RectF(cx - 35f, tokenY - 35f, cx + 35f, tokenY + 35f);
                c.drawRoundRect(r, 8f, 8f, fill);
                c.drawRoundRect(r, 8f, 8f, stroke);
                stroke.setStyle(Paint.Style.FILL);

                // Cast Shadow at bottom: bigger when token is higher
                float shadowScale = 1.0f + ((boxY + 160f) - tokenY) * 0.025f;
                fill.setColor(0xCC000000);
                float sw = 50f * shadowScale;
                float sh = 20f * shadowScale;
                RectF shadowRect = new RectF(cx - sw, boxY + 280f - sh, cx + sw, boxY + 280f);
                c.drawOval(shadowRect, fill);

                // Explanation Text
                text.setTextSize(22f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xFFE8DCC8);
                c.drawText("Shadows behave according to physical optics.", cx, topY + 450f, text);
                c.drawText("Drag a token UP (closer to the flame) to enlarge its shadow.", cx, topY + 490f, text);
                c.drawText("Drag it DOWN (away from the flame) to shrink it.", cx, topY + 530f, text);
                break;
            }

            case 1: { // Rotation & Orientation
                text.setTextSize(32f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFD572);
                c.drawText("II. MECHANICAL ROTATION", cx, topY + 20f, text);

                float boxY = topY + 70f;
                drawIllustrationBox(c, cx, boxY, 680f, 320f);

                // Rotary dial demonstration
                float dialR = 80f;
                float rotAngle = time * 45f;
                float knobX = cx + dialR * (float) Math.cos(Math.toRadians(rotAngle));
                float knobY = (boxY + 160f) + dialR * (float) Math.sin(Math.toRadians(rotAngle));

                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2f);
                stroke.setColor(0x55FFE0A8);
                c.drawCircle(cx, boxY + 160f, dialR, stroke);

                // Rotating token
                int s = c.save();
                c.translate(cx, boxY + 160f);
                c.rotate(rotAngle);
                fill.setColor(0xFF352416);
                stroke.setColor(0xFFD4AF37);
                RectF r = new RectF(-40f, -25f, 40f, 25f);
                c.drawRoundRect(r, 6f, 6f, fill);
                c.drawRoundRect(r, 6f, 6f, stroke);
                c.restoreToCount(s);
                stroke.setStyle(Paint.Style.FILL);

                // Amber Knob
                fill.setColor(0xFFFFD54F);
                c.drawCircle(knobX, knobY, 14f, fill);
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(knobX, knobY, 4f, fill);

                // Explanation
                text.setTextSize(22f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xFFE8DCC8);
                c.drawText("Spin tokens using the outer amber knob,", cx, topY + 450f, text);
                c.drawText("or use two fingers anywhere on screen to pinch and twist.", cx, topY + 490f, text);
                c.drawText("Haptic clicks notify you every 15 degrees.", cx, topY + 530f, text);
                break;
            }

            case 2: { // Magnetic Resonance & Snap
                text.setTextSize(32f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFD572);
                c.drawText("III. RESONANCE & SNAP", cx, topY + 20f, text);

                float boxY = topY + 70f;
                drawIllustrationBox(c, cx, boxY, 680f, 320f);

                // Target ghost on left, token on right with tether
                float targetX = cx - 120f;
                float targetY = boxY + 160f;
                float tokenX = cx + 80f;
                float tokenY = boxY + 160f;

                // Target ghost
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.5f);
                stroke.setColor(0x66FFD54F);
                c.drawCircle(targetX, targetY, 40f, stroke);

                // Magnetic tether line
                stroke.setStrokeWidth(2.2f);
                stroke.setColor(0xAAFFD54F);
                c.drawLine(tokenX, tokenY, targetX, targetY, stroke);
                stroke.setStyle(Paint.Style.FILL);

                // Resonant Token with glowing aura
                fill.setColor(0x44FFD54F);
                c.drawCircle(tokenX, tokenY, 52f, fill);
                fill.setColor(0xFF352416);
                c.drawCircle(tokenX, tokenY, 40f, fill);
                fill.setColor(0xFFC5A059);
                c.drawCircle(tokenX, tokenY, 8f, fill);

                // Explanation
                text.setTextSize(22f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xFFE8DCC8);
                c.drawText("Tokens emit a golden aura when close to target.", cx, topY + 450f, text);
                c.drawText("An animated magnetic flux line guides you to the spot.", cx, topY + 490f, text);
                c.drawText("Release your finger to automatically snap into lock!", cx, topY + 530f, text);
                break;
            }

            case 3: { // Constellation & Stars
                text.setTextSize(32f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFD572);
                c.drawText("IV. ASTRAL REVEAL & STARS", cx, topY + 20f, text);

                float boxY = topY + 70f;
                drawIllustrationBox(c, cx, boxY, 680f, 320f);

                // Star polygon reveal
                float midY = boxY + 140f;
                drawConstellationStar(c, cx, midY, 60f);

                // 3 Stars rating underneath
                float starSpacing = 50f;
                for (int i = 0; i < 3; i++) {
                    float sx = cx + (i - 1) * starSpacing;
                    drawSmallStar(c, sx, boxY + 265f, 16f, true);
                }

                // Explanation
                text.setTextSize(22f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xFFE8DCC8);
                c.drawText("Reach 85% match to complete the silhouette.", cx, topY + 450f, text);
                c.drawText("1 Star: Solved   *   2 Stars: Solved under par time", cx, topY + 490f, text);
                c.drawText("3 Stars: Solved under par time AND within par moves!", cx, topY + 530f, text);
                break;
            }
        }
    }

    private static void drawIllustrationBox(Canvas c, float cx, float y, float w, float h) {
        RectF r = new RectF(cx - w * 0.5f, y, cx + w * 0.5f, y + h);
        fill.setColor(0x440C0704);
        c.drawRoundRect(r, 14f, 14f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x33C5A059);
        c.drawRoundRect(r, 14f, 14f, stroke);
        stroke.setStyle(Paint.Style.FILL);
    }

    private static void drawConstellationStar(Canvas c, float cx, float cy, float r) {
        fill.setColor(0xCCFFE082);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3f);
        stroke.setColor(0xFFFFD54F);

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
        c.drawPath(path, stroke);
        stroke.setStyle(Paint.Style.FILL);
    }

    private static void drawSmallStar(Canvas c, float cx, float cy, float r, boolean filled) {
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
        fill.setColor(filled ? 0xFFFFD54F : 0x44FFE0A8);
        c.drawPath(path, fill);
    }
}
