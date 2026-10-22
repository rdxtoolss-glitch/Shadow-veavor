package com.mobodevs.shadowweaver;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Shadow Weaver - HapticManager
 * =============================
 * Safe, low-latency haptic feedback for game feel and micro-interactions.
 * Supports Android API 21 through modern API 35+, with automatic fallback
 * when modern VibrationEffect APIs are not available.
 */
public class HapticManager {

    private final Vibrator vibrator;
    private boolean enabled = true;

    public HapticManager(Context context) {
        Vibrator v = null;
        if (context != null) {
            try {
                v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            } catch (Throwable ignored) {
            }
        }
        this.vibrator = v;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Subtle micro-pulse for rotation notches (every 15 degrees).
     */
    public void detent() {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(8);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Light impact tap for picking up or releasing a puppet or tapping buttons.
     */
    public void tap() {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(14, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(14);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Celebratory rhythmic pulse sequence on completing a level.
     */
    public void victory() {
        if (!enabled || vibrator == null || !vibrator.hasVibrator()) return;
        try {
            long[] timings = new long[]{0, 35, 50, 45, 60, 90};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int[] amplitudes = new int[]{0, 140, 0, 180, 0, 255};
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
            } else {
                vibrator.vibrate(timings, -1);
            }
        } catch (Throwable ignored) {
        }
    }
}
