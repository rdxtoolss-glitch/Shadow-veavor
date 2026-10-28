package com.mobodevs.shadowweaver;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Shadow Weaver - AudioManager
 * ===========================
 * Pure procedural audio engine synthesized mathematically in code.
 * Zero external audio assets, zero native dependencies, zero latency.
 *
 * Employs Android SoundPool with AudioAttributes USAGE_GAME and CONTENT_TYPE_MUSIC
 * (STREAM_MUSIC) to guarantee clear, reliable, polyphonic playback across all
 * Android devices (even when device ringer is set to vibrate or silent mode).
 * Includes an AudioTrack fallback for environments where SoundPool is restricted.
 */
public class AudioManager {

    private static final String TAG = "ShadowWeaverAudio";

    // Audio format
    private static final int SAMPLE_RATE = 22050;
    private static final int AMBIENT_SECONDS = 6;
    private static final float CROSSFADE_SECONDS = 0.40f;

    // Audible, rich harmonic alchemical candle drone (optimized for phone speakers)
    private static final float AMBIENT_GAIN = 0.50f;
    private static float masterVolume = 0.95f;

    // Silhouette match chime marks
    public static final int CHIME_HALF = SilhouetteMatcher.CHIME_HALF;
    public static final int CHIME_GOOD = SilhouetteMatcher.CHIME_GOOD;
    public static final int CHIME_DONE = SilhouetteMatcher.CHIME_DONE;

    // Sound indexes for SoundPool
    private static final int IDX_CLICK = 0;
    private static final int IDX_TICK = 1;
    private static final int IDX_PICKUP = 2;
    private static final int IDX_DROP = 3;
    private static final int IDX_SNAP = 4;
    private static final int IDX_CHIME_HALF = 5;
    private static final int IDX_CHIME_GOOD = 6;
    private static final int IDX_CHIME_DONE = 7;
    private static final int IDX_CELEBRATION = 8;
    private static final int IDX_AMBIENT = 9;
    private static final int SOUND_COUNT = 10;

    // State & synchronization
    private final Object lock = new Object();
    private Context context;

    private SoundPool soundPool;
    private final int[] soundPoolIds = new int[SOUND_COUNT];
    private final boolean[] isLoaded = new boolean[SOUND_COUNT];
    private final boolean[] pendingPlay = new boolean[SOUND_COUNT];
    private final long[] pendingPlayTime = new long[SOUND_COUNT];
    private final float[] pendingGain = new float[SOUND_COUNT];
    private final Map<Integer, Integer> sampleToSoundIndex = new HashMap<>();

    private int ambientStreamId = 0;
    private AudioTrack fallbackAmbientTrack;
    private AudioTrack fallbackClickTrack;

    private Thread prepareThread;
    private volatile boolean released;
    private volatile boolean muted;
    private volatile boolean started;

    // Cached PCM buffers (shared statically across instances)
    private static short[] ambientBuffer;
    private static short[] clickBuffer;
    private static short[] tickBuffer;
    private static short[] pickupBuffer;
    private static short[] dropBuffer;
    private static short[] snapBuffer;
    private static short[] chimeHalfBuffer;
    private static short[] chimeGoodBuffer;
    private static short[] chimeDoneBuffer;
    private static short[] celebrationBuffer;

    // ======================================================================
    // CONSTRUCTORS & INITIALIZATION
    // ======================================================================

    public AudioManager() {
        this(null);
    }

    public AudioManager(Context ctx) {
        if (ctx != null) {
            this.context = ctx.getApplicationContext();
        }
        initSoundPool();
        start();
    }

    private void initSoundPool() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                soundPool = new SoundPool.Builder()
                        .setMaxStreams(12)
                        .setAudioAttributes(attrs)
                        .build();
            } else {
                @SuppressWarnings("deprecation")
                SoundPool legacy = new SoundPool(12, android.media.AudioManager.STREAM_MUSIC, 0);
                soundPool = legacy;
            }

            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool sp, int sampleId, int status) {
                    synchronized (lock) {
                        if (released) return;
                        Integer idx = sampleToSoundIndex.get(sampleId);
                        if (idx != null && status == 0) {
                            isLoaded[idx] = true;
                            // If ambient loaded and game is active, start ambient loop
                            if (idx == IDX_AMBIENT) {
                                if (started && !muted && ambientStreamId == 0) {
                                    startAmbientLoop();
                                }
                            }
                            // Process any pending play triggered during cold start
                            if (pendingPlay[idx]) {
                                pendingPlay[idx] = false;
                                if (System.currentTimeMillis() - pendingPlayTime[idx] < 600) {
                                    playByIndex(idx, pendingGain[idx]);
                                }
                            }
                        }
                    }
                }
            });

            // Diagnostic volume check
            if (context != null) {
                try {
                    android.media.AudioManager am = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (am != null) {
                        int cur = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
                        int max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                        Log.i(TAG, "Audio initialized. STREAM_MUSIC volume: " + cur + " / " + max);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "Failed to initialize SoundPool, fallback will be used", t);
        }
    }

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
                    prepareAudioFilesAndLoad();
                } catch (Throwable t) {
                    Log.w(TAG, "Audio synthesis thread error", t);
                }
            }
        }, "sw-audio-init");
        prepareThread.setPriority(Thread.NORM_PRIORITY - 1);
        prepareThread.start();
    }

    /**
     * Synthesizes procedural PCM buffers and loads them into SoundPool.
     */
    private void prepareAudioFilesAndLoad() {
        File cacheDir = null;
        if (context != null) {
            try {
                cacheDir = new File(context.getCacheDir(), "sfx");
                if (!cacheDir.exists()) cacheDir.mkdirs();
            } catch (Throwable ignored) {
            }
        }

        // Priority 1: Instant tactile interaction sounds (click, tick, snap)
        loadOrSynthesize(IDX_CLICK, "sfx_click.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (clickBuffer == null) clickBuffer = buildClickBuffer();
                return clickBuffer;
            }
        });

        loadOrSynthesize(IDX_TICK, "sfx_tick.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (tickBuffer == null) tickBuffer = buildTickBuffer();
                return tickBuffer;
            }
        });

        loadOrSynthesize(IDX_SNAP, "sfx_snap.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (snapBuffer == null) snapBuffer = buildSnapBuffer();
                return snapBuffer;
            }
        });

        // Priority 2: In-game manipulation sounds (pickup, drop)
        loadOrSynthesize(IDX_PICKUP, "sfx_pickup.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (pickupBuffer == null) pickupBuffer = buildPickupBuffer();
                return pickupBuffer;
            }
        });

        loadOrSynthesize(IDX_DROP, "sfx_drop.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (dropBuffer == null) dropBuffer = buildDropBuffer();
                return dropBuffer;
            }
        });

        // Priority 3: Silhouette chimes and celebration
        loadOrSynthesize(IDX_CHIME_HALF, "sfx_chime_half.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (chimeHalfBuffer == null) chimeHalfBuffer = buildChimeBuffer(392.00f, 0.75f);
                return chimeHalfBuffer;
            }
        });

        loadOrSynthesize(IDX_CHIME_GOOD, "sfx_chime_good.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (chimeGoodBuffer == null) chimeGoodBuffer = buildChimeBuffer(523.25f, 0.82f);
                return chimeGoodBuffer;
            }
        });

        loadOrSynthesize(IDX_CHIME_DONE, "sfx_chime_done.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (chimeDoneBuffer == null) chimeDoneBuffer = buildChimeBuffer(659.25f, 0.90f);
                return chimeDoneBuffer;
            }
        });

        loadOrSynthesize(IDX_CELEBRATION, "sfx_celebration.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (celebrationBuffer == null) celebrationBuffer = buildCelebrationBuffer();
                return celebrationBuffer;
            }
        });

        // Priority 4: Ambient drone
        loadOrSynthesize(IDX_AMBIENT, "sfx_ambient.wav", cacheDir, new BufferGenerator() {
            @Override public short[] generate() {
                if (ambientBuffer == null) ambientBuffer = buildAmbientBuffer();
                return ambientBuffer;
            }
        });
    }

    private interface BufferGenerator {
        short[] generate();
    }

    private void loadOrSynthesize(int soundIndex, String filename, File cacheDir, BufferGenerator gen) {
        if (released) return;
        try {
            File targetFile = (cacheDir != null) ? new File(cacheDir, filename) : null;
            if (targetFile != null && (!targetFile.exists() || targetFile.length() <= 44)) {
                short[] pcm = gen.generate();
                if (pcm != null && pcm.length > 0) {
                    byte[] wav = pcmToWav(pcm, SAMPLE_RATE);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    fos.write(wav);
                    fos.flush();
                    fos.close();
                }
            }

            if (targetFile != null && targetFile.exists() && soundPool != null) {
                synchronized (lock) {
                    if (released) return;
                    int id = soundPool.load(targetFile.getAbsolutePath(), 1);
                    soundPoolIds[soundIndex] = id;
                    sampleToSoundIndex.put(id, soundIndex);
                }
            } else if (soundPool == null) {
                // If SoundPool is not available, generate buffer into memory for AudioTrack fallback
                gen.generate();
            }
        } catch (Throwable t) {
            Log.w(TAG, "Error preparing " + filename, t);
        }
    }

    public void pause() {
        synchronized (lock) {
            if (soundPool != null) {
                try {
                    soundPool.autoPause();
                } catch (Throwable ignored) {
                }
            }
            if (fallbackAmbientTrack != null) {
                try {
                    if (fallbackAmbientTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        fallbackAmbientTrack.pause();
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public void resume() {
        synchronized (lock) {
            if (released) return;
            if (!started) {
                start();
                return;
            }
            if (soundPool != null) {
                try {
                    soundPool.autoResume();
                    if (!muted && isLoaded[IDX_AMBIENT]) {
                        if (ambientStreamId != 0) {
                            soundPool.setVolume(ambientStreamId, masterVolume * AMBIENT_GAIN, masterVolume * AMBIENT_GAIN);
                        } else {
                            startAmbientLoop();
                        }
                    }
                } catch (Throwable ignored) {
                }
            } else if (fallbackAmbientTrack != null && !muted) {
                try {
                    fallbackAmbientTrack.play();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    public void release() {
        released = true;
        started = false;
        synchronized (lock) {
            if (soundPool != null) {
                try {
                    soundPool.release();
                } catch (Throwable ignored) {
                }
                soundPool = null;
            }
            safeReleaseTrack(fallbackAmbientTrack); fallbackAmbientTrack = null;
            safeReleaseTrack(fallbackClickTrack); fallbackClickTrack = null;
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
        float ambVol = muted ? 0f : masterVolume * AMBIENT_GAIN;
        if (soundPool != null) {
            try {
                if (ambientStreamId != 0) {
                    soundPool.setVolume(ambientStreamId, ambVol, ambVol);
                } else if (!muted && isLoaded[IDX_AMBIENT]) {
                    startAmbientLoop();
                }
            } catch (Throwable ignored) {
            }
        }
        if (fallbackAmbientTrack != null) {
            try {
                setTrackVolume(fallbackAmbientTrack, ambVol);
                if (!muted && fallbackAmbientTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                    fallbackAmbientTrack.play();
                }
            } catch (Throwable ignored) {
            }
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

    @SuppressWarnings("deprecation")
    private static void setTrackVolume(AudioTrack track, float v) {
        if (track == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track.setVolume(v);
            } else {
                track.setStereoVolume(v, v);
            }
        } catch (Throwable ignored) {
        }
    }

    // ======================================================================
    // PLAYBACK METHODS
    // ======================================================================

    private void startAmbientLoop() {
        if (released || muted || soundPool == null || !isLoaded[IDX_AMBIENT]) return;
        try {
            float vol = masterVolume * AMBIENT_GAIN;
            int sid = soundPoolIds[IDX_AMBIENT];
            ambientStreamId = soundPool.play(sid, vol, vol, 1, -1, 1.0f);
        } catch (Throwable t) {
            Log.w(TAG, "Error starting SoundPool ambient loop", t);
        }
    }

    private void playByIndex(int soundIdx, float gain) {
        if (released || muted) return;

        synchronized (lock) {
            if (soundPool != null) {
                if (isLoaded[soundIdx]) {
                    int sid = soundPoolIds[soundIdx];
                    float vol = Math.max(0f, Math.min(1.0f, masterVolume * gain));
                    soundPool.play(sid, vol, vol, 1, 0, 1.0f);
                    return;
                } else {
                    // Record pending play to trigger once onLoadComplete fires
                    pendingPlay[soundIdx] = true;
                    pendingPlayTime[soundIdx] = System.currentTimeMillis();
                    pendingGain[soundIdx] = gain;
                    return;
                }
            }

            // Direct AudioTrack fallback if SoundPool is unavailable
            playFallback(soundIdx, gain);
        }
    }

    /**
     * High-reliability AudioTrack fallback using STREAM_MUSIC / USAGE_GAME + CONTENT_TYPE_MUSIC.
     */
    private void playFallback(int soundIdx, float gain) {
        if (soundIdx == IDX_CLICK) {
            if (fallbackClickTrack == null) {
                if (clickBuffer == null) clickBuffer = buildClickBuffer();
                fallbackClickTrack = buildStaticTrack(clickBuffer, SAMPLE_RATE);
            }
            playTrackFallback(fallbackClickTrack, gain);
        }
    }

    private static void playTrackFallback(AudioTrack track, float gain) {
        if (track == null) return;
        try {
            if (track.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                track.stop();
            }
            track.setPlaybackHeadPosition(0);
            track.reloadStaticData();
            float v = Math.min(1.0f, masterVolume * gain);
            setTrackVolume(track, v);
            track.play();
        } catch (Throwable t) {
            Log.w(TAG, "playTrackFallback error", t);
        }
    }

    /**
     * Tactile UI button click (crisp 1850Hz crystal tap + 820Hz resonant wooden body).
     */
    public void playClick() {
        playByIndex(IDX_CLICK, 0.90f);
    }

    /**
     * Shape pickup / touch down (warm upward lift sweep: 440 Hz -> 720 Hz).
     */
    public void playPickup() {
        playByIndex(IDX_PICKUP, 0.85f);
    }

    /**
     * Shape drop / touch release without snap (resonant wooden table contact: 340 Hz -> 220 Hz).
     */
    public void playDrop() {
        playByIndex(IDX_DROP, 0.80f);
    }

    /**
     * Rotation detent tick (crisp dual-tone mechanical notch: 2200 Hz + 1400 Hz).
     */
    public void playTick() {
        playByIndex(IDX_TICK, 0.85f);
    }

    /**
     * Magnetic snap assist (resonant astral bell lock chord: D5 587Hz + A5 880Hz + D6 1174Hz).
     */
    public void playSnap() {
        playByIndex(IDX_SNAP, 0.95f);
    }

    /**
     * Silhouette progress chimes (Tibetan singing bowl harmonics).
     */
    public void playChime(int which) {
        switch (which) {
            case CHIME_HALF:
                playByIndex(IDX_CHIME_HALF, 0.90f);
                break;
            case CHIME_GOOD:
                playByIndex(IDX_CHIME_GOOD, 0.92f);
                break;
            case CHIME_DONE:
                playByIndex(IDX_CHIME_DONE, 0.95f);
                break;
        }
    }

    public void playChimes(int bitmask) {
        if ((bitmask & (1 << CHIME_HALF)) != 0) playChime(CHIME_HALF);
        if ((bitmask & (1 << CHIME_GOOD)) != 0) playChime(CHIME_GOOD);
        if ((bitmask & (1 << CHIME_DONE)) != 0) playChime(CHIME_DONE);
    }

    /**
     * Victory celebration fanfare (ascending celestial chord: C5, E5, G5, C6).
     */
    public void playCelebration() {
        playByIndex(IDX_CELEBRATION, 1.0f);
    }

    // ======================================================================
    // PROCEDURAL SYNTHESIS (ACOUSTICALLY TUNED FOR SMARTPHONE SPEAKERS)
    // ======================================================================

    /**
     * Tactile UI click: 75ms dual-frequency tap (1850Hz transient + 820Hz wooden body).
     */
    public static short[] buildClickBuffer() {
        final float duration = 0.075f;
        final int n = (int) (duration * SAMPLE_RATE);
        final short[] pcm = new short[n];

        double phaseA = 0;
        double phaseB = 0;
        double wA = 2.0 * Math.PI * 1850.0 / SAMPLE_RATE;
        double wB = 2.0 * Math.PI * 820.0 / SAMPLE_RATE;

        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float attack = Math.min(1.0f, t / 0.002f);
            float envA = (float) Math.exp(-t * 90f);
            float envB = (float) Math.exp(-t * 45f);

            float s = (float) (Math.sin(phaseA) * envA * 0.55f + Math.sin(phaseB) * envB * 0.45f) * attack;
            phaseA += wA;
            phaseB += wB;

            float saturated = (float) Math.tanh(s * 1.5f);
            pcm[i] = (short) (saturated * 32000);
        }
        return pcm;
    }

    /**
     * Rotation detent tick: 45ms mechanical rotary notch (2200Hz + 1400Hz).
     */
    public static short[] buildTickBuffer() {
        final float duration = 0.045f;
        final int n = (int) (duration * SAMPLE_RATE);
        final short[] pcm = new short[n];

        double phaseA = 0;
        double phaseB = 0;
        double wA = 2.0 * Math.PI * 2200.0 / SAMPLE_RATE;
        double wB = 2.0 * Math.PI * 1400.0 / SAMPLE_RATE;

        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float attack = Math.min(1.0f, t / 0.0015f);
            float envA = (float) Math.exp(-t * 140f);
            float envB = (float) Math.exp(-t * 80f);

            float s = (float) (Math.sin(phaseA) * envA * 0.60f + Math.sin(phaseB) * envB * 0.40f) * attack;
            phaseA += wA;
            phaseB += wB;

            float saturated = (float) Math.tanh(s * 1.4f);
            pcm[i] = (short) (saturated * 31000);
        }
        return pcm;
    }

    /**
     * Shape pickup: 110ms warm upward wooden lift (440 Hz -> 720 Hz).
     */
    public static short[] buildPickupBuffer() {
        final float duration = 0.110f;
        final int n = (int) (duration * SAMPLE_RATE);
        final short[] pcm = new short[n];

        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float attack = Math.min(1.0f, t / 0.004f);
            float hz = 440f + 280f * (float) Math.sqrt(t / duration);
            double w = 2.0 * Math.PI * hz / SAMPLE_RATE;
            float env = (float) Math.sin(Math.PI * (t / duration));

            float s = (float) (Math.sin(phase) * env) * attack;
            phase += w;

            float saturated = (float) Math.tanh(s * 1.35f);
            pcm[i] = (short) (saturated * 31500);
        }
        return pcm;
    }

    /**
     * Shape drop: 120ms resonant wooden desk contact (340 Hz -> 220 Hz).
     */
    public static short[] buildDropBuffer() {
        final float duration = 0.120f;
        final int n = (int) (duration * SAMPLE_RATE);
        final short[] pcm = new short[n];

        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float attack = Math.min(1.0f, t / 0.003f);
            float hz = 340f - 120f * (t / duration);
            double w = 2.0 * Math.PI * hz / SAMPLE_RATE;
            float env = (float) Math.exp(-t * 32f);

            float s = (float) (Math.sin(phase) * env) * attack;
            phase += w;

            float saturated = (float) Math.tanh(s * 1.35f);
            pcm[i] = (short) (saturated * 31000);
        }
        return pcm;
    }

    /**
     * Magnetic snap assist: 220ms resonant dual-bell celestial lock (D5 + A5 + D6).
     */
    public static short[] buildSnapBuffer() {
        final float duration = 0.220f;
        final int n = (int) (duration * SAMPLE_RATE);
        final short[] pcm = new short[n];

        double phaseA = 0;
        double phaseB = 0;
        double phaseC = 0;
        double wA = 2.0 * Math.PI * 587.33 / SAMPLE_RATE;
        double wB = 2.0 * Math.PI * 880.00 / SAMPLE_RATE;
        double wC = 2.0 * Math.PI * 1174.66 / SAMPLE_RATE;

        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float attack = Math.min(1.0f, t / 0.003f);
            float envA = (float) Math.exp(-t * 18f);
            float envB = (float) Math.exp(-t * 22f);
            float envC = (float) Math.exp(-t * 36f);

            float sA = (float) (Math.sin(phaseA) * envA * 0.55f);
            float sB = (float) (Math.sin(phaseB) * envB * 0.35f);
            float sC = (float) (Math.sin(phaseC) * envC * 0.20f);
            phaseA += wA;
            phaseB += wB;
            phaseC += wC;

            float combined = (sA + sB + sC) * attack;
            float saturated = (float) Math.tanh(combined * 1.35f);
            pcm[i] = (short) (saturated * 32000);
        }
        return pcm;
    }

    /**
     * Silhouette match chimes: Tibetan singing bowl harmonic chime.
     */
    public static short[] buildChimeBuffer(float rootHz, float gain) {
        final float duration = 1.8f;
        final int n = (int) (duration * SAMPLE_RATE);
        final float[] out = new float[n];

        final float[] ratios = {1.0f, 1.5f, 2.0f, 2.76f};
        final float[] amps = {1.00f, 0.50f, 0.35f, 0.18f};
        final float[] decays = {2.2f, 2.8f, 3.6f, 5.0f};

        for (int v = 0; v < ratios.length; v++) {
            double w = 2.0 * Math.PI * rootHz * ratios[v] / SAMPLE_RATE;
            double phase = v * 1.7;
            for (int i = 0; i < n; i++) {
                float t = i / (float) SAMPLE_RATE;
                float env = (float) Math.exp(-t * decays[v]);
                float attack = Math.min(1f, t / 0.008f);
                out[i] += (float) (amps[v] * env * attack * Math.sin(phase));
                phase += w;
            }
        }

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (float) Math.tanh(out[i] * gain * 1.30f);
            pcm[i] = (short) (v * 32500);
        }
        return pcm;
    }

    /**
     * Victory celebration: 4-note ascending celestial fanfare (C5, E5, G5, C6) over 2.4s.
     */
    public static short[] buildCelebrationBuffer() {
        final float duration = 2.4f;
        final int n = (int) (duration * SAMPLE_RATE);
        final float[] out = new float[n];

        float[] notes = {523.25f, 659.25f, 783.99f, 1046.50f};
        float[] startTimes = {0.0f, 0.18f, 0.36f, 0.54f};
        float[] ratios = {1.0f, 2.0f, 3.0f};
        float[] amps = {1.0f, 0.45f, 0.22f};

        for (int note = 0; note < notes.length; note++) {
            float noteStart = startTimes[note];
            int startIdx = (int) (noteStart * SAMPLE_RATE);
            float baseHz = notes[note];

            for (int r = 0; r < ratios.length; r++) {
                double w = 2.0 * Math.PI * (baseHz * ratios[r]) / SAMPLE_RATE;
                double phase = note * 0.9 + r * 1.3;
                float harmonicAmp = amps[r];

                for (int i = startIdx; i < n; i++) {
                    float t = (i - startIdx) / (float) SAMPLE_RATE;
                    float env = (float) Math.exp(-t * 1.5f);
                    float attack = Math.min(1f, t / 0.008f);
                    out[i] += (float) (harmonicAmp * env * attack * Math.sin(phase) * 0.42f);
                    phase += w;
                }
            }
        }

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (float) Math.tanh(out[i] * 1.25f);
            pcm[i] = (short) (v * 32500);
        }
        return pcm;
    }

    /**
     * Ambient candle drone: Warm, meditative harmonic sanctuary.
     * Tuned to 220Hz-660Hz acoustic sweet spot of smartphone speakers.
     */
    public static short[] buildAmbientBuffer() {
        final int n = SAMPLE_RATE * AMBIENT_SECONDS;
        final float[] out = new float[n];
        final float seconds = (float) n / SAMPLE_RATE;

        final float[] partials = {220.0f, 329.6f, 440.0f, 554.4f, 659.2f};
        final float[] gains = {0.85f, 0.65f, 0.50f, 0.35f, 0.20f};

        for (int p = 0; p < partials.length; p++) {
            float hz = snapToWholeCycles(partials[p], seconds);
            if (hz <= 0f) continue;
            double w = 2.0 * Math.PI * hz / SAMPLE_RATE;
            double phase = p * 1.1;
            for (int i = 0; i < n; i++) {
                out[i] += (float) (gains[p] * Math.sin(phase));
                phase += w;
            }
        }

        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float lfo = 1.0f
                    + 0.12f * (float) Math.sin(2 * Math.PI * 0.05f * t)
                    + 0.06f * (float) Math.sin(2 * Math.PI * 0.02f * t + 0.8f);
            out[i] *= lfo;
        }

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
        float norm = peak > 1e-6f ? 0.92f / peak : 0f;

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (float) Math.tanh(out[i] * norm * 1.20f);
            pcm[i] = (short) (v * 30000);
        }
        return pcm;
    }

    private static float snapToWholeCycles(float hz, float seconds) {
        if (hz <= 0f || seconds <= 0f) return 0f;
        long cycles = Math.round(hz * seconds);
        if (cycles < 1) cycles = 1;
        return cycles / seconds;
    }

    // ======================================================================
    // WAV FILE FORMAT CONVERSION
    // ======================================================================

    public static byte[] pcmToWav(short[] pcm, int sampleRate) {
        int channels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * (bitsPerSample / 8);
        int blockAlign = channels * (bitsPerSample / 8);
        int subChunk2Size = pcm.length * 2;
        int chunkSize = 36 + subChunk2Size;

        ByteBuffer buf = ByteBuffer.allocate(44 + subChunk2Size).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 'R').put((byte) 'I').put((byte) 'F').put((byte) 'F');
        buf.putInt(chunkSize);
        buf.put((byte) 'W').put((byte) 'A').put((byte) 'V').put((byte) 'E');
        buf.put((byte) 'f').put((byte) 'm').put((byte) 't').put((byte) ' ');
        buf.putInt(16);
        buf.putShort((short) 1);
        buf.putShort((short) channels);
        buf.putInt(sampleRate);
        buf.putInt(byteRate);
        buf.putShort((short) blockAlign);
        buf.putShort((short) bitsPerSample);
        buf.put((byte) 'd').put((byte) 'a').put((byte) 't').put((byte) 'a');
        buf.putInt(subChunk2Size);
        for (short s : pcm) {
            buf.putShort(s);
        }
        return buf.array();
    }

    // ======================================================================
    // FALLBACK AUDIOTRACK BUILDER (STREAM_MUSIC / USAGE_GAME)
    // ======================================================================

    private static AudioTrack buildStaticTrack(short[] pcm, int sampleRate) {
        if (pcm == null || pcm.length == 0) return null;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, encoding);
        int trackBytes = Math.max(minBuf, pcm.length * 2);

        short[] dataToWrite = pcm;
        if (trackBytes > pcm.length * 2) {
            int targetShorts = trackBytes / 2;
            dataToWrite = new short[targetShorts];
            System.arraycopy(pcm, 0, dataToWrite, 0, pcm.length);
        }

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
                @SuppressWarnings("deprecation")
                AudioTrack legacy = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, sampleRate,
                        channelConfig, encoding, trackBytes, AudioTrack.MODE_STATIC);
                t = legacy;
            }

            if (t.getState() != AudioTrack.STATE_INITIALIZED) {
                t.release();
                return null;
            }

            t.write(dataToWrite, 0, dataToWrite.length);
            return t;
        } catch (Throwable t2) {
            Log.w(TAG, "buildStaticTrack failed", t2);
            if (t != null) {
                try { t.release(); } catch (Throwable ignored) {}
            }
            return null;
        }
    }
}
