package com.mobodevs.shadowweaver;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

/**
 * Shadow Weaver - AudioManager
 * ===========================
 * Ambient soundscapes and procedural tactile sound effects.
 * Synthesized mathematically in code with zero external dependencies.
 */
public class AudioManager {

    private static final String TAG = "ShadowWeaverAudio";

    // ---- Audio Format & Tuning ---------------------------------------------
    private static final int SAMPLE_RATE = 22050;
    private static final int AMBIENT_SECONDS = 6;
    private static final float CROSSFADE_SECONDS = 0.35f;

    // Audible, warm alchemical candle drone
    private static final float AMBIENT_GAIN = 0.22f;
    private static float masterVolume = 0.85f;

    private static final int CHIME_RATE = 22050;
    private static final float CHIME_LENGTH = 1.6f;

    // ---- Match chime marks (SilhouetteMatcher) -----------------------------
    public static final int CHIME_HALF = SilhouetteMatcher.CHIME_HALF;
    public static final int CHIME_GOOD = SilhouetteMatcher.CHIME_GOOD;
    public static final int CHIME_DONE = SilhouetteMatcher.CHIME_DONE;

    // ---- State -------------------------------------------------------------
    private final Object lock = new Object();

    private AudioTrack ambientTrack;
    private AudioTrack tickTrack;
    private AudioTrack snapTrack;
    private AudioTrack chimeHalfTrack;
    private AudioTrack chimeGoodTrack;
    private AudioTrack chimeDoneTrack;
    private AudioTrack celebrationTrack;

    private Thread prepareThread;
    private volatile boolean released;
    private volatile boolean muted;
    private volatile boolean started;

    // Cached PCM buffers
    private static short[] ambientBuffer;
    private static short[] tickBuffer;
    private static short[] snapBuffer;
    private static short[] chimeHalfBuffer;
    private static short[] chimeGoodBuffer;
    private static short[] chimeDoneBuffer;
    private static short[] celebrationBuffer;

    // ======================================================================
    // LIFECYCLE
    // ======================================================================

    public void start() {
        if (released || started) return;
        started = true;

        prepareThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    initAllAudioTracks();
                } catch (Throwable t) {
                    Log.w(TAG, "Audio synthesis thread error", t);
                }
            }
        }, "sw-audio-init");
        prepareThread.start();
    }

    private void initAllAudioTracks() {
        if (ambientBuffer == null) ambientBuffer = buildAmbientBuffer();
        if (tickBuffer == null) tickBuffer = buildTickBuffer();
        if (snapBuffer == null) snapBuffer = buildSnapBuffer();
        if (chimeHalfBuffer == null) chimeHalfBuffer = buildChimeBuffer(392.00f, 0.45f);
        if (chimeGoodBuffer == null) chimeGoodBuffer = buildChimeBuffer(523.25f, 0.50f);
        if (chimeDoneBuffer == null) chimeDoneBuffer = buildChimeBuffer(659.25f, 0.60f);
        if (celebrationBuffer == null) celebrationBuffer = buildCelebrationBuffer();

        if (released) return;

        synchronized (lock) {
            if (released) return;
            if (ambientTrack == null) {
                ambientTrack = buildLoopingTrack(ambientBuffer, SAMPLE_RATE);
                applyVolume();
            }
            if (tickTrack == null) tickTrack = buildStaticTrack(tickBuffer, CHIME_RATE);
            if (snapTrack == null) snapTrack = buildStaticTrack(snapBuffer, CHIME_RATE);
            if (chimeHalfTrack == null) chimeHalfTrack = buildStaticTrack(chimeHalfBuffer, CHIME_RATE);
            if (chimeGoodTrack == null) chimeGoodTrack = buildStaticTrack(chimeGoodBuffer, CHIME_RATE);
            if (chimeDoneTrack == null) chimeDoneTrack = buildStaticTrack(chimeDoneBuffer, CHIME_RATE);
            if (celebrationTrack == null) celebrationTrack = buildStaticTrack(celebrationBuffer, CHIME_RATE);
        }
    }

    public void pause() {
        synchronized (lock) {
            if (ambientTrack != null) {
                try {
                    if (ambientTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        ambientTrack.pause();
                    }
                    ambientTrack.flush();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public void resume() {
        synchronized (lock) {
            if (ambientTrack != null) {
                try {
                    if (!muted) {
                        ambientTrack.setVolume(masterVolume * AMBIENT_GAIN);
                        ambientTrack.play();
                    }
                } catch (Throwable ignored) {
                }
            } else if (started && !released) {
                start();
            }
        }
    }

    public void release() {
        released = true;
        started = false;
        synchronized (lock) {
            safeReleaseTrack(ambientTrack); ambientTrack = null;
            safeReleaseTrack(tickTrack); tickTrack = null;
            safeReleaseTrack(snapTrack); snapTrack = null;
            safeReleaseTrack(chimeHalfTrack); chimeHalfTrack = null;
            safeReleaseTrack(chimeGoodTrack); chimeGoodTrack = null;
            safeReleaseTrack(chimeDoneTrack); chimeDoneTrack = null;
            safeReleaseTrack(celebrationTrack); celebrationTrack = null;
        }
    }

    private static void safeReleaseTrack(AudioTrack t) {
        if (t != null) {
            try {
                if (t.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    t.stop();
                }
                t.release();
            } catch (Throwable ignored) {
            }
        }
    }

    // ======================================================================
    // VOLUME & MUTE
    // ======================================================================

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        synchronized (lock) {
            applyVolume();
        }
    }

    public boolean toggleMute() {
        muted = !muted;
        synchronized (lock) {
            applyVolume();
        }
        return muted;
    }

    private void applyVolume() {
        if (ambientTrack == null) return;
        try {
            float v = muted ? 0f : masterVolume * AMBIENT_GAIN;
            ambientTrack.setVolume(v);
            if (!muted && ambientTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                ambientTrack.play();
            }
        } catch (Throwable ignored) {
        }
    }

    public float volume() {
        return masterVolume;
    }

    public void setVolume(float v) {
        masterVolume = Math.max(0f, Math.min(1f, v));
        synchronized (lock) {
            applyVolume();
        }
    }

    // ======================================================================
    // PLAYBACK METHODS
    // ======================================================================

    private void playTrack(AudioTrack track, float gain) {
        if (released || muted || track == null) return;
        try {
            if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop();
            }
            track.reloadStaticData();
            float v = Math.min(1.0f, masterVolume * gain);
            track.setVolume(v);
            track.play();
        } catch (Throwable t) {
            Log.w(TAG, "playTrack error", t);
        }
    }

    public void playTick() {
        if (released || muted) return;
        synchronized (lock) {
            if (tickTrack == null) {
                if (tickBuffer == null) tickBuffer = buildTickBuffer();
                tickTrack = buildStaticTrack(tickBuffer, CHIME_RATE);
            }
            playTrack(tickTrack, 0.55f);
        }
    }

    public void playSnap() {
        if (released || muted) return;
        synchronized (lock) {
            if (snapTrack == null) {
                if (snapBuffer == null) snapBuffer = buildSnapBuffer();
                snapTrack = buildStaticTrack(snapBuffer, CHIME_RATE);
            }
            playTrack(snapTrack, 0.75f);
        }
    }

    public void playChime(int which) {
        if (released || muted) return;
        synchronized (lock) {
            switch (which) {
                case CHIME_HALF:
                    if (chimeHalfTrack == null) {
                        if (chimeHalfBuffer == null) chimeHalfBuffer = buildChimeBuffer(392.00f, 0.45f);
                        chimeHalfTrack = buildStaticTrack(chimeHalfBuffer, CHIME_RATE);
                    }
                    playTrack(chimeHalfTrack, 0.65f);
                    break;
                case CHIME_GOOD:
                    if (chimeGoodTrack == null) {
                        if (chimeGoodBuffer == null) chimeGoodBuffer = buildChimeBuffer(523.25f, 0.50f);
                        chimeGoodTrack = buildStaticTrack(chimeGoodBuffer, CHIME_RATE);
                    }
                    playTrack(chimeGoodTrack, 0.72f);
                    break;
                case CHIME_DONE:
                    if (chimeDoneTrack == null) {
                        if (chimeDoneBuffer == null) chimeDoneBuffer = buildChimeBuffer(659.25f, 0.60f);
                        chimeDoneTrack = buildStaticTrack(chimeDoneBuffer, CHIME_RATE);
                    }
                    playTrack(chimeDoneTrack, 0.80f);
                    break;
            }
        }
    }

    public void playChimes(int bitmask) {
        if ((bitmask & (1 << CHIME_HALF)) != 0) playChime(CHIME_HALF);
        if ((bitmask & (1 << CHIME_GOOD)) != 0) playChime(CHIME_GOOD);
        if ((bitmask & (1 << CHIME_DONE)) != 0) playChime(CHIME_DONE);
    }

    public void playCelebration() {
        if (released || muted) return;
        synchronized (lock) {
            if (celebrationTrack == null) {
                if (celebrationBuffer == null) celebrationBuffer = buildCelebrationBuffer();
                celebrationTrack = buildStaticTrack(celebrationBuffer, CHIME_RATE);
            }
            playTrack(celebrationTrack, 0.85f);
        }
    }

    // ======================================================================
    // TRACK BUILDERS (CORRECT STATIC SIZING & ANDROID ATTRIBUTES)
    // ======================================================================

    private static AudioTrack buildLoopingTrack(short[] pcm, int sampleRate) {
        if (pcm == null || pcm.length == 0) return null;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding);
        int trackBytes = Math.max(minBuf, pcm.length * 2);

        AudioTrack t = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                AudioFormat format = new AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build();
                t = new AudioTrack.Builder()
                        .setAudioAttributes(attrs)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(trackBytes)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
            } else {
                t = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, sampleRate,
                        channelConfig, encoding, trackBytes, AudioTrack.MODE_STATIC);
            }

            if (t.getState() != AudioTrack.STATE_INITIALIZED) {
                t.release();
                return null;
            }

            t.write(pcm, 0, pcm.length);
            t.setLoopPoints(0, pcm.length, -1);
            t.play();
            return t;
        } catch (Throwable t2) {
            Log.w(TAG, "buildLoopingTrack failed", t2);
            if (t != null) {
                try { t.release(); } catch (Throwable ignored) {}
            }
            return null;
        }
    }

    private static AudioTrack buildStaticTrack(short[] pcm, int sampleRate) {
        if (pcm == null || pcm.length == 0) return null;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding);
        int trackBytes = Math.max(minBuf, pcm.length * 2);

        AudioTrack t = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                AudioFormat format = new AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build();
                t = new AudioTrack.Builder()
                        .setAudioAttributes(attrs)
                        .setAudioFormat(format)
                        .setBufferSizeInBytes(trackBytes)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
            } else {
                t = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, sampleRate,
                        channelConfig, encoding, trackBytes, AudioTrack.MODE_STATIC);
            }

            if (t.getState() != AudioTrack.STATE_INITIALIZED) {
                t.release();
                return null;
            }

            t.write(pcm, 0, pcm.length);
            return t;
        } catch (Throwable t2) {
            Log.w(TAG, "buildStaticTrack failed", t2);
            if (t != null) {
                try { t.release(); } catch (Throwable ignored) {}
            }
            return null;
        }
    }

    // ======================================================================
    // PROCEDURAL SYNTHESIS (PURE PCM MATH)
    // ======================================================================

    private static short[] buildAmbientBuffer() {
        final int n = SAMPLE_RATE * AMBIENT_SECONDS;
        final float[] out = new float[n];
        final float seconds = (float) n / SAMPLE_RATE;

        final float[] partials = {55.0f, 82.5f, 110.0f, 164.8f};
        final float[] gains = {1.00f, 0.62f, 0.34f, 0.13f};

        for (int p = 0; p < partials.length; p++) {
            float hz = snapToWholeCycles(partials[p], seconds);
            if (hz <= 0f) continue;
            double w = 2.0 * Math.PI * hz / SAMPLE_RATE;
            double phase = 0;
            for (int i = 0; i < n; i++) {
                out[i] += (float) (gains[p] * Math.sin(phase));
                phase += w;
            }
        }

        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float lfo = 1f
                    + 0.16f * (float) Math.sin(2 * Math.PI * 0.0431f * t)
                    + 0.07f * (float) Math.sin(2 * Math.PI * 0.0173f * t + 1.1f);
            out[i] *= lfo;
        }

        addCrackle(out, n, seconds);

        int fade = (int) (CROSSFADE_SECONDS * SAMPLE_RATE);
        if (fade * 2 < n) {
            for (int i = 0; i < fade; i++) {
                float t = i / (float) fade;
                out[n - fade + i] *= (1f - t);
                out[i] = out[i] * t + out[n - fade + i] * (1f - t);
            }
        }

        float peak = 0f;
        for (int i = 0; i < n; i++) {
            float a = Math.abs(out[i]);
            if (a > peak) peak = a;
        }
        float norm = peak > 1e-6f ? 0.95f / peak : 0f;

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (float) Math.tanh(out[i] * norm * 1.1f);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }

    private static void addCrackle(float[] out, int n, float seconds) {
        final int bursts = (int) (seconds * 14f);
        final int maxLen = (int) (0.010f * SAMPLE_RATE);
        long seed = 0x5DEECE66DL;

        for (int b = 0; b < bursts; b++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            int start = (int) Math.abs(seed % n);

            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            int len = 1 + (int) Math.abs(seed % maxLen);

            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            float amp = 0.10f + 0.30f * (Math.abs(seed % 1000L) / 1000f);

            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            if (Math.abs(seed % 1000L) < 60) amp *= 2.2f;

            float decay = 1f;
            float prev = 0f;
            for (int i = 0; i < len; i++) {
                int idx = start + i;
                if (idx >= n) break;
                decay *= 0.82f;

                seed = (seed * 6364136223846793005L + 1442695040888963407L);
                float white = (Math.abs(seed % 20001L) / 10000f) - 1f;
                float hp = white - prev;
                prev = white;

                int j = idx % n;
                out[j] += hp * decay * amp;
            }
        }
    }

    private static float snapToWholeCycles(float hz, float seconds) {
        if (hz <= 0f || seconds <= 0f) return 0f;
        long cycles = Math.round(hz * seconds);
        if (cycles < 1) cycles = 1;
        return cycles / seconds;
    }

    private static short[] buildChimeBuffer(float rootHz, float gain) {
        final int n = (int) (CHIME_LENGTH * CHIME_RATE);
        final float[] out = new float[n];

        final float[] ratios = {1.0f, 1.5f, 2.0f, 3.0f};
        final float[] amps = {1.00f, 0.45f, 0.30f, 0.10f};
        final float[] decays = {2.6f, 3.4f, 4.2f, 6.0f};

        for (int v = 0; v < ratios.length; v++) {
            double w = 2.0 * Math.PI * rootHz * ratios[v] / CHIME_RATE;
            double phase = v * 1.7;
            for (int i = 0; i < n; i++) {
                float t = i / (float) CHIME_RATE;
                float env = (float) Math.exp(-t * decays[v]);
                float attack = Math.min(1f, t / 0.006f);
                out[i] += (float) (amps[v] * env * attack * Math.sin(phase));
                phase += w;
            }
        }

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (float) Math.tanh(out[i] * gain * 1.2f);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }

    private static short[] buildTickBuffer() {
        final int n = (int) (0.020f * CHIME_RATE);
        final short[] pcm = new short[n];
        double phase = 0;
        double w = 2.0 * Math.PI * 1150.0 / CHIME_RATE;
        for (int i = 0; i < n; i++) {
            float t = i / (float) CHIME_RATE;
            float env = (float) Math.exp(-t * 280f);
            float s = (float) (Math.sin(phase) * env);
            phase += w;
            pcm[i] = (short) (s * Short.MAX_VALUE * 0.65f);
        }
        return pcm;
    }

    private static short[] buildSnapBuffer() {
        final float duration = 0.14f;
        final int n = (int) (duration * CHIME_RATE);
        final short[] pcm = new short[n];
        double phaseThud = 0;
        double phaseRing = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) CHIME_RATE;
            float hzThud = 340f - 180f * (t / duration);
            double wThud = 2.0 * Math.PI * hzThud / CHIME_RATE;
            float envThud = (float) Math.exp(-t * 36f);
            float sThud = (float) (Math.sin(phaseThud) * envThud * 0.7f);
            phaseThud += wThud;

            double wRing = 2.0 * Math.PI * 880.0 / CHIME_RATE;
            float envRing = (float) Math.exp(-t * 22f);
            float sRing = (float) (Math.sin(phaseRing) * envRing * 0.25f);
            phaseRing += wRing;

            float combined = (float) Math.tanh(sThud + sRing);
            pcm[i] = (short) (combined * Short.MAX_VALUE * 0.75f);
        }
        return pcm;
    }

    private static short[] buildCelebrationBuffer() {
        final float duration = 2.4f;
        final int n = (int) (duration * CHIME_RATE);
        final float[] out = new float[n];

        float[] notes = {523.25f, 659.25f, 783.99f, 1046.50f};
        float[] startTimes = {0.0f, 0.16f, 0.32f, 0.48f};
        float[] ratios = {1.0f, 2.0f, 3.0f};
        float[] amps = {1.0f, 0.35f, 0.15f};

        for (int note = 0; note < notes.length; note++) {
            float noteStart = startTimes[note];
            int startIdx = (int) (noteStart * CHIME_RATE);
            float baseHz = notes[note];

            for (int r = 0; r < ratios.length; r++) {
                double w = 2.0 * Math.PI * (baseHz * ratios[r]) / CHIME_RATE;
                double phase = note * 0.9 + r * 1.3;
                float harmonicAmp = amps[r];

                for (int i = startIdx; i < n; i++) {
                    float t = (i - startIdx) / (float) CHIME_RATE;
                    float env = (float) Math.exp(-t * 1.8f);
                    float attack = Math.min(1f, t / 0.008f);
                    out[i] += (float) (harmonicAmp * env * attack * Math.sin(phase) * 0.35f);
                    phase += w;
                }
            }
        }

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (float) Math.tanh(out[i] * 1.15f);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }
}
