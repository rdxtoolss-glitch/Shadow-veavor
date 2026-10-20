package com.mobodevs.shadowweaver;

import java.util.Arrays;

/**
 * Shadow Weaver - SilhouetteMatcher
 * =================================
 * THE CORE UNIQUE SYSTEM, PART 2: "IS THE PICTURE RIGHT YET?"
 *
 * This class answers one question: how close is the set of shadows the player
 * has built to the ghost picture the level wants? It is the only place in the
 * game that can declare a level finished, so everything about how forgiving
 * the game feels is decided by the constants at the top of this file.
 *
 * ------------------------------------------------------------------
 * WHY WE RASTERISE INSTEAD OF COMPARING POLYGONS
 * ------------------------------------------------------------------
 * The obvious approach is a polygon-overlap test, but the combined shadow is
 * a UNION of up to six arbitrary polygons, and computing an exact
 * intersection area of a union of six convex polygons is a lot of code and a
 * lot of edge cases for zero gameplay benefit. Rasterising both sides onto a
 * coarse grid and comparing the cells is:
 *   - trivially correct (no edge cases, no ordering, no winding rules),
 *   - O(cells) instead of O(n^2 in polygon edges),
 *   - and it handles the union completely automatically, because painting a
 *     cell twice is the same as painting it once.
 *
 * The grid is deliberately low resolution. 96x40 cells over the wall works out
 * to roughly 10x10 scene units per cell, so a cell is a smallish blob and
 * minor finger tremor does not read as a failure. It is a *painting* scale,
 * not a pixel-exact one, which is exactly the tone the game wants.
 *
 * ------------------------------------------------------------------
 * THE SCORE, AND WHY IT IS IoU
 * ------------------------------------------------------------------
 * The score is Intersection over Union:
 *
 *      score = |pixels where a shadow is AND the target is|
 *            -------------------------------------------------
 *            |pixels where a shadow is OR the target is|
 *
 * Plain "how much of the target did I cover" would be broken: the player
 * could park six enormous shapes so that everything is covered and score 100%
 * while the picture is a meaningless black slab. IoU punishes that, because
 * the extra coverage inflates the union just as much as it inflates the
 * intersection. Overshooting and undershooting are both penalised, which is
 * what makes aiming for an exact silhouette the interesting part of the game.
 *
 * ------------------------------------------------------------------
 * THE ONE KNOB YOU PROBABLY WANT
 * ------------------------------------------------------------------
 *      MATCH_THRESHOLD   the IoU required to finish the level.
 *
 * Everything else in the UI is derived from it, so changing this single
 * number re-tunes the whole game: the "match %" the player sees, the gentle
 * chime at halfway, and whether a level completes. Lower it to 0.70 for a
 * very relaxed, forgiving game; raise it to 0.90 for a precision game.
 *
 * See reportMatch() for how the raw IoU is turned into the 0-100% the player
 * actually reads.
 *
 * Pure Java, no android.* imports, so it is unit testable on a plain JVM.
 */
public class SilhouetteMatcher {

    // ------------------------------------------------------------------
    // TUNING
    // ------------------------------------------------------------------

    /**
     * THE MATCH THRESHOLD. The single constant to tune the difficulty of the
     * entire game. Expressed as an IoU (intersection over union) of the
     * combined shadow region against the target silhouette.
     *
     * 0.85 = the design intent: you really do have to trace the outline.
     * 0.70 = forgiving; a rough, generous silhouette counts.
     * 0.90 = demanding; near pixel-exact placement.
     *
     * A level is complete when match >= MATCH_THRESHOLD.
     */
    public static float MATCH_THRESHOLD = 0.85f;

    /**
     * Target size of one comparison cell, in scene units. The grid is sized
     * from this and the wall's dimensions, so the comparison automatically
     * stays in step if the wall layout ever changes.
     *
     * 10 units is a "painting" scale, not a pixel-exact one: it is coarse
     * enough that minor finger tremor does not read as a failure, and fine
     * enough that the thinnest shape in the game (a rod, 22-30 units thick)
     * is always several cells across.
     */
    public static float cellSize = 10f;

    /**
     * Grid dimensions, derived from the wall in the constructor: 920 wide and
     * 420 tall at cellSize 10 gives 92 x 42. These defaults only exist so the
     * field is never observably zero; every instance recomputes them.
     */
    public static int gridWidth = 92;
    public static int gridHeight = 42;

    // ------------------------------------------------------------------
    // STATE
    // ------------------------------------------------------------------

    private final boolean[] targetMask;
    private final boolean[] shadowMask;
    private final int cellCount;
    private int targetCount;
    private boolean targetReady;

    /**
     * The last threshold index the game has already celebrated for this
     * level, so the chime fires once and only once on the way up.
     * -1 = nothing played yet. Reset by resetCelebration().
     */
    private int celebratedUpTo = -1;

    public SilhouetteMatcher() {
        // Size the grid to the wall at the requested cell scale.
        gridWidth = Math.max(8, Math.round(
                (ShadowCalculator.WALL_RIGHT - ShadowCalculator.WALL_LEFT) / cellSize));
        gridHeight = Math.max(8, Math.round(
                (ShadowCalculator.WALL_BOTTOM - ShadowCalculator.WALL_TOP) / cellSize));
        cellCount = gridWidth * gridHeight;
        targetMask = new boolean[cellCount];
        shadowMask = new boolean[cellCount];
    }

    // ------------------------------------------------------------------
    // BUILDING THE TARGET
    // ------------------------------------------------------------------

    /**
     * Load the level's target silhouette into the grid.
     *
     * Takes several polygons because the target is a union of the projected
     * shadows of the level's solution layout, not one tidy outline. Painting
     * them all into the same grid is what produces the union.
     *
     * @param polygons an array of flat [x0,y0,x1,y1,...] scene-space polygons
     */
    public void setTarget(float[][] polygons) {
        Arrays.fill(targetMask, false);
        targetCount = 0;
        for (float[] poly : polygons) {
            if (paintPolygon(poly, targetMask)) {
                // count is computed at the end instead, to avoid double counting
            }
        }
        targetCount = 0;
        for (int i = 0; i < cellCount; i++) {
            if (targetMask[i]) targetCount++;
        }
        targetReady = targetCount > 0;
        celebratedUpTo = -1;
    }

    public boolean hasTarget() {
        return targetReady;
    }

    public int targetCellCount() {
        return targetCount;
    }

    public boolean[] targetMask() {
        return targetMask;
    }

    public boolean[] shadowMask() {
        return shadowMask;
    }

    // ------------------------------------------------------------------
    // RASTERISING
    // ------------------------------------------------------------------

    /**
     * Paint one polygon into a boolean grid using an even-odd scanline fill.
     * Even-odd rather than winding means the result does not depend on which
     * way the vertices happen to be listed, which removes a whole class of
     * "why is my triangle inverted" bugs.
     *
     * @return true if anything was painted
     */
    private boolean paintPolygon(float[] poly, boolean[] mask) {
        if (poly == null || poly.length < 6) return false;

        final float cellW = (ShadowCalculator.WALL_RIGHT - ShadowCalculator.WALL_LEFT) / gridWidth;
        final float cellH = (ShadowCalculator.WALL_BOTTOM - ShadowCalculator.WALL_TOP) / gridHeight;

        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 1; i < poly.length; i += 2) {
            if (poly[i] < minY) minY = poly[i];
            if (poly[i] > maxY) maxY = poly[i];
        }
        int rowStart = rowFor(minY, cellH);
        int rowEnd = rowFor(maxY, cellH);
        if (rowStart < 0) rowStart = 0;
        if (rowEnd > gridHeight - 1) rowEnd = gridHeight - 1;

        // Reusable crossing buffer, grown on demand. A convex shape crosses a
        // scanline at most twice, but a wobbly circle approximation is convex
        // too, so 8 is comfortably enough; growth keeps it safe anyway.
        float[] xs = new float[8];

        int n = poly.length / 2;
        boolean painted = false;

        for (int gy = rowStart; gy <= rowEnd; gy++) {
            // Sample the horizontal line through the middle of this row.
            float y = ShadowCalculator.WALL_TOP + (gy + 0.5f) * cellH;

            // Collect, then sort, the x where the scanline crosses an edge.
            int crossings = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float ay = poly[i * 2 + 1];
                float by = poly[j * 2 + 1];
                if ((ay > y) == (by > y)) continue;   // edge does not straddle y
                if (crossings == xs.length) {
                    float[] bigger = new float[crossings * 2];
                    System.arraycopy(xs, 0, bigger, 0, crossings);
                    xs = bigger;
                }
                float ax = poly[i * 2];
                float bx = poly[j * 2];
                xs[crossings++] = ax + (y - ay) * (bx - ax) / (by - ay);
            }
            if (crossings < 2) continue;
            Arrays.sort(xs, 0, crossings);

            // Even-odd: fill between crossing 0-1, then 2-3, and so on.
            for (int c = 0; c + 1 < crossings; c += 2) {
                int gx0 = colFor(xs[c], cellW);
                int gx1 = colFor(xs[c + 1], cellW);
                if (gx0 < 0) gx0 = 0;
                if (gx1 > gridWidth - 1) gx1 = gridWidth - 1;
                for (int gx = gx0; gx <= gx1; gx++) {
                    int idx = gy * gridWidth + gx;
                    if (!mask[idx]) {
                        mask[idx] = true;
                        painted = true;
                    }
                }
            }
        }
        return painted;
    }

    private int rowFor(float y, float cellH) {
        return (int) Math.floor((y - ShadowCalculator.WALL_TOP) / cellH);
    }

    private int colFor(float x, float cellW) {
        return (int) Math.ceil((x - ShadowCalculator.WALL_LEFT) / cellW - 0.5f);
    }

    // ------------------------------------------------------------------
    // THE MATCH TEST
    // ------------------------------------------------------------------

    /**
     * Rasterise the CURRENT combined shadow and return the match.
     *
     * @param shadowPolygons one polygon per shape, each as a flat
     *                       [x0,y0,x1,y1,...] array, already projected onto
     *                       the wall by ShadowCalculator.project()
     * @return raw IoU, 0..1
     */
    public float evaluate(float[][] shadowPolygons) {
        Arrays.fill(shadowMask, false);
        if (!targetReady) return 0f;
        for (float[] poly : shadowPolygons) {
            paintPolygon(poly, shadowMask);
        }
        return intersectionOverUnion();
    }

    /** Intersection over union of the two masks. 0..1. */
    public float intersectionOverUnion() {
        int inter = 0;
        int union = 0;
        for (int i = 0; i < cellCount; i++) {
            boolean t = targetMask[i];
            boolean s = shadowMask[i];
            if (t && s) inter++;
            if (t || s) union++;
        }
        if (union == 0) return 0f;
        return (float) inter / (float) union;
    }

    /**
     * How much of the target is covered, ignoring any over-coverage.
     * Only used for a diagnostic readout, never for completing a level.
     */
    public float coverageOnly() {
        if (targetCount == 0) return 0f;
        int inter = 0;
        for (int i = 0; i < cellCount; i++) {
            if (targetMask[i] && shadowMask[i]) inter++;
        }
        return (float) inter / (float) targetCount;
    }

    /**
     * THE SCORE THE PLAYER SEES, 0..100.
     *
     * The raw IoU is rescaled against MATCH_THRESHOLD, so 100% always means
     * "this level is finished" regardless of what MATCH_THRESHOLD is set to.
     * That keeps the HUD honest when you retune the difficulty, and it means
     * the gentle chimes at 50% / 75% always line up with real progress rather
     * than with a raw number that means nothing to a player.
     */
    public float reportMatch(float rawIoU) {
        if (MATCH_THRESHOLD <= 0f) return 0f;
        float progress = rawIoU / MATCH_THRESHOLD;
        if (progress < 0f) progress = 0f;
        if (progress > 1f) progress = 1f;
        return progress * 100f;
    }

    public boolean isComplete(float rawIoU) {
        return targetReady && rawIoU >= MATCH_THRESHOLD;
    }

    // ------------------------------------------------------------------
    // GENTLE FEEDBACK CHIMES
    // ------------------------------------------------------------------

    /** Threshold indices, in the order the player will cross them. */
    public static final int CHIME_HALF = 0;
    public static final int CHIME_GOOD = 1;
    public static final int CHIME_DONE = 2;

    /**
     * Check whether the player has just crossed one of the 50 / 75 / 100
     * marks, and remember it so it only ever fires once per crossing.
     *
     * Call this with the raw IoU after evaluate(). The returned value is a
     * bitmask, or 0 for "nothing new to celebrate".
     *
     * The player is nudged *backwards* freely, and that is allowed: the
     * celebratory marks are one-way for a given level attempt so a shape that
     * drifts off by accident does not machine-gun the sound effect. Reset
     * with resetCelebration() when a level restarts.
     */
    public int newlyCrossedChimes(float rawIoU) {
        int fired = 0;
        float pct = reportMatch(rawIoU);

        float[] marks = {50f, 75f, 100f};
        for (int i = 0; i < marks.length; i++) {
            if (pct >= marks[i] && celebratedUpTo < i) {
                celebratedUpTo = i;
                fired |= (1 << i);
            }
        }
        return fired;
    }

    /** Allow the chimes to fire again; call when a level (re)starts. */
    public void resetCelebration() {
        celebratedUpTo = -1;
    }
}
