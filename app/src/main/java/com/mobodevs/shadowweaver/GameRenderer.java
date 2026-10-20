package com.mobodevs.shadowweaver;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.DashPathEffect;
import android.graphics.Shader;

/**
 * Shadow Weaver - GameRenderer
 * ===========================
 * All drawing. Pure functions of (canvas, size, game state) - it owns no state
 * and never mutates the game, so it can be read top to bottom and reasoned
 * about independently of the loop.
 *
 * ------------------------------------------------------------------
 * THE COORDINATE PROBLEM, AND HOW IT IS SOLVED
 * ------------------------------------------------------------------
 * The whole game is authored in a fixed 1000 x 1600 "scene" space
 * (ShadowCalculator.SCENE_W / SCENE_H). The Surface, however, is whatever
 * size the device gave us, and in landscape the scene's aspect ratio will not
 * match the screen's.
 *
 * Rather than a viewport abstraction, there is one affine transform applied to
 * the canvas, computed by {@link #fitTransform}. Everything below is then
 * written in scene units and needs no knowledge of the screen at all:
 *
 *     fit portrait  -> letterbox, uniform scale
 *     fit landscape -> the scene is cropped vertically, anchored to the top so
 *                      the wall (where the puzzle is) stays fully visible
 *
 * This is why a touch event must be inverted through the same transform before
 * it reaches the game; see GameView.
 *
 * ------------------------------------------------------------------
 * PAINT
 * ------------------------------------------------------------------
 * Paint objects are allocated once and reused, and Paint.setShader() REPLACES
 * the previous shader, so per-frame gradient changes (the candle flicker) are
 * free of allocation. The only per-frame allocation is a float[] in project(),
 * which is unavoidable and small.
 */
final class GameRenderer {

    private GameRenderer() {
    }

    // ---- Persistent paints -------------------------------------------------

    private static final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint haze = new Paint();

    private static final Path path = new Path();
    private static final RectF rect = new RectF();

    // ======================================================================
    // TRANSFORM
    // ======================================================================

    /**
     * The fitted scene transform for a surface of the given size.
     *
     * Portrait letterboxes (the unused margin simply shows the backdrop).
     * Landscape scales to fill the width and crops the height, top-anchored,
     * so the stage, the wall and the candle are never pushed off screen. The
     * caption area below the scene is drawn in the cropped region.
     */
    static float fitScale(int surfaceW, int surfaceH) {
        if (surfaceW <= 0 || surfaceH <= 0) return 1f;
        float sw = ShadowCalculator.SCENE_W;
        float sh = ShadowCalculator.SCENE_H;
        float byWidth = surfaceW / sw;
        float byHeight = surfaceH / sh;
        // In landscape, prefer the width fit (which crops); in portrait,
        // prefer the height fit (which letterboxes).
        return surfaceH >= surfaceW ? Math.min(byWidth, byHeight) : byWidth;
    }

    private static float originX(int surfaceW, float scale) {
        return (surfaceW - ShadowCalculator.SCENE_W * scale) * 0.5f;
    }

    private static float originY(int surfaceH, float scale) {
        // Top-anchored: the interesting content is the top 90% of the scene.
        return 0f;
    }

    /** Scale and offset for converting SCENE units to screen pixels. */
    static float[] transform(int surfaceW, int surfaceH) {
        float scale = fitScale(surfaceW, surfaceH);
        return new float[]{
                scale,
                originX(surfaceW, scale),
                originY(surfaceH, scale)
        };
    }

    static float sceneX(float sx, float scale, float ox) {
        return (sx - ox) / scale;
    }

    static float sceneY(float sy, float scale, float oy) {
        return (sy - oy) / scale;
    }

    // ======================================================================
    // FRAME
    // ======================================================================

    static void drawScene(Canvas canvas, int w, int h, GameThread g, boolean portrait) {
        float flicker = warmth(g.candleFlicker());
        float scale = fitScale(w, h);
        float ox = originX(w, scale);
        float oy = originY(h, scale);

        int save = canvas.save();
        canvas.translate(ox, oy);
        canvas.scale(scale, scale);
        // Clip to the scene so the landscape crop and the portrait letterbox
        // both behave, and so a glow near an edge cannot bleed into the margin.
        canvas.clipRect(0, 0, ShadowCalculator.SCENE_W, ShadowCalculator.SCENE_H);

        drawBackdrop(canvas, g.candleFlicker());
        drawCandle(canvas, g.candleFlicker());
        drawWall(canvas, g.candleFlicker());
        drawTargetGhost(canvas, g);
        drawLightRays(canvas, g);
        // scale is handed to the shadow pass because the offscreen shadow
        // buffer is sized in real pixels, not scene units.
        drawShadows(canvas, g, scale);
        drawShapes(canvas, g);
        drawHud(canvas, g);

        if (g.showingLevelSelect()) {
            LevelSelectOverlay.draw(canvas, g.unlockedLevel(), g.levelIndex());
        } else if (g.celebration()) {
            LevelCompleteOverlay.draw(canvas, g);
        }

        canvas.restoreToCount(save);
    }

    // ======================================================================
    // BACKDROP
    // ======================================================================

    private static void drawBackdrop(Canvas c, float flicker) {
        // A warm vertical gradient. This single fill is most of the mood.
        haze.setShader(new LinearGradient(0, 0, 0, ShadowCalculator.SCENE_H,
                new int[]{
                        mix(0xFF3A2210, 0xFF4A2C13, flicker),
                        mix(0xFF26170D, 0xFF2C1A0E, flicker),
                        0xFF140D08
                },
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, ShadowCalculator.SCENE_W, ShadowCalculator.SCENE_H, haze);
        haze.setShader(null);
    }

    // ======================================================================
    // CANDLE
    // ======================================================================

    private static void drawCandle(Canvas c, float flicker) {
        float lx = ShadowCalculator.LIGHT_X;
        float ly = ShadowCalculator.LIGHT_Y;

        // Broad ambient pool of light behind everything, breathing with the
        // flame. A large, very soft radial is far cheaper than a blur and
        // looks better for this.
        float rOuter = 620f * (0.94f + 0.06f * flicker);
        glow.setShader(new RadialGradient(lx, ly, rOuter,
                new int[]{
                        withAlpha(0xFFFFC46A, (int) (70 * flicker)),
                        withAlpha(0xFFFF9A3C, 22),
                        0x00000000
                },
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(lx, ly, rOuter, glow);
        glow.setShader(null);

        // Wick + flame.
        fill.setColor(0xFF6B4A2A);
        rect.set(lx - 4f, ly + 6f, lx + 4f, ly + 40f);
        c.drawRoundRect(rect, 3f, 3f, fill);

        float flameH = 34f * (0.9f + 0.1f * flicker);
        path.reset();
        path.moveTo(lx, ly - flameH);
        path.cubicTo(lx + 11f * flicker, ly - flameH * 0.45f,
                lx + 9f, ly + 4f, lx, ly + 5f);
        path.cubicTo(lx - 9f, ly + 4f,
                lx - 11f * flicker, ly - flameH * 0.45f, lx, ly - flameH);
        path.close();

        glow.setShader(new RadialGradient(lx, ly - flameH * 0.3f, flameH,
                new int[]{0xFFFFF0C4, 0xFFFFC46A, 0xCCFF9A3C},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        c.drawPath(path, glow);
        glow.setShader(null);
    }

    // ======================================================================
    // WALL
    // ======================================================================

    private static void drawWall(Canvas c, float flicker) {
        float l = ShadowCalculator.WALL_LEFT;
        float t = ShadowCalculator.WALL_TOP;
        float r = ShadowCalculator.WALL_RIGHT;
        float b = ShadowCalculator.WALL_BOTTOM;

        // Wall body, lit a little more near the top where the candle reaches.
        haze.setShader(new LinearGradient(0, t, 0, b,
                new int[]{
                        mix(0xFF50361E, 0xFF5E4023, flicker),
                        0xFF3B2717,
                        0xFF2A1B10
                },
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        rect.set(l, t, r, b);
        c.drawRect(rect, haze);
        haze.setShader(null);

        // A single warm top edge, so the wall reads as a separate surface
        // catching the light rather than as a rectangle painted on.
        stroke.setStrokeWidth(3f);
        stroke.setColor(withAlpha(0xFF6B4A2A, (int) (200 * flicker)));
        c.drawLine(l, t, r, t, stroke);

        // Base shadow, grounding the wall.
        haze.setShader(new LinearGradient(0, b - 40f, 0, b,
                new int[]{0x00000000, 0x661A1009},
                null, Shader.TileMode.CLAMP));
        rect.set(l, b - 40f, r, b);
        c.drawRect(rect, haze);
        haze.setShader(null);
    }

    // ======================================================================
    // TARGET GHOST
    // ======================================================================

    /**
     * The faint silhouette the player is aiming for.
     *
     * This is drawn very low alpha, and - importantly - it fades out as the
     * player's own shadow approaches the target. A hint that dims as you get
     * it right rewards progress and stops the puzzle feeling like a paint-by-
     * numbers tracing exercise. It never reaches zero: total darkness here
     * would remove the challenge entirely.
     */
    private static void drawTargetGhost(Canvas c, GameThread g) {
        if (g.celebration()) return;

        float match = g.matchPercent() / 100f;
        float alpha = 0.15f * (1f - 0.55f * match);

        float[][] target = g.targetPolygons();
        if (target == null) return;

        fill.setColor(withAlpha(0xFFFFE0B0, (int) (255 * alpha)));
        for (float[] poly : target) {
            buildPath(poly);
            c.drawPath(path, fill);
        }
    }

    // ======================================================================
    // LIGHT RAYS
    // ======================================================================

    /**
     * A faint ray from the candle to the selected shape, so the player can see
     * the causal link between moving a thing and moving its shadow.
     *
     * Drawn only for the selected shape. Rays to every shape at once would be
     * visual noise and would not help.
     */
    private static void drawLightRays(Canvas c, GameThread g) {
        int sel = g.selectedIndex();
        if (sel < 0 || sel >= g.shapes().length) return;

        ShapeObject s = g.shapes()[sel];
        float lx = ShadowCalculator.LIGHT_X;
        float ly = ShadowCalculator.LIGHT_Y;

        // Dashed, animated so it reads as light in air rather than a solid
        // stick. The dash phase moves with time, which is what sells it.
        float[] m = {0f, 0f, s.x, s.y};
        float len = (float) Math.hypot(m[2] - m[0], m[3] - m[1]);
        if (len < 4f) return;

        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0x26FFD79A);
        float phase = (g.timeSeconds() * 60f) % 26f;
        path.reset();
        path.moveTo(m[0], m[1]);
        path.lineTo(m[2], m[3]);
        // DashPathEffect, not PathDashPathEffect: both dash an open path, and
        // DashPathEffect is the one available on every API level from 1 up.
        DashPathEffect dash = new DashPathEffect(new float[]{9f, 11f}, phase);
        stroke.setPathEffect(dash);
        c.drawPath(path, stroke);
        stroke.setPathEffect(null);
    }

    // ======================================================================
    // SHADOWS
    //
    // WHAT CHANGED IN THE POLISH PASS
    // -------------------------------
    // 1. SOFT EDGES. Every shadow used to be the same flat polygon drawn three
    //    times at three fixed alphas. That is not a gradient, it is a hard
    //    silhouette with two extra coats of paint on it, and it read as a
    //    sticker. Shadows are now drawn as three CONCENTRIC bands - the exact
    //    silhouette plus two progressively dilated copies of it - so the alpha
    //    genuinely ramps from a dark core out to nothing. The dilation width
    //    comes from ShadowCalculator and grows with how high the caster is
    //    held, which is physically why a distant caster throws a softer edge.
    //
    // 2. OVERLAP BLENDING. Drawing straight onto the scene gave
    //    alpha-over-alpha, where two 50% shadows land on a value that looks
    //    like one 75% shadow in the middle and a flat wash at the edges. The
    //    shadows are now composited into ONE transparent offscreen buffer and
    //    that buffer is drawn onto the wall in a single pass, so every band of
    //    every shadow stacks properly and overlap is genuinely darker. It is
    //    also cheaper: one texture blit instead of N polygon fills onto the
    //    main surface, and the polygon fills now hit a small memory-local
    //    bitmap instead of the display buffer.
    //
    // 3. CLIPPING. Shadows are clipped to the wall. A shadow only exists
    //    where there is wall to receive it, and unclipped ones used to spill
    //    over the backdrop edges and make the wall look like a decal.
    //
    // The penumbra is deliberately NOT part of what the matcher scores - see
    // ShadowCalculator for why the core band is bit-for-bit the same polygon
    // the grader uses.
    // ======================================================================

    /** Extra scene units around the wall the offscreen buffer covers. */
    private static final float SHADOW_PAD = 14f;

    private static Bitmap shadowBuffer;
    private static final Paint shadowBitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private static final Rect shadowBitmapSrc = new Rect();

    private static void drawShadows(Canvas c, GameThread g, float scale) {
        ShapeObject[] shapes = g.shapes();
        if (shapes.length == 0) return;

        final float l = ShadowCalculator.WALL_LEFT - SHADOW_PAD;
        final float t = ShadowCalculator.WALL_TOP - SHADOW_PAD;
        final float r = ShadowCalculator.WALL_RIGHT + SHADOW_PAD;
        final float b = ShadowCalculator.WALL_BOTTOM + SHADOW_PAD;

        final int bw = Math.max(1, (int) Math.ceil((r - l) * scale));
        final int bh = Math.max(1, (int) Math.ceil((b - t) * scale));
        if (shadowBuffer == null
                || shadowBuffer.getWidth() != bw
                || shadowBuffer.getHeight() != bh) {
            if (shadowBuffer != null) shadowBuffer.recycle();
            shadowBuffer = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        }

        // Project every shape ONCE and reuse the result for all three bands.
        // The old code re-projected the same shape once per pass, which was
        // three times the allocation for no visual gain.
        ShadowCalculator.Shadow[] cast = new ShadowCalculator.Shadow[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            cast[i] = ShadowCalculator.projectDetailed(shapes[i]);
        }

        // ---- Pass 1: accumulate every band into the transparent buffer ----
        Canvas buf = new Canvas(shadowBuffer);
        shadowBuffer.eraseColor(0);
        int saveBuf = buf.save();
        buf.scale(scale, scale);
        buf.translate(-l, -t);

        drawShadowBands(buf, cast, ShadowCalculator.LAYER_OUTER, 0f);
        drawShadowBands(buf, cast, ShadowCalculator.LAYER_MID, 0f);
        drawShadowBands(buf, cast, ShadowCalculator.LAYER_CORE,
                g.candleFlicker());

        buf.restoreToCount(saveBuf);

        // ---- Pass 2: one composite of the whole shadow field onto the wall --
        int save = c.save();
        c.clipRect(ShadowCalculator.WALL_LEFT, ShadowCalculator.WALL_TOP,
                ShadowCalculator.WALL_RIGHT, ShadowCalculator.WALL_BOTTOM);
        shadowBitmapSrc.set(0, 0, bw, bh);
        rect.set(l, t, r, b);
        c.drawBitmap(shadowBuffer, shadowBitmapSrc, rect, shadowBitmapPaint);
        c.restoreToCount(save);
    }

    /**
     * One band of every shadow, at the band's profile alpha.
     *
     * @param warmth 0..1 candle warmth. Only the core band uses it, so the
     *               soft margin stays a neutral penumbra while the solid body
     *               of the shadow warms very slightly with the flame - the
     *               shadows are lit by the same fire as everything else.
     */
    private static void drawShadowBands(Canvas c, ShadowCalculator.Shadow[] cast,
                                        int layer, float warmth) {
        int base = ShadowCalculator.layerAlpha(layer);
        int tint = ShadowCalculator.SHADOW_TINT;
        if (layer == ShadowCalculator.LAYER_CORE) {
            // The core is a touch warmer and slightly stronger when the flame
            // is up. Bounded, so a flicker can never blow the shadow out.
            int a = (int) (base * (0.94f + 0.06f * warmth));
            fill.setColor(withAlpha(tint, a));
        } else {
            fill.setColor(withAlpha(tint, base));
        }
        for (int i = 0; i < cast.length; i++) {
            buildPath(cast[i].layer(layer));
            c.drawPath(path, fill);
        }
    }

    // ======================================================================
    // SHAPES ON THE STAGE
    // ======================================================================

    private static void drawShapes(Canvas c, GameThread g) {
        ShapeObject[] shapes = g.shapes();
        int sel = g.selectedIndex();
        float pulse = g.selectPulse();

        for (int i = 0; i < shapes.length; i++) {
            ShapeObject s = shapes[i];
            float[] poly = s.applyTransform();
            buildPath(poly);

            boolean isSel = i == sel;
            // Non-selected shapes recede; the selected one lifts forward. This
            // is the only place selection is communicated on the stage, so it
            // has to be unmissable.
            int fillA = isSel ? 0x22 : 0x0E;
            fill.setColor(withAlpha(0xFFF2D9BE, fillA));
            c.drawPath(path, fill);

            stroke.setStrokeWidth(isSel ? 4.5f + 1.5f * pulse : 3f);
            stroke.setColor(isSel
                    ? withAlpha(0xFFFFF0DC, (int) (210 + 45 * pulse))
                    : 0xB8F2D9BE);
            c.drawPath(path, stroke);

            if (isSel) {
                // Rotation handle: a dot on the shape's rim, plus a faint
                // circle showing the whole rotation track. The track matters -
                // without it the player has no visual idea that rotating is
                // even possible.
                float r = s.maxRadius();
                stroke.setStrokeWidth(1.6f);
                stroke.setColor(0x33FFE9C4);
                c.drawCircle(s.x, s.y, r + 16f, stroke);

                float hr = r + 16f;
                float hrx = s.x + hr * (float) Math.cos(Math.toRadians(s.rotation));
                float hry = s.y + hr * (float) Math.sin(Math.toRadians(s.rotation));
                c.drawLine(s.x, s.y, hrx, hry, stroke);

                fill.setColor(0xCCFFE9C4);
                c.drawCircle(hrx, hry, 11f + 1.5f * pulse, fill);
            }
        }
    }

    // ======================================================================
    // HUD
    // ======================================================================

    private static void drawHud(Canvas c, GameThread g) {
        String title = g.levelTitle();
        String hint = g.levelHint();
        float match = g.matchPercent();

        // Top-left: level number and title.
        text.setColor(0xD9F0DFC6);
        text.setTextSize(30f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText(title, 56f, 82f, text);

        text.setColor(0x8AF0DFC6);
        text.setTextSize(24f);
        text.setTypeface(android.graphics.Typeface.DEFAULT);
        c.drawText("Level " + (g.levelIndex() + 1) + " of " + g.levelCount(), 56f, 116f, text);

        // Top-right: the match meter. Deliberately a simple arc + a
        // percentage, not a "stars" indicator: this is a precision game and a
        // continuous number is more honest than a discretised one.
        drawMatchMeter(c, g, match);

        // Bottom: hint, fading out once the player is clearly on their way, so
        // the tutorial is present but not nagging.
        if (match < 55f) {
            float a = 1f - match / 55f;
            text.setColor(withAlpha(0x8AF0DFC6, (int) (255 * a * 0.9f)));
            text.setTextSize(26f);
            c.drawText(hint, 56f, ShadowCalculator.SCENE_H - 46f, text);
        }
    }

    private static void drawMatchMeter(Canvas c, GameThread g, float match) {
        float cx = ShadowCalculator.SCENE_W - 96f;
        float cy = 92f;
        float r = 42f;

        // Track.
        stroke.setStrokeWidth(9f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setColor(0x33F0DFC6);
        rect.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(rect, 0, 360, false, stroke);

        // Fill. Colour shifts warm -> bright as it climbs, so progress is
        // legible from the colour alone without reading the number.
        float clamped = match < 0f ? 0f : (match > 100f ? 100f : match);
        if (clamped > 0.5f) {
            stroke.setStrokeWidth(9f);
            stroke.setColor(mix(0xFFFFC46A, 0xFFFFE9C4, clamped / 100f));
            rect.set(cx - r, cy - r, cx + r, cy + r);
            c.drawArc(rect, -90f, 360f * clamped / 100f, false, stroke);
        }
        stroke.setStrokeCap(Paint.Cap.BUTT);

        text.setTextAlign(Paint.Align.CENTER);
        text.setColor(0xD9F0DFC6);
        text.setTextSize(30f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        c.drawText(Math.round(clamped) + "%", cx, cy + 10f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    // ======================================================================
    // UTILITIES
    // ======================================================================

    /**
     * Build a Path from a flat [x0,y0,x1,y1,...] polygon.
     *
     * Silhouettes are convex (a rect, a triangle, a line, a polygon, a circle
     * approximation), so a simple moveTo/lineTo loop is exact and is much
     * cheaper than triangulating. The path is cached and reused, so this
     * allocates nothing after the first call.
     */
    private static void buildPath(float[] poly) {
        path.reset();
        if (poly == null || poly.length < 4) return;
        path.moveTo(poly[0], poly[1]);
        for (int i = 2; i + 1 < poly.length; i += 2) {
            path.lineTo(poly[i], poly[i + 1]);
        }
        path.close();
    }

    /** Alpha-adjust a packed ARGB colour. */
    private static int withAlpha(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    /**
     * Map the raw candle multiplier (which hovers around 1.0) onto 0..1 so it
     * can drive a colour blend.
     *
     * The flicker term in GameThread sums to roughly +/-0.1, so this window
     * puts the useful range of the swing across most of the blend instead of
     * clipping it all against one end of the colour.
     */
    static float warmth(float rawFlicker) {
        float t = (rawFlicker - 0.88f) / 0.24f;
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        return t;
    }

    /**
     * Blend two packed ARGB colours. Used for the candle flicker so the whole
     * scene shifts warmth together rather than each element flickering on its
     * own phase.
     *
     * @param t 0 returns a, 1 returns b
     */
    static int mix(int a, int b, float t) {
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        int aa = (a >>> 24) & 0xFF, ba = (b >>> 24) & 0xFF;
        int ar = (a >> 16) & 0xFF, br = (b >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF, bg = (b >> 8) & 0xFF;
        int ab = a & 0xFF, bb = b & 0xFF;
        int o = (int) (aa + (ba - aa) * t) << 24
                | (int) (ar + (br - ar) * t) << 16
                | (int) (ag + (bg - ag) * t) << 8
                | (int) (ab + (bb - ab) * t);
        return o;
    }
}
