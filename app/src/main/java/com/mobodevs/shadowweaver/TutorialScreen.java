package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/**
 * Shadow Weaver - TutorialScreen
 * ==============================
 * 4-page interactive illustrated guide explaining:
 *  1. Distance to candle = scale
 *  2. Dial & two-finger rotation
 *  3. Magnetic resonance & snap-assist
 *  4. Constellation completion & 3-star mastery
 *
 * Fully responsive: dynamically centered horizontally and vertically on any screen aspect ratio.
 * Backed by the rich living animated celestial chamber.
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

    private static Typeface safeTypeface(Typeface family, int style) {
        try {
            return Typeface.create(family, style);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final Typeface SERIF_BOLD = safeTypeface(Typeface.SERIF, Typeface.BOLD);
    private static final Typeface SERIF_NORMAL = safeTypeface(Typeface.SERIF, Typeface.NORMAL);

    private static float cardL;
    private static float cardT;
    private static float cardR;
    private static float cardB;
    private static float cachedHeaderY;

    private static final UiButton btnPrev = new UiButton();
    private static final UiButton btnNext = new UiButton();
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
        float cardW = 880f;
        float cardH = 940f;
        cardL = cx - cardW * 0.5f;
        cardR = cardL + cardW;

        // Dynamic vertical centering algorithm
        float totalBlockH = 110f + 24f + cardH + 28f + 88f; // 1190f
        float remainingH = Math.max(50f, sceneH - totalBlockH);
        float topPad = remainingH * 0.38f + 30f;

        cachedHeaderY = topPad + 45f;
        cardT = cachedHeaderY + 75f;
        cardB = cardT + cardH;

        // Navigation buttons inside guide card
        float btnW = 270f;
        float btnH = 86f;
        float btnY = cardB - 110f;

        btnPrev.setBounds(cardL + 44f, btnY, cardL + 44f + btnW, btnY + btnH);
        btnPrev.cornerRadius = 22f;

        float nextW = 290f;
        btnNext.setBounds(cardR - 44f - nextW, btnY, cardR - 44f, btnY + btnH);
        btnNext.isPrimary = true;
        btnNext.cornerRadius = 22f;

        // Main Menu button centered below guide card
        float backW = 540f;
        float backH = 88f;
        float backY = cardB + 28f;
        btnBack.setBounds(cx - backW * 0.5f, backY, cx + backW * 0.5f, backY + backH);
        btnBack.cornerRadius = 22f;

        layoutReady = true;
    }

    public static void setPage(int page) {
        currentPage = Math.max(0, Math.min(page, TOTAL_PAGES - 1));
    }

    public static Action onTouchDown(float sx, float sy) {
        return onTouchDown(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchDown(float sx, float sy, float sceneH) {
        layout(sceneH);
        btnPrev.isPressed = btnPrev.hitTest(sx, sy);
        btnNext.isPressed = btnNext.hitTest(sx, sy);
        btnBack.isPressed = btnBack.hitTest(sx, sy);
        return Action.NONE;
    }

    public static Action onTouchUp(float sx, float sy) {
        return onTouchUp(sx, sy, ShadowCalculator.SCENE_H);
    }

    public static Action onTouchUp(float sx, float sy, float sceneH) {
        layout(sceneH);
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
        draw(c, time, ShadowCalculator.SCENE_H);
    }

    public static void draw(Canvas c, float time, float sceneH) {
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
        c.drawText("HOW TO PLAY", cx, headerY + 2f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("HOW TO PLAY", cx, headerY, text);

        text.setTextSize(20f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xCC000000);
        c.drawText("THE PRINCIPLES OF ASTRAL PERSPECTIVE", cx, headerY + 42f + 2f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("THE PRINCIPLES OF ASTRAL PERSPECTIVE", cx, headerY + 42f, text);

        // Ornamental brass dividing line
        float divY = headerY + 60f;
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

        // 3. Frosted Obsidian Guide Card
        fill.setColor(0xFA141A28);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.8f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Top specular highlight
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x55FFFFFF);
        c.drawLine(cardL + 24f, cardT + 2f, cardR - 24f, cardT + 2f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Corner filigree brackets
        drawCornerBrackets(c, cardL, cardT, cardR, cardB);

        // 4. Page Content
        drawCardContent(c, cx, cardT + 42f, currentPage, time);

        // 5. Page Dots Indicator
        float dotSpacing = 36f;
        float dotStartX = cx - (TOTAL_PAGES - 1) * dotSpacing * 0.5f;
        float dotY = cardB - 130f;

        for (int i = 0; i < TOTAL_PAGES; i++) {
            float dx = dotStartX + i * dotSpacing;
            if (i == currentPage) {
                fill.setColor(0xFFFFD54F);
                c.drawCircle(dx, dotY, 7.5f, fill);
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(dx, dotY, 3f, fill);
            } else {
                fill.setColor(0x44607080);
                c.drawCircle(dx, dotY, 5f, fill);
            }
        }

        // 6. Navigation Buttons
        btnPrev.label = "PREV";
        btnPrev.enabled = (currentPage > 0);

        if (currentPage == TOTAL_PAGES - 1) {
            btnNext.label = "BEGIN WEAVING";
        } else {
            btnNext.label = "NEXT";
        }

        btnBack.label = "MAIN MENU";

        btnPrev.draw(c, fill, stroke, text, 1.0f, time);
        btnNext.draw(c, fill, stroke, text, 1.0f, time);
        btnBack.draw(c, fill, stroke, text, 1.0f, time);

        // 7. Grounding footer
        text.setTextSize(16f);
        text.setColor(0x77D4AF37);
        c.drawText("Swipe or tap Next to navigate principles", cx, sh - 36f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCardContent(Canvas c, float cx, float topY, int page, float time) {
        text.setTextAlign(Paint.Align.CENTER);

        switch (page) {
            case 0: { // The Candle & Scale
                text.setTextSize(30f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xCC000000);
                c.drawText("I. DISTANCE & SCALE", cx, topY + 21.5f, text);
                text.setColor(0xFFFFD54F);
                c.drawText("I. DISTANCE & SCALE", cx, topY + 20f, text);

                // Luminous parchment illustration box
                float boxY = topY + 50f;
                drawIllustrationBox(c, cx, boxY, 740f, 360f);

                // Draw Candle at top
                fill.setColor(0xFFFFB74D);
                c.drawCircle(cx, boxY + 52f, 18f, fill);
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(cx, boxY + 50f, 7f, fill);

                // Token oscillating vertically
                float bounce = (float) Math.sin(time * 3f) * 32f;
                float tokenY = boxY + 175f + bounce;

                // Token body
                fill.setColor(0xFF4A301E);
                c.drawRoundRect(cx - 42f, tokenY - 42f, cx + 42f, tokenY + 42f, 10f, 10f, fill);
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(3.0f);
                stroke.setColor(0xFFE5B048);
                c.drawRoundRect(cx - 42f, tokenY - 42f, cx + 42f, tokenY + 42f, 10f, 10f, stroke);
                stroke.setStyle(Paint.Style.FILL);

                // Center gold pivot
                fill.setColor(0xFFFFD54F);
                c.drawCircle(cx, tokenY, 8f, fill);

                // Cast Shadow on lower parchment wall: expands as token moves up toward candle
                float shadowScale = 1.0f + ((boxY + 175f) - tokenY) * 0.022f;
                fill.setColor(0xDD140A04);
                float sw = 64f * shadowScale;
                float sh = 26f * shadowScale;
                c.drawRoundRect(cx - sw, boxY + 325f - sh, cx + sw, boxY + 325f, 12f, 12f, fill);

                // Explanation Text
                text.setTextSize(23f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText("Shadows obey physical optics and distance.", cx, topY + 475f, text);

                text.setTextSize(19f);
                text.setTypeface(SERIF_NORMAL);
                text.setColor(0xDDFFE0A8);
                c.drawText("Drag a token UP toward the candle flame to ENLARGE its shadow.", cx, topY + 518f, text);
                c.drawText("Drag it DOWN away from the flame to SHRINK the shadow.", cx, topY + 556f, text);
                c.drawText("Watch the soft penumbra edge blur physically with elevation.", cx, topY + 594f, text);
                break;
            }

            case 1: { // Rotation & Orientation
                text.setTextSize(30f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xCC000000);
                c.drawText("II. MECHANICAL ROTATION", cx, topY + 21.5f, text);
                text.setColor(0xFFFFD54F);
                c.drawText("II. MECHANICAL ROTATION", cx, topY + 20f, text);

                float boxY = topY + 50f;
                drawIllustrationBox(c, cx, boxY, 740f, 360f);

                // Rotary dial demonstration
                float dialR = 90f;
                float rotAngle = time * 45f;
                float knobX = cx + dialR * (float) Math.cos(Math.toRadians(rotAngle));
                float knobY = (boxY + 180f) + dialR * (float) Math.sin(Math.toRadians(rotAngle));

                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.5f);
                stroke.setColor(0x889E7840);
                c.drawCircle(cx, boxY + 180f, dialR, stroke);

                // Rotating token
                int s = c.save();
                c.translate(cx, boxY + 180f);
                c.rotate(rotAngle);
                fill.setColor(0xFF4A301E);
                c.drawRoundRect(-52f, -32f, 52f, 32f, 8f, 8f, fill);
                stroke.setColor(0xFFE5B048);
                c.drawRoundRect(-52f, -32f, 52f, 32f, 8f, 8f, stroke);
                c.restoreToCount(s);
                stroke.setStyle(Paint.Style.FILL);

                // Spoke arm to knob
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.5f);
                stroke.setColor(0xFF9E7840);
                c.drawLine(cx, boxY + 180f, knobX, knobY, stroke);
                stroke.setStyle(Paint.Style.FILL);

                // Amber Knob with white core
                fill.setColor(0xFFFFD54F);
                c.drawCircle(knobX, knobY, 18f, fill);
                fill.setColor(0xFF1B0E05);
                stroke.setStrokeWidth(2.2f);
                stroke.setColor(0xFF1B0E05);
                c.drawCircle(knobX, knobY, 18f, stroke);
                stroke.setStyle(Paint.Style.FILL);
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(knobX, knobY, 6f, fill);

                // Explanation
                text.setTextSize(23f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText("Precision angle alignment and orientation.", cx, topY + 475f, text);

                text.setTextSize(19f);
                text.setTypeface(SERIF_NORMAL);
                text.setColor(0xDDFFE0A8);
                c.drawText("Spin tokens using the outer tactile amber rotary knob,", cx, topY + 518f, text);
                c.drawText("or place two fingers anywhere on screen to pinch and twist.", cx, topY + 556f, text);
                c.drawText("Haptic clicks pulse smoothly every 15 degrees of rotation.", cx, topY + 594f, text);
                break;
            }

            case 2: { // Magnetic Resonance & Snap
                text.setTextSize(30f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xCC000000);
                c.drawText("III. RESONANCE & SNAP", cx, topY + 21.5f, text);
                text.setColor(0xFFFFD54F);
                c.drawText("III. RESONANCE & SNAP", cx, topY + 20f, text);

                float boxY = topY + 50f;
                drawIllustrationBox(c, cx, boxY, 740f, 360f);

                // Target ghost on left, token on right with tether
                float targetX = cx - 140f;
                float targetY = boxY + 180f;
                float tokenX = cx + 110f;
                float tokenY = boxY + 180f;

                // Target ghost outline
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(3.0f);
                stroke.setColor(0xFFB87820);
                c.drawCircle(targetX, targetY, 48f, stroke);

                // Magnetic tether line
                stroke.setStrokeWidth(2.8f);
                stroke.setColor(0xFFE5A836);
                c.drawLine(tokenX, tokenY, targetX, targetY, stroke);
                stroke.setStyle(Paint.Style.FILL);

                // Resonant Token with glowing aura
                fill.setColor(0x55FFD54F);
                c.drawCircle(tokenX, tokenY, 64f, fill);
                fill.setColor(0xFF4A301E);
                c.drawCircle(tokenX, tokenY, 46f, fill);
                fill.setColor(0xFFFFD54F);
                c.drawCircle(tokenX, tokenY, 10f, fill);

                // Explanation
                text.setTextSize(23f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText("Magnetic resonance guides every alignment.", cx, topY + 475f, text);

                text.setTextSize(19f);
                text.setTypeface(SERIF_NORMAL);
                text.setColor(0xDDFFE0A8);
                c.drawText("Tokens emit a radiant golden aura when close to target.", cx, topY + 518f, text);
                c.drawText("An animated magnetic flux tether shows the exact solution point.", cx, topY + 556f, text);
                c.drawText("Release your finger to automatically snap into perfect lock!", cx, topY + 594f, text);
                break;
            }

            case 3: { // Constellation & Stars
                text.setTextSize(30f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xCC000000);
                c.drawText("IV. ASTRAL REVEAL & 3 STARS", cx, topY + 21.5f, text);
                text.setColor(0xFFFFD54F);
                c.drawText("IV. ASTRAL REVEAL & 3 STARS", cx, topY + 20f, text);

                float boxY = topY + 50f;
                drawIllustrationBox(c, cx, boxY, 740f, 360f);

                // Star polygon reveal
                float midY = boxY + 150f;
                drawConstellationStar(c, cx, midY, 64f);

                // 3 Stars rating underneath
                float starSpacing = 64f;
                for (int i = 0; i < 3; i++) {
                    float sx = cx + (i - 1) * starSpacing;
                    drawSmallStar(c, sx, boxY + 295f, 20f, true);
                }

                // Explanation
                text.setTextSize(23f);
                text.setTypeface(SERIF_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText("Master trials with speed and efficiency.", cx, topY + 475f, text);

                text.setTextSize(19f);
                text.setTypeface(SERIF_NORMAL);
                text.setColor(0xDDFFE0A8);
                c.drawText("Reach 90% match to weave the silhouette and win.", cx, topY + 518f, text);
                c.drawText("\u2605 1 Star: Silhouette solved   \u2605\u2605 2 Stars: Solved under par time", cx, topY + 556f, text);
                c.drawText("\u2605\u2605\u2605 3 Stars: Solved under par time AND within par moves!", cx, topY + 594f, text);
                break;
            }
        }
    }

    private static void drawIllustrationBox(Canvas c, float cx, float y, float w, float h) {
        float l = cx - w * 0.5f;
        float r = cx + w * 0.5f;
        float b = y + h;

        fill.setColor(0xFFEADBCE);
        c.drawRoundRect(l, y, r, b, 18f, 18f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.4f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(l, y, r, b, 18f, 18f, stroke);

        // Specular top highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66FFFFFF);
        c.drawLine(l + 18f, y + 1.5f, r - 18f, y + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);
    }

    private static void drawConstellationStar(Canvas c, float cx, float cy, float r) {
        fill.setColor(0xDDFFE082);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.5f);
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

        // Center white star burst
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(cx, cy, 6f, fill);
    }

    private static void drawSmallStar(Canvas c, float cx, float cy, float r, boolean filled) {
        GameIcons.drawStar(c, cx, cy, r, filled);
        if (filled) {
            fill.setColor(0xFFFFFFFF);
            c.drawCircle(cx, cy - 2f, 3.5f, fill);
        }
    }

    private static void drawCornerBrackets(Canvas c, float l, float t, float r, float b) {
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);

        float len = 22f;
        c.drawLine(l + 10f, t + 10f, l + 10f + len, t + 10f, stroke);
        c.drawLine(l + 10f, t + 10f, l + 10f, t + 10f + len, stroke);

        c.drawLine(r - 10f, t + 10f, r - 10f - len, t + 10f, stroke);
        c.drawLine(r - 10f, t + 10f, r - 10f, t + 10f + len, stroke);

        c.drawLine(l + 10f, b - 10f, l + 10f + len, b - 10f, stroke);
        c.drawLine(l + 10f, b - 10f, l + 10f, b - 10f - len, stroke);

        c.drawLine(r - 10f, b - 10f, r - 10f - len, b - 10f, stroke);
        c.drawLine(r - 10f, b - 10f, r - 10f, b - 10f - len, stroke);

        stroke.setStyle(Paint.Style.FILL);
    }
}
