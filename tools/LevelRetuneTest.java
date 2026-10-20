import com.mobodevs.shadowweaver.ShapeObject;
import com.mobodevs.shadowweaver.ShadowCalculator;
import com.mobodevs.shadowweaver.SilhouetteMatcher;
import com.mobodevs.shadowweaver.LevelLoader;

/**
 * Projection-retune regression test.
 *
 * The 15 levels are authored in WALL space: each entry says where a shadow
 * must land, and the stage position the player has to drag it to is solved at
 * load time by ShadowCalculator.solveStagePositionFor(). The point of this
 * test is to prove that indirection actually works.
 *
 * If the level numbers were secretly coupled to today's constants, then
 * changing perspectiveStrength, the height falloff, the vertical range or the
 * matcher's cell size would break some levels. Running every level across a
 * spread of those values shows the levels are self-consistent, so the
 * projection can be retuned for feel without redesigning content.
 *
 * Run:
 *   javac -d /tmp/t app/src/main/java/com/mobodevs/shadowweaver/*.java tools/LevelRetuneTest.java
 *   java  -cp /tmp/t com.mobodevs.shadowweaver.LevelRetuneTest
 */
public class LevelRetuneTest {

    /**
     * Each row retunes one axis of the projection. The first row is
     * deliberately NOT the shipped constants, so the test would catch a level
     * that only happens to work at exactly the values in ShadowCalculator.
     */
    private static final float[] PERSPECTIVE = {0.35f, 0.45f, 0.55f, 0.65f};
    private static final float[] HEIGHT_NEAR_FLOOR = {0.06f, 0.08f, 0.10f, 0.12f};
    private static final float[] HEIGHT_NEAR_CANDLE = {0.40f, 0.46f, 0.52f, 0.58f};
    private static final float[] VERTICAL_RANGE = {200f, 210f, 220f, 230f};
    private static final float[] CELL_SIZE = {10f, 8f, 14f, 6f};

    public static void main(String[] args) {
        int failures = 0;

        System.out.println("Shadow Weaver - projection retune regression");
        System.out.println("Each row changes the projection AND the match grid.");
        System.out.println();

        for (int t = 0; t < PERSPECTIVE.length; t++) {
            ShadowCalculator.perspectiveStrength = PERSPECTIVE[t];
            ShadowCalculator.heightNearFloor = HEIGHT_NEAR_FLOOR[t];
            ShadowCalculator.heightNearCandle = HEIGHT_NEAR_CANDLE[t];
            ShadowCalculator.verticalRange = VERTICAL_RANGE[t];
            ShadowCalculator.refreshDerived();

            SilhouetteMatcher.cellSize = CELL_SIZE[t];
            SilhouetteMatcher.MATCH_THRESHOLD = 0.85f;

            int bad = 0;
            String firstFailure = "";
            int gw = 0;
            int gh = 0;

            for (int i = 0; i < LevelLoader.count(); i++) {
                SilhouetteMatcher m = new SilhouetteMatcher();
                gw = SilhouetteMatcher.gridWidth;
                gh = SilhouetteMatcher.gridHeight;
                m.setTarget(LevelLoader.forgeTarget(i));

                float soln = m.evaluate(project(LevelLoader.createSolutionShapes(i)));

                ShapeObject[] start = LevelLoader.get(i).createStartingShapes();
                float startIoU = m.evaluate(project(start));

                if (soln < 0.999f || startIoU > 0.40f) {
                    bad++;
                    if (firstFailure.isEmpty()) {
                        firstFailure = "L" + (i + 1) + " " + LevelLoader.get(i).title
                                + " soln=" + String.format("%.3f", soln)
                                + " start=" + String.format("%.3f", startIoU);
                    }
                }
            }

            System.out.printf("persp=%.2f  cell=%2.0f  grid=%3dx%-3d  -> %s%n",
                    PERSPECTIVE[t], CELL_SIZE[t], gw, gh,
                    bad == 0 ? "all 15 levels pass"
                            : (bad + " FAILED: " + firstFailure));
            if (bad != 0) failures += bad;
        }

        System.out.println();
        if (failures == 0) {
            System.out.println("ALL SETTINGS PASS - levels are independent of "
                    + "the projection constants");
        } else {
            System.out.println(failures + " failures");
            System.exit(1);
        }
    }

    private static float[][] project(ShapeObject[] shapes) {
        float[][] out = new float[shapes.length][];
        for (int i = 0; i < shapes.length; i++) {
            out[i] = ShadowCalculator.project(shapes[i]);
        }
        return out;
    }
}
