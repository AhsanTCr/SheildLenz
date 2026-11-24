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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Tactile audio feedback engine for Sheildlenz.
 *
 * Synthesizes crisp, short, comforting ceramic/stone tile ticks with steady
 * modal resonances (zero pitch-drop bubble sweeps) plus an optional Auto Sound
 * Selector palette of 8 friendly micro-tactile tones so every touch feels
 * fresh, cool, and soothing.
 */
object AegisSoundEffects {

    private const val SAMPLE_RATE = 44100

    internal data class TileToneSpec(
        val name: String,
        val primaryHz: Double,
        val secondaryHz: Double,
        val tertiaryHz: Double,
        val durationSec: Double,
        val decaySec: Double,
        val amplitude: Double
    )

    internal val AUTO_TILE_PALETTE: List<TileToneSpec> = listOf(
        TileToneSpec("Porcelain Tile", 1320.0, 2640.0, 3960.0, 0.036, 0.0050, 0.24),
        TileToneSpec("Smooth Bamboo", 940.0, 1880.0, 2820.0, 0.038, 0.0056, 0.25),
        TileToneSpec("Crystal Pebble", 1660.0, 3320.0, 4150.0, 0.035, 0.0046, 0.22),
        TileToneSpec("Velvet Slate", 1120.0, 2240.0, 3360.0, 0.035, 0.0048, 0.24),
        TileToneSpec("Soft Marimba", 1046.5, 2093.0, 3139.5, 0.040, 0.0060, 0.24),
        TileToneSpec("Glazed Ceramic", 1480.0, 2960.0, 4440.0, 0.035, 0.0047, 0.23),
        TileToneSpec("Nordic Cedar", 840.0, 1680.0, 2520.0, 0.039, 0.0058, 0.25),
        TileToneSpec("Moonstone Tick", 1396.9, 2093.0, 2793.8, 0.041, 0.0062, 0.23)
    )

    private val DEFAULT_TILE_SPEC = AUTO_TILE_PALETTE[0]

    private val audioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SheildlenzAudioEngine").apply { isDaemon = true }
    }

    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var autoSelectorEnabled: Boolean = false

    private val autoSelectorCursor = AtomicInteger(0)

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

    private val autoPaletteSoundIds = IntArray(AUTO_TILE_PALETTE.size)

    @Volatile
    private var isWarmedUp: Boolean = false

    private val tapPcm: ShortArray by lazy { synthesizeTileTick(DEFAULT_TILE_SPEC) }
    private val autoPalettePcm: List<ShortArray> by lazy {
        AUTO_TILE_PALETTE.map { synthesizeTileTick(it) }
    }
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

    fun setAutoSelectorEnabled(isEnabled: Boolean) {
        autoSelectorEnabled = isEnabled
    }

    fun isAutoSelectorEnabled(): Boolean = autoSelectorEnabled

    internal fun nextAutoVariationIndex(): Int {
        // Step by 3 mod 8 (coprime to 8) so every touch visits a fresh non-adjacent pitch
        val idx = autoSelectorCursor.getAndUpdate { (it + 3) % AUTO_TILE_PALETTE.size }
        return idx % AUTO_TILE_PALETTE.size
    }

    internal fun currentModeOnElapsedMs(): Long = lastModeOnAtMs

    /**
     * Initializes SoundPool and pre-loads synthesized tile tick WAV waveforms
     * into memory so touch feedback plays with zero cold-start delay.
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
                .setMaxStreams(10)
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
                val tapFile = File(targetDir, "sheildlenz_tile_tick_v4.wav")
                val modeFile = File(targetDir, "sheildlenz_mode_tile_v4.wav")

                writePcmToWavFile(tapFile, tapPcm, SAMPLE_RATE)
                writePcmToWavFile(modeFile, modeOnPcm, SAMPLE_RATE)

                if (tapSoundId == 0) {
                    tapSoundId = pool.load(tapFile.absolutePath, 1)
                }
                if (modeOnSoundId == 0) {
                    modeOnSoundId = pool.load(modeFile.absolutePath, 1)
                }

                autoPalettePcm.forEachIndexed { idx, pcm ->
                    if (autoPaletteSoundIds[idx] == 0) {
                        val file = File(targetDir, "sheildlenz_auto_tile_${idx}_v4.wav")
                        writePcmToWavFile(file, pcm, SAMPLE_RATE)
                        autoPaletteSoundIds[idx] = pool.load(file.absolutePath, 1)
                    }
                }
                isWarmedUp = true
            }
        } catch (_: Throwable) {
            // Keep retryable if context was not yet ready
        }
    }

    /**
     * Plays a crisp, comforting tile tick (or varied friendly tile tone when Auto
     * Sound Selector is enabled) immediately on press.
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

        val variationIdx = if (autoSelectorEnabled) nextAutoVariationIndex() else -1
        audioExecutor.execute {
            if (!enabled) return@execute
            dispatchTapSound(context, variationIdx)
        }
    }

    /**
     * Always plays a preview of the tile tick (or next auto variation if enabled)
     * when the user taps TEST SOUND.
     */
    fun playPreviewTap(context: Context? = null) {
        if (context != null) {
            appContext = context.applicationContext
        }
        lastTapAtMs = SystemClock.elapsedRealtime()
        val variationIdx = if (autoSelectorEnabled) nextAutoVariationIndex() else -1
        audioExecutor.execute {
            dispatchTapSound(context, variationIdx)
        }
    }

    private fun dispatchTapSound(context: Context?, variationIdx: Int) {
        ensureInitialized(context)

        val pool = soundPool
        val targetSoundId = if (variationIdx in autoPaletteSoundIds.indices && autoPaletteSoundIds[variationIdx] != 0) {
            autoPaletteSoundIds[variationIdx]
        } else {
            tapSoundId
        }
        val targetPcm = if (variationIdx in autoPalettePcm.indices) {
            autoPalettePcm[variationIdx]
        } else {
            tapPcm
        }

        var played = false
        if (pool != null && targetSoundId != 0 && loadedSoundIds.contains(targetSoundId)) {
            val streamId = pool.play(targetSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
            played = (streamId != 0)
        }

        // Direct AudioTrack playback fallback if SoundPool stream wasn't ready
        if (!played) {
            played = playViaDirectAudioTrack(targetPcm)
        }

        // Fallback to soft system click only if both synthesized tracks failed
        if (!played) {
            try {
                val ctx = appContext ?: context?.applicationContext
                val audioManager = ctx?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.7f)
            } catch (_: Throwable) {
            }
        }
    }

    /**
     * Plays a soothing two-note ceramic/crystal double-tick chime when activating
     * protection modes.
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

    private fun playViaDirectAudioTrack(samples: ShortArray): Boolean {
        return try {
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
                true
            } else {
                runCatching { track.release() }
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun writePcmToWavFile(file: File, pcm: ShortArray, sampleRate: Int) {
        val totalAudioLen = pcm.size * 2
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * 2

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
        header[20] = 1; header[21] = 0
        header[22] = 1; header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = 2; header[33] = 0
        header[34] = 16; header[35] = 0
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
     * Synthesizes a clean, soothing ~36ms ceramic/stone tile tick with steady
     * modal resonances (no pitch drop / zero bubble bloop).
     */
    internal fun synthesizeTileTick(spec: TileToneSpec = DEFAULT_TILE_SPEC): ShortArray {
        val totalSamples = (SAMPLE_RATE * spec.durationSec).toInt()
        val out = ShortArray(totalSamples)
        val attackSec = 0.0012
        val tailStartSec = spec.durationSec - 0.004

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val attackEnv = if (t < attackSec) {
                0.5 * (1.0 - cos(PI * t / attackSec))
            } else {
                1.0
            }
            val bodyEnv = exp(-t / spec.decaySec)
            val sheenEnv = exp(-t / (spec.decaySec * 0.45))
            val tailTaper = if (t > tailStartSec) {
                ((spec.durationSec - t) / 0.004).coerceIn(0.0, 1.0)
            } else {
                1.0
            }

            val body = sin(2.0 * PI * spec.primaryHz * t) * bodyEnv
            val overtone = 0.40 * sin(2.0 * PI * spec.secondaryHz * t) * sheenEnv
            val transient = 0.16 * sin(2.0 * PI * spec.tertiaryHz * t) * exp(-t / 0.0016)
            val mixed = (body + overtone + transient) * attackEnv * tailTaper * spec.amplitude

            out[i] = (mixed * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }

    internal fun synthesizeWaterDropTap(): ShortArray = synthesizeTileTick(DEFAULT_TILE_SPEC)

    /**
     * ~180ms calm two-note ceramic/crystal double-tick chime for activating protection modes.
     */
    internal fun synthesizeModeActivatedVoice(): ShortArray {
        val durationSec = 0.180
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(totalSamples)

        val attackSec = 0.0022
        val secondTickStart = 0.052
        // Warm D6 -> G6 steady resonant chime (zero pitch-drop/bubble bend)
        val f1 = 1174.66
        val f2 = 1567.98

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var mixed = 0.0

            val atk1 = if (t < attackSec) 0.5 * (1.0 - cos(PI * t / attackSec)) else 1.0
            val dec1 = exp(-t / 0.022)
            mixed += (sin(2.0 * PI * f1 * t) + 0.34 * sin(2.0 * PI * (f1 * 2.0) * t) * exp(-t / 0.010)) * atk1 * dec1

            if (t >= secondTickStart) {
                val t2 = t - secondTickStart
                val atk2 = if (t2 < attackSec) 0.5 * (1.0 - cos(PI * t2 / attackSec)) else 1.0
                val dec2 = exp(-t2 / 0.030)
                mixed += 1.05 * (sin(2.0 * PI * f2 * t2) + 0.32 * sin(2.0 * PI * (f2 * 2.0) * t2) * exp(-t2 / 0.014)) * atk2 * dec2
            }

            val tailTaper = if (t > 0.158) ((durationSec - t) / 0.022).coerceIn(0.0, 1.0) else 1.0
            val scaled = mixed * 0.42 * tailTaper
            out[i] = (scaled * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return out
    }
}
