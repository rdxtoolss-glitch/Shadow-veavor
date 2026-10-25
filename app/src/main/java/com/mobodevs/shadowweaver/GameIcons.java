package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Shadow Weaver - GameIcons
 * =========================
 * Industry-grade vector icon renderer for Canvas.
 * Zero-allocation: reuses static Path instances for all UI and Relic icons.
 */
public final class GameIcons {

    private static final Path path = new Path();
    private static final Path path2 = new Path();

    private GameIcons() {
    }

    // ======================================================================
    // HUD & NAVIGATION ICONS
    // ======================================================================

    /**
     * Draw an ornate Pause icon: twin beveled golden pillars inside an antique brass bezel.
     */
    public static void drawPause(Canvas c, float cx, float cy, float r, Paint fill, Paint stroke, boolean pressed) {
        // Outer medallion rim
        fill.setColor(pressed ? 0xFA141A28 : 0xEE1C2234);
        c.drawCircle(cx, cy, r, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawCircle(cx, cy, r, stroke);

        // Specular highlight
        stroke.setStrokeWidth(1.2f);
        stroke.setColor(0x55FFFFFF);
        c.drawArc(cx - r + 2f, cy - r + 2f, cx + r - 2f, cy + r - 2f, 200, 140, false, stroke);

        // Double golden pillars with caps
        float pw = r * 0.22f;
        float ph = r * 0.72f;
        float gap = r * 0.24f;

        fill.setColor(0xFFFFFDF8);
        stroke.setStyle(Paint.Style.FILL);

        // Left pillar
        float x1 = cx - gap * 0.5f - pw;
        c.drawRoundRect(x1, cy - ph * 0.5f, x1 + pw, cy + ph * 0.5f, 3f, 3f, fill);

        // Right pillar
        float x2 = cx + gap * 0.5f;
        c.drawRoundRect(x2, cy - ph * 0.5f, x2 + pw, cy + ph * 0.5f, 3f, 3f, fill);

        // Inner jewel dots
        fill.setColor(0xFFD4AF37);
        c.drawCircle(x1 + pw * 0.5f, cy, 2.5f, fill);
        c.drawCircle(x2 + pw * 0.5f, cy, 2.5f, fill);
    }

    /**
     * Draw the Hint icon: radiant alchemical lantern with sunburst star rays.
     */
    public static void drawHint(Canvas c, float cx, float cy, float r, Paint fill, Paint stroke, boolean active) {
        fill.setColor(active ? 0xFFE5A836 : 0xEE1C2234);
        c.drawCircle(cx, cy, r, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(active ? 0xFFFFF4D0 : 0xFFD4AF37);
        c.drawCircle(cx, cy, r, stroke);

        // Lantern / Torch Body
        float ly = cy - 2f;
        fill.setColor(active ? 0xFF140802 : 0xFFFFFDF8);
        stroke.setStyle(Paint.Style.FILL);

        // Incandescent bulb / flame
        c.drawCircle(cx, ly - 4f, 10f, fill);
        c.drawRoundRect(cx - 5f, ly + 6f, cx + 5f, ly + 14f, 2f, 2f, fill);

        // Radiating sunburst rays when active
        if (active) {
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.2f);
            stroke.setColor(0xFF140802);
            for (int deg = 0; deg < 360; deg += 45) {
                if (deg == 90) continue; // skip bottom
                float rad = (float) Math.toRadians(deg);
                float cos = (float) Math.cos(rad);
                float sin = (float) Math.sin(rad);
                c.drawLine(cx + cos * 15f, ly - 4f + sin * 15f, cx + cos * 20f, ly - 4f + sin * 20f, stroke);
            }
            stroke.setStyle(Paint.Style.FILL);
        } else {
            // White pearl center
            fill.setColor(0xFFFFD54F);
            c.drawCircle(cx, ly - 4f, 4f, fill);
        }
    }

    /**
     * Draw the Reset icon: precision astronomical chronometer rewind arrow.
     */
    public static void drawReset(Canvas c, float cx, float cy, float r, Paint fill, Paint stroke) {
        fill.setColor(0xEE1C2234);
        c.drawCircle(cx, cy, r, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.2f);
        stroke.setColor(0xFFD4AF37);
        c.drawCircle(cx, cy, r, stroke);

        // Chronometer Rewind Circular Arc
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(0xFFFFFDF8);
        float arcR = r * 0.44f;
        c.drawArc(cx - arcR, cy - arcR, cx + arcR, cy + arcR, 45, 275, false, stroke);

        // Precision Arrowhead
        path.reset();
        float ax = cx + arcR * (float) Math.cos(Math.toRadians(45));
        float ay = cy + arcR * (float) Math.sin(Math.toRadians(45));
        path.moveTo(ax - 2f, ay - 10f);
        path.lineTo(ax + 8f, ay - 2f);
        path.lineTo(ax - 6f, ay + 3f);
        path.close();

        stroke.setStyle(Paint.Style.FILL);
        fill.setColor(0xFFFFFDF8);
        c.drawPath(path, fill);

        // Center jewel rivet
        fill.setColor(0xFFFFD54F);
        c.drawCircle(cx, cy, 3.5f, fill);
    }

    /**
     * Draw an antique brass Vault Padlock with arched shackle and keyhole.
     */
    public static void drawPadlock(Canvas c, float cx, float cy, Paint fill, Paint stroke) {
        // Arched shackle
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3.0f);
        stroke.setColor(0xFF8898AA);
        c.drawArc(cx - 14f, cy - 24f, cx + 14f, cy - 2f, 180, 180, false, stroke);

        // Solid brass body
        fill.setColor(0xFF556070);
        stroke.setStyle(Paint.Style.FILL);
        c.drawRoundRect(cx - 18f, cy - 4f, cx + 18f, cy + 22f, 5f, 5f, fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.8f);
        stroke.setColor(0xFF90A0B8);
        c.drawRoundRect(cx - 18f, cy - 4f, cx + 18f, cy + 22f, 5f, 5f, stroke);

        // Keyhole
        fill.setColor(0xFF1B2032);
        stroke.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, cy + 5f, 4f, fill);
        path.reset();
        path.moveTo(cx - 2.5f, cy + 5f);
        path.lineTo(cx + 2.5f, cy + 5f);
        path.lineTo(cx + 1.8f, cy + 14f);
        path.lineTo(cx - 1.8f, cy + 14f);
        path.close();
        c.drawPath(path, fill);
    }

    // ======================================================================
    // 15 BESPOKE RELIC ICONS FOR ASTRONOMY JOURNAL
    // ======================================================================

    /**
     * Render a distinct, authentic celestial artifact silhouette for each relic.
     */
    public static void drawRelicIcon(Canvas c, int relicIndex, float cx, float cy, float r, Paint fill, Paint stroke, boolean solved) {
        int primaryColor = solved ? 0xFFFFD54F : 0x88FFE0A8;
        int secondaryColor = solved ? 0xFFFFFDF8 : 0x44607080;

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2.0f);
        stroke.setColor(primaryColor);
        fill.setColor(primaryColor);

        path.reset();
        float s = r * 0.04f;

        switch (relicIndex) {
            case 0: // 1. House (The Apprentice's Domicile)
                path.moveTo(cx, cy - 18f * s);
                path.lineTo(cx + 20f * s, cy - 2f * s);
                path.lineTo(cx + 15f * s, cy - 2f * s);
                path.lineTo(cx + 15f * s, cy + 18f * s);
                path.lineTo(cx - 15f * s, cy + 18f * s);
                path.lineTo(cx - 15f * s, cy - 2f * s);
                path.lineTo(cx - 20f * s, cy - 2f * s);
                path.close();
                c.drawPath(path, stroke);
                fill.setColor(secondaryColor);
                stroke.setStyle(Paint.Style.FILL);
                c.drawRoundRect(cx - 4f * s, cy + 4f * s, cx + 4f * s, cy + 18f * s, 2f, 2f, fill);
                break;

            case 1: // 2. Tree (The Arbor of Wisdom)
                path.moveTo(cx, cy - 20f * s);
                path.lineTo(cx + 16f * s, cy - 4f * s);
                path.lineTo(cx + 10f * s, cy - 4f * s);
                path.lineTo(cx + 18f * s, cy + 8f * s);
                path.lineTo(cx + 4f * s, cy + 8f * s);
                path.lineTo(cx + 4f * s, cy + 20f * s);
                path.lineTo(cx - 4f * s, cy + 20f * s);
                path.lineTo(cx - 4f * s, cy + 8f * s);
                path.lineTo(cx - 18f * s, cy + 8f * s);
                path.lineTo(cx - 10f * s, cy - 4f * s);
                path.lineTo(cx - 16f * s, cy - 4f * s);
                path.close();
                c.drawPath(path, stroke);
                break;

            case 2: // 3. Star (The Astral Star)
                for (int i = 0; i < 16; i++) {
                    float rad = (float) Math.toRadians(i * 22.5f - 90);
                    float radDist = (i % 2 == 0) ? 22f * s : 9f * s;
                    float px = cx + (float) Math.cos(rad) * radDist;
                    float py = cy + (float) Math.sin(rad) * radDist;
                    if (i == 0) path.moveTo(px, py);
                    else path.lineTo(px, py);
                }
                path.close();
                c.drawPath(path, stroke);
                fill.setColor(secondaryColor);
                stroke.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy, 4f * s, fill);
                break;

            case 3: // 4. Arrow (The Vector Arrow)
                path.moveTo(cx, cy - 22f * s);
                path.lineTo(cx + 14f * s, cy - 8f * s);
                path.lineTo(cx + 5f * s, cy - 8f * s);
                path.lineTo(cx + 5f * s, cy + 14f * s);
                path.lineTo(cx + 12f * s, cy + 22f * s);
                path.lineTo(cx, cy + 17f * s);
                path.lineTo(cx - 12f * s, cy + 22f * s);
                path.lineTo(cx - 5f * s, cy + 14f * s);
                path.lineTo(cx - 5f * s, cy - 8f * s);
                path.lineTo(cx - 14f * s, cy - 8f * s);
                path.close();
                c.drawPath(path, stroke);
                break;

            case 4: // 5. Fish (The Golden Carp)
                path.moveTo(cx - 18f * s, cy);
                path.cubicTo(cx - 10f * s, cy - 15f * s, cx + 12f * s, cy - 15f * s, cx + 18f * s, cy);
                path.cubicTo(cx + 12f * s, cy + 15f * s, cx - 10f * s, cy + 15f * s, cx - 18f * s, cy);
                path.lineTo(cx - 26f * s, cy - 10f * s);
                path.lineTo(cx - 22f * s, cy);
                path.lineTo(cx - 26f * s, cy + 10f * s);
                path.close();
                c.drawPath(path, stroke);
                fill.setColor(secondaryColor);
                stroke.setStyle(Paint.Style.FILL);
                c.drawCircle(cx + 11f * s, cy - 2f * s, 2.5f * s, fill);
                break;

            case 5: // 6. Boat (The Celestial Galleon)
                // Hull
                path.moveTo(cx - 20f * s, cy + 6f * s);
                path.lineTo(cx + 20f * s, cy + 6f * s);
                path.lineTo(cx + 12f * s, cy + 18f * s);
                path.lineTo(cx - 12f * s, cy + 18f * s);
                path.close();
                c.drawPath(path, stroke);
                // Mast & Sail
                c.drawLine(cx - 2f * s, cy + 6f * s, cx - 2f * s, cy - 20f * s, stroke);
                path2.reset();
                path2.moveTo(cx, cy - 18f * s);
                path2.lineTo(cx + 16f * s, cy - 4f * s);
                path2.lineTo(cx, cy - 4f * s);
                path2.close();
                c.drawPath(path2, stroke);
                break;

            case 6: // 7. Cat (The Sacred Feline)
                // Head with pointed ears
                path.moveTo(cx - 10f * s, cy - 4f * s);
                path.lineTo(cx - 12f * s, cy - 18f * s);
                path.lineTo(cx - 4f * s, cy - 12f * s);
                path.lineTo(cx + 4f * s, cy - 12f * s);
                path.lineTo(cx + 12f * s, cy - 18f * s);
                path.lineTo(cx + 10f * s, cy - 4f * s);
                // Body & Tail
                path.cubicTo(cx + 14f * s, cy + 6f * s, cx + 12f * s, cy + 18f * s, cx, cy + 18f * s);
                path.cubicTo(cx - 12f * s, cy + 18f * s, cx - 14f * s, cy + 6f * s, cx - 10f * s, cy - 4f * s);
                path.close();
                c.drawPath(path, stroke);
                break;

            case 7: // 8. Bird (The Phoenix Falcon)
                path.moveTo(cx, cy - 18f * s);
                path.lineTo(cx + 6f * s, cy - 8f * s);
                path.lineTo(cx + 24f * s, cy - 14f * s);
                path.lineTo(cx + 14f * s, cy + 4f * s);
                path.lineTo(cx + 6f * s, cy + 6f * s);
                path.lineTo(cx + 8f * s, cy + 20f * s);
                path.lineTo(cx, cy + 14f * s);
                path.lineTo(cx - 8f * s, cy + 20f * s);
                path.lineTo(cx - 6f * s, cy + 6f * s);
                path.lineTo(cx - 14f * s, cy + 4f * s);
                path.lineTo(cx - 24f * s, cy - 14f * s);
                path.lineTo(cx - 6f * s, cy - 8f * s);
                path.close();
                c.drawPath(path, stroke);
                break;

            case 8: // 9. Butterfly (The Psyche Butterfly)
                path.moveTo(cx, cy - 10f * s);
                path.cubicTo(cx + 22f * s, cy - 22f * s, cx + 24f * s, cy, cx + 4f * s, cy + 4f * s);
                path.cubicTo(cx + 18f * s, cy + 18f * s, cx + 8f * s, cy + 20f * s, cx, cy + 12f * s);
                path.cubicTo(cx - 8f * s, cy + 20f * s, cx - 18f * s, cy + 18f * s, cx - 4f * s, cy + 4f * s);
                path.cubicTo(cx - 24f * s, cy, cx - 22f * s, cy - 22f * s, cx, cy - 10f * s);
                path.close();
                c.drawPath(path, stroke);
                c.drawLine(cx, cy - 14f * s, cx, cy + 16f * s, stroke);
                break;

            case 9: // 10. Flower (The Alchemical Blossom)
                for (int i = 0; i < 6; i++) {
                    float rad = (float) Math.toRadians(i * 60);
                    float px = cx + (float) Math.cos(rad) * 11f * s;
                    float py = cy + (float) Math.sin(rad) * 11f * s;
                    c.drawCircle(px, py, 6f * s, stroke);
                }
                fill.setColor(secondaryColor);
                stroke.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy, 5f * s, fill);
                break;

            case 10: // 11. Candle (The Eternal Flame)
                stroke.setStyle(Paint.Style.STROKE);
                c.drawRoundRect(cx - 7f * s, cy - 2f * s, cx + 7f * s, cy + 20f * s, 2f, 2f, stroke);
                c.drawLine(cx, cy - 2f * s, cx, cy - 6f * s, stroke);
                path.moveTo(cx, cy - 20f * s);
                path.cubicTo(cx + 7f * s, cy - 14f * s, cx + 6f * s, cy - 7f * s, cx, cy - 6f * s);
                path.cubicTo(cx - 6f * s, cy - 7f * s, cx - 7f * s, cy - 14f * s, cx, cy - 20f * s);
                path.close();
                c.drawPath(path, stroke);
                break;

            case 11: // 12. Key (The Skeleton Key)
                stroke.setStyle(Paint.Style.STROKE);
                c.drawCircle(cx, cy - 11f * s, 8f * s, stroke);
                c.drawLine(cx, cy - 3f * s, cx, cy + 20f * s, stroke);
                c.drawLine(cx, cy + 12f * s, cx + 7f * s, cy + 12f * s, stroke);
                c.drawLine(cx, cy + 18f * s, cx + 9f * s, cy + 18f * s, stroke);
                break;

            case 12: // 13. Person (The Shadow Weaver)
                stroke.setStyle(Paint.Style.STROKE);
                c.drawCircle(cx, cy - 14f * s, 6f * s, stroke);
                path.moveTo(cx - 16f * s, cy + 2f * s);
                path.lineTo(cx, cy - 6f * s);
                path.lineTo(cx + 16f * s, cy + 2f * s);
                path.moveTo(cx, cy - 6f * s);
                path.lineTo(cx, cy + 18f * s);
                path.lineTo(cx - 10f * s, cy + 22f * s);
                path.moveTo(cx, cy + 18f * s);
                path.lineTo(cx + 10f * s, cy + 22f * s);
                c.drawPath(path, stroke);
                break;

            case 13: // 14. Peaks (The Obsidian Peaks)
                path.moveTo(cx - 22f * s, cy + 18f * s);
                path.lineTo(cx - 8f * s, cy - 10f * s);
                path.lineTo(cx + 2f * s, cy + 4f * s);
                path.lineTo(cx + 14f * s, cy - 16f * s);
                path.lineTo(cx + 26f * s, cy + 18f * s);
                path.close();
                c.drawPath(path, stroke);
                fill.setColor(secondaryColor);
                stroke.setStyle(Paint.Style.FILL);
                c.drawCircle(cx + 14f * s, cy - 23f * s, 3f * s, fill);
                break;

            case 14: // 15. Compass (The Astrolabe Compass)
            default:
                stroke.setStyle(Paint.Style.STROKE);
                c.drawCircle(cx, cy, 18f * s, stroke);
                c.drawCircle(cx, cy, 22f * s, stroke);
                c.drawLine(cx, cy - 24f * s, cx, cy + 24f * s, stroke);
                c.drawLine(cx - 24f * s, cy, cx + 24f * s, cy, stroke);

                // North arrow (solid gold)
                path.moveTo(cx, cy - 18f * s);
                path.lineTo(cx + 5f * s, cy);
                path.lineTo(cx, cy);
                path.lineTo(cx - 5f * s, cy);
                path.close();
                fill.setColor(primaryColor);
                stroke.setStyle(Paint.Style.FILL);
                c.drawPath(path, fill);

                // South arrow (open outline)
                path2.reset();
                path2.moveTo(cx, cy + 18f * s);
                path2.lineTo(cx + 5f * s, cy);
                path2.lineTo(cx, cy);
                path2.lineTo(cx - 5f * s, cy);
                path2.close();
                stroke.setStyle(Paint.Style.STROKE);
                c.drawPath(path2, stroke);

                // Pivot jewel
                fill.setColor(secondaryColor);
                stroke.setStyle(Paint.Style.FILL);
                c.drawCircle(cx, cy, 3.5f * s, fill);
                break;
        }
    }
}
