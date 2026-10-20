package com.mobodevs.shadowweaver;

/**
 * LevelDesignTest - a plain-JVM verification harness for the pure-Java core.
 *
 * Run with:
 *   javac -d out app/src/main/java/com/mobodevs/shadowweaver/{ShapeObject,ShadowCalculator,SilhouetteMatcher,LevelData,LevelLoader}.java tools/LevelDesignTest.java
 *   java -cp out com.mobodevs.shadowweaver.LevelDesignTest
 *
 * It checks, for every shipped level:
 *   1. the forged target is non-empty and fits inside the wall
 *   2. the solution layout scores a near-perfect match (so the level CAN be solved)
 *   3. the starting layout is nowhere near a match (so there is something to do)
 *   4. no two starting shapes overlap (so every shape is grabbable)
 *   5. the projection is a genuine inverse: solve then project == the authored
 *      wall position, which is what guarantees solvability
 */
public class LevelDesignTest {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("Shadow Weaver - level design verification");
        System.out.println("grid: " + SilhouetteMatcher.gridWidth + " x " + SilhouetteMatcher.gridHeight
                + "   match threshold: " + SilhouetteMatcher.MATCH_THRESHOLD);
        System.out.println();

        System.out.printf("%-4s %-11s %-5s %-7s %-9s %-9s %-7s%n",
                "lvl", "title", "shapes", "cells", "solnIoU", "startIoU", "status");
        System.out.println("----------------------------------------------------------------");

        for (int i = 0; i < LevelLoader.count(); i++) {
            LevelData lv = LevelLoader.get(i);

            float[][] target = LevelLoader.forgeTarget(i);
            SilhouetteMatcher m = new SilhouetteMatcher();
            m.setTarget(target);

            int cells = m.targetCellCount();
            boolean inWall = targetInsideWall(target);

            // 2. the solution must score ~1.0
            ShapeObject[] sol = LevelLoader.createSolutionShapes(i);
            float[][] solShadows = new float[sol.length][];
            for (int k = 0; k < sol.length; k++) solShadows[k] = ShadowCalculator.project(sol[k]);
            float solnIoU = m.evaluate(solShadows);

            // 3. the starting layout must be far from a match
            ShapeObject[] start = LevelLoader.createShapes(i);
            float[][] startShadows = new float[start.length][];
            for (int k = 0; k < start.length; k++) startShadows[k] = ShadowCalculator.project(start[k]);
            float startIoU = m.evaluate(startShadows);

            // 4. starting shapes must not overlap
            float gap = minGap(start);

            // 4b. every authored wallY must sit inside the reachable centre
            // band. If it does not, the loader silently clamps it and the
            // level no longer means what the table says it means.
            boolean inBand = true;
            float lo = ShadowCalculator.wallCentreRangeLo();
            float hi = ShadowCalculator.wallCentreRangeHi();
            for (LevelData.SolutionShape s : lv.solution) {
                if (s.wallY < lo - 0.01f || s.wallY > hi + 0.01f) inBand = false;
            }

            boolean ok = cells > 0 && inWall && inBand && solnIoU > 0.999f
                    && startIoU < 0.40f && gap > 0f;
            if (!ok) failures++;

            System.out.printf("%-4d %-11s %-5d %-7d %-9.4f %-9.4f %-7s%n",
                    lv.number, lv.title, lv.shapeCount(), cells, solnIoU, startIoU,
                    ok ? "ok" : "FAIL" + flags(cells, inWall, inBand, solnIoU, startIoU, gap));
        }

        System.out.println();
        // 5. the projection must be an exact inverse of the loader's solve
        checkInverse();

        System.out.println();
        System.out.println(failures == 0 ? "ALL LEVELS PASS" : failures + " LEVEL(S) FAILED");
        if (failures != 0) System.exit(1);
    }

    private static String flags(int cells, boolean inWall, boolean inBand,
                               float soln, float start, float gap) {
        StringBuilder sb = new StringBuilder("(");
        if (cells == 0) sb.append("noCells ");
        if (!inWall) sb.append("outsideWall ");
        if (!inBand) sb.append("wallYClamped ");
        if (soln <= 0.999f) sb.append("unsolvable=").append(soln).append(' ');
        if (start >= 0.40f) sb.append("startTooClose=").append(start).append(' ');
        if (gap <= 0f) sb.append("shapesOverlap ");
        return sb.append(')').toString();
    }

    private static boolean targetInsideWall(float[][] polys) {
        for (float[] p : polys) {
            for (int i = 0; i < p.length; i += 2) {
                if (p[i] < ShadowCalculator.WALL_LEFT - 0.5f) return false;
                if (p[i] > ShadowCalculator.WALL_RIGHT + 0.5f) return false;
                if (p[i + 1] < ShadowCalculator.WALL_TOP - 0.5f) return false;
                if (p[i + 1] > ShadowCalculator.WALL_BOTTOM + 0.5f) return false;
            }
        }
        return true;
    }

    private static float minGap(ShapeObject[] shapes) {
        float best = Float.MAX_VALUE;
        for (int a = 0; a < shapes.length; a++) {
            for (int b = a + 1; b < shapes.length; b++) {
                float d = (float) Math.hypot(shapes[a].x - shapes[b].x, shapes[a].y - shapes[b].y);
                float gap = d - shapes[a].baseRadius() - shapes[b].baseRadius();
                if (gap < best) best = gap;
            }
        }
        return shapes.length < 2 ? 1f : best;
    }

    /**
     * The heart of the guarantee. Solving a wall position for a stage position
     * and projecting it straight back must land in the same place, otherwise a
     * level could ask for a shadow the player physically cannot produce.
     */
    private static void checkInverse() {
        float worst = 0f;
        float[] pos = new float[2];
        for (int i = 0; i < LevelLoader.count(); i++) {
            LevelData lv = LevelLoader.get(i);
            for (LevelData.SolutionShape s : lv.solution) {
                ShadowCalculator.solveStagePositionFor(s.wallX, s.wallY, pos);
                float gotX = ShadowCalculator.wallXFor(pos[0], pos[1]);
                float gotY = ShadowCalculator.wallYFor(pos[1]);
                worst = Math.max(worst, Math.abs(gotX - s.wallX));
                worst = Math.max(worst, Math.abs(gotY - s.wallY));
            }
        }
        System.out.printf("projection inverse: worst error %.6f scene units -> %s%n",
                worst, worst < 0.01f ? "EXACT" : "*** NOT AN INVERSE ***");
        if (worst >= 0.01f) failures++;
    }
}
