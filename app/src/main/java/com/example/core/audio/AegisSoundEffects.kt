package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.SoundPool
import android.os.SystemClock
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * Synthesizes a crisp, soft bubbly pitch-drop glassy pop for UI touches and
 * a richer dual-droplet crystalline resonance for activating protection modes.
 *
 * Uses SoundPool backed by STREAM_MUSIC / USAGE_MEDIA for zero-latency,
 * high-volume playback on all Android versions and emulators, with AudioTrack
 * and system click fallbacks.
 */
object AegisSoundEffects {

    private const val SAMPLE_RATE = 44100

    private val audioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SheildlenzAudioEngine").apply { isDaemon = true }
    }

    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var lastTapAtMs: Long = 0L

    @Volatile
    private var lastModeOnAtMs: Long = 0L

    @Volatile
    private var soundPool: SoundPool? = null

    @Volatile
    private var tapSoundId: Int = 0

    @Volatile
    private var modeOnSoundId: Int = 0

    @Volatile
    private var isWarmedUp: Boolean = false

    private val tapPcm: ShortArray by lazy { synthesizeWaterDropTap() }
    private val modeOnPcm: ShortArray by lazy { synthesizeModeActivatedVoice() }

    @Volatile
    private var appContext: Context? = null

    private val loadedSoundIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

    @Volatile
    private var lastDirectTrack: AudioTrack? = null

    @Volatile
    private var lastDirectTrackAtMs: Long = 0L

    fun setEnabled(isEnabled: Boolean) {
        enabled = isEnabled
    }

    fun isEnabled(): Boolean = enabled

    internal fun currentModeOnElapsedMs(): Long = lastModeOnAtMs

    /**
     * Initializes SoundPool and pre-loads synthesized WAV waveforms into memory
     * so touch feedback plays instantly with zero cold-start delay.
     */
    fun prewarm(context: Context? = null) {
        if (context != null) {
            appContext = context.applicationContext
        }
        if (isWarmedUp) return
        audioExecutor.execute {
            ensureInitialized(context)
        }
    }

    private fun ensureInitialized(context: Context?) {
        if (context != null) {
            appContext = context.applicationContext
        }
        val ctx = appContext ?: context?.applicationContext
        if (isWarmedUp && soundPool != null) return
        try {
            val pool = soundPool ?: SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build().also {
                    it.setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            loadedSoundIds.add(sampleId)
                        }
                    }
                    soundPool = it
                }

            val targetDir = ctx?.cacheDir ?: ctx?.filesDir
            if (targetDir != null) {
                val tapFile = File(targetDir, "sheildlenz_tap_pop_v3.wav")
                val modeFile = File(targetDir, "sheildlenz_mode_on_v3.wav")

                writePcmToWavFile(tapFile, tapPcm, SAMPLE_RATE)
                writePcmToWavFile(modeFile, modeOnPcm, SAMPLE_RATE)

                if (tapSoundId == 0) {
                    tapSoundId = pool.load(tapFile.absolutePath, 1)
                }
                if (modeOnSoundId == 0) {
                    modeOnSoundId = pool.load(modeFile.absolutePath, 1)
                }
                isWarmedUp = true
            }
        } catch (_: Throwable) {
            // Keep retryable if context was not yet ready
        }
    }

    /**
     * Plays a crisp ~48ms soft bubbly pitch-drop glassy pop immediately on press.
     */
    fun playTap(context: Context? = null) {
        if (!enabled) return
        if (context != null) {
            appContext = context.applicationContext
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastModeOnAtMs < 140L) return
        if (now - lastTapAtMs < 32L) return
        lastTapAtMs = now

        audioExecutor.execute {
            if (!enabled) return@execute
            dispatchTapSound(context)
        }
    }

    /**
     * Always plays a preview of the bubbly pop when user taps TEST SOUND.
     */
    fun playPreviewTap(context: Context? = null) {
        if (context != null) {
            appContext = context.applicationContext
        }
        lastTapAtMs = SystemClock.elapsedRealtime()
        audioExecutor.execute {
            dispatchTapSound(context)
        }
    }

    private fun dispatchTapSound(context: Context?) {
        ensureInitialized(context)

        val pool = soundPool
        val sid = tapSoundId
        var played = false
        if (pool != null && sid != 0 && loadedSoundIds.contains(sid)) {
            val streamId = pool.play(sid, 1.0f, 1.0f, 1, 0, 1.0f)
            played = (streamId != 0)
        }

        // Direct AudioTrack playback fallback if SoundPool stream wasn't ready
        if (!played) {
            playViaDirectAudioTrack(tapPcm)
        }

        // Also trigger system touch sound effect as companion
        try {
            val ctx = appContext ?: context?.applicationContext
            val audioManager = ctx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 1.0f)
        } catch (_: Throwable) {
        }
    }

    /**
     * Plays a fuller, crystalline dual-droplet chime when activating protection modes.
     */
    fun playModeOn(context: Context? = null) {
        if (!enabled) return
        if (context != null) {
            appContext = context.applicationContext
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastModeOnAtMs < 140L) return
        lastModeOnAtMs = now
        lastTapAtMs = now

        audioExecutor.execute {
            if (!enabled) return@execute
            ensureInitialized(context)

            val pool = soundPool
            val sid = modeOnSoundId
            var played = false
            if (pool != null && sid != 0 && loadedSoundIds.contains(sid)) {
                val streamId = pool.play(sid, 1.0f, 1.0f, 2, 0, 1.0f)
                played = (streamId != 0)
            }

            if (!played) {
                playViaDirectAudioTrack(modeOnPcm)
            }
        }
    }

    private fun playViaDirectAudioTrack(samples: ShortArray) {
        try {
            val now = SystemClock.elapsedRealtime()
            val prev = lastDirectTrack
            if (prev != null && now - lastDirectTrackAtMs > 240L) {
                lastDirectTrack = null
                runCatching {
                    prev.stop()
                    prev.release()
                }
            }

            val rawByteCount = samples.size * 2
            val minBufBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(0)
            val paddedSamples = if (minBufBytes > rawByteCount) {
                val targetSamples = (minBufBytes + 1) / 2
                ShortArray(targetSamples).also { padded ->
                    samples.copyInto(padded)
                }
            } else {
                samples
            }
            val byteCount = paddedSamples.size * 2

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteCount)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            if (track.state == AudioTrack.STATE_INITIALIZED) {
                track.write(paddedSamples, 0, paddedSamples.size)
                track.setVolume(1.0f)
                track.play()
                lastDirectTrack = track
                lastDirectTrackAtMs = now
            } else {
                runCatching { track.release() }
            }
        } catch (_: Throwable) {
        }
    }

    private fun writePcmToWavFile(file: File, pcm: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcm.size * 2
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * 2

        val header = ByteArray(44)
        // "RIFF" chunk
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        // "WAVE"
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // "fmt " subchunk
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // 16 bytes for PCM format
        header[20] = 1; header[21] = 0 // PCM = 1
        header[22] = 1; header[23] = 0 // 1 channel (Mono)
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2; header[33] = 0 // 2 bytes per sample block
        header[34] = 16; header[35] = 0 // 16 bits per sample
        // "data" subchunk
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        FileOutputStream(file).use { out ->
            out.write(header)
            val buffer = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) {
                buffer.putShort(s)
            }
            out.write(buffer.array())
        }
    }

    /**
     * ~18ms ultra-clean, glassy mechanical tick tap:
     * Crisp high-frequency metallic tap (2400Hz -> 800Hz) with instantaneous attack and sharp exponential decay.
     */
    internal fun synthesizeWaterDropTap(): ShortArray {
        val durationSec = 0.018
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(totalSamples)
        var phase = 0.0
        val attackSec = 0.0008

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = t / durationSec
            val freq = 2400.0 * 2.0.pow(-2.2 * progress)
            phase += 2.0 * PI * freq / SAMPLE_RATE

            val attackEnv = if (t < attackSec) {
                t / attackSec
            } else {
                1.0
            }
            val decayEnv = exp(-t / 0.0035)

            val wave = sin(phase) + 0.35 * sin(phase * 2.0)
            val amplitude = 0.28 * attackEnv * decayEnv
            out[i] = (wave * amplitude * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    /**
     * ~195ms fuller, higher-presence dual-droplet chime for activating protection modes.
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

            // Droplet 2
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
            val scaled = mixed * 0.58 * tailTaper
            out[i] = (scaled * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }
}
