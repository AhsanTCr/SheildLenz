package com.example.detection.preprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.media.Image
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class FramePreprocessor(
    inputWidth: Int = 224,
    inputHeight: Int = 224,
    isQuantizedUInt8: Boolean = false
) {
    var inputWidth: Int = inputWidth
        private set
    var inputHeight: Int = inputHeight
        private set
    var isQuantizedUInt8: Boolean = isQuantizedUInt8
        private set

    private var tensorBuffer: ByteBuffer = allocateBuffer(inputWidth, inputHeight, isQuantizedUInt8)
    private var intValues: IntArray = IntArray(inputWidth * inputHeight)

    // Reusable scratch bitmaps to eliminate per-frame heap allocations & GC churn
    private var reusableRowBitmap: Bitmap? = null
    private var reusableCroppedBitmap: Bitmap? = null
    private var reusableDeferredBitmap: Bitmap? = null
    private var reusableDeferredCanvas: Canvas? = null
    private var reusableTightBuffer: ByteBuffer? = null
    private var reusableRowScratch: ByteArray? = null
    private var reusableScaledBitmap: Bitmap? = null
    private var reusableScaledCanvas: Canvas? = null
    private val scaleSrcRect = Rect()
    private val scaleDstRect = Rect(0, 0, inputWidth, inputHeight)
    private val scalePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    // Perceptual 5x5 grid luma fingerprint to skip both identical and near-static frames
    private var hasProcessedFrame: Boolean = false
    private val lastSampleLumas = IntArray(LUMA_GRID_SIZE * LUMA_GRID_SIZE)
    private val currentSampleLumas = IntArray(LUMA_GRID_SIZE * LUMA_GRID_SIZE)

    fun configure(width: Int, height: Int, quantizedUInt8: Boolean) {
        if (this.inputWidth == width && this.inputHeight == height && this.isQuantizedUInt8 == quantizedUInt8) {
            return
        }
        this.inputWidth = width
        this.inputHeight = height
        this.isQuantizedUInt8 = quantizedUInt8
        this.tensorBuffer = allocateBuffer(width, height, quantizedUInt8)
        this.intValues = IntArray(width * height)
        this.scaleDstRect.set(0, 0, width, height)
        this.reusableScaledCanvas = null
        this.reusableScaledBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        this.reusableScaledBitmap = null
    }

    private fun allocateBuffer(width: Int, height: Int, quantizedUInt8: Boolean): ByteBuffer {
        val bytesPerChannel = if (quantizedUInt8) 1 else 4
        return ByteBuffer.allocateDirect(1 * width * height * 3 * bytesPerChannel).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    /**
     * Extracts an ARGB Bitmap from an ImageReader Image buffer using pooled reusable bitmaps.
     * Safely handles GPU row padding including devices whose final row omits trailing padding bytes.
     * Callers must NOT call Bitmap.recycle() on the returned pooled bitmap.
     */
    fun extractBitmapFromImage(image: Image): Bitmap? {
        val planes = image.planes
        if (planes.isEmpty()) return null

        val width = image.width.coerceAtLeast(1)
        val height = image.height.coerceAtLeast(1)
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride.coerceAtLeast(1)
        val rowStride = planes[0].rowStride.coerceAtLeast(width * pixelStride)
        val rowPadding = (rowStride - pixelStride * width).coerceAtLeast(0)

        buffer.rewind()
        val expectedTightBytes = width * height * 4

        // Fast zero-copy-padding path when rows are tightly packed
        if (rowPadding == 0 && pixelStride == 4 && buffer.remaining() >= expectedTightBytes) {
            val directBitmap = obtainPooledBitmap(
                existing = reusableCroppedBitmap,
                width = width,
                height = height
            ).also { reusableCroppedBitmap = it }
            directBitmap.copyPixelsFromBuffer(buffer)
            return directBitmap
        }

        // Fast blitted path when buffer includes full padded size on every row
        val rowBitmapWidth = width + rowPadding / pixelStride
        if (pixelStride == 4 && rowStride % pixelStride == 0 && buffer.remaining() >= rowBitmapWidth * height * 4) {
            val rowBitmap = obtainPooledBitmap(
                existing = reusableRowBitmap,
                width = rowBitmapWidth,
                height = height
            ).also { reusableRowBitmap = it }

            rowBitmap.copyPixelsFromBuffer(buffer)

            val cropped = obtainPooledBitmap(
                existing = reusableCroppedBitmap,
                width = width,
                height = height
            ).also { reusableCroppedBitmap = it }

            val canvas = Canvas(cropped)
            scaleSrcRect.set(0, 0, width, height)
            val dst = Rect(0, 0, width, height)
            canvas.drawBitmap(rowBitmap, scaleSrcRect, dst, null)
            return cropped
        }

        // Safe row-by-row copy when GPU omits final-row trailing padding or uses unusual stride alignment
        val tightBuffer = reusableTightBuffer?.takeIf { it.capacity() >= expectedTightBytes }
            ?: ByteBuffer.allocateDirect(expectedTightBytes).also { reusableTightBuffer = it }
        tightBuffer.clear()
        tightBuffer.limit(expectedTightBytes)

        val rowBytes = width * pixelStride.coerceAtMost(4)
        val rowScratch = reusableRowScratch?.takeIf { it.size >= rowBytes }
            ?: ByteArray(rowBytes).also { reusableRowScratch = it }

        val limit = buffer.limit()
        for (y in 0 until height) {
            val rowStart = y * rowStride
            if (rowStart + rowBytes <= limit) {
                buffer.position(rowStart)
                buffer.get(rowScratch, 0, rowBytes)
                tightBuffer.put(rowScratch, 0, rowBytes)
            } else {
                break
            }
        }
        tightBuffer.rewind()

        val cropped = obtainPooledBitmap(
            existing = reusableCroppedBitmap,
            width = width,
            height = height
        ).also { reusableCroppedBitmap = it }
        cropped.copyPixelsFromBuffer(tightBuffer)
        return cropped
    }

    /**
     * Copies [source] into a separate pooled bitmap so trailing-edge deferred inspection
     * never gets overwritten by intermediate pooled frame extractions.
     */
    fun retainDeferredBitmap(source: Bitmap): Bitmap {
        val copy = obtainPooledBitmap(
            existing = reusableDeferredBitmap,
            width = source.width.coerceAtLeast(1),
            height = source.height.coerceAtLeast(1)
        )
        if (reusableDeferredBitmap !== copy) {
            reusableDeferredBitmap = copy
            reusableDeferredCanvas = Canvas(copy)
        }
        val canvas = reusableDeferredCanvas ?: Canvas(copy).also { reusableDeferredCanvas = it }
        scaleSrcRect.set(0, 0, source.width, source.height)
        val dst = Rect(0, 0, copy.width, copy.height)
        canvas.drawBitmap(source, scaleSrcRect, dst, null)
        return copy
    }

    private fun obtainPooledBitmap(existing: Bitmap?, width: Int, height: Int): Bitmap {
        if (existing != null && !existing.isRecycled && existing.width == width && existing.height == height) {
            return existing
        }
        if (existing != null && !existing.isRecycled) {
            existing.recycle()
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * Perceptual 5x5 block-luma delta check:
     * Returns true when a newly captured frame is visually identical or near-static
     * (e.g. blinking cursor, clock minute change, minor compression noise) so we can skip inference
     * without missing real content changes.
     */
    fun isFrameDuplicate(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return false

        var idx = 0
        for (gy in 0 until LUMA_GRID_SIZE) {
            val y = ((gy + 0.5f) * h / LUMA_GRID_SIZE).toInt().coerceIn(0, h - 1)
            for (gx in 0 until LUMA_GRID_SIZE) {
                val x = ((gx + 0.5f) * w / LUMA_GRID_SIZE).toInt().coerceIn(0, w - 1)
                val px = bitmap.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                currentSampleLumas[idx++] = (r * 30 + g * 59 + b * 11) / 100
            }
        }

        if (!hasProcessedFrame) {
            hasProcessedFrame = true
            System.arraycopy(currentSampleLumas, 0, lastSampleLumas, 0, lastSampleLumas.size)
            return false
        }

        var sumDiff = 0
        var maxDiff = 0
        for (i in 0 until lastSampleLumas.size) {
            val diff = abs(currentSampleLumas[i] - lastSampleLumas[i])
            sumDiff += diff
            if (diff > maxDiff) maxDiff = diff
        }
        val avgDiff = sumDiff / lastSampleLumas.size

        // Skip only when both overall frame average delta AND single-cell peak delta stay within perceptual noise tolerance
        val isDuplicate = avgDiff <= MAX_AVG_LUMA_DELTA && maxDiff <= MAX_PEAK_LUMA_DELTA
        if (!isDuplicate) {
            System.arraycopy(currentSampleLumas, 0, lastSampleLumas, 0, lastSampleLumas.size)
        }
        return isDuplicate
    }

    /**
     * Scales bitmap (or optional [srcRect] region) into pooled scratch surface and fills the direct ByteBuffer
     * with normalized Float32 RGB [0.0f..1.0f] or UInt8 [0..255] with zero heap churn.
     */
    fun preprocessBitmapToTensorBuffer(bitmap: Bitmap, srcRect: Rect? = null): ByteBuffer {
        val needsCropOrScale = srcRect != null || bitmap.width != inputWidth || bitmap.height != inputHeight
        val sourceBitmap = if (needsCropOrScale) {
            val scaled = obtainPooledBitmap(reusableScaledBitmap, inputWidth, inputHeight)
                .also { reusableScaledBitmap = it }
            val canvas = reusableScaledCanvas ?: Canvas(scaled).also { reusableScaledCanvas = it }
            if (srcRect != null) {
                scaleSrcRect.set(
                    srcRect.left.coerceIn(0, (bitmap.width - 1).coerceAtLeast(0)),
                    srcRect.top.coerceIn(0, (bitmap.height - 1).coerceAtLeast(0)),
                    srcRect.right.coerceIn(1, bitmap.width.coerceAtLeast(1)),
                    srcRect.bottom.coerceIn(1, bitmap.height.coerceAtLeast(1))
                )
            } else {
                scaleSrcRect.set(0, 0, bitmap.width, bitmap.height)
            }
            scaleDstRect.set(0, 0, inputWidth, inputHeight)
            canvas.drawBitmap(bitmap, scaleSrcRect, scaleDstRect, scalePaint)
            scaled
        } else {
            bitmap
        }

        sourceBitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        tensorBuffer.rewind()
        val pixelCount = inputWidth * inputHeight
        if (isQuantizedUInt8) {
            for (i in 0 until pixelCount) {
                val pixel = intValues[i]
                tensorBuffer.put(((pixel shr 16) and 0xFF).toByte())
                tensorBuffer.put(((pixel shr 8) and 0xFF).toByte())
                tensorBuffer.put((pixel and 0xFF).toByte())
            }
        } else {
            for (i in 0 until pixelCount) {
                val pixel = intValues[i]
                tensorBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
                tensorBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
                tensorBuffer.putFloat((pixel and 0xFF) / 255.0f)
            }
        }

        tensorBuffer.rewind()
        return tensorBuffer
    }

    fun reset() {
        hasProcessedFrame = false
        lastSampleLumas.fill(0)
    }

    fun recyclePools() {
        reset()
        reusableRowBitmap?.let { if (!it.isRecycled) it.recycle() }
        reusableRowBitmap = null
        reusableCroppedBitmap?.let { if (!it.isRecycled) it.recycle() }
        reusableCroppedBitmap = null
        reusableDeferredCanvas = null
        reusableDeferredBitmap?.let { if (!it.isRecycled) it.recycle() }
        reusableDeferredBitmap = null
        reusableTightBuffer = null
        reusableRowScratch = null
        reusableScaledCanvas = null
        reusableScaledBitmap?.let { if (!it.isRecycled) it.recycle() }
        reusableScaledBitmap = null
    }

    companion object {
        private const val LUMA_GRID_SIZE = 5
        private const val MAX_AVG_LUMA_DELTA = 4
        private const val MAX_PEAK_LUMA_DELTA = 14

        /**
         * Returns non-distorted 1:1 inspection crops tailored for portrait browsers (excluding Chrome top toolbar/URL bar
         * & bottom navigation bar) alongside a full-screen pass, or centered square + full-screen for landscape videos.
         */
        fun computeInspectionRegions(width: Int, height: Int): List<Rect> {
            if (width <= 0 || height <= 0) return emptyList()
            val regions = ArrayList<Rect>(3)

            if (height > (width * 1.25f).toInt()) {
                val topExclude = (height * 0.11f).toInt().coerceIn(0, height / 4)
                val bottomExclude = (height * 0.07f).toInt().coerceIn(0, height / 5)
                val bottomLimit = (height - bottomExclude).coerceAtLeast(topExclude + 1)

                val upperBottom = (topExclude + width).coerceAtMost(height)
                if (upperBottom > topExclude) {
                    regions.add(Rect(0, topExclude, width, upperBottom))
                }

                val lowerTop = (bottomLimit - width).coerceAtLeast(0)
                if (lowerTop > topExclude + width / 4 && bottomLimit > lowerTop) {
                    regions.add(Rect(0, lowerTop, width, bottomLimit))
                }

                regions.add(Rect(0, 0, width, height))
            } else if (width > (height * 1.25f).toInt()) {
                val left = ((width - height) / 2).coerceAtLeast(0)
                regions.add(Rect(left, 0, (left + height).coerceAtMost(width), height))
                regions.add(Rect(0, 0, width, height))
            } else {
                regions.add(Rect(0, 0, width, height))
            }

            return regions
        }
    }
}
