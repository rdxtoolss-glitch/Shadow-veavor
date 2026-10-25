package com.mobodevs.shadowweaver;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Generates industry-grade, ultra-sharp raster icons for all Android mipmap densities.
 */
public class IconGenerator {

    public static void main(String[] args) throws Exception {
        System.out.println("Rendering professional app launcher icons...");

        int[] sizes = {48, 72, 96, 144, 192};
        String[] dirs = {"mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"};

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            String dir = "app/src/main/res/" + dirs[i];

            // 1. Standard square launcher icon with subtle rounded corner
            BufferedImage square = renderIcon(size, false);
            ImageIO.write(square, "png", new File(dir, "ic_launcher.png"));

            // 2. Circular round launcher icon for Android round launchers
            BufferedImage round = renderIcon(size, true);
            ImageIO.write(round, "png", new File(dir, "ic_launcher_round.png"));

            System.out.println("Generated " + size + "x" + size + " icons in " + dirs[i]);
        }

        System.out.println("App icons generated successfully!");
    }

    private static BufferedImage renderIcon(int size, boolean round) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float s = size / 108f;

        // Clip to round or squircle
        if (round) {
            g.setClip(new Ellipse2D.Float(2 * s, 2 * s, 104 * s, 104 * s));
        } else {
            g.setClip(new RoundRectangle2D.Float(2 * s, 2 * s, 104 * s, 104 * s, 24 * s, 24 * s));
        }

        // 1. Deep Midnight Twilight Background
        Point2D center = new Point2D.Float(54 * s, 48 * s);
        float radius = 64 * s;
        float[] dist = {0.0f, 0.45f, 1.0f};
        Color[] colors = {new Color(0x4A, 0x2E, 0x1B), new Color(0x1B, 0x20, 0x32), new Color(0x0C, 0x0F, 0x1C)};
        RadialGradientPaint bgGrad = new RadialGradientPaint(center, radius, dist, colors);
        g.setPaint(bgGrad);
        g.fillRect(0, 0, size, size);

        // 2. Concentric Astrolabe Brass Rings
        g.setColor(new Color(0xD4, 0xAF, 0x37, 0x44));
        g.setStroke(new BasicStroke(1.5f * s));
        g.draw(new Ellipse2D.Float((54 - 38) * s, (54 - 38) * s, 76 * s, 76 * s));
        g.setColor(new Color(0xD4, 0xAF, 0x37, 0x66));
        g.draw(new Ellipse2D.Float((54 - 30) * s, (54 - 30) * s, 60 * s, 60 * s));

        // Compass Azimuth lines
        g.setColor(new Color(0xFF, 0xE0, 0x82, 0x44));
        g.draw(new Line2D.Float(54 * s, 16 * s, 54 * s, 92 * s));
        g.draw(new Line2D.Float(16 * s, 54 * s, 92 * s, 54 * s));

        // 3. Central Golden Astrolabe Ring
        g.setColor(new Color(0xD4, 0xAF, 0x37, 0xFF));
        g.setStroke(new BasicStroke(2.2f * s));
        g.draw(new Ellipse2D.Float((54 - 24) * s, (54 - 24) * s, 48 * s, 48 * s));

        // 4. Soaring Shadow Origami Falcon Silhouette
        Path2D.Float falcon = new Path2D.Float();
        falcon.moveTo(54 * s, 38 * s);
        falcon.lineTo(72 * s, 30 * s);
        falcon.lineTo(67 * s, 50 * s);
        falcon.lineTo(82 * s, 48 * s);
        falcon.lineTo(60 * s, 68 * s);
        falcon.lineTo(54 * s, 60 * s);
        falcon.lineTo(48 * s, 68 * s);
        falcon.lineTo(26 * s, 48 * s);
        falcon.lineTo(41 * s, 50 * s);
        falcon.lineTo(36 * s, 30 * s);
        falcon.closePath();

        // Drop shadow for falcon
        g.setColor(new Color(0, 0, 0, 160));
        AffineTransform txShadow = AffineTransform.getTranslateInstance(0, 3 * s);
        g.fill(txShadow.createTransformedShape(falcon));

        // Falcon deep obsidian-umber body
        g.setColor(new Color(0x18, 0x0E, 0x07));
        g.fill(falcon);

        // Radiant Gold filigree outline
        g.setColor(new Color(0xFF, 0xD5, 0x4F));
        g.setStroke(new BasicStroke(1.4f * s));
        g.draw(falcon);

        // Internal origami wing creases
        g.setColor(new Color(0xFF, 0xE0, 0x82, 180));
        g.setStroke(new BasicStroke(0.9f * s));
        g.draw(new Line2D.Float(54 * s, 38 * s, 54 * s, 60 * s));
        g.draw(new Line2D.Float(54 * s, 46 * s, 67 * s, 50 * s));
        g.draw(new Line2D.Float(54 * s, 46 * s, 41 * s, 50 * s));

        // 5. Candlestick Dish & Ivory Pillar
        g.setColor(new Color(0xD4, 0xAF, 0x37));
        g.fill(new RoundRectangle2D.Float(46 * s, 76 * s, 16 * s, 4 * s, 2 * s, 2 * s));

        g.setColor(new Color(0xED, 0xE3, 0xD2));
        g.fill(new RoundRectangle2D.Float(51 * s, 64 * s, 6 * s, 12 * s, 2 * s, 2 * s));

        // 6. Multi-Tone Incandescent Candle Flame
        Path2D.Float flame = new Path2D.Float();
        flame.moveTo(54 * s, 46 * s);
        flame.curveTo(58 * s, 52 * s, 59 * s, 59 * s, 54 * s, 61 * s);
        flame.curveTo(49 * s, 59 * s, 50 * s, 52 * s, 54 * s, 46 * s);
        flame.closePath();

        // Outer Flame (Radiant Amber)
        g.setColor(new Color(0xFF, 0x8F, 0x00));
        g.fill(flame);

        // Inner Core (Pure Gold)
        Path2D.Float innerFlame = new Path2D.Float();
        innerFlame.moveTo(54 * s, 49 * s);
        innerFlame.curveTo(56.5f * s, 53 * s, 57 * s, 58 * s, 54 * s, 59.5f * s);
        innerFlame.curveTo(51 * s, 58 * s, 51.5f * s, 53 * s, 54 * s, 49 * s);
        innerFlame.closePath();
        g.setColor(new Color(0xFF, 0xD5, 0x4F));
        g.fill(innerFlame);

        // White Pearl Center
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(52.5f * s, 52.5f * s, 3 * s, 5 * s));

        // Blue wick root
        g.setColor(new Color(0x3F, 0x51, 0xB5, 200));
        g.fill(new Ellipse2D.Float(52.5f * s, 58.5f * s, 3 * s, 2.5f * s));

        // 7. Celestial Apex Diamond Star
        g.setColor(new Color(0xFF, 0xFF, 0xFF));
        g.fill(new Ellipse2D.Float(52 * s, 32 * s, 4 * s, 4 * s));
        g.setColor(new Color(0xFF, 0xD5, 0x4F));
        g.setStroke(new BasicStroke(1.2f * s));
        g.draw(new Line2D.Float(54 * s, 28 * s, 54 * s, 40 * s));
        g.draw(new Line2D.Float(48 * s, 34 * s, 60 * s, 34 * s));

        // Outer Rim Specular Bevel
        g.setClip(null);
        if (round) {
            g.setColor(new Color(0xD4, 0xAF, 0x37, 220));
            g.setStroke(new BasicStroke(2.5f * s));
            g.draw(new Ellipse2D.Float(2 * s, 2 * s, 104 * s, 104 * s));
        } else {
            g.setColor(new Color(0xD4, 0xAF, 0x37, 220));
            g.setStroke(new BasicStroke(2.5f * s));
            g.draw(new RoundRectangle2D.Float(2 * s, 2 * s, 104 * s, 104 * s, 24 * s, 24 * s));
        }

        g.dispose();
        return img;
    }
}
