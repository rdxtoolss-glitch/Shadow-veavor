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
        if (alpha <= 0f) return;

        int save = c.save();
        if (isPressed) {
            float cx = centerX();
            float cy = centerY();
            c.scale(0.96f, 0.96f, cx, cy);
        }

        int a = (int) (255 * alpha);

        // 1. Button Background
        if (!enabled) {
            fill.setColor(withAlpha(0x331C130B, a));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, fill);
        } else if (isPrimary) {
            // Luminous golden alchemical brass
            int baseColor = isPressed ? 0xFF9E6E28 : 0xFFC5943C;
            fill.setColor(withAlpha(baseColor, a));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, fill);

            // Inner subtle warm highlight
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(1.8f);
            stroke.setColor(withAlpha(0xFFFFE8B8, (int) (180 * alpha)));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, stroke);
        } else {
            // Frosted translucent obsidian plate
            int baseColor = isPressed ? 0xDD2A1C12 : 0xBB1E140C;
            fill.setColor(withAlpha(baseColor, a));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, fill);

            // Antique brass perimeter filigree
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(2.0f);
            stroke.setColor(withAlpha(0x88C5A059, (int) (200 * alpha)));
            c.drawRoundRect(left, top, right, bottom, cornerRadius, cornerRadius, stroke);
        }
        stroke.setStyle(Paint.Style.FILL);

        // 2. Button Typography
        float cx = centerX();
        float cy = centerY();

        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);

        if (subLabel == null || subLabel.isEmpty()) {
            // Single line centered
            text.setTextSize(Math.min(28f, height() * 0.44f));
            if (!enabled) {
                text.setColor(withAlpha(0x55E0D0C0, a));
            } else if (isPrimary) {
                text.setColor(withAlpha(0xFF1E1005, a)); // Deep umber on gold
            } else {
                text.setColor(withAlpha(0xFFF0DFC6, a)); // Parchment ivory on dark
            }
            float textOffset = (text.descent() + text.ascent()) * 0.5f;
            c.drawText(label, cx, cy - textOffset, text);
        } else {
            // Main label + Sub label
            text.setTextSize(Math.min(24f, height() * 0.36f));
            if (!enabled) {
                text.setColor(withAlpha(0x55E0D0C0, a));
            } else if (isPrimary) {
                text.setColor(withAlpha(0xFF1E1005, a));
            } else {
                text.setColor(withAlpha(0xFFF0DFC6, a));
            }
            c.drawText(label, cx, cy - 6f, text);

            text.setTextSize(Math.min(18f, height() * 0.26f));
            text.setTypeface(Typeface.DEFAULT);
            if (!enabled) {
                text.setColor(withAlpha(0x33E0D0C0, a));
            } else if (isPrimary) {
                text.setColor(withAlpha(0xCC351C08, a));
            } else {
                text.setColor(withAlpha(0x99D5C4A8, a));
            }
            c.drawText(subLabel, cx, cy + 20f, text);
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
