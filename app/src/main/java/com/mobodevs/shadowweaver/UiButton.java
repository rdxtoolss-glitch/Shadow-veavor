package com.mobodevs.shadowweaver;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Shadow Weaver - UiButton
 * ========================
 * High-performance, zero-allocation interactive tactile button for Canvas UI.
 * Supports spring press feedback, antique brass framing, primary/secondary styles,
 * and high-contrast typography.
 */
public class UiButton {

    public float left;
    public float top;
    public float right;
    public float bottom;
    public float cornerRadius = 16f;

    public String label = "";
    public String subLabel = "";
    public boolean isPrimary;
    public boolean enabled = true;
    public boolean isPressed;
    private float animatedScale = 1.0f;

    private static Typeface safeTypeface(Typeface family, int style) {
        try {
            return Typeface.create(family, style);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static final Typeface SERIF_BOLD = safeTypeface(Typeface.SERIF, Typeface.BOLD);
    private static final Typeface SERIF_NORMAL = safeTypeface(Typeface.SERIF, Typeface.NORMAL);

    public UiButton() {
    }

    public UiButton(float left, float top, float right, float bottom, String label, boolean isPrimary) {
        setBounds(left, top, right, bottom);
        this.label = label;
        this.isPrimary = isPrimary;
    }

    public void setBounds(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public float width() {
        return right - left;
    }

    public float height() {
        return bottom - top;
    }

    public float centerX() {
        return (left + right) * 0.5f;
    }

    public float centerY() {
        return (top + bottom) * 0.5f;
    }

    public boolean hitTest(float sx, float sy) {
        if (!enabled) return false;
        return sx >= left && sx <= right && sy >= top && sy <= bottom;
    }

    /**
     * Draw the button with antique brass filigree and tactile press animation.
     */
    public void draw(Canvas c, Paint fill, Paint stroke, Paint text, float alpha) {
        draw(c, fill, stroke, text, alpha, 0f);
    }

    /**
     * Draw the button with antique brass filigree, tactile press animation, and specular sweep.
     */
    public void draw(Canvas c, Paint fill, Paint stroke, Paint text, float alpha, float time) {
        if (alpha <= 0f) return;

        float targetScale = (isPressed && enabled) ? 0.96f : 1.0f;
        animatedScale += (targetScale - animatedScale) * 0.45f;

        int save = c.save();
        if (Math.abs(animatedScale - 1.0f) > 0.001f) {
            float cx = centerX();
            float cy = centerY();
            c.scale(animatedScale, animatedScale, cx, cy);
        }

        int a = (int) (255 * alpha);

        // 1. Button Background
        if (!enabled) {
            fill.setColor(withAlpha(0x332A3245, a));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, fill);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.5f);
            stroke.setColor(withAlpha(0x446A758A, a));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, stroke);
        } else if (isPrimary) {
            // Luminous warm amber brass plate
            int baseColor = isPressed ? 0xFFC68824 : 0xFFE5A836;
            fill.setColor(withAlpha(baseColor, a));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, fill);

            // Subtle top bevel highlight
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.6f);
            stroke.setColor(withAlpha(0xFFFFF4D0, (int) (220 * alpha)));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, stroke);
        } else {
            // Refined obsidian slate plate
            int baseColor = isPressed ? 0xFA141A28 : 0xEE1C2234;
            fill.setColor(withAlpha(baseColor, a));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, fill);

            // Antique brass border
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.6f);
            stroke.setColor(withAlpha(0xFFD4AF37, (int) (210 * alpha)));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, stroke);

            // Top hairline edge highlight
            stroke.setStrokeWidth(1.0f);
            stroke.setColor(withAlpha(0x40FFFFFF, (int) (160 * alpha)));
            c.drawLine(left + cornerRadius, top + 1f, right - cornerRadius, top + 1f, stroke);
        }
        stroke.setStyle(Paint.Style.FILL);

        // 2. Button Typography
        float cx = centerX();
        float cy = centerY();

        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(SERIF_BOLD);

        if (subLabel == null || subLabel.isEmpty()) {
            // Single line centered
            text.setTextSize(Math.min(36f, height() * 0.44f));
            if (!enabled) {
                text.setColor(withAlpha(0x6690A0B8, a));
                float textOffset = (text.descent() + text.ascent()) * 0.5f;
                c.drawText(label, cx, cy - textOffset, text);
            } else if (isPrimary) {
                text.setColor(withAlpha(0xFF140802, a)); // Deepest umber on gold for maximum contrast
                float textOffset = (text.descent() + text.ascent()) * 0.5f;
                c.drawText(label, cx, cy - textOffset, text);
            } else {
                // Secondary button: drop shadow for sharp readability
                float textOffset = (text.descent() + text.ascent()) * 0.5f;
                text.setColor(withAlpha(0xFF000000, (int) (180 * alpha)));
                c.drawText(label, cx, cy - textOffset + 1.8f, text);
                text.setColor(withAlpha(0xFFFFFDF8, a)); // Radiant ivory
                c.drawText(label, cx, cy - textOffset, text);
            }
        } else {
            // Main label + Sub label
            text.setTextSize(Math.min(30f, height() * 0.35f));
            if (!enabled) {
                text.setColor(withAlpha(0x6690A0B8, a));
                c.drawText(label, cx, cy - 7f, text);
            } else if (isPrimary) {
                text.setColor(withAlpha(0xFF140802, a));
                c.drawText(label, cx, cy - 7f, text);
            } else {
                text.setColor(withAlpha(0xFF000000, (int) (180 * alpha)));
                c.drawText(label, cx, cy - 5.2f, text);
                text.setColor(withAlpha(0xFFFFFDF8, a));
                c.drawText(label, cx, cy - 7f, text);
            }

            text.setTextSize(Math.min(20f, height() * 0.24f));
            text.setTypeface(SERIF_NORMAL);
            if (!enabled) {
                text.setColor(withAlpha(0x4490A0B8, a));
            } else if (isPrimary) {
                text.setColor(withAlpha(0xDD2A1404, a));
            } else {
                text.setColor(withAlpha(0xFFE2D4BA, a));
            }
            c.drawText(subLabel, cx, cy + 22f, text);
        }

        c.restoreToCount(save);
        text.setTextAlign(Paint.Align.LEFT);
    }

    private static int withAlpha(int color, int alpha) {
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
