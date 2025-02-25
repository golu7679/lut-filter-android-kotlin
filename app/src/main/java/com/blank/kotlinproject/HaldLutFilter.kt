package com.blank.kotlinproject

import android.graphics.Bitmap
import kotlin.math.floor

class HaldLutFilter {
    fun applyLut(sourceBitmap: Bitmap, lutBitmap: Bitmap): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val lutSize = 64 // Standard HALD LUT size (64x64x64)
        val lutSteps = lutSize * lutSize
        val lutWidth = lutBitmap.width
        val lutHeight = lutBitmap.height

        // Load LUT pixels into an array for fast access
        val lutPixels = IntArray(lutWidth * lutHeight)
        lutBitmap.getPixels(lutPixels, 0, lutWidth, 0, 0, lutWidth, lutHeight)

        // Load source image pixels
        val pixels = IntArray(width * height)
        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Lookup function to get LUT color
        fun getLutPixel(rr: Int, gg: Int, bb: Int): Int {
            val lutIndex = rr + gg * lutSize + bb * lutSteps
            val lutX = lutIndex % lutWidth
            val lutY = lutIndex / lutWidth
            return if (lutX < lutWidth && lutY < lutHeight) {
                lutPixels[lutY * lutWidth + lutX]
            } else 0
        }

        // Faster trilinear interpolation using precomputed fractional values
        for (i in pixels.indices) {
            val pixel = pixels[i]

            // Extract RGBA components using bit shifts
            val a = pixel ushr 24 and 0xff
            val r = pixel ushr 16 and 0xff
            val g = pixel ushr 8 and 0xff
            val b = pixel and 0xff

            // Convert to LUT coordinates
            val lutR = r * (lutSize - 1) / 255.0
            val lutG = g * (lutSize - 1) / 255.0
            val lutB = b * (lutSize - 1) / 255.0

            // Precompute integer and fractional parts
            val r0 = floor(lutR).toInt()
            val g0 = floor(lutG).toInt()
            val b0 = floor(lutB).toInt()
            val r1 = (r0 + 1).coerceAtMost(lutSize - 1)
            val g1 = (g0 + 1).coerceAtMost(lutSize - 1)
            val b1 = (b0 + 1).coerceAtMost(lutSize - 1)

            val fr = lutR - r0
            val fg = lutG - g0
            val fb = lutB - b0
            val fr1 = 1 - fr
            val fg1 = 1 - fg
            val fb1 = 1 - fb

            // Fetch 8 neighboring LUT colors
            val c000 = getLutPixel(r0, g0, b0)
            val c001 = getLutPixel(r0, g0, b1)
            val c010 = getLutPixel(r0, g1, b0)
            val c011 = getLutPixel(r0, g1, b1)
            val c100 = getLutPixel(r1, g0, b0)
            val c101 = getLutPixel(r1, g0, b1)
            val c110 = getLutPixel(r1, g1, b0)
            val c111 = getLutPixel(r1, g1, b1)

            // Trilinear interpolation using precomputed factors
            val rFinal = (((c000 ushr 16 and 0xff) * fr1 + (c100 ushr 16 and 0xff) * fr) * fg1 +
                    ((c010 ushr 16 and 0xff) * fr1 + (c110 ushr 16 and 0xff) * fr) * fg) * fb1 +
                    (((c001 ushr 16 and 0xff) * fr1 + (c101 ushr 16 and 0xff) * fr) * fg1 +
                            ((c011 ushr 16 and 0xff) * fr1 + (c111 ushr 16 and 0xff) * fr) * fg) * fb

            val gFinal = (((c000 ushr 8 and 0xff) * fr1 + (c100 ushr 8 and 0xff) * fr) * fg1 +
                    ((c010 ushr 8 and 0xff) * fr1 + (c110 ushr 8 and 0xff) * fr) * fg) * fb1 +
                    (((c001 ushr 8 and 0xff) * fr1 + (c101 ushr 8 and 0xff) * fr) * fg1 +
                            ((c011 ushr 8 and 0xff) * fr1 + (c111 ushr 8 and 0xff) * fr) * fg) * fb

            val bFinal = (((c000 and 0xff) * fr1 + (c100 and 0xff) * fr) * fg1 +
                    ((c010 and 0xff) * fr1 + (c110 and 0xff) * fr) * fg) * fb1 +
                    (((c001 and 0xff) * fr1 + (c101 and 0xff) * fr) * fg1 +
                            ((c011 and 0xff) * fr1 + (c111 and 0xff) * fr) * fg) * fb

            // Store new pixel value
            pixels[i] = (a shl 24) or ((rFinal.toInt() and 0xff) shl 16) or
                    ((gFinal.toInt() and 0xff) shl 8) or (bFinal.toInt() and 0xff)
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}