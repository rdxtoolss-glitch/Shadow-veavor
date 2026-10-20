package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Shadow Weaver - LevelCompleteOverlay
 * =====================================
 * The win panel, drawn over the solved scene.
 *
 * ------------------------------------------------------------------
 * WHY THIS IS DRAWN, NOT A VIEW
 * ------------------------------------------------------------------
 * A separate Android View would need to be added, measured and laid out on
 * every frame it animates in, and it would sit in a different window from the
 * SurfaceView - so the moment it appeared the shadows behind it would be a
 * frame out of date. Drawing it inside the same Canvas, from the same loop
 * thread, guarantees it is pixel-synchronised with the scene and costs
 * nothing to animate.
 *
 * The interaction contract is therefore tiny: GameView owns the hit testing
 * for the buttons and hands the result to the overlay's action enum. That
 * keeps all pointer logic in one place instead of splitting it across two
 * windows that can disagree about what is under the finger.
 */
final class LevelCompleteOverlay {

    /** What the player tapped. */
    enum Action {
        NONE, NEXT, RETRY, LEVEL_SELECT
    }

    private static final RectF rect = new RectF();

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ---- Button geometry, in SCENE units. Shared by draw() and hitTest()
    // so the two can never disagree about where a button is. ---------------

    private static final float CARD_W = 620f;
    private static final float CARD_H = 380f;

    private static float cardLeft;
    private static float cardTop;
    private static float nextLeft, nextRight, nextTop, nextBottom;
    private static float retryLeft, retryRight, retryTop, retryBottom;
    private static float selectLeft, selectRight, selectTop, selectBottom;

    private static void layout() {
        float cx = ShadowCalculator.SCENE_W * 0.5f;
        cardLeft = cx - CARD_W * 0.5f;
        cardTop = (ShadowCalculator.SCENE_H - CARD_H) * 0.5f;

        float pad = 26f;
        float btnY = cardTop + 208f;
        float btnH = 64f;
        float btnW = 268f;

        nextLeft = cx - btnW - pad * 0.5f;
        nextRight = nextLeft + btnW;
        nextTop = btnY;
        nextBottom = btnY + btnH;

        retryLeft = cx + pad * 0.5f;
        retryRight = retryLeft + btnW;
        retryTop = btnY;
        retryBottom = btnY + btnH;

        // Level select sits full width under the pair.
        selectLeft = cardLeft + 40f;
        selectRight = cardLeft + CARD_W - 40f;
        selectTop = btnBottom() + 20f;
        selectBottom = selectTop + 52f;
    }

    private static float btnBottom() {
        return nextBottom;
    }

    /** Recompute geometry. Called by hitTest() so it works even before a draw. */
    static void ensureLayout() {
        layout();
    }

    // ======================================================================
    // HIT TESTING  (UI thread, SCENE coordinates)
    // ======================================================================

    /**
     * Which button, if any, is at this point.
     *
     * The card background swallows taps so a stray tap in the scrim does not
     * fall through to the scene behind - important because the scene is still
     * live underneath, and a tap landing on a shape would grab it.
     */
    static Action hitTest(float sceneX, float sceneY) {
        layout();

        if (inRect(nextLeft, nextTop, nextRight, nextBottom, sceneX, sceneY)) {
            return Action.NEXT;
        }
        if (inRect(retryLeft, retryTop, retryRight, retryBottom, sceneX, sceneY)) {
            return Action.RETRY;
        }
        if (inRect(selectLeft, selectTop, selectRight, selectBottom, sceneX, sceneY)) {
            return Action.LEVEL_SELECT;
        }
        if (inRect(cardLeft, cardTop, cardLeft + CARD_W, cardTop + CARD_H, sceneX, sceneY)) {
            return Action.NONE;
        }
        // Outside the card: treat as a dismiss tap on the scrim.
        return Action.NONE;
    }

    private static boolean inRect(float l, float t, float r, float b, float x, float y) {
        return x >= l && x <= r && y >= t && y <= b;
    }

    // ======================================================================
    // DRAW  (loop thread)
    // ======================================================================

    /**
     * @param reveal 0..1 progress of the panel's entrance, so it can slide up
     *                and fade in rather than appearing instantly
     */
    static void draw(Canvas c, GameThread g) {
        draw(c, g, 1f);
    }

    static void draw(Canvas c, GameThread g, float reveal) {
        layout();
        if (reveal <= 0f) return;
        if (reveal > 1f) reveal = 1f;

        // Ease-out, so the panel decelerates into place.
        float e = 1f - (1f - reveal) * (1f - reveal);

        // Scrim, so the scene is clearly inactive behind the card. The scrim
        // does not slide - only the card does.
        fill.setColor(withAlpha(0xFF100703, (int) (200 * e)));
        c.drawRect(0, 0, ShadowCalculator.SCENE_W, ShadowCalculator.SCENE_H, fill);

        // Slide the whole card group as one. Translating the canvas is used
        // rather than offsetting every rect, so hitTest() can keep using the
        // static layout and never has to know about the animation.
        int save = c.save();
        c.translate(0f, (1f - e) * 60f);

        // Card.
        fill.setColor(withAlpha(0xFF26170D, (int) (246 * e)));
        rect.set(cardLeft, cardTop, cardLeft + CARD_W, cardTop + CARD_H);
        c.drawRoundRect(rect, 22f, 22f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
        stroke.setColor(withAlpha(0xFF6B4A2A, (int) (220 * e)));
        c.drawRoundRect(rect, 22f, 22f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float textBase = cardTop;

        // Title.
        text.setTextAlign(Paint.Align.CENTER);
        text.setColor(withAlpha(0xFFF0DFC6, (int) (255 * e)));
        text.setTextSize(46f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText("Shadow Matched", cx, textBase + 78f, text);

        // The level name, so the player is reminded what they just solved.
        text.setColor(withAlpha(0x8AF0DFC6, (int) (255 * e)));
        text.setTextSize(27f);
        text.setTypeface(android.graphics.Typeface.DEFAULT);
        c.drawText(g.levelTitle(), cx, textBase + 118f, text);

        // Elapsed time. Shown because it is quietly motivating and because
        // players like seeing it; it is never a fail condition - there is no
        // time limit, and the game's whole pitch is that it cannot be rushed
        // into a mistake.
        long ms = g.elapsedMillis();
        text.setColor(withAlpha(0x8AF0DFC6, (int) (200 * e)));
        text.setTextSize(24f);
        c.drawText(formatTime(ms), cx, textBase + 164f, text);

        // Buttons.
        drawButton(c, "Next", nextLeft, nextTop, nextRight, nextBottom, e, false);
        drawButton(c, "Retry", retryLeft, retryTop, retryRight, retryBottom, e, false);
        drawButton(c, "All levels", selectLeft, selectTop, selectRight, selectBottom, e, true);

        // Final score line, small and quiet.
        text.setColor(withAlpha(0x4DF0DFC6, (int) (255 * e)));
        text.setTextSize(21f);
        c.drawText("level " + (g.levelIndex() + 1) + " / " + g.levelCount(), cx, textBase + CARD_H - 18f, text);

        c.restoreToCount(save);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawButton(Canvas c, String label,
                                   float l, float t, float r, float b,
                                   float e, boolean secondary) {
        fill.setColor(withAlpha(secondary ? 0x1AFFC98A : 0x33FFC98A, (int) (255 * e)));
        rect.set(l, t, r, b);
        c.drawRoundRect(rect, 14f, 14f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(withAlpha(0xFF6B4A2A, (int) (190 * e)));
        c.drawRoundRect(rect, 14f, 14f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        text.setTextAlign(Paint.Align.CENTER);
        text.setColor(withAlpha(0xFFF0DFC6, (int) (235 * e)));
        text.setTextSize(26f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText(label, (l + r) * 0.5f, (t + b) * 0.5f + 9f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static String formatTime(long ms) {
        long total = ms / 1000L;
        long m = total / 60L;
        long s = total % 60L;
        if (m > 0) return m + "m " + (s < 10 ? "0" : "") + s + "s";
        return s + "." + ((ms % 1000L) / 100L) + "s";
    }

    private static int withAlpha(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
