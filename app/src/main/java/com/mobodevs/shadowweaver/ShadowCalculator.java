package com.mobodevs.shadowweaver;

/**
 * Shadow Weaver - ShadowCalculator
 * ================================
 * THE CORE UNIQUE SYSTEM, PART 1: TURNING A SHAPE INTO A SHADOW.
 *
 * The whole game is a fake-3D "shadow puppet" box. This class owns the
 * coordinate spaces and the projection rule, so it is the only place in the
 * project that knows how light behaves. Everything else just feeds it shapes
 * and draws the polygons it returns.
 *
 * ==================================================================
 * WHAT CHANGED IN THE POLISH PASS (and why)
 * ==================================================================
 * 1. THE SHADOW IS NOW SCALED ABOUT THE SHAPE'S TRUE CENTRE, NOT ITS
 *    VERTEX MEAN. The old code recentred every silhouette on the average of
 *    its vertices. That is only the real centre for a circle - for an
 *    isosceles triangle the vertex mean sits one third of the height BELOW
 *    the geometric centre, so every triangle in the game had its shadow
 *    quietly nudged off the point the level asked for. It was self-
 *    consistent (the level table is authored in wall space, so the picture
 *    still looked right) but it meant "where the shadow lands" and "where the
 *    shape is" disagreed by a shape-dependent amount. Now the scale is taken
 *    about (shape.x, shape.y), which is the point rotation is about and
 *    therefore the only defensible centre.
 *
 * 2. THE PROJECTION IS NOW WRITTEN AS THE ACTUAL RAY, NOT AS A SHORTCUT.
 *    The old wallXFor looked like "a linear offset from the candle" and was
 *    easy to misread as a fake. It is the exact similar-triangle result for
 *    a shape whose plane is parallel to the wall (parallel planes under a
 *    point light project to similar figures - there is no keystoning, which
 *    is why the scale is uniform). The trigonometry is now spelled out step
 *    by step, with the free parameter exposed, so the physical claim can be
 *    checked rather than trusted. See projectCore() and wallXFor().
 *
 * 3. A RAKE TERM WAS ADDED. A candle is never directly above the middle of
 *    its wall; light arrives at an angle, and an off-axis light shears every
 *    shadow sideways in proportion to how far the caster sits above the
 *    floor. This was missing entirely, and it is why the old shadows looked
 *    stamped on rather than cast: they never leaned as they grew. Controlled
 *    by rakeStrength; set it to 0 for the old perfectly-vertical behaviour.
 *
 * 4. SHADOWS NOW HAVE A PENUMBRA. A point light produces a knife-sharp edge,
 *    a real flame is a disc about 1-2mm across, and the further the caster is
 *    from the wall the wider that disc smears the edge. So the soft band is
 *    computed from the actual gap distance (t - 1) plus how obliquely the
 *    shape sits off the light's axis. The upshot is both prettier and more
 *    informative: a shape held high is soft AND large, a shape held low is
 *    crisp AND small, so the softness doubles as a depth cue. The edge is
 *    built as a true Minkowski dilation of the silhouette (a convex offset
 *    along vertex bisectors), not a scale-up - a scale-up would stretch the
 *    ENDS of a rod as well as its sides, which reads as a zoom rather than
 *    as blur.
 *
 * 5. MULTI-LAYER FALLOFF. One shadow is no longer one flat translucent
 *    polygon. It is three concentric bands (outer / mid / core) with falling
 *    alpha, which is what produces a gradient from a dark core to nothing.
 *    See the LAYER_* constants and Shadow.projected layers.
 *
 * 6. OVERLAP BLENDING IS THE RENDERER'S JOB, BUT THE DATA IS HERE. The
 *    correct way to get "overlapping shadows are darker" is to composite
 *    every shadow into one transparent buffer and then draw that buffer once;
 *    stacking flat shapes on the main canvas gives you alpha-over-alpha,
 *    which saturates differently and looks muddy. The layers below are built
 *    so that GameRenderer can do exactly that, and the core band of every
 *    shadow is the SAME polygon the matcher scores - so what you see and what
 *    is graded can never drift apart.
 *
 * ------------------------------------------------------------------
 * THE THREE COORDINATE SPACES
 * ------------------------------------------------------------------
 * 1) SCENE SPACE (units, resolution independent)
 *    A fixed virtual stage of SCENE_W x SCENE_H units. Every shape position
 *    and every level definition is stored here, NOT in pixels. That is what
 *    makes the game look identical on a 720p phone and a 1440p phone, and it
 *    is also what lets the match test be resolution independent.
 *    GameView keeps one float[] called "sceneToScreen" (a simple scale + pan)
 *    to convert scene -> pixels at draw time.
 *
 * 2) STAGE AREA (a rectangle in scene space)
 *    The band between STAGE_TOP and STAGE_BOTTOM where shapes live. Shapes
 *    higher up the stage are physically CLOSER TO THE CANDLE, therefore
 *    higher above the floor, therefore cast a LARGER shadow. This is the
 *    mechanic the whole game is built on and it falls straight out of the
 *    projection below - it is not a special case in the code.
 *
 * 3) WALL SPACE (a rectangle in scene space, the bottom strip of the screen)
 *    Where shadows land. WALL_TOP..WALL_BOTTOM, WALL_LEFT..WALL_RIGHT.
 *
 * ------------------------------------------------------------------
 * THE PROJECTION, IN PLAIN ENGLISH
 * ------------------------------------------------------------------
 * Physically, a point light at height Lz above a floor projects a point that
 * sits at height pz above that floor onto the floor along the ray between
 * them, and the magnification is the classic ratio  Lz / (Lz - pz):
 *
 *      t = Lz / (Lz - pz)          <- "how much bigger does it get"
 *      scale = 1 + K * (t - 1)     <- softened, see below
 *
 * A shape at pz -> 0 sits almost on the floor and casts almost no shadow
 * (t -> 1). A shape lifted high, close to the candle, casts a huge one
 * (t -> large). That is real perspective behaviour and it is exactly the
 * "closer to the light = bigger, more spread out shadow" rule from the design.
 *
 * Two deliberate stylisations keep it readable on a small screen:
 *
 *   (a) scale = 1 + K*(t-1) instead of raw t. Raw perspective is far too
 *       violent (it triples the shadow) and would make almost every placement
 *       blow up off the wall. K blends between "orthographic" (K=0, no size
 *       change with distance, which is physically wrong but very forgiving)
 *       and "full perspective" (K=1). 0.55 is the shipped sweet spot: close
 *       shapes are about 1.6x the size of far shapes, which is clearly
 *       readable but never blows up. Raise K for drama, drop it for a
 *       gentler puzzle.
 *
 *   (b) The shape's height above the floor is derived from how high it sits
 *       on the stage, and the shadow's vertical position on the wall travels
 *       over a fixed VERT_RANGE band. So:
 *         - drag a shape UP  -> bigger shadow, lower on the wall, softer edge,
 *           and (new) a little further to the side because of the rake
 *         - drag a shape DOWN-> smaller shadow, higher on the wall, crisper edge
 *         - drag a shape LEFT/RIGHT -> the shadow slides away from the
 *           candle's vertical line, magnified by the current scale
 *       Moving a shape therefore gives the player 3 meaningful controls
 *       (size, depth on the wall, sideways offset) plus rotation. That is the
 *       puzzle.
 *
 * The candle's x/y position is what the "position relative to the light
 * source's projection line" rule means: the shadow is always measured from
 * the candle, so shapes to the left of it throw shadows further left.
 *
 * NOTE: this class is pure Java (no android.* imports) so the projection can
 * be unit tested on a plain JVM. It returns flat float[] arrays of points.
 */
public final class ShadowCalculator {

    // ------------------------------------------------------------------
    // SCENE GEOMETRY - all the level design is expressed against these
    // ------------------------------------------------------------------

    /** Virtual scene is 1000 x 1600 units. Everything below is in these units. */
    public static final float SCENE_W = 1000f;
    public static final float SCENE_H = 1600f;

    /** The candle: fixed at the top, centre horizontally. */
    public static final float LIGHT_X = 500f;
    public static final float LIGHT_Y = 215f;
    /** Candle height above the floor, in normalised units. Keep at 1.0. */
    public static final float LIGHT_Z = 1.0f;

    /** Where shapes may be dragged. */
    public static final float STAGE_LEFT = 120f;
    public static final float STAGE_RIGHT = 880f;
    public static final float STAGE_TOP = 290f;
    public static final float STAGE_BOTTOM = 1050f;

    /** The wall: the bottom strip of the screen that receives the shadows. */
    public static final float WALL_LEFT = 40f;
    public static final float WALL_RIGHT = 960f;
    public static final float WALL_TOP = 1110f;
    public static final float WALL_BOTTOM = 1530f;

    /**
     * A shadow's CENTRE is only ever allowed to travel across the middle of
     * the wall, leaving a margin at the top and bottom so that even the
     * largest possible shadow still lands fully on the wall instead of being
     * clipped by the screen edge.
     *
     * SIZING RULE if you add a new shape type: base half-height must stay at
     * or below about 62 units, because at the bottom of the stage the
     * magnification is ~1.6 and 62 * 1.6 ~= 100 = WALL_MARGIN.
     */
    public static final float WALL_MARGIN = 100f;

    // ------------------------------------------------------------------
    // TUNING - see the notes above before changing any of these
    // ------------------------------------------------------------------

    /**
     * Perspective strength, 0..1.
     * 0 = shadows never change size with distance (very forgiving).
     * 1 = physically exact perspective (very dramatic, shadows fly off).
     * 0.55 is the shipped value.
     */
    public static float perspectiveStrength = 0.55f;

    /** Height above the floor of a shape sitting at the very bottom of the stage. */
    public static float heightNearFloor = 0.10f;

    /** Height above the floor of a shape sitting at the very top of the stage. */
    public static float heightNearCandle = 0.52f;

    /**
     * How far (in scene units) a shadow's centre travels vertically on the
     * wall as its shape is dragged from the bottom of the stage to the top.
     * This is what turns vertical dragging into a second, separate control.
     *
     * The wall is deliberately sized to exactly
     *     WALL_TOP + WALL_MARGIN + verticalRange + WALL_MARGIN == WALL_BOTTOM
     * so the whole travel range is usable and no wall is wasted. The upper
     * bound of the centre band is therefore DERIVED as
     * (centreLo + verticalRange) and never stated separately, so the two can
     * never disagree and silently clamp level data.
     *
     * The shipped level table is verified against 220; change this and re-run
     * tools/LevelDesignTest.java to re-verify.
     */
    public static float verticalRange = 220f;

    // ------------------------------------------------------------------
    // NEW IN THE POLISH PASS - RAKE
    //
    // A real candle does not sit directly above the centre of its wall: the
    // flame leans, the wick is off-centre, and the box is not symmetric. An
    // off-axis light produces a RAKED shadow - a shadow that leans further
    // sideways the higher its caster is held. Physically that displacement is
    //
    //     shear = pz * tan(rakeAngle) * (light-to-floor distance)
    //
    // which is linear in the caster's height pz, so that is the form used
    // here. RAKE_REACH is the combined constant: it is
    // tan(rakeAngle) * LIGHT_Z mapped into scene units, chosen so the total
    // sweep across the whole stage (pz 0.10 -> 0.52) is about 33 units, or
    // 3% of the screen width - enough that you can see a shadow tilt as it
    // grows, not enough to fight the player for control.
    //
    // Setting this to 0 restores the pre-polish, perfectly-vertical shadows.
    // It is additive and depends only on pz, so the loader's inverse stays
    // exact (see solveStagePositionFor).
    // ------------------------------------------------------------------

    public static float rakeStrength = 1f;

    /** Scene units of sideways shear per unit of caster height. */
    private static final float RAKE_REACH = 70f;

    // ------------------------------------------------------------------
    // NEW IN THE POLISH PASS - PENUMBRA
    //
    // A point light makes a hard-edged shadow. A flame is a disc roughly a
    // millimetre or two across, and that disc is what actually occludes, so
    // the edge it casts is soft - and the softness scales with how far the
    // caster is from the receiving surface, because the shadow of the light's
    // own edge grows with the gap. In projection terms the soft band is
    // therefore proportional to (t - 1), which is exactly the quantity that
    // also drives magnification. That is why in this game a big shadow is
    // always a soft one, and it costs the player nothing to learn: the
    // softness doubles as a depth read.
    //
    // A second, smaller term softens shadows cast from far off the light's
    // axis, because those rays arrive at a much more oblique angle and so
    // sweep the light's disc further.
    // ------------------------------------------------------------------

    /** Edge softness even for a shape sitting on the stage floor, scene units. */
    public static float penumbraBase = 2.5f;

    /** Extra softness per unit of (t - 1), i.e. per unit of caster height. */
    public static float penumbraGain = 20f;

    /** Extra softness at the far edge of the stage, scene units. */
    public static float offAxisSoftening = 3.0f;

    /** Hard ceiling on the soft band, so a huge shadow cannot wash out. */
    public static float penumbraMax = 30f;

    /**
     * Physical size of the flame in scene units, kept for documentation and
     * for anyone who wants to re-derive penumbraBase/Gain from it. The
     * penumbra of a disc of radius R at gap g, seen from a source at distance
     * L, is  R * g / (L - g)  - which is what penumbraGain encodes.
     */
    public static float lightRadius = 7f;

    // ------------------------------------------------------------------
    // LAYER / ALPHA PROFILE
    //
    // A shadow is drawn as three nested bands, widest and faintest first.
    // The centre of the shadow therefore accumulates
    //     1 - (1-outer)*(1-mid)*(1-core)
    // of the tint, and steps down smoothly to 0 at the outer edge. Two or
    // three bands is the sweet spot on a software-rasterised SurfaceView: more
    // bands look marginally smoother and cost noticeably more fill rate.
    // ------------------------------------------------------------------

    public static final int LAYER_OUTER = 0;
    public static final int LAYER_MID = 1;
    public static final int LAYER_CORE = 2;

    /**
     * Per-band alpha and outward offset, as a fraction of the shadow's total
     * penumbra. Offset fractions are 1.0 / 0.45 / 0.0, so the bands tile the
     * soft edge without leaving gaps between them.
     */
    private static final float[] LAYER_ALPHA = {30f, 52f, 224f};
    private static final float[] LAYER_SPREAD = {1.0f, 0.45f, 0.0f};

    /** The near-black the shadow is composited in, so it is never pure black. */
    public static final int SHADOW_TINT = 0x0E0703;

    /** Alpha for one band, 0..255. Layer ids are the LAYER_* constants. */
    public static int layerAlpha(int layer) {
        if (layer < 0) layer = 0;
        if (layer > LAYER_CORE) layer = LAYER_CORE;
        return Math.round(LAYER_ALPHA[layer]);
    }

    /** Outward offset of one band, as a fraction of the shadow's penumbra. */
    public static float layerSpread(int layer) {
        if (layer < 0) layer = 0;
        if (layer > LAYER_CORE) layer = LAYER_CORE;
        return LAYER_SPREAD[layer];
    }

    // Cached derived values, refreshed by refreshDerived() after any retune.
    private static float sCenterLo = WALL_TOP + WALL_MARGIN;
    private static float sCenterHi = sCenterLo + verticalRange;
    private static boolean sDirty = true;

    private ShadowCalculator() {
    }

    /** Recompute the cached centre band. Call after mutating verticalRange. */
    public static void refreshDerived() {
        sDirty = true;
    }

    private static void ensureDerived() {
        if (sDirty) {
            sCenterLo = WALL_TOP + WALL_MARGIN;
            sCenterHi = sCenterLo + verticalRange;
            sDirty = false;
        }
    }

    // ==================================================================
    // A COMPLETE SHADOW: core silhouette plus its two soft bands.
    //
    // Returned by projectDetailed() for the renderer. The matcher only ever
    // wants `core`, which is bit-for-bit the same polygon project() would
    // have returned, so the picture being graded and the picture being drawn
    // are the same object.
    // ==================================================================

    public static final class Shadow {

        /** The silhouette itself, in wall space. This is what gets scored. */
        public final float[] core;
        /** Inner soft band, slightly dilated. Draw after `outer`. */
        public final float[] mid;
        /** Outer soft band, the full penumbra. Draw first. */
        public final float[] outer;

        /** Total soft-edge width in scene units. */
        public final float penumbra;
        /** 0..1, where 0 is a razor edge and 1 is the softness ceiling. */
        public final float softness;
        /** The magnification that was applied, for effects that want it. */
        public final float scale;
        /** Where the shadow's true centre landed on the wall. */
        public final float centerX;
        public final float centerY;

        Shadow(float[] core, float[] mid, float[] outer,
               float penumbra, float scale, float cx, float cy) {
            this.core = core;
            this.mid = mid;
            this.outer = outer;
            this.penumbra = penumbra;
            this.scale = scale;
            this.centerX = cx;
            this.centerY = cy;
            float lo = penumbraBase;
            this.softness = penumbraMax > lo
                    ? clamp((penumbra - lo) / (penumbraMax - lo), 0f, 1f)
                    : 0f;
        }

        /** The band for a LAYER_* id, so callers can loop without branching. */
        public float[] layer(int which) {
            if (which == LAYER_OUTER) return outer;
            if (which == LAYER_MID) return mid;
            return core;
        }
    }

    // ------------------------------------------------------------------
    // THE MODEL, QUERY BY QUERY
    // ------------------------------------------------------------------

    /**
     * 0 at the bottom of the stage, 1 at the top.
     * This single value drives both the shadow's size and its depth on the
     * wall, which is why it is centralised here.
     */
    public static float heightFraction(float shapeY) {
        float u = (STAGE_BOTTOM - shapeY) / (STAGE_BOTTOM - STAGE_TOP);
        if (u < 0f) u = 0f;
        if (u > 1f) u = 1f;
        return u;
    }

    /** Shape height above the floor, from its position on the stage. */
    public static float heightAboveFloor(float shapeY) {
        return heightNearFloor
                + (heightNearCandle - heightNearFloor) * heightFraction(shapeY);
    }

    /** Raw perspective magnification t = Lz/(Lz-pz). */
    public static float perspectiveRatio(float shapeY) {
        return LIGHT_Z / (LIGHT_Z - heightAboveFloor(shapeY));
    }

    /** The softened magnification actually used for drawing. 1.0 .. ~1.6. */
    public static float scaleFor(float shapeY) {
        float t = perspectiveRatio(shapeY);
        return 1f + perspectiveStrength * (t - 1f);
    }

    /**
     * How far the light is RAKED off the wall's centre line, for a caster at
     * this height, in scene units.
     *
     * Linear in the caster's height because the shear of an off-axis light is
     * linear in the caster's height - the same reason a long shadow from a
     * low sun gets longer as the object rises. Purely a function of y, which
     * is what keeps solveStagePositionFor() an exact inverse: the loader
     * solves for y first, then subtracts this before dividing by the scale.
     */
    public static float rakeFor(float shapeY) {
        return rakeStrength * RAKE_REACH * heightAboveFloor(shapeY);
    }

    /**
     * Where the centre of that shape's shadow lands vertically on the wall.
     *
     * NOTE ON WHY THIS IS NOT A RAY. Projecting the shape's own y through the
     * light the same way as its x would put a shadow cast from the middle of
     * the stage at y ~ 800, which is in the middle of the screen, not on the
     * wall at all. That is physically what would happen if the wall were a
     * floor - and for this game, the wall IS a floor, seen from above at a
     * fixed camera angle. Rather than fake the camera, the depth axis is
     * remapped into a compact band of the wall. Every x value still goes
     * through the real ray in wallXFor(); only the depth axis is stylised,
     * and it is stylised consistently for the target, the player's shadows
     * and the level loader, so nothing can desynchronise.
     */
    public static float wallYFor(float shapeY) {
        ensureDerived();
        return sCenterLo + verticalRange * heightFraction(shapeY);
    }

    /**
     * The shadow's centre x, from the light-to-shape ray extended to the wall.
     *
     * Spelled out, because this is the one piece of the game that has to be
     * real:
     *
     *   The flame is the point L = (LIGHT_X, *, LIGHT_Z) over the floor.
     *   The shape is a point P = (shapeX, *, pz) - same plane, so no depth
     *   of its own; pz comes from where it sits on the stage.
     *   The ray L -> P hits the receiving surface at Q, and by similar
     *   triangles:
     *
     *       Qx - LIGHT_X  =  (Px - LIGHT_X) * Lz / (Lz - pz)
     *                        \_________________________/
     *                                 t, the magnification
     *
     *   so Qx = LIGHT_X + t * (Px - LIGHT_X). With the softening constant K
     *   folded in, the shipped rule is
     *
     *       Qx = LIGHT_X + scaleFor(y) * (Px - LIGHT_X) + rakeFor(y)
     *
     *   The three cases, which is the whole design in one line:
     *     - a shape directly under the flame (Px == LIGHT_X) always casts
     *       straight down, whatever its height: the (Px - LIGHT_X) term is
     *       zero, so height alone changes size, never position;
     *     - a shape to the LEFT of the flame throws a shadow further left,
     *       and further still the higher it is held, because t > 1;
     *     - the rake term leans every shadow by an amount that grows with
     *       height, which is what a real off-axis flame does.
     *
     * Because the two position terms depend only on y and the scale is
     * invertible, solveStagePositionFor() can still recover the stage
     * position exactly, and the level table stays authored in wall space.
     */
    public static float wallXFor(float shapeX, float shapeY) {
        return LIGHT_X + scaleFor(shapeY) * (shapeX - LIGHT_X) + rakeFor(shapeY);
    }

    /**
     * Half-width of the soft edge for a caster at this position.
     *
     * Two physical terms, plus a floor and a ceiling:
     *   (1 - t) is proportional to the caster-to-wall gap, and the penumbra of
     *         a finite light source is exactly that gap times the light's own
     *         angular radius - so this is the dominant term;
     *   |x - LIGHT_X| makes shadows cast from far off-axis softer, because
     *         their rays cross the flame's disc at a much shallower angle.
     *
     * penumbraBase keeps even a floor-sitting shape from having a
     * mathematically perfect edge, which would look like a cut-out.
     */
    public static float penumbraFor(float shapeX, float shapeY) {
        float t = perspectiveRatio(shapeY);
        float offAxis = Math.abs(shapeX - LIGHT_X)
                / Math.max(1f, (WALL_RIGHT - WALL_LEFT));
        float p = penumbraBase
                + penumbraGain * (t - 1f)
                + offAxisSoftening * offAxis;
        return clamp(p, 0f, penumbraMax);
    }

    // ------------------------------------------------------------------
    // THE PROJECTION ITSELF
    // ------------------------------------------------------------------

    /**
     * PROJECTION: the one function that turns a shape into a shadow.
     *
     * Pipeline:
     *   1. ShapeObject.applyTransform()  -> silhouette rotated + moved into
     *      scene space (rotation is about the shape centre).
     *   2. here: scale the silhouette about ITS OWN CENTRE (shape.x, shape.y)
     *      by scaleFor(), because a real shadow grows away from the light.
     *      This is the point the rotation was taken about, so scaling here
     *      cannot slide the shadow off the position the level asked for.
     *   3. here: move the scaled silhouette to the wall position
     *      (wallXFor / wallYFor), which is measured from the candle.
     *
     * Steps 2 and 3 are a "scale about centre, then translate" affine map,
     * which is exactly a perspective projection of a flat object whose plane
     * is parallel to the receiving surface. Doing it this way (rather than
     * projecting each vertex independently from the candle) is what keeps a
     * rotated rod's thickness proportional. It cannot come apart or invert
     * for the same reason: parallel planes under a point light produce
     * similar figures, so the map is a similarity and is exactly invertible.
     *
     * @return flat [x0,y0, x1,y1, ...] polygon in SCENE space, on the wall.
     */
    public static float[] project(ShapeObject shape) {
        float[] scene = shape.applyTransform();
        float s = scaleFor(shape.y);
        float wx = wallXFor(shape.x, shape.y);
        float wy = wallYFor(shape.y);

        float[] out = new float[scene.length];
        // Scale about the shape's own position, not the mean of its vertices.
        // For a circle these agree; for a triangle they do not, and using the
        // vertex mean put every triangle's shadow a third of its height away
        // from where the level had asked for it.
        float cx = shape.x;
        float cy = shape.y;
        for (int i = 0; i < scene.length; i += 2) {
            out[i] = wx + (scene[i] - cx) * s;
            out[i + 1] = wy + (scene[i + 1] - cy) * s;
        }
        return out;
    }

    /**
     * The full soft-edged shadow for drawing: the core polygon plus the two
     * penumbra bands around it.
     *
     * The bands are the core silhouette dilated outwards by a fixed distance
     * along each vertex's angle bisector - a true offset curve, not a scale-up.
     * The distinction matters for two reasons. A scale-up multiplies every
     * offset by the distance from the shape's centre, so the "penumbra" would
     * be thickest at the corners and ends and vanish in the middle, and it
     * would make the DARK part of the shadow a different size from the true
     * projection. A dilation keeps the core pixel-identical to the silhouette
     * and adds a constant-d wide margin all the way round, so the shape the
     * player is asked to match is exactly the shape they can see. Nothing
     * here is scored, so the extra geometry costs the matcher nothing.
     */
    public static Shadow projectDetailed(ShapeObject shape) {
        float[] core = project(shape);
        float p = penumbraFor(shape.x, shape.y);
        if (p < 0.5f) {
            // Sharp enough that dilating would only add invisible fill.
            return new Shadow(core, core, core, 0f, scaleFor(shape.y),
                    wallXFor(shape.x, shape.y), wallYFor(shape.y));
        }
        float[] mid = offsetConvex(core, p * LAYER_SPREAD[LAYER_MID]);
        float[] outer = offsetConvex(core, p * LAYER_SPREAD[LAYER_OUTER]);
        return new Shadow(core, mid, outer, p, scaleFor(shape.y),
                wallXFor(shape.x, shape.y), wallYFor(shape.y));
    }

    /**
     * Dilate a convex polygon outward by a constant distance d.
     *
     * This is the Minkowski sum of the polygon with a disc of radius d, which
     * is the correct shape for a penumbra: everywhere on the result is exactly
     * d away from the original, so the soft margin is a constant thickness all
     * the way round. It is NOT the same as scaling the polygon about its
     * centre, which is what a naive "make the shadow bigger and fainter" does
     * - that makes the apparent blur grow with distance from the middle, so a
     * rod's ends look far softer than its middle and the silhouette itself
     * changes length.
     *
     * Per vertex, the offset direction is the bisector of the two adjacent
     * edge normals, scaled by the miter factor 1/cos(theta/2) so the surface
     * stays a constant distance out even at a corner. The miter is clamped so
     * a needle-sharp corner cannot throw a long spike across the wall.
     *
     * Every silhouette in the game is convex (rect, square, line-as-rect,
     * triangle, or a regular polygon approximating a circle), which is the
     * precondition for the bisector construction to be valid.
     */
    private static float[] offsetConvex(float[] p, float d) {
        final int n = p.length / 2;
        if (n < 3 || d == 0f) return p;

        float[] out = new float[p.length];
        for (int i = 0; i < n; i++) {
            int prev = (i - 1 + n) % n;
            int next = (i + 1) % n;

            float e1x = p[i * 2] - p[prev * 2];
            float e1y = p[i * 2 + 1] - p[prev * 2 + 1];
            float e2x = p[next * 2] - p[i * 2];
            float e2y = p[next * 2 + 1] - p[i * 2 + 1];

            float l1 = (float) Math.sqrt(e1x * e1x + e1y * e1y);
            float l2 = (float) Math.sqrt(e2x * e2x + e2y * e2y);
            if (l1 < 1e-6f) l1 = 1f;
            if (l2 < 1e-6f) l2 = 1f;

            // Outward normal of an edge (ex,ey) in a y-down, clockwise
            // silhouette is (ey,-ex). Every shape in the game is wound this
            // way, so one rule covers all of them.
            float m1x = e1y / l1, m1y = -e1x / l1;
            float m2x = e2y / l2, m2y = -e2x / l2;

            float bx = m1x + m2x;
            float by = m1y + m2y;
            float bl = (float) Math.sqrt(bx * bx + by * by);

            float ox, oy;
            if (bl < 1e-4f) {
                // 180-degree reversal (a degenerate spike): fall back to one
                // of the face normals rather than dividing by zero.
                ox = m2x;
                oy = m2y;
            } else {
                ox = bx / bl;
                oy = by / bl;
                float dot = ox * m2x + oy * m2y;
                // Clamp at cos(70 degrees) == 0.34, i.e. a miter of at most
                // ~2.9x. Past that the edge is so acute that the true offset
                // would shoot off across the wall.
                float miter = (dot > 0.34f) ? (1f / dot) : 1f;
                if (miter < 1f) miter = 1f;
                if (miter > 3f) miter = 3f;
                ox *= miter;
                oy *= miter;
            }

            out[i * 2] = p[i * 2] + ox * d;
            out[i * 2 + 1] = p[i * 2 + 1] + oy * d;
        }
        return out;
    }

    /**
     * INVERSE of the projection, used by the level loader.
     *
     * Levels are authored in WALL space (where you can actually see the
     * picture being composed) and converted into stage positions here. This
     * is what guarantees a hand-written level is always reachable: we solve
     * for the exact stage position that produces the shadow we asked for.
     *
     * The two axes are solved independently, which is only possible because
     * the depth mapping is a pure function of y and the lateral mapping is
     * linear in x for a given y:
     *
     *   wallY = centreLo + verticalRange * u,  u from y   -> solve y first
     *   wallX = LIGHT_X + scale(y) * (x - LIGHT_X) + rake(y)
     *                                      -> then remove BOTH offsets
     *                                         (the light's x, and the rake)
     *                                         and divide by the scale
     *
     * Order matters: the rake has to be removed AFTER y is known, because it
     * depends on y. tools/LevelDesignTest.java asserts this round-trips to
     * within 0.01 scene units for every shipped level, which is what lets the
     * level table be authored as a picture instead of as stage coordinates.
     *
     * @param wallX  desired shadow centre x on the wall, in scene units
     * @param wallY  desired shadow centre y on the wall, in scene units
     * @param out    length-2 array; receives the stage position {x, y}
     */
    public static void solveStagePositionFor(float wallX, float wallY, float[] out) {
        ensureDerived();
        float u = (wallY - sCenterLo) / verticalRange;
        if (u < 0f) u = 0f;
        if (u > 1f) u = 1f;

        float stageY = STAGE_BOTTOM - u * (STAGE_BOTTOM - STAGE_TOP);
        float s = 1f + perspectiveStrength * (perspectiveRatio(stageY) - 1f);
        // wallX - LIGHT_X - rake = s * (stageX - LIGHT_X)
        float stageX = LIGHT_X + (wallX - LIGHT_X - rakeFor(stageY)) / s;

        out[0] = stageX;
        out[1] = stageY;
    }

    /**
     * The vertical band on the wall that a shadow's CENTRE may occupy.
     * The loader uses this to keep every level inside the drawable wall.
     */
    public static float wallCentreRangeLo() {
        ensureDerived();
        return sCenterLo;
    }

    public static float wallCentreRangeHi() {
        ensureDerived();
        return sCenterHi;
    }

    // ------------------------------------------------------------------
    // STAGE CLAMPING - keeps shapes draggable where they belong
    // ------------------------------------------------------------------

    public static float clampStageX(float x) {
        return clamp(x, STAGE_LEFT, STAGE_RIGHT);
    }

    public static float clampStageY(float y) {
        return clamp(y, STAGE_TOP, STAGE_BOTTOM);
    }

    /**
     * Damped stage positioning during active drag (Emil Kowalski boundary damping).
     * Applies smooth logarithmic resistance past the boundary instead of a rigid wall.
     */
    public static float dampStageX(float x) {
        if (x < STAGE_LEFT) {
            float over = STAGE_LEFT - x;
            return STAGE_LEFT - (float) Math.log1p(over * 0.04f) * 16f;
        } else if (x > STAGE_RIGHT) {
            float over = x - STAGE_RIGHT;
            return STAGE_RIGHT + (float) Math.log1p(over * 0.04f) * 16f;
        }
        return x;
    }

    public static float dampStageY(float y) {
        if (y < STAGE_TOP) {
            float over = STAGE_TOP - y;
            return STAGE_TOP - (float) Math.log1p(over * 0.04f) * 16f;
        } else if (y > STAGE_BOTTOM) {
            float over = y - STAGE_BOTTOM;
            return STAGE_BOTTOM + (float) Math.log1p(over * 0.04f) * 16f;
        }
        return y;
    }

    public static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
