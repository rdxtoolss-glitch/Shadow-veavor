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
import android.graphics.Typeface;

/**
 * Shadow Weaver - GameRenderer
 * ===========================
 * Masterwork Canvas rendering engine for Shadow Weaver.
 * Fully zero-allocation in all per-frame loops.
 *
 * Implements:
 *  - Dynamic full-height responsive viewport across all device aspect ratios.
 *  - Notch/safe-area cleared top HUD with antique brass framing.
 *  - Handcrafted living candlelight with volumetric god rays and ember sparks.
 *  - 2.5D Polished Bronze & Inlaid Obsidian Alchemical Tokens with specular bevels.
 *  - Fine Italian vellum illuminated shadow projection wall with deckled borders.
 *  - Celestial Constellation Target Blueprint with star diamond nodes.
 *  - Full-height Grand Alchemical Console Deck spanning from wall to screen bottom,
 *    featuring an Oracle Scroll, 3-Star Trial Telemetry tablet, and tactile command dials.
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

    private static Typeface safeTypeface(Typeface family, int style) {
        try {
            return Typeface.create(family, style);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final Typeface SERIF_BOLD = safeTypeface(Typeface.SERIF, Typeface.BOLD);
    private static final Typeface SERIF_NORMAL = safeTypeface(Typeface.SERIF, Typeface.NORMAL);

    private static final Path path = new Path();
    private static final Path path2 = new Path();
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
        return 180f;
    }

    public static float getResetX() {
        return 820f;
    }

    public static float getHintY(float sceneH) {
        if (sceneH > 1800f) {
            return sceneH - 120f;
        } else if (sceneH > 1620f) {
            return sceneH - 95f;
        } else {
            return 1565f;
        }
    }

    public static float getResetY(float sceneH) {
        return getHintY(sceneH);
    }

    /**
     * Maps the full scene width (1000 units) edge-to-edge across the screen in portrait,
     * allowing sceneHeight to dynamically expand to eliminate black letterbox bars on modern tall devices.
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
        return 0f;
    }

    static float sceneX(float sx, float scale, float ox) {
        return (sx - ox) / scale;
    }

    static float sceneY(float sy, float scale, float oy) {
        return (sy - oy) / scale;
    }

    // ======================================================================
    // FRAME ROUTING
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

            // Shadow pass
            drawShadows(canvas, g, scale);

            // Shapes on workbench
            drawShapes(canvas, g);
            g.particles().drawSparks(canvas, fill);

            // Onboarding FTUE hint for Level 1 House when moveCount == 0
            if (g.levelIndex() == 0 && g.moveCount() == 0 && !g.isSolved()) {
                drawOnboardingCue(canvas, g, g.timeSeconds());
            }

            // HUD and Alchemical Control Console
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

        // Screen transition veil
        float transAlpha = g.screenTransitionAlpha();
        if (transAlpha < 1.0f) {
            int veilAlpha = (int) ((1.0f - transAlpha) * 160);
            fill.setStyle(Paint.Style.FILL);
            fill.setColor((veilAlpha << 24) | 0x05070D);
            canvas.drawRect(0, 0, ShadowCalculator.SCENE_W, sceneH, fill);
        }

        canvas.restoreToCount(save);
    }

    private static void drawLevelBanner(Canvas c, GameThread g, float alpha) {
        if (alpha <= 0f) return;
        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float y = 520f;
        float bw = 540f;
        float bh = 88f;
        rect.set(cx - bw * 0.5f, y - bh * 0.5f, cx + bw * 0.5f, y + bh * 0.5f);

        int a = (int) (240 * alpha);
        fill.setColor(withAlpha(0xF0181F2E, a));
        c.drawRoundRect(rect, 20f, 20f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(withAlpha(0xFFFFD54F, (int) (220 * alpha)));
        c.drawRoundRect(rect, 20f, 20f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(30f);
        text.setColor(withAlpha(0xFFFFE9C4, (int) (255 * alpha)));
        c.drawText(g.levelTitle(), cx, y - 6f, text);

        text.setTextSize(19f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(withAlpha(0xBBFFE0B0, (int) (240 * alpha)));
        c.drawText("Chapter " + ((g.levelIndex() / 5) + 1) + " • Level " + (g.levelIndex() + 1), cx, y + 24f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawOnboardingCue(Canvas c, GameThread g, float time) {
        ShapeObject[] shapes = g.shapes();
        if (shapes == null || shapes.length == 0) return;

        ShapeObject s0 = shapes[0];
        float sx = s0.x;
        float sy = s0.y;

        // 1. Pulsing golden beacon around the first shape
        float pulse = (float) Math.sin(time * 3.5f);
        float r = 58f + pulse * 8f;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.6f);
        stroke.setColor(withAlpha(0xFFFFD54F, (int) (180 + 60 * pulse)));
        c.drawCircle(sx, sy, r, stroke);

        // Animated dashed orbit around it
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(withAlpha(0xFFFFF9C4, (int) (140 + 50 * pulse)));
        c.drawCircle(sx, sy, r + 14f, stroke);

        // 2. Animated upward-pointing chevron guides indicating drag towards flame
        float flow = (time * 75f) % 60f;
        float arrowY = sy - 60f - flow;
        int arrowAlpha = (int) (220 * (1.0f - (flow / 60f)));
        if (arrowAlpha > 0) {
            stroke.setStrokeWidth(3.0f);
            stroke.setColor(withAlpha(0xFFFFD54F, arrowAlpha));
            c.drawLine(sx - 18f, arrowY + 12f, sx, arrowY, stroke);
            c.drawLine(sx + 18f, arrowY + 12f, sx, arrowY, stroke);
        }

        // 3. Elegant parchment banner centered on workbench
        float cx = ShadowCalculator.SCENE_W * 0.5f;
        float cardY = 250f;
        float cardW = 680f;
        float cardH = 82f;
        float cL = cx - cardW * 0.5f;
        float cR = cx + cardW * 0.5f;
        float cT = cardY - cardH * 0.5f;
        float cB = cardY + cardH * 0.5f;

        fill.setColor(0xEE141C2B);
        c.drawRoundRect(cL, cT, cR, cB, 18f, 18f, fill);

        stroke.setStrokeWidth(2.0f);
        stroke.setColor(0xCCFFD54F);
        c.drawRoundRect(cL, cT, cR, cB, 18f, 18f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);
        text.setTextSize(17f);
        text.setColor(0xFFFFD54F);
        c.drawText("✦ TOUCH & DRAG PIECES ON WORKBENCH ✦", cx, cT + 30f, text);

        text.setTypeface(SERIF_NORMAL);
        text.setTextSize(15f);
        text.setColor(0xFFFFFDF8);
        c.drawText("Closer to candle (up) = larger shadow • Twist to rotate", cx, cT + 60f, text);
        text.setTextAlign(Paint.Align.LEFT);
    }

    // ======================================================================
    // BACKDROP
    // ======================================================================

    private static void drawBackdrop(Canvas c, float flicker, float sceneH) {
        // Deep celestial chamber: incandescent golden candlelight halo at top,
        // descending into midnight indigo sapphire and dark obsidian vignette.
        haze.setShader(new LinearGradient(0, 0, 0, sceneH,
                new int[]{
                        mix(0xFF6E4224, 0xFF8A532C, flicker), // Luminous amber warmth near candle
                        0xFF1B2032,                           // Deep celestial midnight sapphire
                        0xFF0F131E                            // Rich obsidian base vignette
                },
                new float[]{0f, 0.38f, 1f},
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

        // Inlaid dark mahogany & midnight wood surface with subtle radial warmth
        rect.set(l, t, r, b);
        haze.setShader(new LinearGradient(l, t, r, b,
                new int[]{0xFF241810, 0xFF2E1F15, 0xFF1C130D},
                null, Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, 18f, 18f, haze);
        haze.setShader(null);

        // Stage border inlay (radiant antique brass)
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(withAlpha(0xFFD4AF37, (int) (200 * flicker)));
        c.drawRoundRect(rect, 18f, 18f, stroke);

        // Specular highlight line on top edge
        stroke.setStrokeWidth(1.6f);
        stroke.setColor(withAlpha(0x66FFFFFF, (int) (180 * flicker)));
        c.drawLine(l + 18f, t + 1.5f, r - 18f, t + 1.5f, stroke);

        // Engraved celestial grid lines on stage surface
        stroke.setStrokeWidth(1.0f);
        stroke.setColor(0x22D4AF37);
        float cx = (l + r) * 0.5f;
        float cy = (t + b) * 0.5f;

        // Subtle concentric orbital lines
        c.drawCircle(cx, cy, 140f, stroke);
        c.drawCircle(cx, cy, 260f, stroke);
        c.drawLine(l + 40f, cy, r - 40f, cy, stroke);
        c.drawLine(cx, t + 40f, cx, b - 40f, stroke);

        // Corner bracket flourishes with brass rivets
        float cw = 34f;
        drawCornerBracket(c, l, t, cw, true, true);
        drawCornerBracket(c, r, t, cw, false, true);
        drawCornerBracket(c, l, b, cw, true, false);
        drawCornerBracket(c, r, b, cw, false, false);

        stroke.setStyle(Paint.Style.FILL);
    }

    private static void drawCornerBracket(Canvas c, float x, float y, float size, boolean left, boolean top) {
        float dx = left ? size : -size;
        float dy = top ? size : -size;

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.4f);
        stroke.setColor(0xFFC5A059);

        path.reset();
        path.moveTo(x + dx, y);
        path.lineTo(x, y);
        path.lineTo(x, y + dy);
        c.drawPath(path, stroke);

        // Corner rivet
        fill.setColor(0xFFFFE082);
        c.drawCircle(x + (left ? 10f : -10f), y + (top ? 10f : -10f), 3.5f, fill);
    }

    // ======================================================================
    // VOLUMETRIC GOD RAYS
    // ======================================================================

    private static void drawVolumetricLightRays(Canvas c, float flicker, float time) {
        float lx = ShadowCalculator.LIGHT_X;
        float ly = 165f;

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
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(0xFF261204);
        c.drawLine(lx, ly + 14f, lx, ly + 4f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Flame parameters
        float flameW = 14f * flicker;
        float flameH = 32f * flicker;

        // 1. Outer warm amber teardrop flame
        path.reset();
        path.moveTo(lx, ly - flameH);
        path.cubicTo(lx + flameW, ly - flameH * 0.4f, lx + flameW * 0.8f, ly + 5f, lx, ly + 5f);
        path.cubicTo(lx - flameW * 0.8f, ly + 5f, lx - flameW, ly - flameH * 0.4f, lx, ly - flameH);
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

        // Luminous warm fine vellum & parchment projection screen
        haze.setShader(new LinearGradient(0, t, 0, b,
                new int[]{
                        mix(0xFFF9EFE0, 0xFFFFF8EC, flicker), // Luminous candlelit ivory top
                        0xFFEEDCC4,                           // Warm fine parchment middle
                        0xFFD6C2A6                            // Softly shaded lower vellum
                },
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP));
        rect.set(l, t, r, b);
        c.drawRoundRect(rect, 16f, 16f, haze);
        haze.setShader(null);

        // Ornate antique brass framing border around projection canvas
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.5f);
        stroke.setColor(withAlpha(0xFFC5A059, (int) (230 * flicker)));
        c.drawRoundRect(rect, 16f, 16f, stroke);

        // Top specular golden highlight line
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(withAlpha(0xFFFFE8B8, (int) (240 * flicker)));
        c.drawLine(l + 16f, t + 1.5f, r - 16f, t + 1.5f, stroke);

        // Subtle inner parchment vignette
        haze.setShader(new LinearGradient(0, b - 36f, 0, b,
                new int[]{0x00000000, 0x442A1608},
                null, Shader.TileMode.CLAMP));
        rect.set(l, b - 36f, r, b);
        c.drawRoundRect(rect, 16f, 16f, haze);
        haze.setShader(null);

        // Corner mounts
        drawCornerBracket(c, l, t, 28f, true, true);
        drawCornerBracket(c, r, t, 28f, false, true);
        drawCornerBracket(c, l, b, 28f, true, false);
        drawCornerBracket(c, r, b, 28f, false, false);

        stroke.setStyle(Paint.Style.FILL);
    }

    // ======================================================================
    // TARGET GHOST (CONSTELLATION BLUEPRINT)
    // ======================================================================

    private static void drawTargetGhost(Canvas c, GameThread g) {
        float[][] target = g.targetPolygons();
        if (target == null) return;

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
            stroke.setStyle(Paint.Style.FILL);
            return;
        }

        // Unsolved state: Enchanting Celestial Blueprint with star diamond nodes
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.4f);
        stroke.setColor(0x88A07A38); // Warm antique sepia

        for (float[] poly : target) {
            buildPath(poly);
            c.drawPath(path, stroke);

            // Draw glowing star diamond nodes at key vertices
            for (int i = 0; i < poly.length; i += 2) {
                float px = poly[i];
                float py = poly[i + 1];
                drawStarNode(c, px, py, 5.0f, 0xBBFFE082);
            }
        }
        stroke.setStyle(Paint.Style.FILL);
    }

    private static void drawStarNode(Canvas c, float cx, float cy, float r, int color) {
        fill.setColor(color);
        path2.reset();
        path2.moveTo(cx, cy - r);
        path2.lineTo(cx + r * 0.45f, cy);
        path2.lineTo(cx, cy + r);
        path2.lineTo(cx - r * 0.45f, cy);
        path2.close();
        c.drawPath(path2, fill);

        // Center white star pearl
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(cx, cy, 1.8f, fill);
    }

    // ======================================================================
    // MAGNETIC TETHERS & LIGHT RAYS
    // ======================================================================

    private static void drawMagneticTethers(Canvas c, GameThread g) {
        ShapeObject s = g.getSelectedShape();
        if (s == null || g.isSolved()) return;

        ShapeObject sol = g.findSolutionFor(s);
        if (sol == null) return;

        float dist = (float) Math.hypot(sol.x - s.x, sol.y - s.y);
        if (dist > 180f) return;

        float alpha = Math.max(0f, 1f - dist / 180f);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(withAlpha(0xFFFFD54F, (int) (160 * alpha)));

        c.drawLine(s.x, s.y, sol.x, sol.y, stroke);
        fill.setColor(withAlpha(0xFFFFE082, (int) (220 * alpha)));
        c.drawCircle(sol.x, sol.y, 6.0f, fill);
        stroke.setStyle(Paint.Style.FILL);
    }

    private static void drawLightRays(Canvas c, GameThread g) {
        ShapeObject s = g.getSelectedShape();
        if (s == null || g.isSolved()) return;

        float lx = ShadowCalculator.LIGHT_X;
        float ly = ShadowCalculator.LIGHT_Y;

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.4f);
        stroke.setColor(0x44FFD54F);
        c.drawLine(lx, ly, s.x, s.y, stroke);
        stroke.setStyle(Paint.Style.FILL);
    }

    // ======================================================================
    // SHADOW PASS
    // ======================================================================

    private static Bitmap shadowBuffer;
    private static Canvas shadowCanvas;
    private static final Rect shadowBitmapSrc = new Rect();
    private static final Paint shadowBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final float SHADOW_PAD = 32f;

    private static void drawShadows(Canvas c, GameThread g, float scale) {
        ShapeObject[] shapes = g.shapes();
        if (shapes == null || shapes.length == 0) return;

        ShadowCalculator.Shadow[] cast = new ShadowCalculator.Shadow[shapes.length];
        for (int i = 0; i < shapes.length; i++) {
            cast[i] = ShadowCalculator.projectDetailed(shapes[i]);
        }

        final float l = ShadowCalculator.WALL_LEFT - SHADOW_PAD;
        final float t = ShadowCalculator.WALL_TOP - SHADOW_PAD;
        final float r = ShadowCalculator.WALL_RIGHT + SHADOW_PAD;
        final float b = ShadowCalculator.WALL_BOTTOM + SHADOW_PAD;
        final float sw = r - l;
        final float sh = b - t;

        int bw = Math.max(1, (int) Math.ceil(sw * scale));
        int bh = Math.max(1, (int) Math.ceil(sh * scale));

        if (shadowBuffer == null || shadowBuffer.getWidth() != bw || shadowBuffer.getHeight() != bh) {
            if (shadowBuffer != null) shadowBuffer.recycle();
            shadowBuffer = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            shadowCanvas = new Canvas(shadowBuffer);
        }

        Canvas buf = shadowCanvas;
        buf.drawColor(0x00000000, android.graphics.PorterDuff.Mode.CLEAR);

        int saveBuf = buf.save();
        buf.scale(scale, scale);
        buf.translate(-l, -t);

        float warmth = warmth(g.candleFlicker());
        drawShadowBands(buf, cast, ShadowCalculator.LAYER_OUTER, warmth);
        drawShadowBands(buf, cast, ShadowCalculator.LAYER_MID, warmth);
        drawShadowBands(buf, cast, ShadowCalculator.LAYER_CORE, warmth);

        buf.restoreToCount(saveBuf);

        int save = c.save();
        c.clipRect(ShadowCalculator.WALL_LEFT, ShadowCalculator.WALL_TOP,
                ShadowCalculator.WALL_RIGHT, ShadowCalculator.WALL_BOTTOM);
        shadowBitmapSrc.set(0, 0, bw, bh);
        rect.set(l, t, r, b);
        c.drawBitmap(shadowBuffer, shadowBitmapSrc, rect, shadowBitmapPaint);
        c.restoreToCount(save);
    }

    private static void drawShadowBands(Canvas c, ShadowCalculator.Shadow[] cast, int layer, float warmth) {
        int base = ShadowCalculator.layerAlpha(layer);
        int tint = ShadowCalculator.SHADOW_TINT;
        if (layer == ShadowCalculator.LAYER_CORE) {
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
    // 2.5D CRAFTED ALCHEMICAL TOKENS
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
            float shadowOffsetY = 12f + s.elevation * 24f;
            float shadowAlpha = 0.28f + s.elevation * 0.20f;
            int saveShadow = c.save();
            c.translate(0, shadowOffsetY);
            fill.setColor(withAlpha(0xFF000000, (int) (255 * shadowAlpha)));
            c.drawPath(path, fill);
            c.restoreToCount(saveShadow);

            // 2. Proximity Resonance Golden Aura
            if (s.proximityResonance > 0.05f) {
                stroke.setStyle(Paint.Style.STROKE);
                float auraWidth = 8f + 10f * s.proximityResonance;
                int auraAlpha = (int) (160 * s.proximityResonance * (0.85f + 0.15f * flicker));
                stroke.setStrokeWidth(auraWidth);
                stroke.setColor(withAlpha(0xFFFFD54F, auraAlpha));
                c.drawPath(path, stroke);
                stroke.setStyle(Paint.Style.FILL);
            }

            // 3. 2.5D Polished Bronze Base & Radiant Inlaid Body
            // Base plate (carved antique dark bronze)
            fill.setColor(isSel ? 0xFF5A3D1E : 0xFF422C16);
            c.drawPath(path, fill);

            // Inner lustrous gold-sheen inlay
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(isSel ? 6.0f : 4.5f);
            stroke.setColor(isSel ? 0xFFFFA000 : 0xFF8D5B23);
            c.drawPath(path, stroke);

            // Polished brass perimeter bezel
            stroke.setStrokeWidth(isSel ? 4.0f + 1.2f * pulse : 2.8f);
            stroke.setColor(isSel
                    ? withAlpha(0xFFFFD54F, (int) (240 + 15 * pulse))
                    : withAlpha(0xFFE5B048, 230));
            c.drawPath(path, stroke);

            // Top-left specular directional highlight (reflecting candle flame)
            stroke.setStrokeWidth(1.8f);
            stroke.setColor(withAlpha(0xFFFFF8E1, isSel ? 240 : 160));
            c.drawPath(path, stroke);
            stroke.setStyle(Paint.Style.FILL);

            // Inlaid Runic Sacred Geometry Lines from center to vertices
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.2f);
            stroke.setColor(isSel ? 0x99FFE082 : 0x44D4AF37);
            for (int k = 0; k < poly.length; k += 2) {
                c.drawLine(s.x, s.y, poly[k], poly[k + 1], stroke);
            }
            stroke.setStyle(Paint.Style.FILL);

            // Center brass astrolabe pivot rivet
            fill.setColor(isSel ? 0xFFFFD54F : 0xFFC5A059);
            c.drawCircle(s.x, s.y, 10f, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.2f);
            stroke.setColor(0xFF1B0E05);
            c.drawCircle(s.x, s.y, 10f, stroke);

            // Center faceted jewel (glowing ruby when selected, lapis lazuli when idle)
            fill.setColor(isSel ? 0xFFE53935 : 0xFF283593);
            c.drawCircle(s.x, s.y, 5.0f, fill);

            fill.setColor(0xFFFFFFFF);
            c.drawCircle(s.x - 1.5f, s.y - 1.5f, 1.6f, fill);
            stroke.setStyle(Paint.Style.FILL);

            // 4. Mechanical Precision Astrolabe Rotation Dial
            if (isSel) {
                float r = s.maxRadius();
                float dialR = r + 30f;

                // Circular dial track
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.4f);
                stroke.setColor(0x99FFE0A8);
                c.drawCircle(s.x, s.y, dialR, stroke);

                // Astrolabe degree notches: 15° minor, 45° semi, 90° major ticks
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

                // Rotary spoke arm
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
                c.drawCircle(hrx, hry, 22f + 2f * pulse, fill);

                fill.setColor(0xFFE5B048);
                c.drawCircle(hrx, hry, 14f, fill);
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.4f);
                stroke.setColor(0xFF1B0E05);
                c.drawCircle(hrx, hry, 14f, stroke);
                stroke.setStyle(Paint.Style.FILL);

                fill.setColor(0xFFFFFFFF);
                c.drawCircle(hrx, hry, 5.5f, fill);
            }

            c.restoreToCount(saveShape);
        }
    }

    // ======================================================================
    // HUD & GRAND ALCHEMICAL CONSOLE DECK
    // ======================================================================

    private static void drawHud(Canvas c, GameThread g, float sceneH) {
        String title = g.levelTitle();
        String hint = g.levelHint();
        float match = g.matchPercent();
        float cx = ShadowCalculator.SCENE_W * 0.5f;

        // 1. Top-Left: Pause button (ample safe margin below status bar/notch)
        float pauseX = getPauseX();
        float pauseY = getPauseY();
        float pauseR = 34f;
        GameIcons.drawPause(c, pauseX, pauseY, pauseR, fill, stroke, false);

        // 2. Top-Center: Level Title and Chapter
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(32f);
        text.setTypeface(SERIF_BOLD);

        // Drop shadow for title
        text.setColor(0xCC000000);
        c.drawText(title, cx, 101.5f, text);
        text.setColor(0xFFFFFDF8);
        c.drawText(title, cx, 100f, text);

        int ch = (g.levelIndex() / 5) + 1;
        String chRoman = (ch == 1 ? "I" : (ch == 2 ? "II" : "III"));
        text.setTextSize(20f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(0xCC000000);
        c.drawText("Chapter " + chRoman + " • Level " + (g.levelIndex() + 1) + " of " + g.levelCount(), cx, 137.5f, text);
        text.setColor(0xFFFFE0A8);
        c.drawText("Chapter " + chRoman + " • Level " + (g.levelIndex() + 1) + " of " + g.levelCount(), cx, 136f, text);

        // 3. Top-Right: Precision Circular Match Meter
        drawMatchMeter(c, g, match);

        // ==================================================================
        // 4. THE GRAND ALCHEMICAL CONSOLE DECK (WALL_BOTTOM to sceneH)
        // ==================================================================
        float deskT = ShadowCalculator.WALL_BOTTOM;
        float deskB = sceneH;
        boolean tall = (sceneH > 1800f);

        // A. Console Mahogany & Slate Desk Backing
        fill.setShader(new LinearGradient(0, deskT, 0, deskB,
                new int[]{0xFF1A130C, 0xFF140E08, 0xFF0D0A06},
                null, Shader.TileMode.CLAMP));
        c.drawRect(0, deskT, ShadowCalculator.SCENE_W, deskB, fill);
        fill.setShader(null);

        // Upper molded brass transition rail
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(0xFFD4AF37);
        c.drawLine(40f, deskT + 2f, ShadowCalculator.SCENE_W - 40f, deskT + 2f, stroke);

        // Specular highlight on rail
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x66FFFFFF);
        c.drawLine(40f, deskT + 4f, ShadowCalculator.SCENE_W - 40f, deskT + 4f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // B. The Oracle's Scroll of Illumination
        boolean hintOn = g.isHintActive();
        float scrollY = tall ? 1640f : (deskT + 60f);
        float scrollW = 820f;
        float scrollH = tall ? 104f : 80f;
        float scL = cx - scrollW * 0.5f;
        float scR = cx + scrollW * 0.5f;
        float scT = scrollY - scrollH * 0.5f;
        float scB = scrollY + scrollH * 0.5f;

        // Vellum scroll background
        fill.setColor(hintOn ? 0xF02A1E34 : 0xEE1E2436);
        c.drawRoundRect(scL, scT, scR, scB, 18f, 18f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(hintOn ? 0xFFFFD54F : 0x88D4AF37);
        c.drawRoundRect(scL, scT, scR, scB, 18f, 18f, stroke);
        stroke.setStyle(Paint.Style.FILL);

        // Oracle Scroll Header
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(16f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(hintOn ? 0xFFFFD54F : 0xCCFFE0A8);
        c.drawText(hintOn ? "✦ THE ORACLE'S REVELATION ✦" : "✦ THE ORACLE'S GUIDANCE ✦", cx, scT + 28f, text);

        // Hint Text
        text.setTextSize(21f);
        text.setTypeface(SERIF_NORMAL);
        text.setColor(hintOn ? 0xFFFFFDF8 : 0xFFFFE0A8);
        c.drawText(hint, cx, scT + 66f, text);

        // C. Live Alchemical Telemetry Tablet (On tall screens)
        if (tall) {
            float teleY = 1820f;
            float teleW = 820f;
            float teleH = 96f;
            float tL = cx - teleW * 0.5f;
            float tR = cx + teleW * 0.5f;
            float tT = teleY - teleH * 0.5f;
            float tB = teleY + teleH * 0.5f;

            fill.setColor(0xF0181F2E);
            c.drawRoundRect(tL, tT, tR, tB, 18f, 18f, fill);

            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.8f);
            stroke.setColor(0x55D4AF37);
            c.drawRoundRect(tL, tT, tR, tB, 18f, 18f, stroke);
            stroke.setStyle(Paint.Style.FILL);

            // 3 Badges inside tablet
            long elapsed = g.elapsedMillis();
            long parTime = LevelProgress.getParTimeMillis(g.levelIndex());
            int parMoves = LevelProgress.getParMoves(g.levelIndex());
            int moves = g.moveCount();

            // Badge 1: Silhouette Match
            float col1X = tL + 140f;
            text.setTextSize(15f);
            text.setTypeface(SERIF_BOLD);
            text.setColor(0xAAFFE0A8);
            c.drawText("SILHOUETTE", col1X, tT + 34f, text);
            text.setTextSize(26f);
            text.setColor(match >= 85f ? 0xFFFFD54F : 0xFFFFFDF8);
            c.drawText(Math.round(match) + "%", col1X, tT + 72f, text);

            // Divider 1
            stroke.setStrokeWidth(1.4f);
            stroke.setColor(0x33D4AF37);
            c.drawLine(tL + 270f, tT + 16f, tL + 270f, tB - 16f, stroke);

            // Badge 2: Chronometer
            float col2X = cx;
            text.setTextSize(15f);
            text.setColor(0xAAFFE0A8);
            c.drawText("CHRONOMETER", col2X, tT + 34f, text);
            text.setTextSize(26f);
            text.setColor(elapsed <= parTime ? 0xFFFFD54F : 0xFFE0E0E0);
            c.drawText(formatTimeSec(elapsed) + " / " + (parTime / 1000L) + "s", col2X, tT + 72f, text);

            // Divider 2
            c.drawLine(tR - 270f, tT + 16f, tR - 270f, tB - 16f, stroke);

            // Badge 3: Move Precision
            float col3X = tR - 140f;
            text.setTextSize(15f);
            text.setColor(0xAAFFE0A8);
            c.drawText("PRECISION", col3X, tT + 34f, text);
            text.setTextSize(26f);
            text.setColor(moves <= parMoves ? 0xFFFFD54F : 0xFFE0E0E0);
            c.drawText(moves + " / " + parMoves, col3X, tT + 72f, text);
            stroke.setStyle(Paint.Style.FILL);
        }

        // D. Tactile Command Controls (Anchored near bottom)
        float ctrlY = getHintY(sceneH);
        float btnR = 52f;

        // 1. Hint Astrolabe Button (Left)
        float hintX = getHintX();
        GameIcons.drawHint(c, hintX, ctrlY, btnR, fill, stroke, hintOn);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(18f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(hintOn ? 0xFFFFD54F : 0xEEFFE0A8);
        c.drawText("ORACLE HINT", hintX, ctrlY + btnR + 28f, text);

        // 2. Central Celestial Match Astrolabe Dial
        drawConsoleMatchAstrolabe(c, cx, ctrlY, match);

        // 3. Reset Chronometer Button (Right)
        float rstX = getResetX();
        GameIcons.drawReset(c, rstX, ctrlY, btnR, fill, stroke);
        text.setColor(0xEEFFE0A8);
        c.drawText("RESTART TRIAL", rstX, ctrlY + btnR + 28f, text);

        text.setTextAlign(Paint.Align.LEFT);
    }

    private static void drawConsoleMatchAstrolabe(Canvas c, float cx, float cy, float match) {
        float r = 62f;

        // Medallion outer bezel
        fill.setColor(0xF01A2234);
        c.drawCircle(cx, cy, r, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(0xFFD4AF37);
        c.drawCircle(cx, cy, r, stroke);

        // Orbital tick marks
        for (int i = 0; i < 360; i += 30) {
            float rad = (float) Math.toRadians(i);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            stroke.setStrokeWidth(1.4f);
            stroke.setColor(0x66FFE0A8);
            c.drawLine(cx + cos * (r - 10f), cy + sin * (r - 10f), cx + cos * (r - 2f), cy + sin * (r - 2f), stroke);
        }

        // Active Arc Progress
        stroke.setStrokeWidth(5.5f);
        stroke.setColor(match >= 85f ? 0xFFFFD54F : 0xFFFFB300);
        float sweep = (match / 100f) * 360f;
        c.drawArc(cx - r + 6f, cy - r + 6f, cx + r - 6f, cy + r - 6f, -90, sweep, false, stroke);

        // Center Percentage
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(26f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(0xFFFFFDF8);
        c.drawText(Math.round(match) + "%", cx, cy + 9f, text);

        text.setTextSize(14f);
        text.setColor(0xCCFFE0A8);
        c.drawText("RESONANCE", cx, cy + r + 26f, text);
        stroke.setStyle(Paint.Style.FILL);
    }

    private static void drawMatchMeter(Canvas c, GameThread g, float match) {
        float cx = HUD_MATCH_X;
        float cy = HUD_TOP_Y;
        float r = 32f;

        // Dial base plate
        fill.setColor(0xF0181F2E);
        c.drawCircle(cx, cy, r, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.6f);
        stroke.setColor(0xFFD4AF37);
        c.drawCircle(cx, cy, r, stroke);

        // Active Progress Arc
        float sweep = (match / 100f) * 360f;
        stroke.setStrokeWidth(4.5f);
        stroke.setColor(match >= 85f ? 0xFFFFD54F : 0xFFFFB300);
        c.drawArc(cx - r + 4f, cy - r + 4f, cx + r - 4f, cy + r - 4f, -90, sweep, false, stroke);

        // Center label
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(18f);
        text.setTypeface(SERIF_BOLD);
        text.setColor(0xFFFFFDF8);
        c.drawText(Math.round(match) + "%", cx, cy + 6.5f, text);
        stroke.setStyle(Paint.Style.FILL);
    }

    private static String formatTimeSec(long ms) {
        long s = ms / 1000L;
        long tenths = (ms % 1000L) / 100L;
        return s + "." + tenths + "s";
    }

    private static float warmth(float flicker) {
        return Math.max(0f, Math.min(1f, flicker));
    }

    static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + t * (a2 - a1));
        int r = (int) (r1 + t * (r2 - r1));
        int g = (int) (g1 + t * (g2 - g1));
        int b = (int) (b1 + t * (b2 - b1));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int withAlpha(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static void buildPath(float[] poly) {
        path.reset();
        if (poly == null || poly.length < 2) return;
        path.moveTo(poly[0], poly[1]);
        for (int i = 2; i < poly.length; i += 2) {
            path.lineTo(poly[i], poly[i + 1]);
        }
        path.close();
    }
}
