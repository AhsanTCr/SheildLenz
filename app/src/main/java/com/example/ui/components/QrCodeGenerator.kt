package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Pure-Kotlin ISO-18004 QR Code encoder (Versions 1–4, Byte Mode, ECC Level L)
 * supporting scannable pairing and guardian recovery URIs.
 */
object QrCodeGenerator {

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun reedSolomonRemainder(data: IntArray, eccCount: Int): IntArray {
        var gen = intArrayOf(1)
        for (i in 0 until eccCount) {
            val next = IntArray(gen.size + 1)
            val factor = GF_EXP[i]
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMul(gen[j], factor)
            }
            gen = next
        }

        val rem = IntArray(eccCount)
        for (byte in data) {
            val factor = byte xor rem[0]
            System.arraycopy(rem, 1, rem, 0, eccCount - 1)
            rem[eccCount - 1] = 0
            for (j in 0 until eccCount) {
                rem[j] = rem[j] xor gfMul(gen[j + 1], factor)
            }
        }
        return rem
    }

    private data class VersionSpec(
        val version: Int,
        val size: Int,
        val totalDataCodewords: Int,
        val eccCodewords: Int,
        val alignmentCenter: Int?
    )

    private val SPECS = listOf(
        VersionSpec(version = 1, size = 21, totalDataCodewords = 19, eccCodewords = 7, alignmentCenter = null),
        VersionSpec(version = 2, size = 25, totalDataCodewords = 34, eccCodewords = 10, alignmentCenter = 18),
        VersionSpec(version = 3, size = 29, totalDataCodewords = 55, eccCodewords = 15, alignmentCenter = 22),
        VersionSpec(version = 4, size = 33, totalDataCodewords = 80, eccCodewords = 20, alignmentCenter = 26)
    )

    fun encode(content: String): Array<BooleanArray> {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val spec = SPECS.firstOrNull { bytes.size <= it.totalDataCodewords - 2 } ?: SPECS.last()
        val payloadBytes = bytes.copyOf(bytes.size.coerceAtMost(spec.totalDataCodewords - 2))

        val bitBuffer = ArrayList<Int>(spec.totalDataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bitBuffer.add((value ushr i) and 1)
            }
        }

        // Byte mode indicator (0100) + 8-bit length + UTF-8 bytes + terminator
        appendBits(0b0100, 4)
        appendBits(payloadBytes.size, 8)
        for (b in payloadBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }
        val maxDataBits = spec.totalDataCodewords * 8
        val terminatorBits = (maxDataBits - bitBuffer.size).coerceAtMost(4)
        appendBits(0, terminatorBits)
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(0)
        }
        var padIndex = 0
        while (bitBuffer.size < maxDataBits) {
            appendBits(if (padIndex % 2 == 0) 0xEC else 0x11, 8)
            padIndex++
        }

        val dataCodewords = IntArray(spec.totalDataCodewords) { idx ->
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or bitBuffer[idx * 8 + b]
            }
            v
        }
        val ecc = reedSolomonRemainder(dataCodewords, spec.eccCodewords)
        val allCodewords = IntArray(dataCodewords.size + ecc.size)
        System.arraycopy(dataCodewords, 0, allCodewords, 0, dataCodewords.size)
        System.arraycopy(ecc, 0, allCodewords, dataCodewords.size, ecc.size)

        val n = spec.size
        val modules = Array(n) { BooleanArray(n) }
        val isFunction = Array(n) { BooleanArray(n) }

        fun setFunctionModule(r: Int, c: Int, dark: Boolean) {
            modules[r][c] = dark
            isFunction[r][c] = true
        }

        fun drawFinderPattern(topRow: Int, leftCol: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val rr = topRow + r
                    val cc = leftCol + c
                    if (rr !in 0 until n || cc !in 0 until n) continue
                    val dark = r in 0..6 && c in 0..6 &&
                        (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                    setFunctionModule(rr, cc, dark)
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(0, n - 7)
        drawFinderPattern(n - 7, 0)

        spec.alignmentCenter?.let { center ->
            for (dr in -2..2) {
                for (dc in -2..2) {
                    val dist = maxOf(abs(dr), abs(dc))
                    setFunctionModule(center + dr, center + dc, dist != 1)
                }
            }
        }

        for (i in 8 until n - 8) {
            val dark = i % 2 == 0
            if (!isFunction[6][i]) setFunctionModule(6, i, dark)
            if (!isFunction[i][6]) setFunctionModule(i, 6, dark)
        }

        // Dark module
        setFunctionModule(n - 8, 8, true)

        // Reserve format info cells
        for (i in 0..8) {
            if (i != 6) {
                isFunction[8][i] = true
                isFunction[i][8] = true
            }
        }
        for (i in n - 8 until n) {
            isFunction[8][i] = true
            if (i != n - 8) isFunction[i][8] = true
        }

        // Place data bits in zigzag column pairs
        var bitIdx = 0
        val totalBits = allCodewords.size * 8
        var col = n - 1
        var upward = true
        while (col > 0) {
            if (col == 6) col--
            for (step in 0 until n) {
                val r = if (upward) n - 1 - step else step
                for (dc in 0..1) {
                    val c = col - dc
                    if (!isFunction[r][c]) {
                        val bit = if (bitIdx < totalBits) {
                            val word = allCodewords[bitIdx ushr 3]
                            ((word ushr (7 - (bitIdx and 7))) and 1) == 1
                        } else {
                            false
                        }
                        modules[r][c] = bit
                        bitIdx++
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // Choose best mask (0..7)
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        for (mask in 0 until 8) {
            val candidate = Array(n) { r ->
                BooleanArray(n) { c ->
                    if (isFunction[r][c]) modules[r][c] else modules[r][c] xor shouldMask(mask, r, c)
                }
            }
            applyFormatBits(candidate, mask)
            val penalty = computePenalty(candidate)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
            }
        }

        for (r in 0 until n) {
            for (c in 0 until n) {
                if (!isFunction[r][c]) {
                    modules[r][c] = modules[r][c] xor shouldMask(bestMask, r, c)
                }
            }
        }
        applyFormatBits(modules, bestMask)
        return modules
    }

    private fun shouldMask(mask: Int, r: Int, c: Int): Boolean = when (mask) {
        0 -> (r + c) % 2 == 0
        1 -> r % 2 == 0
        2 -> c % 3 == 0
        3 -> (r + c) % 3 == 0
        4 -> ((r / 2) + (c / 3)) % 2 == 0
        5 -> (r * c) % 2 + (r * c) % 3 == 0
        6 -> (((r * c) % 2 + (r * c) % 3) % 2) == 0
        else -> (((r + c) % 2 + (r * c) % 3) % 2) == 0
    }

    private fun applyFormatBits(modules: Array<BooleanArray>, mask: Int) {
        // ECC Level L (01) + 3-bit mask -> BCH(15,5) + XOR 0x5412
        val data = (0b01 shl 3) or mask
        var rem = data
        for (i in 0 until 10) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412
        val n = modules.size

        for (i in 0 until 6) modules[i][8] = ((bits ushr i) and 1) == 1
        modules[7][8] = ((bits ushr 6) and 1) == 1
        modules[8][8] = ((bits ushr 7) and 1) == 1
        modules[8][7] = ((bits ushr 8) and 1) == 1
        for (i in 9 until 15) modules[8][14 - i] = ((bits ushr i) and 1) == 1

        for (i in 0 until 8) modules[8][n - 1 - i] = ((bits ushr i) and 1) == 1
        for (i in 8 until 15) modules[n - 15 + i][8] = ((bits ushr i) and 1) == 1
        modules[n - 8][8] = true
    }

    private fun computePenalty(modules: Array<BooleanArray>): Int {
        val n = modules.size
        var penalty = 0
        for (r in 0 until n) {
            var runLen = 1
            for (c in 1 until n) {
                if (modules[r][c] == modules[r][c - 1]) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        for (c in 0 until n) {
            var runLen = 1
            for (r in 1 until n) {
                if (modules[r][c] == modules[r - 1][c]) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)
        }
        return penalty
    }

    /**
     * Renders [content] to a crisp ARGB_8888 Bitmap with a quiet-zone margin for Android View overlays.
     */
    fun encodeBitmap(
        content: String,
        sizePx: Int = 420,
        darkColor: Int = AndroidColor.parseColor("#141824"),
        lightColor: Int = AndroidColor.WHITE
    ): Bitmap {
        val matrix = encode(content)
        val count = matrix.size
        val quietModules = 2
        val totalModules = count + quietModules * 2
        val safeSize = sizePx.coerceAtLeast(totalModules * 4)
        val cellPx = (safeSize / totalModules).coerceAtLeast(1)
        val bitmapSize = totalModules * cellPx
        val pixels = IntArray(bitmapSize * bitmapSize) { lightColor }

        for (r in 0 until count) {
            for (c in 0 until count) {
                if (matrix[r][c]) {
                    val startY = (r + quietModules) * cellPx
                    val startX = (c + quietModules) * cellPx
                    for (y in startY until startY + cellPx) {
                        val rowOffset = y * bitmapSize
                        for (x in startX until startX + cellPx) {
                            pixels[rowOffset + x] = darkColor
                        }
                    }
                }
            }
        }
        return Bitmap.createBitmap(pixels, bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
    }
}

@Composable
fun QrCodeImage(
    payload: String,
    modifier: Modifier = Modifier,
    padding: Dp = 12.dp,
    darkColor: Color = Color(0xFF141824),
    lightColor: Color = Color.White,
    testTag: String? = "child_pairing_qr_matrix"
) {
    val matrix = remember(payload) { QrCodeGenerator.encode(payload) }
    Box(
        modifier = modifier
            .background(lightColor, RoundedCornerShape(16.dp))
            .padding(padding)
            .semantics { contentDescription = "Pairing QR code for $payload" }
            .let { base -> if (!testTag.isNullOrBlank()) base.testTag(testTag) else base }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = matrix.size
            val cellSize = minOf(size.width, size.height) / count.toFloat()
            val offsetX = (size.width - cellSize * count) / 2f
            val offsetY = (size.height - cellSize * count) / 2f
            for (r in 0 until count) {
                for (c in 0 until count) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(offsetX + c * cellSize, offsetY + r * cellSize),
                            size = Size(cellSize + 0.4f, cellSize + 0.4f)
                        )
                    }
                }
            }
        }
    }
}
