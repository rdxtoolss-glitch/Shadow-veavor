package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
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

    private static float cardL;
    private static float cardT;
    private static float cardR;
    private static float cardB;

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
        float cardW = 820f;
        float cardH = 1040f;
        cardL = cx - cardW * 0.5f;
        cardT = Math.max(120f, (sceneH - (cardH + 110f)) * 0.5f);
        cardR = cardL + cardW;
        cardB = cardT + cardH;

        float btnW = 240f;
        float btnH = 76f;
        float btnY = cardB - 110f;

        btnPrev.setBounds(cardL + 40f, btnY, cardL + 40f + btnW, btnY + btnH);
        btnPrev.cornerRadius = 18f;

        btnNext.setBounds(cardR - 40f - btnW, btnY, cardR - 40f, btnY + btnH);
        btnNext.isPrimary = true;
        btnNext.cornerRadius = 18f;

        float backW = 380f;
        float backH = 72f;
        float backY = cardB + 36f;
        btnBack.setBounds(cx - backW * 0.5f, backY, cx + backW * 0.5f, backY + backH);
        btnBack.cornerRadius = 18f;

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

        // 1. Celestial Twilight Backdrop Gradient (Fills 100% of the screen)
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
        c.drawText("HOW TO PLAY", cx, 97f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText("HOW TO PLAY", cx, 95f, text);

        text.setTextSize(20f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xCC000000);
        c.drawText("THE PRINCIPLES OF ASTRAL PERSPECTIVE", cx, 142f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("THE PRINCIPLES OF ASTRAL PERSPECTIVE", cx, 140f, text);

        // 3. Frosted Obsidian Guide Card
        fill.setColor(0xFA1A2234);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.8f);
        stroke.setColor(0xFFD4AF37);
        c.drawRoundRect(cardL, cardT, cardR, cardB, 24f, 24f, stroke);

        // Top specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66FFFFFF);
        c.drawLine(cardL + 24f, cardT + 1.5f, cardR - 24f, cardT + 1.5f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // 4. Page Content
        drawCardContent(c, cx, cardT + 45f, currentPage, time);

        // 5. Page Dots Indicator
        float dotSpacing = 36f;
        float dotStartX = cx - (TOTAL_PAGES - 1) * dotSpacing * 0.5f;
        float dotY = cardB - 148f;

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
            btnNext.label = "LET'S PLAY";
        } else {
            btnNext.label = "NEXT";
        }

        btnBack.label = "MAIN MENU";

        btnPrev.draw(c, fill, stroke, text, 1.0f);
        btnNext.draw(c, fill, stroke, text, 1.0f);
        btnBack.draw(c, fill, stroke, text, 1.0f);

        // 7. Grounding footer
        text.setTextSize(16f);
        text.setColor(0x77D4AF37);
        c.drawText("Swipe or tap Next to navigate chapters", cx, sh - 45f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawCardContent(Canvas c, float cx, float topY, int page, float time) {
        text.setTextAlign(Paint.Align.CENTER);

        switch (page) {
            case 0: { // The Candle & Scale
                text.setTextSize(32f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xCC000000);
                c.drawText("I. DISTANCE & SCALE", cx, topY + 21.5f, text);
                text.setColor(0xFFFFD54F);
                c.drawText("I. DISTANCE & SCALE", cx, topY + 20f, text);

                // Luminous parchment illustration box
                float boxY = topY + 55f;
                drawIllustrationBox(c, cx, boxY, 740f, 380f);

                // Draw Candle at top
                fill.setColor(0xFFFFB74D);
                c.drawCircle(cx, boxY + 55f, 18f, fill);
                fill.setColor(0xFFFFFFFF);
                c.drawCircle(cx, boxY + 53f, 7f, fill);

                // Token oscillating vertically
                float bounce = (float) Math.sin(time * 3f) * 32f;
                float tokenY = boxY + 180f + bounce;

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
                float shadowScale = 1.0f + ((boxY + 180f) - tokenY) * 0.022f;
                fill.setColor(0xDD140A04);
                float sw = 64f * shadowScale;
                float sh = 26f * shadowScale;
                c.drawRoundRect(cx - sw, boxY + 340f - sh, cx + sw, boxY + 340f, 12f, 12f, fill);

                // Explanation Text
                text.setTextSize(24f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText("Shadows obey physical optics and distance.", cx, topY + 500f, text);

                text.setTextSize(20f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xDDFFE0A8);
                c.drawText("Drag a token UP toward the candle flame to ENLARGE its shadow.", cx, topY + 545f, text);
                c.drawText("Drag it DOWN away from the flame to SHRINK the shadow.", cx, topY + 585f, text);
                c.drawText("Watch the soft penumbra edge blur physically with elevation.", cx, topY + 625f, text);
                break;
            }

            case 1: { // Rotation & Orientation
                text.setTextSize(32f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xCC000000);
                c.drawText("II. MECHANICAL ROTATION", cx, topY + 21.5f, text);
                text.setColor(0xFFFFD54F);
                c.drawText("II. MECHANICAL ROTATION", cx, topY + 20f, text);

                float boxY = topY + 55f;
                drawIllustrationBox(c, cx, boxY, 740f, 380f);

                // Rotary dial demonstration
                float dialR = 95f;
                float rotAngle = time * 45f;
                float knobX = cx + dialR * (float) Math.cos(Math.toRadians(rotAngle));
                float knobY = (boxY + 190f) + dialR * (float) Math.sin(Math.toRadians(rotAngle));

                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.5f);
                stroke.setColor(0x889E7840);
                c.drawCircle(cx, boxY + 190f, dialR, stroke);

                // Rotating token
                int s = c.save();
                c.translate(cx, boxY + 190f);
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
                c.drawLine(cx, boxY + 190f, knobX, knobY, stroke);
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
                text.setTextSize(24f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText("Precision angle alignment and orientation.", cx, topY + 500f, text);

                text.setTextSize(20f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xDDFFE0A8);
                c.drawText("Spin tokens using the outer tactile amber rotary knob,", cx, topY + 545f, text);
                c.drawText("or place two fingers anywhere on screen to pinch and twist.", cx, topY + 585f, text);
                c.drawText("Haptic clicks pulse smoothly every 15 degrees of rotation.", cx, topY + 625f, text);
                break;
            }

            case 2: { // Magnetic Resonance & Snap
                text.setTextSize(32f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xCC000000);
                c.drawText("III. RESONANCE & SNAP", cx, topY + 21.5f, text);
                text.setColor(0xFFFFD54F);
                c.drawText("III. RESONANCE & SNAP", cx, topY + 20f, text);

                float boxY = topY + 55f;
                drawIllustrationBox(c, cx, boxY, 740f, 380f);

                // Target ghost on left, token on right with tether
                float targetX = cx - 140f;
                float targetY = boxY + 190f;
                float tokenX = cx + 110f;
                float tokenY = boxY + 190f;

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
                text.setTextSize(24f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText("Magnetic resonance guides every alignment.", cx, topY + 500f, text);

                text.setTextSize(20f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xDDFFE0A8);
                c.drawText("Tokens emit a radiant golden aura when close to target.", cx, topY + 545f, text);
                c.drawText("An animated magnetic flux tether shows the exact solution point.", cx, topY + 585f, text);
                c.drawText("Release your finger to automatically snap into perfect lock!", cx, topY + 625f, text);
                break;
            }

            case 3: { // Constellation & Stars
                text.setTextSize(32f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xCC000000);
                c.drawText("IV. ASTRAL REVEAL & 3 STARS", cx, topY + 21.5f, text);
                text.setColor(0xFFFFD54F);
                c.drawText("IV. ASTRAL REVEAL & 3 STARS", cx, topY + 20f, text);

                float boxY = topY + 55f;
                drawIllustrationBox(c, cx, boxY, 740f, 380f);

                // Star polygon reveal
                float midY = boxY + 160f;
                drawConstellationStar(c, cx, midY, 68f);

                // 3 Stars rating underneath
                float starSpacing = 64f;
                for (int i = 0; i < 3; i++) {
                    float sx = cx + (i - 1) * starSpacing;
                    drawSmallStar(c, sx, boxY + 310f, 20f, true);
                }

                // Explanation
                text.setTextSize(24f);
                text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setColor(0xFFFFFDF8);
                c.drawText("Master trials with speed and efficiency.", cx, topY + 500f, text);

                text.setTextSize(20f);
                text.setTypeface(Typeface.DEFAULT);
                text.setColor(0xDDFFE0A8);
                c.drawText("Reach 85% match to weave the silhouette and win.", cx, topY + 545f, text);
                c.drawText("⭐ 1 Star: Silhouette solved   ⭐⭐ 2 Stars: Solved under par time", cx, topY + 585f, text);
                c.drawText("⭐⭐⭐ 3 Stars: Solved under par time AND within par moves!", cx, topY + 625f, text);
                break;
            }
        }
    }

    private static void drawIllustrationBox(Canvas c, float cx, float y, float w, float h) {
        float l = cx - w * 0.5f;
        float r = cx + w * 0.5f;
        float b = y + h;

        // Luminous warm studio limestone canvas
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
        fill.setColor(filled ? 0xFFFFD54F : 0x44607080);
        c.drawPath(path, fill);

        if (filled) {
            fill.setColor(0xFFFFFFFF);
            c.drawCircle(cx, cy - 2f, 3.5f, fill);
        }
    }
}
