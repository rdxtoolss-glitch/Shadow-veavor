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
    // TRANSFORM & RESPONSIVE PROJECTION
    // ======================================================================

    public static final float HUD_TOP_Y = 118f;
    public static final float HUD_PAUSE_X = 68f;
    public static final float HUD_MATCH_X = 932f;

    public static float getPauseX() {
        return HUD_PAUSE_X;
    }

    public static float getPauseY() {
        return HUD_TOP_Y;
    }

    public static float getHintX() {
        return 120f;
    }

    public static float getHintY(float sceneH) {
        if (sceneH > 1750f) {
            return Math.min(sceneH - 110f, 1750f);
        } else if (sceneH > 1620f) {
            return sceneH - 85f;
        } else {
            return 1565f;
        }
    }

    public static float getResetX() {
        return 880f;
    }

    public static float getResetY(float sceneH) {
        return getHintY(sceneH);
    }

    /**
     * The fitted scene transform for a surface of the given size.
     *
     * In portrait (surfaceH >= surfaceW), maps the full scene width (1000 units)
     * edge-to-edge across the screen, allowing sceneHeight to dynamically expand
     * to eliminate black letterbox bars on modern tall devices (16:9, 19.5:9, 21:9).
     */
    static float fitScale(int surfaceW, int surfaceH) {
        if (surfaceW <= 0 || surfaceH <= 0) return 1f;
        float sw = ShadowCalculator.SCENE_W;
        return surfaceH >= surfaceW ? (surfaceW / sw) : (surfaceH / ShadowCalculator.SCENE_H);
    }

    static float sceneHeight(int surfaceW, int surfaceH) {
        float scale = fitScale(surfaceW, surfaceH);
        return (scale > 0f) ? (surfaceH / scale) : ShadowCalculator.SCENE_H;
    }

    private static float originX(int surfaceW, float scale) {
        return Math.max(0f, (surfaceW - ShadowCalculator.SCENE_W * scale) * 0.5f);
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
        float sceneH = sceneHeight(w, h);
        float ox = originX(w, scale);
        float oy = originY(h, scale);

        // Update ambient particle bounds to span the entire screen
        g.particles().updateDimensions(ShadowCalculator.SCENE_W, sceneH);

        int save = canvas.save();
        canvas.translate(ox, oy);
        canvas.scale(scale, scale);
        // Clip to the true visible scene height (no bottom letterboxing)
        canvas.clipRect(0, 0, ShadowCalculator.SCENE_W, sceneH);

        ScreenState state = g.getScreenState();

        if (state == ScreenState.TITLE) {
            TitleScreen.draw(canvas, g.progress(), g.timeSeconds(), g.candleFlicker(), sceneH);
            g.particles().drawAmbient(canvas, fill, ShadowCalculator.LIGHT_X, ShadowCalculator.LIGHT_Y, g.candleFlicker());
        } else if (state == ScreenState.HOW_TO_PLAY) {
            TutorialScreen.draw(canvas, g.timeSeconds(), sceneH);
            g.particles().drawAmbient(canvas, fill, ShadowCalculator.LIGHT_X, ShadowCalculator.LIGHT_Y, g.candleFlicker());
        } else if (state == ScreenState.CODEX) {
            CodexScreen.draw(canvas, g.progress(), g.timeSeconds(), sceneH);
            g.particles().drawAmbient(canvas, fill, ShadowCalculator.LIGHT_X, ShadowCalculator.LIGHT_Y, g.candleFlicker());
        } else if (state == ScreenState.LEVEL_SELECT) {
            LevelSelectOverlay.draw(canvas, g.progress(), g.levelIndex(), sceneH);
            g.particles().drawAmbient(canvas, fill, ShadowCalculator.LIGHT_X, ShadowCalculator.LIGHT_Y, g.candleFlicker());
        } else if (state == ScreenState.SETTINGS && g.getPreviousScreenState() == ScreenState.TITLE) {
            TitleScreen.draw(canvas, g.progress(), g.timeSeconds(), g.candleFlicker(), sceneH);
            g.particles().drawAmbient(canvas, fill, ShadowCalculator.LIGHT_X, ShadowCalculator.LIGHT_Y, g.candleFlicker());
            SettingsOverlay.draw(canvas, g.progress(), g.audio(), g.haptics(), sceneH);
        } else {
            // In-Game Puzzle Stage (PLAY, PAUSE, LEVEL_COMPLETE, or in-game SETTINGS)
            drawBackdrop(canvas, g.candleFlicker(), sceneH);
            drawStageWorkbench(canvas, g.candleFlicker());
            drawVolumetricLightRays(canvas, g.candleFlicker(), g.timeSeconds());
            drawCandle(canvas, g.candleFlicker());
            g.particles().drawAmbient(canvas, fill, ShadowCalculator.LIGHT_X, ShadowCalculator.LIGHT_Y, g.candleFlicker());
            drawWall(canvas, g.candleFlicker());
            drawTargetGhost(canvas, g);
            drawMagneticTethers(canvas, g);
            drawLightRays(canvas, g);
            // scale is handed to the shadow pass because the offscreen shadow
            // buffer is sized in real pixels, not scene units.
            drawShadows(canvas, g, scale);
            drawShapes(canvas, g);
            g.particles().drawSparks(canvas, fill);
            drawHud(canvas, g, sceneH);

            // Floating Level Introduction Banner
            float bannerAlpha = g.levelBannerAlpha();
            if (bannerAlpha > 0f) {
                drawLevelBanner(canvas, g, bannerAlpha);
            }

            if (state == ScreenState.PAUSE) {
                PauseOverlay.draw(canvas, g, sceneH);
            } else if (state == ScreenState.LEVEL_COMPLETE) {
                LevelCompleteOverlay.draw(canvas, g, g.celebrationReveal(), sceneH);
            } else if (state == ScreenState.SETTINGS) {
                SettingsOverlay.draw(canvas, g.progress(), g.audio(), g.haptics(), sceneH);
            }
        }

        canvas.restoreToCount(save);
    }

    private static void drawLevelBanner(Canvas c, GameThread g, float alpha) {
        if (alpha <= 0f) return;
        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float y = 520f;
        float bw = 520f;
        float bh = 84f;
        rect.set(cx - bw * 0.5f, y - bh * 0.5f, cx + bw * 0.5f, y + bh * 0.5f);

        int a = (int) (230 * alpha);
        fill.setColor(withAlpha(0xF0180F08, a));
        c.drawRoundRect(rect, 18f, 18f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(withAlpha(0xFFFFD54F, (int) (200 * alpha)));
        c.drawRoundRect(rect, 18f, 18f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.setTextSize(28f);
        text.setColor(withAlpha(0xFFFFE9C4, (int) (255 * alpha)));
        c.drawText(g.levelTitle(), cx, y - 6f, text);

        text.setTextSize(18f);
        text.setTypeface(android.graphics.Typeface.DEFAULT);
        text.setColor(withAlpha(0xBBFFE0B0, (int) (240 * alpha)));
        c.drawText("Chapter " + ((g.levelIndex() / 5) + 1) + " • Level " + (g.levelIndex() + 1), cx, y + 22f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    // ======================================================================
    // BACKDROP
    // ======================================================================

    private static void drawBackdrop(Canvas c, float flicker) {
        drawBackdrop(c, flicker, ShadowCalculator.SCENE_H);
    }

    private static void drawBackdrop(Canvas c, float flicker, float sceneH) {
        // Rich celestial chamber gradient: incandescent golden candlelight halo at top,
        // descending into deep celestial midnight sapphire and dark obsidian vignette.
        haze.setShader(new LinearGradient(0, 0, 0, sceneH,
                new int[]{
                        mix(0xFF56341C, 0xFF6E4224, flicker), // Luminous amber warmth near candle
                        0xFF1B2032,                           // Deep celestial midnight indigo chamber
                        0xFF101320                            // Rich obsidian vignette
                },
                new float[]{0f, 0.40f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, ShadowCalculator.SCENE_W, sceneH, haze);
        haze.setShader(null);
    }

    // ======================================================================
    // ASTROLABE STAGE WORKBENCH
    // ======================================================================

    private static void drawStageWorkbench(Canvas c, float flicker) {
        float l = ShadowCalculator.STAGE_LEFT;
        float r = ShadowCalculator.STAGE_RIGHT;
        float t = ShadowCalculator.STAGE_TOP;
        float b = ShadowCalculator.STAGE_BOTTOM;

        // Inlaid dark mahogany & walnut stage surface with warm radial sheen
        rect.set(l, t, r, b);
        haze.setShader(new LinearGradient(l, t, r, b,
                new int[]{0xFF2D1D14, 0xFF38241A, 0xFF22160E},
                null, Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, 16f, 16f, haze);
        haze.setShader(null);

        // Stage border inlay (radiant antique brass)
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(withAlpha(0xFFD4AF37, (int) (180 * flicker)));
        c.drawRoundRect(rect, 16f, 16f, stroke);

        // Corner bracket flourishes
        float cw = 32f;
        drawCornerBracket(c, l, t, cw, 0f);
        drawCornerBracket(c, r, t, cw, 90f);
        drawCornerBracket(c, r, b, cw, 180f);
        drawCornerBracket(c, l, b, cw, 270f);

        // Concentric Astrolabe Coordinate Rings
        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f;

        stroke.setStrokeWidth(1.6f);
        stroke.setColor(withAlpha(0xFFFFE0B0, 65));
        c.drawCircle(cx, cy, 120f, stroke);
        c.drawCircle(cx, cy, 240f, stroke);
        c.drawCircle(cx, cy, 360f, stroke);

        // Outer ring with fine degree ticks
        stroke.setColor(withAlpha(0xFFFFD54F, (int) (120 * flicker)));
        c.drawCircle(cx, cy, 460f, stroke);

        for (int deg = 0; deg < 360; deg += 10) {
            float rad = (float) Math.toRadians(deg);
            boolean major = (deg % 30 == 0);
            float len = major ? 14f : 8f;
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            stroke.setStrokeWidth(major ? 2.2f : 1.2f);
            stroke.setColor(major ? withAlpha(0xFFFFD54F, 180) : withAlpha(0xFFFFE0B0, 80));
            c.drawLine(cx + cos * (460f - len), cy + sin * (460f - len), cx + cos * 460f, cy + sin * 460f, stroke);
        }

        // Cardinal and diagonal compass azimuth rays
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(withAlpha(0xFFFFE0B0, 50));
        for (int deg = 0; deg < 180; deg += 45) {
            float rad = (float) Math.toRadians(deg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            c.drawLine(cx - cos * 460f, cy - sin * 460f, cx + cos * 460f, cy + sin * 460f, stroke);
        }

        stroke.setStyle(Paint.Style.FILL);
    }

    private static void drawCornerBracket(Canvas c, float x, float y, float len, float rotDeg) {
        int save = c.save();
        c.translate(x, y);
        c.rotate(rotDeg);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0x88C5A059);
        path.reset();
        path.moveTo(0f, len);
        path.lineTo(len * 0.4f, len * 0.4f);
        path.lineTo(len, 0f);
        c.drawPath(path, stroke);

        fill.setColor(0xAAC5A059);
        c.drawCircle(len * 0.35f, len * 0.35f, 2.5f, fill);
        c.restoreToCount(save);
    }

    // ======================================================================
    // VOLUMETRIC GOD RAYS
    // ======================================================================

    private static void drawVolumetricLightRays(Canvas c, float flicker, float time) {
        float lx = ShadowCalculator.LIGHT_X;
        float ly = ShadowCalculator.LIGHT_Y;

        // 5 conical volumetric shafts fanning downward across the chamber
        float length = 1100f;
        float halfSpread = 8f;

        glow.setStyle(Paint.Style.FILL);
        for (int k = -2; k <= 2; k++) {
            float baseAngle = 90f + k * 14f;
            float wobble = 2.5f * (float) Math.sin(time * 0.8f + k * 1.3f);
            float centerAngle = baseAngle + wobble;

            float radL = (float) Math.toRadians(centerAngle - halfSpread);
            float radR = (float) Math.toRadians(centerAngle + halfSpread);

            float x1 = lx + length * (float) Math.cos(radL);
            float y1 = ly + length * (float) Math.sin(radL);
            float x2 = lx + length * (float) Math.cos(radR);
            float y2 = ly + length * (float) Math.sin(radR);

            path.reset();
            path.moveTo(lx, ly);
            path.lineTo(x1, y1);
            path.lineTo(x2, y2);
            path.close();

            int rayAlpha = (int) ((k == 0 ? 32 : 22) * flicker);
            glow.setShader(new LinearGradient(lx, ly, lx, ly + length,
                    new int[]{
                            withAlpha(0xFFFFD54F, rayAlpha),
                            withAlpha(0xFFFFB74D, (int) (rayAlpha * 0.6f)),
                            0x00000000
                    },
                    new float[]{0f, 0.4f, 1f},
                    Shader.TileMode.CLAMP));
            c.drawPath(path, glow);
        }
        glow.setShader(null);
    }

    // ======================================================================
    // CANDLE
    // ======================================================================

    private static void drawCandle(Canvas c, float flicker) {
        float lx = ShadowCalculator.LIGHT_X;
        float ly = 165f;

        // Broad ambient pool of light behind everything
        float rOuter = 650f * (0.94f + 0.06f * flicker);
        glow.setShader(new RadialGradient(lx, ly, rOuter,
                new int[]{
                        withAlpha(0xFFFFC46A, (int) (75 * flicker)),
                        withAlpha(0xFFFF9A3C, 24),
                        0x00000000
                },
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        c.drawCircle(lx, ly, rOuter, glow);
        glow.setShader(null);

        // Candle ivory tallow pillar
        fill.setColor(0xFFDFD4C4);
        rect.set(lx - 10f, ly + 14f, lx + 10f, ly + 105f);
        c.drawRoundRect(rect, 4f, 4f, fill);

        // Shaded side of pillar
        fill.setColor(0x332A180C);
        rect.set(lx + 2f, ly + 14f, lx + 10f, ly + 105f);
        c.drawRoundRect(rect, 4f, 4f, fill);

        // Melted wax drips
        fill.setColor(0xFFE8E0D2);
        c.drawCircle(lx - 7f, ly + 36f, 4f, fill);
        c.drawCircle(lx + 7f, ly + 46f, 3.5f, fill);
        c.drawCircle(lx - 6f, ly + 58f, 3f, fill);

        // Brass candleholder dish rim resting directly on stage workbench
        rect.set(lx - 28f, ly + 100f, lx + 28f, ly + 112f);
        fill.setColor(0xFFC5A059);
        c.drawRoundRect(rect, 4f, 4f, fill);

        // Wick
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.5f);
        stroke.setColor(0xFF3E2723);
        c.drawLine(lx, ly + 16f, lx, ly + 4f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Multi-lobe flame
        float flameH = 38f * (0.88f + 0.12f * flicker);
        float flameW = 14f * (0.92f + 0.08f * flicker);

        // 1. Outer warm amber corona
        path.reset();
        path.moveTo(lx, ly - flameH);
        path.cubicTo(lx + flameW * 1.2f * flicker, ly - flameH * 0.45f,
                lx + flameW, ly + 6f, lx, ly + 7f);
        path.cubicTo(lx - flameW, ly + 6f,
                lx - flameW * 1.2f * flicker, ly - flameH * 0.45f, lx, ly - flameH);
        path.close();

        glow.setShader(new RadialGradient(lx, ly - flameH * 0.35f, flameH * 1.1f,
                new int[]{0xFFFFD54F, 0xFFFF8F00, 0x00FF6F00},
                new float[]{0f, 0.6f, 1f},
                Shader.TileMode.CLAMP));
        c.drawPath(path, glow);

        // 2. Inner incandescent white core
        float coreH = flameH * 0.62f;
        float coreW = flameW * 0.52f;
        path.reset();
        path.moveTo(lx, ly - flameH * 0.2f - coreH);
        path.cubicTo(lx + coreW, ly - flameH * 0.2f - coreH * 0.4f,
                lx + coreW * 0.8f, ly + 4f, lx, ly + 4f);
        path.cubicTo(lx - coreW * 0.8f, ly + 4f,
                lx - coreW, ly - flameH * 0.2f - coreH * 0.4f, lx, ly - flameH * 0.2f - coreH);
        path.close();

        fill.setColor(0xFFFFFFFF);
        c.drawPath(path, fill);

        // 3. Blue wick base
        fill.setColor(0xBB3F51B5);
        c.drawCircle(lx, ly + 4f, 4.5f, fill);

        glow.setShader(null);
    }

    // ======================================================================
    // WALL (PROJECTION CANVAS)
    // ======================================================================

    private static void drawWall(Canvas c, float flicker) {
        float l = ShadowCalculator.WALL_LEFT;
        float t = ShadowCalculator.WALL_TOP;
        float r = ShadowCalculator.WALL_RIGHT;
        float b = ShadowCalculator.WALL_BOTTOM;

        // Luminous warm parchment & limestone projection screen.
        // Provides the critical high-contrast backdrop that allows cast shadows
        // to appear rich, deep, and razor-sharp like authentic shadow puppetry.
        haze.setShader(new LinearGradient(0, t, 0, b,
                new int[]{
                        mix(0xFFF7ECDA, 0xFFFFF6E6, flicker), // Luminous candlelit ivory top
                        0xFFE8D6BC,                           // Warm fine parchment middle
                        0xFFCEB89A                            // Softly shaded lower stucco
                },
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        rect.set(l, t, r, b);
        c.drawRoundRect(rect, 14f, 14f, haze);
        haze.setShader(null);

        // Ornate antique brass framing border around projection canvas
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(withAlpha(0xFFC5A059, (int) (220 * flicker)));
        c.drawRoundRect(rect, 14f, 14f, stroke);

        // Top specular golden highlight line
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(withAlpha(0xFFFFE8B8, (int) (240 * flicker)));
        c.drawLine(l + 14f, t + 1.5f, r - 14f, t + 1.5f, stroke);

        // Grounding base drop shadow
        haze.setShader(new LinearGradient(0, b - 32f, 0, b,
                new int[]{0x00000000, 0x442A1608},
                null, Shader.TileMode.CLAMP));
        rect.set(l, b - 32f, r, b);
        c.drawRoundRect(rect, 14f, 14f, haze);
        haze.setShader(null);
        stroke.setStyle(Paint.Style.FILL);
    }

    // ======================================================================
    // TARGET GHOST
    // ======================================================================

    /**
     * The delicate celestial blueprint the player is weaving.
     * Drawn as an authentic alchemist's sepia blueprint that illuminates
     * with radiant golden sunlight as the player's shadow aligns.
     */
    private static void drawTargetGhost(Canvas c, GameThread g) {
        float[][] target = g.targetPolygons();
        if (target == null) return;

        ShapeObject[] shapes = g.shapes();

        if (g.isSolved() || g.celebration()) {
            // Radiant celestial constellation reveal on victory
            fill.setColor(0xDDFFE082);
            for (float[] poly : target) {
                buildPath(poly);
                c.drawPath(path, fill);
            }

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(4.5f);
            stroke.setColor(0xFFFFD54F);
            for (float[] poly : target) {
                buildPath(poly);
                c.drawPath(path, stroke);
            }

            // Draw golden constellation star lines and diamond star vertices
            stroke.setStrokeWidth(2.0f);
            stroke.setColor(0xFFFFF9C4);
            for (float[] poly : target) {
                for (int i = 0; i + 1 < poly.length; i += 2) {
                    float vx = poly[i];
                    float vy = poly[i + 1];
                    fill.setColor(0xFFFFFFFF);
                    c.drawCircle(vx, vy, 4.5f, fill);
                    c.drawLine(vx - 8f, vy, vx + 8f, vy, stroke);
                    c.drawLine(vx, vy - 8f, vx, vy + 8f, stroke);
                }
            }
            stroke.setStyle(Paint.Style.FILL);
            return;
        }

        float match = g.matchPercent() / 100f;
        float baseAlpha = 0.22f * (1f - 0.40f * match);
        if (g.isHintActive()) {
            baseAlpha = 0.48f;
        }

        // Luminous drafting guide on parchment
        fill.setColor(withAlpha(0xFF8D5E32, (int) (255 * baseAlpha)));
        for (float[] poly : target) {
            buildPath(poly);
            c.drawPath(path, fill);
        }

        // Blueprint outline
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(withAlpha(0xFF9E6E28, (int) (255 * (baseAlpha + 0.15f))));
        for (float[] poly : target) {
            buildPath(poly);
            c.drawPath(path, stroke);
        }
        stroke.setStyle(Paint.Style.FILL);

        // Segmented Silhouette Illumination: individual aligned shapes light up!
        if (shapes != null) {
            for (int i = 0; i < Math.min(target.length, shapes.length); i++) {
                ShapeObject s = shapes[i];
                if (s.proximityResonance > 0.35f) {
                    float t = (s.proximityResonance - 0.35f) / 0.65f;
                    fill.setColor(withAlpha(0xFFFFB300, (int) (140 * t)));
                    buildPath(target[i]);
                    c.drawPath(path, fill);

                    stroke.setStyle(Paint.Style.STROKE);
                    stroke.setStrokeWidth(2.8f + 1.2f * t);
                    stroke.setColor(withAlpha(0xFFFFD54F, (int) (220 * t)));
                    c.drawPath(path, stroke);
                    stroke.setStyle(Paint.Style.FILL);
                }
            }
        }

        if (g.isHintActive()) {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.5f);
            stroke.setColor(0xCCFFB300);
            for (float[] poly : target) {
                buildPath(poly);
                c.drawPath(path, stroke);
            }
            stroke.setStyle(Paint.Style.FILL);
        }
    }

    // ======================================================================
    // MAGNETIC TETHERS
    // ======================================================================

    private static void drawMagneticTethers(Canvas c, GameThread g) {
        ShapeObject[] shapes = g.shapes();
        if (shapes == null || g.isSolved()) return;

        stroke.setStyle(Paint.Style.STROKE);
        for (ShapeObject s : shapes) {
            if (s.proximityResonance > 0.25f && s.isDragging) {
                ShapeObject sol = g.findSolutionFor(s);
                if (sol != null) {
                    float t = (s.proximityResonance - 0.25f) / 0.75f;
                    int alpha = (int) (190 * t);
                    stroke.setStrokeWidth(2.5f * t + 1.0f);
                    stroke.setColor(withAlpha(0xFFFFD54F, alpha));

                    float phase = (g.timeSeconds() * 80f) % 20f;
                    DashPathEffect dash = new DashPathEffect(new float[]{8f, 6f}, phase);
                    stroke.setPathEffect(dash);

                    path.reset();
                    path.moveTo(s.x, s.y);
                    path.quadTo((s.x + sol.x) * 0.5f, (s.y + sol.y) * 0.5f - 20f * t, sol.x, sol.y);
                    c.drawPath(path, stroke);

                    stroke.setPathEffect(null);

                    // Glowing target reticle at solution
                    stroke.setStrokeWidth(1.8f);
                    stroke.setColor(withAlpha(0xFFFFD54F, (int) (120 * t)));
                    c.drawCircle(sol.x, sol.y, 14f * (1f - 0.3f * t), stroke);
                }
            }
        }
        stroke.setStyle(Paint.Style.FILL);
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
        float flicker = g.candleFlicker();

        for (int i = 0; i < shapes.length; i++) {
            ShapeObject s = shapes[i];
            boolean isSel = (i == sel);

            int saveShape = c.save();

            // Pseudo-3D velocity tilt
            c.translate(s.x, s.y);
            float tiltSkewX = (float) Math.tan(Math.toRadians(s.tiltY * 0.35f));
            float tiltSkewY = (float) Math.tan(Math.toRadians(s.tiltX * 0.35f));
            c.skew(tiltSkewX, tiltSkewY);
            c.translate(-s.x, -s.y);

            float[] poly = s.applyTransform();
            buildPath(poly);

            // 1. Stage drop shadow (expands with elevation)
            float shadowOffsetY = 12f + s.elevation * 20f;
            float shadowAlpha = 0.25f + s.elevation * 0.18f;
            int saveShadow = c.save();
            c.translate(0, shadowOffsetY);
            fill.setColor(withAlpha(0xFF000000, (int) (255 * shadowAlpha)));
            c.drawPath(path, fill);
            c.restoreToCount(saveShadow);

            // 2. Proximity Resonance Golden Aura
            if (s.proximityResonance > 0.05f) {
                stroke.setStyle(Paint.Style.STROKE);
                float auraWidth = 6f + 8f * s.proximityResonance;
                int auraAlpha = (int) (140 * s.proximityResonance * (0.85f + 0.15f * flicker));
                stroke.setStrokeWidth(auraWidth);
                stroke.setColor(withAlpha(0xFFFFD54F, auraAlpha));
                c.drawPath(path, stroke);
                stroke.setStyle(Paint.Style.FILL);
            }

            // 3. 2.5D Crafted Obsidian & Polished Bronze Body
            int fillA = isSel ? 0xFF : 0xF2;
            int bodyColor = isSel ? 0xFF583A20 : 0xFF3F2A18;
            fill.setColor(withAlpha(bodyColor, fillA));
            c.drawPath(path, fill);

            // Directional bevel shading facing candle (light top-left)
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.8f);
            stroke.setColor(withAlpha(0xFFFFF4D0, isSel ? 240 : 160));
            c.drawPath(path, stroke);

            // Perimeter polished brass border
            stroke.setStrokeWidth(isSel ? 5.0f + 1.5f * pulse : 3.5f);
            stroke.setColor(isSel
                    ? withAlpha(0xFFFFD54F, (int) (240 + 15 * pulse))
                    : withAlpha(0xFFE5B048, 220));
            c.drawPath(path, stroke);
            stroke.setStyle(Paint.Style.FILL);

            // Center brass astrolabe pivot rivet
            fill.setColor(isSel ? 0xFFFFE082 : 0xFFD4AF37);
            c.drawCircle(s.x, s.y, 9f, fill);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.0f);
            stroke.setColor(0xFF1B0E05);
            c.drawCircle(s.x, s.y, 9f, stroke);

            // Engraved star glyph on pivot
            stroke.setStrokeWidth(1.4f);
            stroke.setColor(0xFFFFFFFF);
            c.drawCircle(s.x, s.y, 3.5f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            // 4. Mechanical Precision Rotation Dial
            if (isSel) {
                float r = s.maxRadius();
                float dialR = r + 28f;

                // Circular dial track
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.4f);
                stroke.setColor(0x99FFE0A8);
                c.drawCircle(s.x, s.y, dialR, stroke);

                // Notches: 15° minor, 45° semi, 90° major ticks
                for (int deg = 0; deg < 360; deg += 15) {
                    float rad = (float) Math.toRadians(deg);
                    boolean isMajor = (deg % 90 == 0);
                    boolean isSemi = (deg % 45 == 0);
                    float tickLen = isMajor ? 14f : (isSemi ? 9f : 6f);
                    float t1 = dialR - tickLen * 0.5f;
                    float t2 = dialR + tickLen * 0.5f;
                    float cos = (float) Math.cos(rad);
                    float sin = (float) Math.sin(rad);
                    stroke.setStrokeWidth(isMajor ? 3.0f : 1.6f);
                    stroke.setColor(isMajor ? 0xFFFFF8E1 : (isSemi ? 0xEEFFE082 : 0xAAFFE082));
                    c.drawLine(s.x + cos * t1, s.y + sin * t1, s.x + cos * t2, s.y + sin * t2, stroke);
                }

                // Spoke arm to rotary knob
                float handleAngleRad = (float) Math.toRadians(s.rotation);
                float cosA = (float) Math.cos(handleAngleRad);
                float sinA = (float) Math.sin(handleAngleRad);
                float hrx = s.x + dialR * cosA;
                float hry = s.y + dialR * sinA;

                stroke.setStrokeWidth(2.8f);
                stroke.setColor(0xDDFFE0A8);
                c.drawLine(s.x, s.y, hrx, hry, stroke);
                stroke.setStyle(Paint.Style.FILL);

                // Rotary knob with glowing amber core and brass bezel
                fill.setColor(withAlpha(0xFFFFC46A, (int) (90 + 55 * pulse)));
                c.drawCircle(hrx, hry, 20f + 2f * pulse, fill);

                fill.setColor(0xFFE5B048);
                c.drawCircle(hrx, hry, 13f, fill);
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.4f);
                stroke.setColor(0xFF1B0E05);
                c.drawCircle(hrx, hry, 13f, stroke);
                stroke.setStyle(Paint.Style.FILL);

                fill.setColor(0xFFFFFFFF);
                c.drawCircle(hrx, hry, 5.0f, fill);
            }

            c.restoreToCount(saveShape);
        }
    }

    // ======================================================================
    // HUD
    // ======================================================================

    private static void drawHud(Canvas c, GameThread g) {
        drawHud(c, g, ShadowCalculator.SCENE_H);
    }

    private static void drawHud(Canvas c, GameThread g, float sceneH) {
        String title = g.levelTitle();
        String hint = g.levelHint();
        float match = g.matchPercent();
        float cx = ShadowCalculator.SCENE_W * 0.5f;

        // 1. Top-Left: Pause button (ample safe margin below status bar/notch)
        float pauseX = getPauseX();
        float pauseY = getPauseY();
        float pauseR = 32f;
        GameIcons.drawPause(c, pauseX, pauseY, pauseR, fill, stroke, false);

        // 2. Top-Center: Level Title and Chapter
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(32f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        // Drop shadow for title
        text.setColor(0xCC000000);
        c.drawText(title, cx, 101.5f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText(title, cx, 100f, text);

        int ch = (g.levelIndex() / 5) + 1;
        String chRoman = (ch == 1 ? "I" : (ch == 2 ? "II" : "III"));
        text.setTextSize(20f);
        text.setTypeface(android.graphics.Typeface.DEFAULT);
        text.setColor(0xCC000000);
        c.drawText("Chapter " + chRoman + " • Level " + (g.levelIndex() + 1) + " of " + g.levelCount(), cx, 137.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("Chapter " + chRoman + " • Level " + (g.levelIndex() + 1) + " of " + g.levelCount(), cx, 136f, text);

        // 3. Top-Right: Precision Circular Match Meter
        drawMatchMeter(c, g, match);

        // 4. Bottom Alchemist Control Shelf (Strictly BELOW the Wall at WALL_BOTTOM = 1530f)
        float shelfY = 1545f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0x55D4AF37);
        c.drawLine(100f, shelfY, ShadowCalculator.SCENE_W - 100f, shelfY, stroke);
        fill.setColor(0xFFFFE082);
        c.drawCircle(cx, shelfY, 4.5f, fill);
        stroke.setStyle(Paint.Style.FILL);

        boolean tall = (sceneH > 1650f);
        float ctrlY = getHintY(sceneH);
        float btnR = 38f;
        boolean hintOn = g.isHintActive();

        // Hint Banner (placed above the buttons in the shelf, NEVER overlapping the shadow wall)
        if (hintOn || match < 55f) {
            float bannerY = tall ? 1605f : (ctrlY - 50f);
            float bannerW = 760f;
            float bannerH = 60f;
            float bL = cx - bannerW * 0.5f;
            float bR = cx + bannerW * 0.5f;
            float bT = bannerY - bannerH * 0.5f;
            float bB = bannerY + bannerH * 0.5f;

            fill.setColor(hintOn ? 0xF0221A2C : 0xDD181E2C);
            c.drawRoundRect(bL, bT, bR, bB, 16f, 16f, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.8f);
            stroke.setColor(hintOn ? 0xFFFFD54F : 0x88D4AF37);
            c.drawRoundRect(bL, bT, bR, bB, 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(21f);
            text.setTypeface(android.graphics.Typeface.DEFAULT);
            text.setColor(0xCC000000);
            c.drawText(hint, cx, bannerY + 7.5f, text);
            text.setColor(hintOn ? 0xFFFFFDF8 : 0xFFFFE0A8);
            c.drawText(hint, cx, bannerY + 6f, text);
        }

        // Hint Button
        float hintX = getHintX();
        GameIcons.drawHint(c, hintX, ctrlY, btnR, fill, stroke, hintOn);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(15f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.setColor(hintOn ? 0xFFFFD54F : 0xCCFFE0A8);
        c.drawText("HINT", hintX, ctrlY + btnR + 22f, text);

        // Reset Button
        float rstX = getResetX();
        GameIcons.drawReset(c, rstX, ctrlY, btnR, fill, stroke);
        text.setColor(0xCCFFE0A8);
        c.drawText("RESET", rstX, ctrlY + btnR + 22f, text);

        // Tactical Efficiency Badge (Centered between Hint & Reset)
        if (tall) {
            float cardW = 460f;
            float cardH = 76f;
            float cL = cx - cardW * 0.5f;
            float cR = cx + cardW * 0.5f;
            float cT = ctrlY - cardH * 0.5f;
            float cB = ctrlY + cardH * 0.5f;

            fill.setColor(0xEE1A2234);
            c.drawRoundRect(cL, cT, cR, cB, 16f, 16f, fill);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.8f);
            stroke.setColor(0x66D4AF37);
            c.drawRoundRect(cL, cT, cR, cB, 16f, 16f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            long elapsed = g.elapsedMillis();
            long parTime = LevelProgress.getParTimeMillis(g.levelIndex());
            int parMoves = LevelProgress.getParMoves(g.levelIndex());

            text.setTextSize(14f);
            text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            text.setColor(0xAAFFE0A8);
            c.drawText("TRIAL PERFORMANCE", cx, cT + 26f, text);

            text.setTextSize(19f);
            text.setTypeface(android.graphics.Typeface.DEFAULT);
            text.setColor(0xFFFFFDF8);
            c.drawText("Moves: " + g.moveCount() + " (Par " + parMoves + ")  •  Time: " + (elapsed / 1000L) + "s", cx, cT + 56f, text);
        }

        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawMatchMeter(Canvas c, GameThread g, float match) {
        float cx = HUD_MATCH_X;
        float cy = HUD_TOP_Y;
        float r = 36f;

        // Frosted obsidian background plate with brass rim
        fill.setColor(0xEE1A2234);
        c.drawCircle(cx, cy, r + 10f, fill);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawCircle(cx, cy, r + 10f, stroke);

        // Meter Track
        stroke.setStrokeWidth(8f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setColor(0x44605030);
        rect.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(rect, 0, 360, false, stroke);

        // Fill arc
        float clamped = match < 0f ? 0f : (match > 100f ? 100f : match);
        if (clamped > 0.5f) {
            stroke.setStrokeWidth(8f);
            stroke.setColor(mix(0xFFFFB300, 0xFFFFE082, clamped / 100f));
            rect.set(cx - r, cy - r, cx + r, cy + r);
            c.drawArc(rect, -90f, 360f * clamped / 100f, false, stroke);

            // Glowing bead at the tip of progress
            float angleRad = (float) Math.toRadians(-90f + 360f * clamped / 100f);
            float beadX = cx + r * (float) Math.cos(angleRad);
            float beadY = cy + r * (float) Math.sin(angleRad);
            stroke.setStyle(Paint.Style.FILL);
            fill.setColor(0xFFFFFFFF);
            c.drawCircle(beadX, beadY, 5.5f, fill);
        }
        stroke.setStrokeCap(Paint.Cap.BUTT);

        // High contrast percentage text
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(26f);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.setColor(0xCC000000);
        c.drawText(Math.round(clamped) + "%", cx, cy + 9.5f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText(Math.round(clamped) + "%", cx, cy + 8f, text);
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
