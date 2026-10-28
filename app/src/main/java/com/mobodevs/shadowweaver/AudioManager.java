package com.mobodevs.shadowweaver;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

/**
 * Shadow Weaver - AudioManager
 * ===========================
 * Pure procedural audio engine synthesized mathematically in code.
 * Zero external audio assets, zero native dependencies, zero latency.
 *
 * Provides:
 *  - Soothing, warm alchemical candle drone with phone-speaker-optimized harmonics (220-660 Hz).
 *  - Tactile UI click for all buttons and tabs (dual-voice alternating).
 *  - Shape pickup (rising warm lift tone).
 *  - Shape drop (wooden desk thud).
 *  - Magnetic snap assist (radiant dual bell lock).
 *  - Angle detent tick (crisp micro-click).
 *  - Silhouette match chimes (Tibetan singing bowls at 33%, 66%, 100%).
 *  - Puzzle solved celebration fanfare (ascending celestial chord).
 *  - Android AudioFocus lifecycle management.
 */
public class AudioManager implements OnAudioFocusChangeListener {

    private static final String TAG = "ShadowWeaverAudio";

    // Audio format
    private static final int SAMPLE_RATE = 22050;
    private static final int AMBIENT_SECONDS = 6;
    private static final float CROSSFADE_SECONDS = 0.40f;

    // Audible, rich harmonic alchemical candle drone (optimized for phone speakers)
    private static final float AMBIENT_GAIN = 0.45f;
    private static float masterVolume = 0.90f;

    private static final int CHIME_RATE = 22050;
    private static final float CHIME_LENGTH = 1.6f;

    // Silhouette match chime marks
    public static final int CHIME_HALF = SilhouetteMatcher.CHIME_HALF;
    public static final int CHIME_GOOD = SilhouetteMatcher.CHIME_GOOD;
    public static final int CHIME_DONE = SilhouetteMatcher.CHIME_DONE;

    // State & synchronization
    private final Object lock = new Object();

    private AudioTrack ambientTrack;

    // Dual-voice tracks for rapid, non-blocking UI interactions
    private AudioTrack clickTrack0;
    private AudioTrack clickTrack1;
    private int clickVoiceIdx = 0;

    private AudioTrack tickTrack0;
    private AudioTrack tickTrack1;
    private int tickVoiceIdx = 0;

    private AudioTrack pickupTrack;
    private AudioTrack dropTrack;
    private AudioTrack snapTrack;

    private AudioTrack chimeHalfTrack;
    private AudioTrack chimeGoodTrack;
    private AudioTrack chimeDoneTrack;
    private AudioTrack celebrationTrack;

    private Thread prepareThread;
    private volatile boolean released;
    private volatile boolean muted;
    private volatile boolean started;
    private volatile boolean hasAudioFocus = true;

    // Audio focus tracking
    private android.media.AudioManager systemAudioManager;
    private AudioFocusRequest audioFocusRequest;

    // Cached PCM buffers (shared across instances, precomputed once)
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
    // LIFECYCLE
    // ======================================================================

    public void initSystemAudio(Context context) {
        if (context != null) {
            try {
                systemAudioManager = (android.media.AudioManager) context.getApplicationContext()
                        .getSystemService(Context.AUDIO_SERVICE);
            } catch (Throwable t) {
                Log.w(TAG, "Failed to get system AudioManager", t);
            }
        }
    }

    public void start() {
        if (released || started) return;
        started = true;

        requestAudioFocus();

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
        prepareThread.setPriority(Thread.NORM_PRIORITY - 1);
        prepareThread.start();
    }

    private void initAllAudioTracks() {
        // 1. Synthesize all mathematical sound buffers
        if (clickBuffer == null) clickBuffer = buildClickBuffer();
        if (pickupBuffer == null) pickupBuffer = buildPickupBuffer();
        if (dropBuffer == null) dropBuffer = buildDropBuffer();
        if (tickBuffer == null) tickBuffer = buildTickBuffer();
        if (snapBuffer == null) snapBuffer = buildSnapBuffer();
        if (chimeHalfBuffer == null) chimeHalfBuffer = buildChimeBuffer(392.00f, 0.70f); // G4
        if (chimeGoodBuffer == null) chimeGoodBuffer = buildChimeBuffer(523.25f, 0.78f); // C5
        if (chimeDoneBuffer == null) chimeDoneBuffer = buildChimeBuffer(659.25f, 0.85f); // E5
        if (celebrationBuffer == null) celebrationBuffer = buildCelebrationBuffer();
        if (ambientBuffer == null) ambientBuffer = buildAmbientBuffer();

        if (released) return;

        synchronized (lock) {
            if (released) return;

            // 2. Build AudioTracks with exact buffer sizes (MODE_STATIC)
            if (clickTrack0 == null) clickTrack0 = buildStaticTrack(clickBuffer, CHIME_RATE);
            if (clickTrack1 == null) clickTrack1 = buildStaticTrack(clickBuffer, CHIME_RATE);

            if (tickTrack0 == null) tickTrack0 = buildStaticTrack(tickBuffer, CHIME_RATE);
            if (tickTrack1 == null) tickTrack1 = buildStaticTrack(tickBuffer, CHIME_RATE);

            if (pickupTrack == null) pickupTrack = buildStaticTrack(pickupBuffer, CHIME_RATE);
            if (dropTrack == null) dropTrack = buildStaticTrack(dropBuffer, CHIME_RATE);
            if (snapTrack == null) snapTrack = buildStaticTrack(snapBuffer, CHIME_RATE);

            if (chimeHalfTrack == null) chimeHalfTrack = buildStaticTrack(chimeHalfBuffer, CHIME_RATE);
            if (chimeGoodTrack == null) chimeGoodTrack = buildStaticTrack(chimeGoodBuffer, CHIME_RATE);
            if (chimeDoneTrack == null) chimeDoneTrack = buildStaticTrack(chimeDoneBuffer, CHIME_RATE);
            if (celebrationTrack == null) celebrationTrack = buildStaticTrack(celebrationBuffer, CHIME_RATE);

            // 3. Build and launch Ambient Looping Track
            if (ambientTrack == null) {
                ambientTrack = buildLoopingTrack(ambientBuffer, SAMPLE_RATE);
                applyVolume();
            }
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
        abandonAudioFocus();
    }

    public void resume() {
        requestAudioFocus();
        synchronized (lock) {
            if (released) return;
            if (!started) {
                start();
                return;
            }
            if (ambientTrack != null) {
                try {
                    if (!muted && hasAudioFocus) {
                        float v = masterVolume * AMBIENT_GAIN;
                        setTrackVolume(ambientTrack, v);
                        if (ambientTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                            ambientTrack.play();
                        }
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Error resuming ambient track", t);
                }
            }
        }
    }

    public void release() {
        released = true;
        started = false;
        abandonAudioFocus();
        synchronized (lock) {
            safeReleaseTrack(ambientTrack); ambientTrack = null;
            safeReleaseTrack(clickTrack0); clickTrack0 = null;
            safeReleaseTrack(clickTrack1); clickTrack1 = null;
            safeReleaseTrack(tickTrack0); tickTrack0 = null;
            safeReleaseTrack(tickTrack1); tickTrack1 = null;
            safeReleaseTrack(pickupTrack); pickupTrack = null;
            safeReleaseTrack(dropTrack); dropTrack = null;
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
    // AUDIO FOCUS MANAGEMENT
    // ======================================================================

    private void requestAudioFocus() {
        if (systemAudioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest == null) {
                    AudioAttributes attrs = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
                    audioFocusRequest = new AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(attrs)
                            .setOnAudioFocusChangeListener(this)
                            .build();
                }
                int res = systemAudioManager.requestAudioFocus(audioFocusRequest);
                hasAudioFocus = (res == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
            } else {
                @SuppressWarnings("deprecation")
                int res = systemAudioManager.requestAudioFocus(this,
                        android.media.AudioManager.STREAM_MUSIC,
                        android.media.AudioManager.AUDIOFOCUS_GAIN);
                hasAudioFocus = (res == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
            }
        } catch (Throwable t) {
            Log.w(TAG, "requestAudioFocus failed", t);
            hasAudioFocus = true; // Fallback to allowing audio
        }
    }

    @SuppressWarnings("deprecation")
    private void abandonAudioFocus() {
        if (systemAudioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                systemAudioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                systemAudioManager.abandonAudioFocus(this);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case android.media.AudioManager.AUDIOFOCUS_LOSS:
            case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                hasAudioFocus = false;
                pause();
                break;
            case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lower ambient volume
                synchronized (lock) {
                    if (ambientTrack != null && !muted) {
                        setTrackVolume(ambientTrack, masterVolume * AMBIENT_GAIN * 0.3f);
                    }
                }
                break;
            case android.media.AudioManager.AUDIOFOCUS_GAIN:
                hasAudioFocus = true;
                resume();
                break;
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
            float v = (muted || !hasAudioFocus) ? 0f : masterVolume * AMBIENT_GAIN;
            setTrackVolume(ambientTrack, v);
            if (!muted && hasAudioFocus && ambientTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
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
    // PLAYBACK METHODS (BULLETPROOF STATIC REPLAY WITH HEAD REWIND)
    // ======================================================================

    private void playTrack(AudioTrack track, float gain) {
        if (released || muted || track == null || !hasAudioFocus) return;
        try {
            // Android AudioTrack MODE_STATIC replay lifecycle
            if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause();
                track.flush();
            }
            track.setPlaybackHeadPosition(0);
            track.reloadStaticData();
            float v = Math.min(1.0f, masterVolume * gain);
            setTrackVolume(track, v);
            track.play();
        } catch (Throwable t) {
            Log.w(TAG, "playTrack error", t);
        }
    }

    /**
     * Tactile UI button click (dual-voice alternating for rapid tapping).
     */
    public void playClick() {
        if (released || muted) return;
        synchronized (lock) {
            AudioTrack target = (clickVoiceIdx == 0) ? clickTrack0 : clickTrack1;
            clickVoiceIdx = 1 - clickVoiceIdx;
            if (target == null) {
                if (clickBuffer == null) clickBuffer = buildClickBuffer();
                target = buildStaticTrack(clickBuffer, CHIME_RATE);
                if (clickVoiceIdx == 1) clickTrack0 = target;
                else clickTrack1 = target;
            }
            playTrack(target, 0.75f);
        }
    }

    /**
     * Shape pickup / touch down (warm upward lift sweep).
     */
    public void playPickup() {
        if (released || muted) return;
        synchronized (lock) {
            if (pickupTrack == null) {
                if (pickupBuffer == null) pickupBuffer = buildPickupBuffer();
                pickupTrack = buildStaticTrack(pickupBuffer, CHIME_RATE);
            }
            playTrack(pickupTrack, 0.65f);
        }
    }

    /**
     * Shape drop / touch release without snap (subtle wooden table thud).
     */
    public void playDrop() {
        if (released || muted) return;
        synchronized (lock) {
            if (dropTrack == null) {
                if (dropBuffer == null) dropBuffer = buildDropBuffer();
                dropTrack = buildStaticTrack(dropBuffer, CHIME_RATE);
            }
            playTrack(dropTrack, 0.60f);
        }
    }

    /**
     * Rotation detent tick (dual-voice alternating).
     */
    public void playTick() {
        if (released || muted) return;
        synchronized (lock) {
            AudioTrack target = (tickVoiceIdx == 0) ? tickTrack0 : tickTrack1;
            tickVoiceIdx = 1 - tickVoiceIdx;
            if (target == null) {
                if (tickBuffer == null) tickBuffer = buildTickBuffer();
                target = buildStaticTrack(tickBuffer, CHIME_RATE);
                if (tickVoiceIdx == 1) tickTrack0 = target;
                else tickTrack1 = target;
            }
            playTrack(target, 0.60f);
        }
    }

    /**
     * Magnetic snap assist (resonant dual-bell celestial lock).
     */
    public void playSnap() {
        if (released || muted) return;
        synchronized (lock) {
            if (snapTrack == null) {
                if (snapBuffer == null) snapBuffer = buildSnapBuffer();
                snapTrack = buildStaticTrack(snapBuffer, CHIME_RATE);
            }
            playTrack(snapTrack, 0.85f);
        }
    }

    /**
     * Silhouette progress chimes (Tibetan singing bowl harmonics).
     */
    public void playChime(int which) {
        if (released || muted) return;
        synchronized (lock) {
            switch (which) {
                case CHIME_HALF:
                    if (chimeHalfTrack == null) {
                        if (chimeHalfBuffer == null) chimeHalfBuffer = buildChimeBuffer(392.00f, 0.70f);
                        chimeHalfTrack = buildStaticTrack(chimeHalfBuffer, CHIME_RATE);
                    }
                    playTrack(chimeHalfTrack, 0.75f);
                    break;
                case CHIME_GOOD:
                    if (chimeGoodTrack == null) {
                        if (chimeGoodBuffer == null) chimeGoodBuffer = buildChimeBuffer(523.25f, 0.78f);
                        chimeGoodTrack = buildStaticTrack(chimeGoodBuffer, CHIME_RATE);
                    }
                    playTrack(chimeGoodTrack, 0.82f);
                    break;
                case CHIME_DONE:
                    if (chimeDoneTrack == null) {
                        if (chimeDoneBuffer == null) chimeDoneBuffer = buildChimeBuffer(659.25f, 0.85f);
                        chimeDoneTrack = buildStaticTrack(chimeDoneBuffer, CHIME_RATE);
                    }
                    playTrack(chimeDoneTrack, 0.90f);
                    break;
            }
        }
    }

    public void playChimes(int bitmask) {
        if ((bitmask & (1 << CHIME_HALF)) != 0) playChime(CHIME_HALF);
        if ((bitmask & (1 << CHIME_GOOD)) != 0) playChime(CHIME_GOOD);
        if ((bitmask & (1 << CHIME_DONE)) != 0) playChime(CHIME_DONE);
    }

    /**
     * Victory celebration fanfare (ascending celestial chord).
     */
    public void playCelebration() {
        if (released || muted) return;
        synchronized (lock) {
            if (celebrationTrack == null) {
                if (celebrationBuffer == null) celebrationBuffer = buildCelebrationBuffer();
                celebrationTrack = buildStaticTrack(celebrationBuffer, CHIME_RATE);
            }
            playTrack(celebrationTrack, 0.95f);
        }
    }

    // ======================================================================
    // TRACK BUILDERS (EXACT BUFFER SIZING & OPTIMAL AUDIO ATTRIBUTES)
    // ======================================================================

    private static AudioTrack buildLoopingTrack(short[] pcm, int sampleRate) {
        if (pcm == null || pcm.length == 0) return null;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int trackBytes = pcm.length * 2; // EXACT size in bytes for MODE_STATIC

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

            t.write(pcm, 0, pcm.length);
            t.setPlaybackHeadPosition(0);
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
        int trackBytes = pcm.length * 2; // EXACT size in bytes for MODE_STATIC

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
                @SuppressWarnings("deprecation")
                AudioTrack legacy = new AudioTrack(android.media.AudioManager.STREAM_MUSIC, sampleRate,
                        channelConfig, encoding, trackBytes, AudioTrack.MODE_STATIC);
                t = legacy;
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
    // PROCEDURAL SYNTHESIS (PURE MATHEMATICAL FM / HARMONIC SYNTHESIS)
    // ======================================================================

    /**
     * Tactile UI click: Crisp 25ms wooden/crystal tap (1350 Hz with sharp exponential decay).
     */
    private static short[] buildClickBuffer() {
        final float duration = 0.025f;
        final int n = (int) (duration * CHIME_RATE);
        final short[] pcm = new short[n];
        double phase = 0;
        double w = 2.0 * Math.PI * 1350.0 / CHIME_RATE;
        for (int i = 0; i < n; i++) {
            float t = i / (float) CHIME_RATE;
            float env = (float) Math.exp(-t * 220f);
            float s = (float) (Math.sin(phase) * env);
            phase += w;
            pcm[i] = (short) (s * Short.MAX_VALUE * 0.85f);
        }
        return pcm;
    }

    /**
     * Shape pickup: Warm upward wooden sweep (420 Hz -> 620 Hz over 60ms).
     */
    private static short[] buildPickupBuffer() {
        final float duration = 0.060f;
        final int n = (int) (duration * CHIME_RATE);
        final short[] pcm = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) CHIME_RATE;
            float hz = 420f + 200f * (t / duration);
            double w = 2.0 * Math.PI * hz / CHIME_RATE;
            float env = (float) Math.sin(Math.PI * (t / duration)); // Smooth bell envelope
            float s = (float) (Math.sin(phase) * env);
            phase += w;
            pcm[i] = (short) (s * Short.MAX_VALUE * 0.75f);
        }
        return pcm;
    }

    /**
     * Shape drop: Resonant wooden desk tap (260 Hz -> 180 Hz over 75ms).
     */
    private static short[] buildDropBuffer() {
        final float duration = 0.075f;
        final int n = (int) (duration * CHIME_RATE);
        final short[] pcm = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            float t = i / (float) CHIME_RATE;
            float hz = 260f - 80f * (t / duration);
            double w = 2.0 * Math.PI * hz / CHIME_RATE;
            float env = (float) Math.exp(-t * 48f);
            float s = (float) (Math.sin(phase) * env);
            phase += w;
            pcm[i] = (short) (s * Short.MAX_VALUE * 0.70f);
        }
        return pcm;
    }

    /**
     * Rotation detent tick: High crisp 12ms micro-click (1600 Hz).
     */
    private static short[] buildTickBuffer() {
        final float duration = 0.016f;
        final int n = (int) (duration * CHIME_RATE);
        final short[] pcm = new short[n];
        double phase = 0;
        double w = 2.0 * Math.PI * 1600.0 / CHIME_RATE;
        for (int i = 0; i < n; i++) {
            float t = i / (float) CHIME_RATE;
            float env = (float) Math.exp(-t * 320f);
            float s = (float) (Math.sin(phase) * env);
            phase += w;
            pcm[i] = (short) (s * Short.MAX_VALUE * 0.70f);
        }
        return pcm;
    }

    /**
     * Magnetic snap assist: Resonant dual-bell celestial lock (587.33 Hz D5 + 880.0 Hz A5, 180ms).
     */
    private static short[] buildSnapBuffer() {
        final float duration = 0.18f;
        final int n = (int) (duration * CHIME_RATE);
        final short[] pcm = new short[n];
        double phaseA = 0;
        double phaseB = 0;
        double wA = 2.0 * Math.PI * 587.33 / CHIME_RATE;
        double wB = 2.0 * Math.PI * 880.00 / CHIME_RATE;
        for (int i = 0; i < n; i++) {
            float t = i / (float) CHIME_RATE;
            float envA = (float) Math.exp(-t * 24f);
            float envB = (float) Math.exp(-t * 32f);
            float sA = (float) (Math.sin(phaseA) * envA * 0.65f);
            float sB = (float) (Math.sin(phaseB) * envB * 0.35f);
            phaseA += wA;
            phaseB += wB;
            float combined = (float) Math.tanh(sA + sB);
            pcm[i] = (short) (combined * Short.MAX_VALUE * 0.85f);
        }
        return pcm;
    }

    /**
     * Silhouette match chimes: Tibetan singing bowl harmonic chime.
     */
    private static short[] buildChimeBuffer(float rootHz, float gain) {
        final int n = (int) (CHIME_LENGTH * CHIME_RATE);
        final float[] out = new float[n];

        final float[] ratios = {1.0f, 1.5f, 2.0f, 3.0f};
        final float[] amps = {1.00f, 0.45f, 0.30f, 0.12f};
        final float[] decays = {2.4f, 3.2f, 4.0f, 5.8f};

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
            float v = (float) Math.tanh(out[i] * gain * 1.25f);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }

    /**
     * Victory celebration: 4-note ascending celestial fanfare (C5, E5, G5, C6) over 2.4s.
     */
    private static short[] buildCelebrationBuffer() {
        final float duration = 2.4f;
        final int n = (int) (duration * CHIME_RATE);
        final float[] out = new float[n];

        float[] notes = {523.25f, 659.25f, 783.99f, 1046.50f}; // C5, E5, G5, C6
        float[] startTimes = {0.0f, 0.18f, 0.36f, 0.54f};
        float[] ratios = {1.0f, 2.0f, 3.0f};
        float[] amps = {1.0f, 0.40f, 0.18f};

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
                    float env = (float) Math.exp(-t * 1.6f);
                    float attack = Math.min(1f, t / 0.008f);
                    out[i] += (float) (harmonicAmp * env * attack * Math.sin(phase) * 0.38f);
                    phase += w;
                }
            }
        }

        short[] pcm = new short[n];
        for (int i = 0; i < n; i++) {
            float v = (float) Math.tanh(out[i] * 1.20f);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }

    /**
     * Ambient candle drone: Warm, meditative harmonic sanctuary.
     * Tuned specifically to 220Hz-660Hz acoustic range so phone speakers play it cleanly and warmly!
     */
    private static short[] buildAmbientBuffer() {
        final int n = SAMPLE_RATE * AMBIENT_SECONDS;
        final float[] out = new float[n];
        final float seconds = (float) n / SAMPLE_RATE;

        // Frequencies in the audible sweet spot of smartphone speakers (A3, E4, A4, C#5, E5)
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

        // Slow soothing thermal chorus modulation
        for (int i = 0; i < n; i++) {
            float t = i / (float) SAMPLE_RATE;
            float lfo = 1.0f
                    + 0.12f * (float) Math.sin(2 * Math.PI * 0.05f * t)
                    + 0.06f * (float) Math.sin(2 * Math.PI * 0.02f * t + 0.8f);
            out[i] *= lfo;
        }

        // Delicate candle crackle and hearth sparks
        addCrackle(out, n, seconds);

        // Seamless loop crossfade
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
            float v = (float) Math.tanh(out[i] * norm * 1.15f);
            pcm[i] = (short) (v * Short.MAX_VALUE);
        }
        return pcm;
    }

    private static void addCrackle(float[] out, int n, float seconds) {
        final int bursts = (int) (seconds * 12f);
        final int maxLen = (int) (0.008f * SAMPLE_RATE);
        long seed = 0x5DEECE66DL;

        for (int b = 0; b < bursts; b++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            int start = (int) Math.abs(seed % n);

            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            int len = 1 + (int) Math.abs(seed % maxLen);

            seed = (seed * 6364136223846793005L + 1442695040888963407L);
            float amp = 0.08f + 0.20f * (Math.abs(seed % 1000L) / 1000f);

            float decay = 1f;
            float prev = 0f;
            for (int i = 0; i < len; i++) {
                int idx = start + i;
                if (idx >= n) break;
                decay *= 0.85f;

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
}
