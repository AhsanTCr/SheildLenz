package com.example.detection.classifier

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.core.models.ThreatCategory
import com.example.data.preferences.AegisPreferences
import com.example.data.preferences.InferenceAccelerationMode
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteContentClassifier(
    private val context: Context,
    private val modelManager: ModelManager? = null,
    private val assetName: String = ModelManager.BUNDLED_ASSET_NAME,
    private val preferences: AegisPreferences? = null
) : ContentClassifier {

    private var interpreter: Interpreter? = null

    override var isModelReady: Boolean = false
        private set

    override var inputWidth: Int = 224
        private set

    override var inputHeight: Int = 224
        private set

    override var isQuantizedInput: Boolean = false
        private set

    override var outputClassCount: Int = 0
        private set

    var activeAcceleratorLabel: String = "CPU (XNNPACK)"
        private set

    private var outputDataType: DataType = DataType.FLOAT32
    private var outputScale: Float = 1.0f
    private var outputZeroPoint: Int = 0
    private var reusableFloatOut: Array<FloatArray>? = null
    private var reusableByteOut: Array<ByteArray>? = null

    var lastErrorMessage: String? = null
        private set

    override val modelSpecification: String
        get() = if (isModelReady) {
            "TFLite ($activeAcceleratorLabel) • Input: [1, $inputHeight, $inputWidth, 3] ${if (isQuantizedInput) "UInt8" else "Float32"} • Output: [1, $outputClassCount]"
        } else {
            "No TFLite Model Loaded • Supported: [1, H, W, 3] Float32/UInt8 -> [1, 2|3|5] Classes"
        }

    init {
        reloadModel()
    }

    private fun buildOptions(useNnapi: Boolean): Interpreter.Options {
        return Interpreter.Options().apply {
            setNumThreads(2)
            setUseNNAPI(useNnapi)
            setUseXNNPACK(!useNnapi)
        }
    }

    private fun createInterpreterWithFallback(): Pair<Interpreter?, String> {
        val desiredMode = preferences?.inferenceAccelerationMode?.value ?: InferenceAccelerationMode.CPU_XNNPACK
        val tryNnapi = (desiredMode == InferenceAccelerationMode.NNAPI_NPU ||
            desiredMode == InferenceAccelerationMode.AUTO_BEST) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        val importedFile = modelManager?.activeModelFile
        val useImported = importedFile != null && importedFile.exists() && importedFile.length() > 0L

        if (tryNnapi) {
            try {
                val nnapiOptions = buildOptions(useNnapi = true)
                val nnapiInterpreter = if (useImported) {
                    Interpreter(importedFile!!, nnapiOptions)
                } else {
                    loadAssetModelFile(context, assetName)?.let { Interpreter(it, nnapiOptions) }
                }
                if (nnapiInterpreter != null) {
                    return nnapiInterpreter to "NNAPI (NPU/GPU)"
                }
            } catch (e: Exception) {
                Log.w(TAG, "NNAPI delegate unavailable, falling back to XNNPACK CPU: ${e.message}")
            }
        }

        val cpuOptions = buildOptions(useNnapi = false)
        val cpuInterpreter = if (useImported) {
            Log.i(TAG, "Loading user-imported TFLite model from ${importedFile!!.absolutePath}")
            Interpreter(importedFile, cpuOptions)
        } else {
            val assetBuffer = loadAssetModelFile(context, assetName)
            if (assetBuffer != null) {
                Log.i(TAG, "Loading bundled TFLite asset model: $assetName")
                Interpreter(assetBuffer, cpuOptions)
            } else {
                null
            }
        }
        return cpuInterpreter to "CPU (XNNPACK)"
    }

    @Synchronized
    override fun reloadModel(): Boolean {
        close()
        lastErrorMessage = null
        try {
            val (createdInterpreter, accelLabel) = createInterpreterWithFallback()
            activeAcceleratorLabel = accelLabel

            if (createdInterpreter == null) {
                isModelReady = false
                outputClassCount = 0
                lastErrorMessage = "No .tflite visual model loaded yet."
                Log.i(TAG, lastErrorMessage!!)
                return false
            }

            val inTensor = createdInterpreter.getInputTensor(0)
            val inShape = inTensor.shape()
            val inType = inTensor.dataType()

            if (inShape.size != 4 || inShape[0] != 1 || inShape[3] != 3) {
                createdInterpreter.close()
                isModelReady = false
                outputClassCount = 0
                lastErrorMessage = "Unsupported input tensor shape ${inShape.contentToString()}."
                return false
            }

            val outTensor = createdInterpreter.getOutputTensor(0)
            val outShape = outTensor.shape()
            val classes = outShape.lastOrNull() ?: 0
            if (classes !in setOf(2, 3, 5)) {
                createdInterpreter.close()
                isModelReady = false
                outputClassCount = 0
                lastErrorMessage = "Unsupported output class count ($classes)."
                return false
            }

            inputHeight = inShape[1]
            inputWidth = inShape[2]
            isQuantizedInput = (inType == DataType.UINT8)
            outputClassCount = classes
            outputDataType = outTensor.dataType()
            outputScale = outTensor.quantizationParams().scale
            outputZeroPoint = outTensor.quantizationParams().zeroPoint

            interpreter = createdInterpreter
            isModelReady = true
            Log.i(TAG, "TFLite ready: ${inputWidth}x${inputHeight} (${if (isQuantizedInput) "UInt8" else "Float32"}), $outputClassCount output classes.")
            return true
        } catch (e: Exception) {
            isModelReady = false
            outputClassCount = 0
            lastErrorMessage = "Failed to load TFLite model: ${e.message}"
            Log.e(TAG, lastErrorMessage!!, e)
            return false
        }
    }

    @Synchronized
    override fun classifyTensorBuffer(tensorBuffer: ByteBuffer): ClassificationResult {
        val tflite = interpreter
        val startTime = SystemClock.uptimeMillis()

        if (tflite == null || !isModelReady) {
            return ClassificationResult(
                safeScore = 1.0f,
                suggestiveScore = 0.0f,
                explicitScore = 0.0f,
                topCategory = ThreatCategory.BENIGN_SAFE,
                confidence = 0.0f,
                isModelLoaded = false,
                latencyMs = SystemClock.uptimeMillis() - startTime,
                diagnosticInfo = lastErrorMessage ?: "No .tflite visual model loaded"
            )
        }

        tensorBuffer.rewind()

        val probabilities: FloatArray = if (outputDataType == DataType.UINT8) {
            val rawOut = reusableByteOut?.takeIf { it[0].size == outputClassCount }
                ?: Array(1) { ByteArray(outputClassCount) }.also { reusableByteOut = it }
            tflite.run(tensorBuffer, rawOut)
            FloatArray(outputClassCount) { idx ->
                val unsignedVal = rawOut[0][idx].toInt() and 0xFF
                val scale = if (outputScale > 0f) outputScale else (1f / 255f)
                ((unsignedVal - outputZeroPoint) * scale).coerceIn(0f, 1f)
            }
        } else {
            val floatOut = reusableFloatOut?.takeIf { it[0].size == outputClassCount }
                ?: Array(1) { FloatArray(outputClassCount) }.also { reusableFloatOut = it }
            tflite.run(tensorBuffer, floatOut)
            val raw = floatOut[0]
            if (raw.any { it < -0.001f || it > 1.001f }) {
                softmax(raw)
            } else {
                FloatArray(outputClassCount) { idx -> raw[idx].coerceIn(0f, 1f) }
            }
        }

        val latency = SystemClock.uptimeMillis() - startTime
        return mapProbabilities(probabilities, latency)
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val exps = FloatArray(logits.size) { i -> kotlin.math.exp((logits[i] - maxLogit).toDouble()).toFloat() }
        val sum = exps.sum().coerceAtLeast(1e-7f)
        return FloatArray(logits.size) { i -> (exps[i] / sum).coerceIn(0f, 1f) }
    }

    private fun mapProbabilities(probs: FloatArray, latency: Long): ClassificationResult {
        val safe: Float
        val suggestive: Float
        val explicit: Float
        val rawMap: Map<String, Float>

        when (probs.size) {
            2 -> {
                // 2-class: [0]=SAFE, [1]=NSFW/EXPLICIT
                safe = probs[0]
                suggestive = 0.0f
                explicit = probs[1]
                rawMap = mapOf("Safe" to probs[0], "NSFW / Explicit" to probs[1])
            }
            3 -> {
                // 3-class: [0]=SAFE, [1]=SUGGESTIVE, [2]=EXPLICIT
                safe = probs[0]
                suggestive = probs[1]
                explicit = probs[2]
                rawMap = mapOf("Safe" to probs[0], "Suggestive" to probs[1], "Explicit" to probs[2])
            }
            5 -> {
                // Standard 5-class NSFW MobileNet/Inception: [Drawings, Hentai, Neutral, Porn, Sexy]
                val drawings = probs[0]
                val hentai = probs[1]
                val neutral = probs[2]
                val porn = probs[3]
                val sexy = probs[4]

                safe = (drawings + neutral).coerceIn(0f, 1f)
                suggestive = sexy.coerceIn(0f, 1f)
                explicit = (hentai + porn).coerceIn(0f, 1f)
                rawMap = mapOf(
                    "Drawings" to drawings,
                    "Hentai" to hentai,
                    "Neutral" to neutral,
                    "Porn" to porn,
                    "Sexy" to sexy
                )
            }
            else -> {
                safe = 1.0f
                suggestive = 0.0f
                explicit = 0.0f
                rawMap = emptyMap()
            }
        }

        val topCategory = when {
            explicit >= suggestive && explicit >= safe -> ThreatCategory.EXPLICIT_ADULT
            suggestive >= safe -> ThreatCategory.SUGGESTIVE_CONTENT
            else -> ThreatCategory.BENIGN_SAFE
        }

        val maxConfidence = maxOf(safe, suggestive, explicit)

        return ClassificationResult(
            safeScore = safe,
            suggestiveScore = suggestive,
            explicitScore = explicit,
            topCategory = topCategory,
            confidence = maxConfidence,
            isModelLoaded = true,
            latencyMs = latency,
            outputClassCount = probs.size,
            rawClassScores = rawMap,
            diagnosticInfo = "Inference in ${latency}ms (${probs.size}-Class) • Safe: ${(safe * 100).toInt()}%, Sugg: ${(suggestive * 100).toInt()}%, Expl: ${(explicit * 100).toInt()}%"
        )
    }

    @Synchronized
    override fun close() {
        try {
            interpreter?.close()
        } catch (_: Exception) {
        }
        interpreter = null
        isModelReady = false
    }

    private fun loadAssetModelFile(context: Context, assetName: String): MappedByteBuffer? {
        return try {
            val fileDescriptor: AssetFileDescriptor = context.assets.openFd(assetName)
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
            }
        } catch (_: IOException) {
            null
        }
    }

    companion object {
        private const val TAG = "TFLiteClassifier"
    }
}
