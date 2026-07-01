package com.sitewatch.app.monitor

import android.graphics.Bitmap
import androidx.core.graphics.scale

/**
 * Difference hash (dHash) of a bitmap.
 *
 * A raw pixel hash of a rendered page is far too noisy — antialiasing, a blinking
 * cursor, or a rotating ad would flip it on every check. dHash instead downscales
 * to a tiny grayscale image and encodes the *gradient direction* between adjacent
 * pixels, which is stable against minor rendering jitter while still reacting to
 * real layout/content changes.
 *
 * The result is a 64-bit hash rendered as 16 hex chars. We compare hashes for
 * exact equality (consistent with the other monitor types); a Hamming-distance
 * threshold would make it even more tolerant and is a natural future refinement.
 */
object PerceptualHash {

    private const val WIDTH = 9  // 9x8 → 8 comparisons per row → 64 bits
    private const val HEIGHT = 8

    fun dHash(bitmap: Bitmap): String {
        val scaled = bitmap.scale(WIDTH, HEIGHT)

        // Row-major luminance grid.
        val gray = Array(HEIGHT) { y ->
            IntArray(WIDTH) { x -> luminance(scaled.getPixel(x, y)) }
        }
        if (scaled != bitmap) scaled.recycle()

        var bits = 0L
        var bitIndex = 0
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH - 1) {
                if (gray[y][x] < gray[y][x + 1]) {
                    bits = bits or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        return "%016x".format(bits)
    }

    private fun luminance(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        // Rec. 601 luma.
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
