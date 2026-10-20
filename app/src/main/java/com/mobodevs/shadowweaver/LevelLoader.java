package com.mobodevs.shadowweaver;

/**
 * Shadow Weaver - LevelLoader
 * ===========================
 * Holds the 15 shipped levels and turns a level into something the game can
 * actually run: the starting shapes for the stage, and the target silhouette
 * for the wall.
 *
 * ------------------------------------------------------------------
 * WHY THE TARGET IS FORGED INSTEAD OF HAND-DRAWN
 * ------------------------------------------------------------------
 * A level's target is not stored as a hand-authored polygon. It is DERIVED
 * from the level's solution layout by running the same projection the player's
 * shadows go through. Three reasons:
 *
 *   1. A level can never be impossible. The target is produced by the exact
 *      same maths the player's shadows use, so if the table below is valid
 *      there is always a reachable position that reproduces it.
 *   2. Retuning the light (perspectiveStrength, verticalRange) automatically
 *      re-forges the target, so the game stays consistent if you change the
 *      feel of the shadows.
 *   3. A level is described in WALL space - "put these shadows here" - which
 *      is a far more readable way to author a picture than a list of polygon
 *      vertices, and it means adding levels is a data-only change.
 *
 * The levels are still hand-designed in the sense that matters: the
 * composition of each picture (a house, a star, a cat) is chosen by hand, and
 * the starting scatter is chosen so the player has real work to do. Only the
 * bookkeeping of turning that intent into pixels is automated. See
 * tools/README.md for the harness that verified all 15.
 *
 * If you would rather hand-draw a target polygon for a level, forgeTarget()
 * is the only method to change: return your own polygon list instead and the
 * rest of the game does not care where it came from.
 *
 * Pure Java, no android.* imports, so it is unit testable.
 */
public final class LevelLoader {

    private LevelLoader() {
    }

    // ------------------------------------------------------------------
    // Tiny factories, purely so the level table below stays readable
    // ------------------------------------------------------------------

    private static LevelData.SolutionShape sol(ShapeObject.Kind k, float w, float h,
                                                float wallX, float wallY, float rot) {
        return new LevelData.SolutionShape(k, w, h, wallX, wallY, rot);
    }

    private static LevelData.StartShape st(ShapeObject.Kind k, float w, float h,
                                            float stageX, float stageY, float rot) {
        return new LevelData.StartShape(k, w, h, stageX, stageY, rot);
    }

    private static LevelData level(int n, String title, String hint,
                                   LevelData.SolutionShape[] sol, LevelData.StartShape[] start) {
        return new LevelData(n, title, hint, sol, start);
    }

    /**
     * Short aliases so the level table below can read "Kind.RECT" instead of
     * the full "ShapeObject.Kind.RECT" on every single line.
     */
    private static final class Kind {
        static final ShapeObject.Kind CIRCLE = ShapeObject.Kind.CIRCLE;
        static final ShapeObject.Kind TRIANGLE = ShapeObject.Kind.TRIANGLE;
        static final ShapeObject.Kind SQUARE = ShapeObject.Kind.SQUARE;
        static final ShapeObject.Kind RECT = ShapeObject.Kind.RECT;
        static final ShapeObject.Kind LINE = ShapeObject.Kind.LINE;

        private Kind() {
        }
    }

    // ------------------------------------------------------------------
    // THE 15 SHIPPED LEVELS
    //
    // Every row below was machine-verified by tools/: the forged target fits
    // inside the wall, the starting layout scores well under 40% IoU, and no
    // two starting shapes overlap so each one is individually grabbable.
    //
    //   solution entry -> [ kind, width, height, shadowX, shadowY, rotation ]
    //   start    entry -> [ kind, width, height, stageX, stageY, rotation ]
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // THE 15 SHIPPED LEVELS
    //
    // Each level is written in WALL space: for every shape, where its shadow
    // has to land and at what angle. The stage position the player must drag
    // it to is solved for at load time by solveStagePositionFor(), so these
    // numbers stay correct even if the projection is retuned later.
    //
    // Every row below was machine-verified (see tools/README.md): the target
    // fits inside the wall, the starting layout scores well under 40% IoU,
    // and no two starting shapes overlap so each one is individually
    // grabbable.
    //
    //   solution entry -> [ kind, width, height, shadowX, shadowY, rotation ]
    //   start    entry -> [ kind, width, height, stageX, stageY, rotation ]
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // THE 15 SHIPPED LEVELS
    //
    // Each level is written in WALL space: for every shape, where its shadow
    // has to land and at what angle. The stage position the player must drag
    // it to is solved for at load time by solveStagePositionFor(), so these
    // numbers stay correct even if the projection is retuned later.
    //
    // Every row below was machine-verified (see tools/README.md): the target
    // fits inside the wall, the starting layout scores well under 40% IoU,
    // and no two starting shapes overlap so each one is individually
    // grabbable.
    //
    //   solution entry -> [ kind, width, height, shadowX, shadowY, rotation ]
    //   start    entry -> [ kind, width, height, stageX, stageY, rotation ]
    // ------------------------------------------------------------------

    // ---- Level 1: House --------------------------------------------
    // target fills 1077 grid cells; starting layout scores 15%
    private static final LevelData LV1 = level(1, "House",
            "A body, a roof above it, a slim chimney beside it.",
            new LevelData.SolutionShape[]{
                    sol(Kind.RECT, 280, 110, 460, 1420, 0),
                    sol(Kind.TRIANGLE, 300, 110, 460, 1300, 0),
                    sol(Kind.LINE, 140, 28, 650, 1280, 90),
            },
            new LevelData.StartShape[]{
                    st(Kind.RECT, 280, 110, 351.9f, 716.9f, 8),
                    st(Kind.TRIANGLE, 300, 110, 728.4f, 389.1f, 267),
                    st(Kind.LINE, 140, 28, 671.7f, 788.1f, 8),
            });

    // ---- Level 2: Tree --------------------------------------------
    // target fills 535 grid cells; starting layout scores 5%
    private static final LevelData LV2 = level(2, "Tree",
            "Two canopies one above the other, a trunk beneath.",
            new LevelData.SolutionShape[]{
                    sol(Kind.TRIANGLE, 260, 80, 500, 1230, 0),
                    sol(Kind.TRIANGLE, 300, 80, 500, 1350, 180),
                    sol(Kind.RECT, 70, 120, 500, 1430, 0),
            },
            new LevelData.StartShape[]{
                    st(Kind.TRIANGLE, 260, 80, 390.5f, 627.3f, 259),
                    st(Kind.TRIANGLE, 300, 80, 647.7f, 435.8f, 241),
                    st(Kind.RECT, 70, 120, 383.6f, 302.9f, 13),
            });

    // ---- Level 3: Star --------------------------------------------
    // target fills 666 grid cells; starting layout scores 16%
    private static final LevelData LV3 = level(3, "Star",
            "Five rods, each turned a little further than the last.",
            new LevelData.SolutionShape[]{
                    sol(Kind.LINE, 300, 28, 500, 1320, 18),
                    sol(Kind.LINE, 300, 28, 500, 1320, 90),
                    sol(Kind.LINE, 300, 28, 500, 1320, 162),
                    sol(Kind.LINE, 300, 28, 500, 1320, 234),
                    sol(Kind.LINE, 300, 28, 500, 1320, 306),
            },
            new LevelData.StartShape[]{
                    st(Kind.LINE, 300, 28, 429, 537.8f, 150),
                    st(Kind.LINE, 300, 28, 551.6f, 842, 66),
                    st(Kind.LINE, 300, 28, 774.2f, 567.2f, 190),
                    st(Kind.LINE, 300, 28, 199.6f, 936.3f, 314),
                    st(Kind.LINE, 300, 28, 146.4f, 322.8f, 122),
            });

    // ---- Level 4: Arrow --------------------------------------------
    // target fills 716 grid cells; starting layout scores 26%
    private static final LevelData LV4 = level(4, "Arrow",
            "A head, a long shaft, two bits of fletching.",
            new LevelData.SolutionShape[]{
                    sol(Kind.TRIANGLE, 220, 120, 800, 1360, 90),
                    sol(Kind.RECT, 260, 60, 480, 1380, 0),
                    sol(Kind.LINE, 220, 26, 250, 1330, 0),
                    sol(Kind.LINE, 160, 24, 300, 1250, 50),
            },
            new LevelData.StartShape[]{
                    st(Kind.TRIANGLE, 220, 120, 467.6f, 448.3f, 41),
                    st(Kind.RECT, 260, 60, 654.6f, 1010.5f, 33),
                    st(Kind.LINE, 220, 26, 244.9f, 971.2f, 102),
                    st(Kind.LINE, 160, 24, 709, 574.6f, 106),
            });

    // ---- Level 5: Fish --------------------------------------------
    // target fills 801 grid cells; starting layout scores 18%
    private static final LevelData LV5 = level(5, "Fish",
            "A round body, a tail behind it, fins above and below.",
            new LevelData.SolutionShape[]{
                    sol(Kind.CIRCLE, 150, 150, 420, 1390, 0),
                    sol(Kind.TRIANGLE, 190, 130, 700, 1380, 90),
                    sol(Kind.LINE, 190, 24, 330, 1250, -35),
                    sol(Kind.LINE, 190, 24, 540, 1300, 30),
            },
            new LevelData.StartShape[]{
                    st(Kind.CIRCLE, 150, 150, 506.2f, 358.7f, 292),
                    st(Kind.TRIANGLE, 190, 130, 269.8f, 562.6f, 196),
                    st(Kind.LINE, 190, 24, 567.9f, 836.8f, 212),
                    st(Kind.LINE, 190, 24, 181.8f, 874.4f, 272),
            });

    // ---- Level 6: Boat --------------------------------------------
    // target fills 1035 grid cells; starting layout scores 16%
    private static final LevelData LV6 = level(6, "Boat",
            "A hull along the bottom, a sail and a mast above it.",
            new LevelData.SolutionShape[]{
                    sol(Kind.RECT, 300, 90, 480, 1420, 0),
                    sol(Kind.TRIANGLE, 200, 130, 540, 1300, 0),
                    sol(Kind.LINE, 150, 26, 400, 1300, 90),
                    sol(Kind.TRIANGLE, 150, 110, 700, 1240, 0),
            },
            new LevelData.StartShape[]{
                    st(Kind.RECT, 300, 90, 544.8f, 1029.2f, 182),
                    st(Kind.TRIANGLE, 200, 130, 155.1f, 796, 178),
                    st(Kind.LINE, 150, 26, 295.8f, 400.3f, 14),
                    st(Kind.TRIANGLE, 150, 110, 605.8f, 628.2f, 324),
            });

    // ---- Level 7: Cat --------------------------------------------
    // target fills 867 grid cells; starting layout scores 12%
    private static final LevelData LV7 = level(7, "Cat",
            "A head with two ears, a body behind it, a curling tail.",
            new LevelData.SolutionShape[]{
                    sol(Kind.CIRCLE, 130, 130, 350, 1300, 0),
                    sol(Kind.TRIANGLE, 110, 90, 290, 1220, 0),
                    sol(Kind.TRIANGLE, 110, 90, 420, 1220, 0),
                    sol(Kind.CIRCLE, 160, 150, 620, 1400, 0),
                    sol(Kind.LINE, 200, 26, 830, 1310, 40),
            },
            new LevelData.StartShape[]{
                    st(Kind.CIRCLE, 130, 130, 583.4f, 939.7f, 73),
                    st(Kind.TRIANGLE, 110, 90, 800.5f, 1029.4f, 160),
                    st(Kind.TRIANGLE, 110, 90, 783.5f, 799.9f, 172),
                    st(Kind.CIRCLE, 160, 150, 140.9f, 435.1f, 164),
                    st(Kind.LINE, 200, 26, 545.3f, 343.9f, 157),
            });

    // ---- Level 8: Bird --------------------------------------------
    // target fills 875 grid cells; starting layout scores 22%
    private static final LevelData LV8 = level(8, "Bird",
            "A body, a head, a beak, a tail and a raised wing.",
            new LevelData.SolutionShape[]{
                    sol(Kind.CIRCLE, 150, 130, 470, 1400, 0),
                    sol(Kind.CIRCLE, 110, 110, 280, 1250, 0),
                    sol(Kind.TRIANGLE, 110, 80, 150, 1230, 90),
                    sol(Kind.TRIANGLE, 160, 110, 750, 1388, -40),
                    sol(Kind.LINE, 180, 24, 600, 1290, -55),
            },
            new LevelData.StartShape[]{
                    st(Kind.CIRCLE, 150, 130, 622, 850.1f, 324),
                    st(Kind.CIRCLE, 110, 110, 685.8f, 502.7f, 142),
                    st(Kind.TRIANGLE, 110, 80, 870.1f, 873.4f, 185),
                    st(Kind.TRIANGLE, 160, 110, 269.5f, 986.6f, 265),
                    st(Kind.LINE, 180, 24, 241.4f, 400.7f, 180),
            });

    // ---- Level 9: Butterfly --------------------------------------------
    // target fills 1112 grid cells; starting layout scores 35%
    private static final LevelData LV9 = level(9, "Butterfly",
            "A body with two wings either side, high and low.",
            new LevelData.SolutionShape[]{
                    sol(Kind.RECT, 60, 220, 500, 1360, 90),
                    sol(Kind.CIRCLE, 130, 130, 350, 1280, 0),
                    sol(Kind.CIRCLE, 130, 130, 350, 1420, 0),
                    sol(Kind.CIRCLE, 130, 130, 650, 1280, 0),
                    sol(Kind.CIRCLE, 130, 130, 650, 1420, 0),
            },
            new LevelData.StartShape[]{
                    st(Kind.RECT, 60, 220, 660.6f, 760.6f, 215),
                    st(Kind.CIRCLE, 130, 130, 382.3f, 634.1f, 282),
                    st(Kind.CIRCLE, 130, 130, 238.8f, 839.1f, 127),
                    st(Kind.CIRCLE, 130, 130, 460.7f, 387.7f, 300),
                    st(Kind.CIRCLE, 130, 130, 433.2f, 1042.9f, 14),
            });

    // ---- Level 10: Flower --------------------------------------------
    // target fills 995 grid cells; starting layout scores 17%
    private static final LevelData LV10 = level(10, "Flower",
            "Six petals, evenly spaced, around a centre.",
            new LevelData.SolutionShape[]{
                    sol(Kind.CIRCLE, 120, 120, 500, 1350, 0),
                    sol(Kind.TRIANGLE, 150, 110, 500, 1230, 0),
                    sol(Kind.TRIANGLE, 150, 110, 660, 1290, 60),
                    sol(Kind.TRIANGLE, 150, 110, 610, 1425, 120),
                    sol(Kind.TRIANGLE, 150, 110, 390, 1425, 240),
                    sol(Kind.TRIANGLE, 150, 110, 340, 1290, 300),
            },
            new LevelData.StartShape[]{
                    st(Kind.CIRCLE, 120, 120, 699.1f, 671.1f, 106),
                    st(Kind.TRIANGLE, 150, 110, 456.5f, 969.5f, 106),
                    st(Kind.TRIANGLE, 150, 110, 818.2f, 897.8f, 287),
                    st(Kind.TRIANGLE, 150, 110, 240.6f, 364, 187),
                    st(Kind.TRIANGLE, 150, 110, 199.6f, 749.1f, 325),
                    st(Kind.TRIANGLE, 150, 110, 557.6f, 316, 57),
            });

    // ---- Level 11: Candle --------------------------------------------
    // target fills 603 grid cells; starting layout scores 0%
    private static final LevelData LV11 = level(11, "Candle",
            "A body, a wick, a flame on top.",
            new LevelData.SolutionShape[]{
                    sol(Kind.RECT, 160, 130, 500, 1420, 0),
                    sol(Kind.LINE, 110, 24, 500, 1310, 90),
                    sol(Kind.TRIANGLE, 130, 100, 500, 1225, 0),
            },
            new LevelData.StartShape[]{
                    st(Kind.RECT, 160, 130, 737.7f, 581.5f, 357),
                    st(Kind.LINE, 110, 24, 341.8f, 442.9f, 88),
                    st(Kind.TRIANGLE, 130, 100, 412.2f, 909.9f, 158),
            });

    // ---- Level 12: Key --------------------------------------------
    // target fills 648 grid cells; starting layout scores 11%
    private static final LevelData LV12 = level(12, "Key",
            "A round head, a straight shaft, two teeth hanging off it.",
            new LevelData.SolutionShape[]{
                    sol(Kind.CIRCLE, 140, 140, 250, 1350, 0),
                    sol(Kind.RECT, 240, 50, 540, 1355, 0),
                    sol(Kind.LINE, 90, 24, 700, 1430, 90),
                    sol(Kind.LINE, 90, 24, 820, 1430, 90),
            },
            new LevelData.StartShape[]{
                    st(Kind.CIRCLE, 140, 140, 776.3f, 492, 248),
                    st(Kind.RECT, 240, 50, 227.2f, 676.3f, 70),
                    st(Kind.LINE, 90, 24, 766.3f, 922.1f, 29),
                    st(Kind.LINE, 90, 24, 560.4f, 316.6f, 323),
            });

    // ---- Level 13: Person --------------------------------------------
    // target fills 810 grid cells; starting layout scores 5%
    private static final LevelData LV13 = level(13, "Person",
            "A head, a body, arms out, two legs below.",
            new LevelData.SolutionShape[]{
                    sol(Kind.CIRCLE, 110, 110, 500, 1225, 0),
                    sol(Kind.RECT, 150, 150, 500, 1350, 0),
                    sol(Kind.LINE, 170, 24, 300, 1340, 65),
                    sol(Kind.LINE, 170, 24, 700, 1340, -65),
                    sol(Kind.LINE, 130, 24, 410, 1425, 80),
                    sol(Kind.LINE, 130, 24, 590, 1425, -80),
            },
            new LevelData.StartShape[]{
                    st(Kind.CIRCLE, 110, 110, 814.9f, 402.5f, 138),
                    st(Kind.RECT, 150, 150, 872.5f, 909.7f, 52),
                    st(Kind.LINE, 170, 24, 360.3f, 934.3f, 260),
                    st(Kind.LINE, 170, 24, 340.3f, 292.9f, 211),
                    st(Kind.LINE, 130, 24, 162, 460.6f, 105),
                    st(Kind.LINE, 130, 24, 559.2f, 477.6f, 29),
            });

    // ---- Level 14: Peaks --------------------------------------------
    // target fills 1087 grid cells; starting layout scores 14%
    private static final LevelData LV14 = level(14, "Peaks",
            "Three mountains of different sizes over a shared base.",
            new LevelData.SolutionShape[]{
                    sol(Kind.TRIANGLE, 200, 100, 260, 1320, 0),
                    sol(Kind.TRIANGLE, 220, 100, 530, 1250, 0),
                    sol(Kind.TRIANGLE, 170, 90, 790, 1400, 0),
                    sol(Kind.RECT, 400, 70, 520, 1425, 0),
            },
            new LevelData.StartShape[]{
                    st(Kind.TRIANGLE, 200, 100, 853.5f, 312.9f, 29),
                    st(Kind.TRIANGLE, 220, 100, 192.8f, 884.4f, 310),
                    st(Kind.TRIANGLE, 170, 90, 397.2f, 556.9f, 196),
                    st(Kind.RECT, 400, 70, 567.1f, 994.5f, 160),
            });

    // ---- Level 15: Compass --------------------------------------------
    // target fills 510 grid cells; starting layout scores 9%
    private static final LevelData LV15 = level(15, "Compass",
            "Four long points, four short ones, and a hub at the centre.",
            new LevelData.SolutionShape[]{
                    sol(Kind.LINE, 300, 26, 500, 1320, 0),
                    sol(Kind.LINE, 300, 26, 500, 1320, 90),
                    sol(Kind.LINE, 205, 24, 500, 1320, 45),
                    sol(Kind.LINE, 205, 24, 500, 1320, 135),
                    sol(Kind.CIRCLE, 135, 135, 500, 1320, 0),
                    sol(Kind.SQUARE, 100, 100, 500, 1320, 0),
            },
            new LevelData.StartShape[]{
                    st(Kind.LINE, 300, 26, 132.1f, 983.4f, 280),
                    st(Kind.LINE, 300, 26, 643.2f, 616.4f, 16),
                    st(Kind.LINE, 205, 24, 867.1f, 830.1f, 338),
                    st(Kind.LINE, 205, 24, 437, 819.1f, 292),
                    st(Kind.CIRCLE, 135, 135, 290.2f, 313.4f, 83),
                    st(Kind.SQUARE, 100, 100, 275, 526.4f, 343),
            });

    private static final LevelData[] ALL = {
            LV1, LV2, LV3, LV4, LV5,
            LV6, LV7, LV8, LV9, LV10,
            LV11, LV12, LV13, LV14, LV15,
    };

    // ------------------------------------------------------------------
    // PUBLIC API
    // ------------------------------------------------------------------

    /** How many levels ship in this build. */
    public static int count() {
        return ALL.length;
    }

    /**
     * @param index 0-based level index
     * @return the level, clamped into range. Never returns null, so a bad
     *         index can never crash the game.
     */
    public static LevelData get(int index) {
        if (index < 0) index = 0;
        if (index >= ALL.length) index = ALL.length - 1;
        return ALL[index];
    }

    public static boolean exists(int index) {
        return index >= 0 && index < ALL.length;
    }

    /**
     * Build the live shapes for a level: the scattered starting layout the
     * player actually interacts with.
     */
    public static ShapeObject[] createShapes(int index) {
        return get(index).createStartingShapes();
    }

    /**
     * Solve a level's wall-space solution layout into the stage positions the
     * player must drag the shapes to.
     *
     * This is the exact inverse of the projection, which is what makes the
     * target guaranteed reachable: feed these positions straight back into
     * ShadowCalculator.project() and you get the authored wall positions.
     */
    public static ShapeObject[] createSolutionShapes(int index) {
        LevelData lv = get(index);
        LevelData.SolutionShape[] sol = lv.solution;
        ShapeObject[] out = new ShapeObject[sol.length];
        float[] pos = new float[2];
        for (int i = 0; i < sol.length; i++) {
            LevelData.SolutionShape s = sol[i];
            ShadowCalculator.solveStagePositionFor(s.wallX, s.wallY, pos);
            out[i] = new ShapeObject(s.kind, s.width, s.height, pos[0], pos[1], s.rotation);
        }
        return out;
    }

    /**
     * THE FORGE: produce the target silhouette for a level as a list of wall
     * polygons, by projecting the solution layout.
     *
     * The matcher only needs the polygons, not a single outline, so the union
     * is left implicit - rasterising several overlapping polygons into one grid
     * IS the union. This is the natural place to hand-author a target instead:
     * return a hand-written polygon list here and nothing else changes.
     *
     * @return one flat [x0,y0,x1,y1,...] polygon per shape
     */
    public static float[][] forgeTarget(int index) {
        ShapeObject[] solution = createSolutionShapes(index);
        float[][] polys = new float[solution.length][];
        for (int i = 0; i < solution.length; i++) {
            polys[i] = ShadowCalculator.project(solution[i]);
        }
        return polys;
    }
}
