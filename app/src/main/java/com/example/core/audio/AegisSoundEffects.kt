package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Synthesizes a minimal, soft bubbly pitch-drop glassy pop for UI touches and
 * a fuller crystalline dual-droplet chime when activating protection modes,
 * backed by pre-warmed static AudioTrack instances for zero-latency playback.
 */
object AegisSoundEffects {

    private const val SAMPLE_RATE = 44100
    private const val TAP_POOL_SIZE = 3

    private val audioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SheildlenzAudioFx").apply { isDaemon = true }
    }

    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var lastTapAtMs: Long = 0L

    @Volatile
    private var lastModeOnAtMs: Long = 0L

    @Volatile
    private var prewarmScheduled: Boolean = false

    private val tapPcm: ShortArray by lazy { synthesizeWaterDropTap() }
    private val modeOnPcm: ShortArray by lazy { synthesizeModeActivatedVoice() }

    private val tapTracks = arrayOfNulls<AudioTrack>(TAP_POOL_SIZE)
    private var tapCursor = 0
    private var modeOnTrack: AudioTrack? = null

    fun setEnabled(isEnabled: Boolean) {
        enabled = isEnabled
    }

    fun isEnabled(): Boolean = enabled

    internal fun currentModeOnElapsedMs(): Long = lastModeOnAtMs

    /**
     * Synthesizes waveforms and pre-populates static AudioTrack buffers asynchronously
     * so first touch feedback triggers immediately with zero cold-init latency.
     */
    fun prewarm() {
        if (prewarmScheduled) return
        prewarmScheduled = true
        audioExecutor.execute {
            ensureWarmedOnAudioThread()
        }
    }

    /**
     * Plays a crisp ~40ms soft bubbly pitch-drop glassy pop immediately on press.
     */
    fun playTap(@Suppress("UNUSED_PARAMETER") context: Context? = null) {
        if (!enabled) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastModeOnAtMs < 140L) return
        if (now - lastTapAtMs < 38L) return
        lastTapAtMs = now
        audioExecutor.execute {
            if (!enabled) return@execute
            ensureWarmedOnAudioThread()
            val slot = tapCursor
            tapCursor = (tapCursor + 1) % TAP_POOL_SIZE
            var track = tapTracks[slot]
            if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                runCatching { track?.release() }
                track = buildStaticTrack(tapPcm)
                tapTracks[slot] = track
            }
            playStaticTrack(track, tapPcm)
        }
    }

    /**
     * Plays a fuller, richer dual-droplet crystalline resonance ("more voice")
     * whenever the user switches ON a protection mode (Shield, Safe DNS, Bedtime, Strict, etc.).
     */
    fun playModeOn(@Suppress("UNUSED_PARAMETER") context: Context? = null) {
        if (!enabled) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastModeOnAtMs < 140L) return
        lastModeOnAtMs = now
        lastTapAtMs = now
        audioExecutor.execute {
            if (!enabled) return@execute
            ensureWarmedOnAudioThread()
            var track = modeOnTrack
            if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                runCatching { track?.release() }
                track = buildStaticTrack(modeOnPcm)
                modeOnTrack = track
            }
            playStaticTrack(track, modeOnPcm)
        }
    }

    private fun ensureWarmedOnAudioThread() {
        val tap = tapPcm
        val mode = modeOnPcm
        for (i in 0 until TAP_POOL_SIZE) {
            val existing = tapTracks[i]
            if (existing == null || existing.state != AudioTrack.STATE_INITIALIZED) {
                tapTracks[i] = buildStaticTrack(tap)
            }
        }
        if (modeOnTrack == null || modeOnTrack?.state != AudioTrack.STATE_INITIALIZED) {
            modeOnTrack = buildStaticTrack(mode)
        }
    }

    private fun buildStaticTrack(samples: ShortArray): AudioTrack? {
        return runCatching {
            val byteCount = samples.size * 2
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            val track = AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(byteCount)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.write(samples, 0, samples.size)
                track
            } else {
                track.release()
                null
            }
        }.getOrNull()
    }

    private fun playStaticTrack(track: AudioTrack?, samples: ShortArray) {
        if (track == null || track.state != AudioTrack.STATE_INITIALIZED) return
        runCatching {
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) {
                track.stop()
            }
            val reloaded = track.reloadStaticData()
            if (reloaded != AudioTrack.SUCCESS) {
                track.write(samples, 0, samples.size)
            }
            track.play()
        }
    }

    /**
     * ~40ms soft bubbly pitch-drop glassy pop:
     * Smooth raised-cosine attack (2.8ms -> zero harsh transient click),
     * rapid downward sine pitch sweep (1040Hz -> ~380Hz) + delicate crystal octave harmonic
     * with fast exponential fade-out and raised-cosine tail taper.
     */
    internal fun synthesizeWaterDropTap(): ShortArray {
        val durationSec = 0.040
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(totalSamples)
        var phase = 0.0
        var harmPhase = 0.0
        val attackSec = 0.0028

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            // Smooth rapid downward pitch drop (1040Hz -> ~380Hz) for bubbly pop feel
            val freq = 1040.0 * 2.0.pow(-1.45 * progress)
            phase += 2.0 * PI * freq / SAMPLE_RATE
            harmPhase += 2.0 * PI * (freq * 2.0) / SAMPLE_RATE

            val attackEnv = if (t < attackSec) {
                0.5 * (1.0 - cos(PI * t / attackSec))
            } else {
                1.0
            }
            val decayEnv = exp(-(t - attackSec).coerceAtLeast(0.0) / 0.0095)
            val endTaper = if (progress > 0.82) {
                0.5 * (1.0 + cos(PI * (progress - 0.82) / 0.18))
            } else {
                1.0
            }

            val wave = sin(phase) + 0.14 * sin(harmPhase)
            val amplitude = 0.17 * attackEnv * decayEnv * endTaper
            out[i] = (wave * amplitude * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    /**
     * ~195ms fuller, higher-presence dual-droplet chime for activating protection modes:
     * Layer 1 (0ms): Warm resonant droplet (480Hz -> 960Hz + 1.5x fifth & 2x octave harmonics).
     * Layer 2 (48ms): Uplifting crystal droplet (720Hz -> 1440Hz + 2x octave chime).
     */
    internal fun synthesizeModeActivatedVoice(): ShortArray {
        val durationSec = 0.195
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(totalSamples)

        var p1 = 0.0
        var p1Fifth = 0.0
        var p1Oct = 0.0
        var p2 = 0.0
        var p2Oct = 0.0

        val attackSec = 0.0045
        val secondDropStart = 0.048

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var mixed = 0.0

            // Droplet 1
            val progress1 = (t / 0.11).coerceIn(0.0, 1.0)
            val f1 = 480.0 * 2.0.pow(0.95 * progress1)
            p1 += 2.0 * PI * f1 / SAMPLE_RATE
            p1Fifth += 2.0 * PI * (f1 * 1.5) / SAMPLE_RATE
            p1Oct += 2.0 * PI * (f1 * 2.0) / SAMPLE_RATE

            val atk1 = if (t < attackSec) 0.5 * (1.0 - cos(PI * t / attackSec)) else 1.0
            val dec1 = exp(-t / 0.036)
            mixed += (sin(p1) + 0.30 * sin(p1Fifth) + 0.22 * sin(p1Oct)) * atk1 * dec1

            // Droplet 2 (uplifting bright confirmation drop)
            if (t >= secondDropStart) {
                val t2 = t - secondDropStart
                val progress2 = (t2 / 0.14).coerceIn(0.0, 1.0)
                val f2 = 720.0 * 2.0.pow(0.98 * progress2)
                p2 += 2.0 * PI * f2 / SAMPLE_RATE
                p2Oct += 2.0 * PI * (f2 * 2.0) / SAMPLE_RATE

                val atk2 = if (t2 < attackSec) 0.5 * (1.0 - cos(PI * t2 / attackSec)) else 1.0
                val dec2 = exp(-t2 / 0.045)
                mixed += 0.95 * (sin(p2) + 0.28 * sin(p2Oct)) * atk2 * dec2
            }

            val tailTaper = if (t > 0.17) (durationSec - t).coerceAtLeast(0.0) / 0.025 else 1.0
            val scaled = mixed * 0.36 * tailTaper
            out[i] = (scaled * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }
}
