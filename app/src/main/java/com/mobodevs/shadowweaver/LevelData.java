package com.mobodevs.shadowweaver;

/**
 * Shadow Weaver - LevelData
 * =========================
 * A pure data description of one level. Contains no behaviour beyond
 * constructing the shape list, so it is trivial to serialise later (a level
 * editor, or a JSON file) without touching the game code.
 *
 * ------------------------------------------------------------------
 * HOW A LEVEL IS DEFINED
 * ------------------------------------------------------------------
 * A level is really two things:
 *
 *   1. A SOLUTION LAYOUT - for each shape, where its shadow must land on the
 *      wall and what angle it must be at, so that all the shadows together
 *      draw the intended picture. The target silhouette is not stored as a
 *      separate hand-drawn polygon; it is DERIVED from this layout by
 *      projecting the shapes, exactly as the player's own shadows are
 *      projected. See TargetForge.
 *
 *   2. A STARTING LAYOUT - the same set of shapes, scrambled around the stage
 *      so the player has something to do.
 *
 * Defining the picture in wall space (where you can actually see it) and
 * solving backwards for the stage positions is deliberate. It is what makes
 * every level guaranteed solvable: the target is produced by the same
 * projection the player's shadows go through, so if the level data is valid
 * there is always a position the player can reach. Levels were additionally
 * verified with the JVM test harness in tools/.
 *
 * The one thing a level does NOT do is hand the player the answer. The
 * starting layout is a permutation of the solution, so no shape begins in
 * the right place.
 */
public class LevelData {

    /** 1-based level number, shown in the HUD. */
    public int number;

    /** Short name of the picture being made, e.g. "House". Never scored. */
    public String title;

    /**
     * A gentle one-line nudge, e.g. "Slide the roof up until the whole
     * shape sits on the wall." Purely flavour text.
     */
    public String hint;

    /** The solution: what the shapes must be made to do. */
    public SolutionShape[] solution;

    /**
     * The puzzle: how the shapes are scattered when the level begins.
     * Index i here lines up with index i in {@link #solution}.
     */
    public StartShape[] start;

    public LevelData(int number, String title, String hint,
                     SolutionShape[] solution, StartShape[] start) {
        this.number = number;
        this.title = title;
        this.hint = hint;
        this.solution = solution;
        this.start = start;
    }

    public int shapeCount() {
        return solution == null ? 0 : solution.length;
    }

    /**
     * Build the live ShapeObject list for this level, i.e. the shapes as the
     * player first meets them.
     *
     * A small idle drift is added here, derived from the shape's index rather
     * than stored, so the level table stays readable numbers only. The drift
     * is purely cosmetic and is damped to zero the moment the player touches
     * the shape, so it can never affect the match.
     */
    public ShapeObject[] createStartingShapes() {
        int n = shapeCount();
        ShapeObject[] out = new ShapeObject[n];
        for (int i = 0; i < n; i++) {
            StartShape st = start[i];
            out[i] = new ShapeObject(st.kind, st.width, st.height,
                    st.stageX, st.stageY, st.rotation);
            applyIdleDrift(out[i], i);
        }
        return out;
    }

    /**
     * Deterministic, repeatable "floating" drift so the scene does not feel
     * frozen. Uses a fixed integer hash of the index, so it is identical on
     * every device and every run - a level always starts the same way.
     */
    private static void applyIdleDrift(ShapeObject s, int i) {
        float a = ((i * 2654435761L) % 1000) / 1000f;          // 0..1
        float b = ((i * 40503 + 17) % 1000) / 1000f;            // 0..1
        float c = ((i * 2246822519L) % 1000) / 1000f;           // 0..1
        s.vx = (a - 0.5f) * 7f;
        s.vy = (b - 0.5f) * 5f;
        s.spin = (c - 0.5f) * 3.5f;
    }

    /**
     * A shape in the solution layout, described by where its SHADOW goes.
     * Positions are in wall space, i.e. the same units and axes the player
     * sees on the wall, so a level reads like a picture.
     */
    public static class SolutionShape {
        public final ShapeObject.Kind kind;
        public final float width;
        public final float height;
        /** Shadow centre x on the wall, scene units. */
        public final float wallX;
        /** Shadow centre y on the wall, scene units. Clamped by the loader. */
        public final float wallY;
        /** Rotation in degrees, clockwise, same as Canvas.rotate(). */
        public final float rotation;

        public SolutionShape(ShapeObject.Kind kind, float width, float height,
                             float wallX, float wallY, float rotation) {
            this.kind = kind;
            this.width = width;
            this.height = height;
            this.wallX = wallX;
            this.wallY = wallY;
            this.rotation = rotation;
        }
    }

    /** Where a shape sits on the stage at the start of a level. */
    public static class StartShape {
        public final ShapeObject.Kind kind;
        public final float width;
        public final float height;
        public final float stageX;
        public final float stageY;
        public final float rotation;

        public StartShape(ShapeObject.Kind kind, float width, float height,
                          float stageX, float stageY, float rotation) {
            this.kind = kind;
            this.width = width;
            this.height = height;
            this.stageX = stageX;
            this.stageY = stageY;
            this.rotation = rotation;
        }
    }
}
