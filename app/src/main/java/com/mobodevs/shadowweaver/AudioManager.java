package com.mobodevs.shadowweaver;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Shadow Weaver - AudioManager
 * ===========================
 * Ambient sound and the gentle match chimes.
 *
 * ------------------------------------------------------------------
 * WHY THE AUDIO IS SYNTHESISED INSTEAD OF SHIPPED AS FILES
 * ------------------------------------------------------------------
 * The brief asked for a looping candle-crackle + ambient drone, plus soft
 * chimes, with no external libraries and nothing to install. Rather than
 * requiring three .wav/.ogg assets that the project does not have, the whole
 * soundtrack is GENERATED in code and pushed to an AudioTrack. That means:
 *
 *   - the app is self-contained and cannot ship with a missing asset,
 *   - the ambience can be retuned by editing numbers, not by re-recording,
 *   - the looping is mathematically seamless (see buildAmbientBuffer), and
 *   - it is a few tens of kilobytes of RAM instead of megabytes of audio.
 *
 * ------------------------------------------------------------------
 * HOW THE LOOP IS MADE SEAMLESS
 * ------------------------------------------------------------------
 * Two tricks, both necessary:
 *
 *   1. Every drone partial uses a frequency that completes a WHOLE number of
 *      cycles in the buffer (f = round(f * seconds) / seconds). If it did not,
 *      the loop point would land mid-waveform and produce an audible click on
 *      every repeat - which for a 6 second loop would be very obvious.
 *
 *   2. The very start and end are cross-faded over CROSSFADE seconds, so even
 *      after that, the splice is smooth.
 *
 * The crackle is a separate layer: sparse, very short, exponentially decaying
 * filtered noise bursts scattered across the buffer. Noise has no phase, so
 * it does not need cycle-snapping; the crossfade is enough.
 *
 * All of this happens on a background thread. AudioTrack construction can
 * block for tens of milliseconds on some devices, and doing that on the UI or
 * render thread is a visible stutter.
 */
public class AudioManager {

    private static final String TAG = "ShadowWeaverAudio";

    // ---- Ambient bed tuning ------------------------------------------------

    private static final int SAMPLE_RATE = 22050;   // plenty for a low drone
    private static final int AMBIENT_SECONDS = 6;
    private static final float CROSSFADE_SECONDS = 0.35f;

    /** Peak amplitude of the ambient bed. Deliberately tiny. */
    private static final float AMBIENT_GAIN = 0.055f;

    /** Volume as a fraction of full scale. 0 = muted, 1 = as loud as it goes. */
    private static float masterVolume = 0.5f;

    // ---- Chime tuning ------------------------------------------------------

    private static final int CHIME_RATE = 22050;
    private static final float CHIME_LENGTH = 1.6f;

    /**
     * Number of independent voices. A voice is reused round-robin, so if the
     * player crosses two match marks almost simultaneously both are heard
     * rather than the second cutting off the first.
     */
    private static final int CHIME_VOICES = 5;

    // Precomputed micro-effects for instant, zero-allocation playback
    private static short[] tickBuffer;
    private static short[] celebrationBuffer;

    // ---- State ------------------------------------------------------------

    private final Object lock = new Object();

    private AudioTrack ambientTrack;
    private Thread prepareThread;
    private volatile boolean released;
    private volatile boolean muted;
    private volatile boolean started;

    private final AudioTrack[] chimes = new AudioTrack[CHIME_VOICES];
    private int nextChime;

    // ======================================================================
    // LIFECYCLE
    // ======================================================================

    /**
     * Start the ambience. Safe to call repeatedly; it will not stack.
     * Returns immediately - the buffer is generated on a background thread.
     */
    public void start() {
        if (released || started) return;
        started = true;

        prepareThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    short[] buffer = buildAmbientBuffer();
                    if (released) return;

                    synchronized (lock) {
                        if (released) return;
                        ambientTrack = buildLoopingTrack(buffer, AMBIENT_SECONDS);
                        applyVolume();
                    }
                } catch (Throwable t) {
                    // Audio is a nicety, never a reason to crash the game.
                    Log.w(TAG, "ambient unavailable, continuing silently", t);
                }
            }
        }, "sw-ambient");
        prepareThread.start();
    }

    /** Pause the ambience, e.g. from Activity.onPause(). */
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

    /** Resume after pause(), rebuilding the track if it was released. */
    public void resume() {
        synchronized (lock) {
            if (ambientTrack != null) {
                try {
                    ambientTrack.play();
                } catch (Throwable ignored) {
                }
            } else if (started && !released) {
                // The track never materialised (or was dropped); build it again.
                start();
            }
        }
    }

    /** Stop and free everything. Call from Activity.onDestroy(). */
    public void release() {
        released = true;
        started = false;
        synchronized (lock) {
            if (ambientTrack != null) {
                try {
                    ambientTrack.stop();
                } catch (Throwable ignored) {
                }
                ambientTrack.release();
                ambientTrack = null;
            }
            for (int i = 0; i < CHIME_VOICES; i++) {
                if (chimes[i] != null) {
                    try {
                        chimes[i].stop();
                    } catch (Throwable ignored) {
                    }
                    chimes[i].release();
                    chimes[i] = null;
                }
            }
        }
    }

    // ======================================================================
    // MUTE
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

    /**
     * Toggle mute.
     *
     * Muting sets the track volume to zero rather than stopping it. Stopping
     * would save a little battery, but it also drops the ambient to a hard
     * silence that is more jarring than a quiet room, and un-muting would need
     * the buffer rebuilt. Volume zero is both cheaper to reason about and
     * gentler, which is the point of the game.
     *
     * @return the new muted state
     */
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
            float v = muted ? 0f : masterVolume;
            ambientTrack.setVolume(v);
        } catch (Throwable ignored) {
        }
    }

    public float volume() {
        return masterVolume;
    }

    public void setVolume(float v) {
        masterVolume = v < 0f ? 0f : (v > 1f ? 1f : v);
        synchronized (lock) {
            applyVolume();
        }
    }

    // ======================================================================
    // CHIMES
    // ======================================================================

    /** The three gentle marks a player can cross, in SilhouetteMatcher's order. */
    public static final int CHIME_HALF = SilhouetteMatcher.CHIME_HALF;
    public static final int CHIME_GOOD = SilhouetteMatcher.CHIME_GOOD;
    public static final int CHIME_DONE = SilhouetteMatcher.CHIME_DONE;

    /**
     * Play the chime for a match threshold that was just crossed.
     *
     * The three marks get slightly different intervals so the player can hear
     * progress without looking at the HUD - a rising figure, so "further
     * along" sounds like "higher up". Deliberately quiet, and deliberately
     * harmonic rather than bell-like: a real bell has a long, bright decay
     * that would be far too insistent for something that can fire three times
     * a minute.
     *
     * @param which one of CHIME_HALF, CHIME_GOOD, CHIME_DONE
     */
    public void playChime(int which) {
        if (released || muted) return;

        float rootHz;
        float gain;
        switch (which) {
            case CHIME_HALF:
                rootHz = 392.0f;   // G4
                gain = 0.16f;
                break;
            case CHIME_GOOD:
                rootHz = 523.25f;  // C5
                gain = 0.19f;
                break;
            case CHIME_DONE:
                rootHz = 659.25f;  // E5
                gain = 0.26f;
                break;
            default:
                return;
        }

        short[] buffer = buildChimeBuffer(rootHz, gain);
        synchronized (lock) {
            if (released) return;
            try {
                AudioTrack voice = chimes[nextChime];
                if (voice == null) {
                    voice = buildStaticTrack(buffer, CHIME_LENGTH);
                    chimes[nextChime] = voice;
                } else {
                    // Rewind and refill the existing voice.
                    voice.pause();
                    voice.flush();
                    voice.reloadStaticData();
                }
                voice.write(buffer, 0, buffer.length);
                voice.setVolume(masterVolume);
                voice.play();
                nextChime = (nextChime + 1) % CHIME_VOICES;
            } catch (Throwable t) {
                Log.w(TAG, "chime unavailable", t);
            }
        }
    }

    /** Convenience: play every mark contained in a bitmask from the matcher. */
    public void playChimes(int bitmask) {
        if ((bitmask & (1 << CHIME_HALF)) != 0) playChime(CHIME_HALF);
        if ((bitmask & (1 << CHIME_GOOD)) != 0) playChime(CHIME_GOOD);
        if ((bitmask & (1 << CHIME_DONE)) != 0) playChime(CHIME_DONE);
    }

    /**
     * Play a subtle acoustic click when a shape turns past a 15-degree detent.
     */
    public void playTick() {
        if (released || muted) return;
        if (tickBuffer == null) {
            tickBuffer = buildTickBuffer();
        }
        playRawPcm(tickBuffer, 0.18f);
    }

    /**
     * Play a lush ascending celebratory chord arpeggio when a level is completed.
     */
    public void playCelebration() {
        if (released || muted) return;
        if (celebrationBuffer == null) {
            celebrationBuffer = buildCelebrationBuffer();
        }
        playRawPcm(celebrationBuffer, 0.40f);
    }

    private void playRawPcm(short[] buffer, float gain) {
        synchronized (lock) {
            if (released) return;
            try {
                AudioTrack voice = chimes[nextChime];
                if (voice == null) {
                    voice = buildStaticTrack(buffer, (float) buffer.length / CHIME_RATE);
                    chimes[nextChime] = voice;
                } else {
                    voice.pause();
                    voice.flush();
                    voice.reloadStaticData();
                }
                voice.write(buffer, 0, buffer.length);
                voice.setVolume(masterVolume * gain);
                voice.play();
                nextChime = (nextChime + 1) % CHIME_VOICES;
            } catch (Throwable t) {
                Log.w(TAG, "sfx voice unavailable", t);
            }
        }
    }

    // ======================================================================
    // SYNTHESIS
    // ======================================================================

    /**
     * Build the looping ambient bed: a warm low drone with slow movement, plus
     * sparse candle crackle.
     *
     * Layout, from lowest frequency up:
     *   55.0 Hz   root, the body of the drone
     *   82.5 Hz   fifth, gives it warmth
     *   110.0 Hz  octave, adds presence
     *   164.8 Hz  a quiet shimmer way up top
     * plus two very slow sine LFOs on the master level, which is what makes
     * it feel like breathing rather than like a held organ note.
     */
    private static short[] buildAmbientBuffer() {
        final int n = SAMPLE_RATE * AMBIENT_SECONDS;
        final float[] out = new float[n];
        final float seconds = (float) n / SAMPLE_RATE;

        // Snap each partial to a whole number of cycles across the buffer, so
        // the loop point lands on zero phase and cannot click.
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

        // Slow breathing. These do not need cycle-snapping because the
        // crossfade at the seam hides them, and their periods are far longer
        // than the fade.
        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float lfo = 1f
                    + 0.16f * (float) Math.sin(2 * Math.PI * 0.0431f * t)
                    + 0.07f * (float) Math.sin(2 * Math.PI * 0.0173f * t + 1.1f);
            out[i] *= lfo;
        }

        addCrackle(out, n, seconds);

        // Crossfade the tail into the head so the splice is inaudible.
        int fade = (int) (CROSSFADE_SECONDS * SAMPLE_RATE);
        if (fade * 2 < n) {
            for (int i = 0; i < fade; i++) {
                float t = i / (float) fade;   // 0 -> 1
                // tail fades out ...
                out[n - fade + i] *= (1f - t);
                // ... while the head fades in, taking the tail's contribution
                out[i] = out[i] * t + out[n - fade + i] * (1f - t);
            }
        }

        // Normalise, then apply the very low ambient gain, then quantise.
        float peak = 0f;
        for (int i = 0; i < n; i++) {
            float a = Math.abs(out[i]);
            if (a > peak) peak = a;
        }
        float norm = peak > 1e-6f ? 0.9f / peak : 0f;

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = out[i] * norm * AMBIENT_GAIN;
            // Soft clip rather than hard wrap, so a stray transient can never
            // produce the crackle of digital distortion.
            v = (float) Math.tanh(v * 1.2f);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }

    /**
     * Sparse candle crackle: short bursts of noise with a fast attack and an
     * exponential decay, scattered pseudo-randomly but deterministically.
     *
     * Each burst is high-passed by differencing the noise, which turns white
     * noise into something with the sharp, bright character of a flame and
     * leaves the low end for the drone.
     */
    private static void addCrackle(float[] out, int n, float seconds) {
        final int bursts = (int) (seconds * 14f);        // ~14 crackles/second
        final int maxLen = (int) (0.010f * SAMPLE_RATE); // 10ms
        long seed = 0x5DEECE66DL;

        for (int b = 0; b < bursts; b++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            int start = (int) Math.abs(seed % n);

            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            int len = 1 + (int) Math.abs(seed % maxLen);

            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            float amp = 0.10f + 0.30f * (Math.abs(seed % 1000L) / 1000f);

            // Occasional bigger pop.
            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            if (Math.abs(seed % 1000L) < 60) amp *= 2.2f;

            float decay = 1f;
            float prev = 0f;
            for (int i = 0; i < len; i++) {
                int idx = start + i;
                if (idx >= n) break;
                decay *= 0.82f;                    // fast exponential tail

                seed = (seed * 6364136223846793005L + 1442695040888963407L);
                float white = (Math.abs(seed % 20001L) / 10000f) - 1f;
                float hp = white - prev;            // crude but effective high-pass
                prev = white;

                // Wrap the add so a burst near the seam does not pop.
                int j = idx % n;
                out[j] += hp * decay * amp;
            }
        }
    }

    /**
     * Snap a frequency to the nearest value that completes a whole number of
     * cycles in {@code seconds}, so it lines up with itself across the loop.
     */
    private static float snapToWholeCycles(float hz, float seconds) {
        if (hz <= 0f || seconds <= 0f) return 0f;
        long cycles = Math.round(hz * seconds);
        if (cycles < 1) cycles = 1;
        return cycles / seconds;
    }

    /**
     * One soft, gently struck chord. Not a bell - a bell rings for many
     * seconds and would be exhausting. This is the root plus a fifth and an
     * octave, each decaying at its own rate so the top fades first and it
     * settles into a warm fifth.
     */
    private static short[] buildChimeBuffer(float rootHz, float gain) {
        final int n = (int) (CHIME_LENGTH * CHIME_RATE);
        final float[] out = new float[n];

        final float[] ratios = {1.0f, 1.5f, 2.0f, 3.0f};
        final float[] amps = {1.00f, 0.45f, 0.30f, 0.10f};
        final float[] decays = {2.6f, 3.4f, 4.2f, 6.0f};

        for (int v = 0; v < ratios.length; v++) {
            double w = 2.0 * Math.PI * rootHz * ratios[v] / CHIME_RATE;
            double phase = 0;
            // Each partial gets its own start phase so they do not all hit
            // zero at once, which would sound like a click on the attack.
            phase = v * 1.7;
            for (int i = 0; i < n; i++) {
                float t = i / (float) CHIME_RATE;
                float env = (float) Math.exp(-t * decays[v]);
                // Short fade-in over the first few ms: a soft mallet, not a
                // hammer, and it also removes the DC step at sample 0.
                float attack = Math.min(1f, t / 0.006f);
                out[i] += (float) (amps[v] * env * attack * Math.sin(phase));
                phase += w;
            }
        }

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = out[i] * gain;
            v = (float) Math.tanh(v);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }

    /**
     * Build an ultra-short, satisfying wooden mechanical click for rotation detents.
     */
    private static short[] buildTickBuffer() {
        final int n = (int) (0.020f * CHIME_RATE); // 20ms
        final short[] pcm = new short[n];
        double phase = 0;
        double w = 2.0 * Math.PI * 1150.0 / CHIME_RATE;
        for (int i = 0; i < n; i++) {
            float t = i / (float) CHIME_RATE;
            float env = (float) Math.exp(-t * 280f);
            float s = (float) (Math.sin(phase) * env);
            phase += w;
            pcm[i] = (short) (s * Short.MAX_VALUE * 0.45f);
        }
        return pcm;
    }

    /**
     * Build a lush ascending celebratory chord arpeggio for level complete.
     */
    private static short[] buildCelebrationBuffer() {
        final float duration = 2.4f;
        final int n = (int) (duration * CHIME_RATE);
        final float[] out = new float[n];

        // Ascending major arpeggio: C5 -> E5 -> G5 -> C6
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
                    out[i] += (float) (harmonicAmp * env * attack * Math.sin(phase) * 0.32f);
                    phase += w;
                }
            }
        }

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (float) Math.tanh(out[i]);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }

    // ======================================================================
    // AUDIOTRACK PLUMBING
    // ======================================================================

    private static int minBufferBytes(short[] pcm) {
        // A generous buffer keeps the ambient smooth on cheap devices without
        // adding perceptible latency; chimes do not care either way.
        return Math.max(pcm.length * 2, SAMPLE_RATE / 4);
    }

    @SuppressWarnings("deprecation")
    private static AudioTrack buildLoopingTrack(short[] pcm, int seconds) {
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;

        int track = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelConfig, encoding);
        if (track <= 0) track = minBufferBytes(pcm);

        AudioTrack t = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                channelConfig, encoding, track, AudioTrack.MODE_STATIC);
        if (t.getState() != AudioTrack.STATE_INITIALIZED) {
            t.release();
            throw new IllegalStateException("AudioTrack did not initialise");
        }

        t.write(pcm, 0, pcm.length);
        // -1 as the frame count means "to the end of the buffer", and asking
        // for an even number of frames keeps the loop on a frame boundary.
        t.setLoopPoints(0, pcm.length / 2, -1);
        t.play();
        return t;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("deprecation")
    private static AudioTrack buildStaticTrack(short[] pcm, float seconds) {
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;

        int track = AudioTrack.getMinBufferSize(CHIME_RATE, channelConfig, encoding);
        if (track <= 0) track = pcm.length * 2;

        AudioTrack t = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, CHIME_RATE,
                channelConfig, encoding, track, AudioTrack.MODE_STATIC);
        if (t.getState() != AudioTrack.STATE_INITIALIZED) {
            t.release();
            throw new IllegalStateException("AudioTrack did not initialise");
        }
        t.write(pcm, 0, pcm.length);
        return t;
    }
}
