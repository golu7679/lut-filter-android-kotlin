package com.blank.kotlinproject

import android.graphics.Bitmap
import kotlin.math.floor

//class HaldLutFilter {
//    fun applyLut(sourceBitmap: Bitmap, lutBitmap: Bitmap): Bitmap {
//        val width = sourceBitmap.width
//        val height = sourceBitmap.height
//        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//
//        val lutSize = 64 // Standard HALD LUT size (64x64x64)
//        val lutSteps = lutSize * lutSize
//
//        val lutPixels = IntArray(lutBitmap.width * lutBitmap.height)
//        lutBitmap.getPixels(lutPixels, 0, lutBitmap.width, 0, 0, lutBitmap.width, lutBitmap.height)
//
//        val pixels = IntArray(width * height)
//        sourceBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
//
//        val lutWidth = lutBitmap.width
//        val lutHeight = lutBitmap.height
//
//        for (i in pixels.indices) {
//            val pixel = pixels[i]
//
//            // Extract RGBA components
//            val a = pixel shr 24 and 0xff
//            val r = pixel shr 16 and 0xff
//            val g = pixel shr 8 and 0xff
//            val b = pixel and 0xff
//
//            // Convert to LUT coordinates (normalized)
//            val lutR = r * (lutSize - 1) / 255.0
//            val lutG = g * (lutSize - 1) / 255.0
//            val lutB = b * (lutSize - 1) / 255.0
//
//            // Floor values to get integer indices
//            val r0 = floor(lutR).toInt()
//            val g0 = floor(lutG).toInt()
//            val b0 = floor(lutB).toInt()
//
//            // Find next indices (for interpolation)
//            val r1 = (r0 + 1).coerceAtMost(lutSize - 1)
//            val g1 = (g0 + 1).coerceAtMost(lutSize - 1)
//            val b1 = (b0 + 1).coerceAtMost(lutSize - 1)
//
//            // Convert to 2D LUT coordinates
//            val getLutPixel = { rr: Int, gg: Int, bb: Int ->
//                val lutIndex = rr + gg * lutSize + bb * lutSteps
//                val lutX = lutIndex % lutWidth
//                val lutY = lutIndex / lutWidth
//                if (lutX in 0 until lutWidth && lutY in 0 until lutHeight) {
//                    lutPixels[lutY * lutWidth + lutX]
//                } else {
//                    pixel // Return original pixel if out of bounds
//                }
//            }
//
//            // Fetch 8 neighboring LUT colors for trilinear interpolation
//            val c000 = getLutPixel(r0, g0, b0)
//            val c001 = getLutPixel(r0, g0, b1)
//            val c010 = getLutPixel(r0, g1, b0)
//            val c011 = getLutPixel(r0, g1, b1)
//            val c100 = getLutPixel(r1, g0, b0)
//            val c101 = getLutPixel(r1, g0, b1)
//            val c110 = getLutPixel(r1, g1, b0)
//            val c111 = getLutPixel(r1, g1, b1)
//
//            // Compute interpolation factors
//            val fr = lutR - r0
//            val fg = lutG - g0
//            val fb = lutB - b0
//
//            // Trilinear interpolation (lerp function)
//            val lerp = { c0: Int, c1: Int, f: Double ->
//                (((1 - f) * (c0 and 0xff) + f * (c1 and 0xff)).toInt() and 0xff)
//            }
//
//            val lerpRGB = { c0: Int, c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
//                val rLerp = lerp(
//                    lerp(lerp(c000 shr 16, c100 shr 16, fr), lerp(c010 shr 16, c110 shr 16, fr), fg),
//                    lerp(lerp(c001 shr 16, c101 shr 16, fr), lerp(c011 shr 16, c111 shr 16, fr), fg),
//                    fb
//                )
//                val gLerp = lerp(
//                    lerp(lerp(c000 shr 8, c100 shr 8, fr), lerp(c010 shr 8, c110 shr 8, fr), fg),
//                    lerp(lerp(c001 shr 8, c101 shr 8, fr), lerp(c011 shr 8, c111 shr 8, fr), fg),
//                    fb
//                )
//                val bLerp = lerp(
//                    lerp(lerp(c000, c100, fr), lerp(c010, c110, fr), fg),
//                    lerp(lerp(c001, c101, fr), lerp(c011, c111, fr), fg),
//                    fb
//                )
//                (a shl 24) or (rLerp shl 16) or (gLerp shl 8) or bLerp
//            }
//
//            // Apply interpolated LUT color
//            pixels[i] = lerpRGB(c000, c001, c010, c011, c100, c101, c110, c111)
//        }
//
//        result.setPixels(pixels, 0, width, 0, 0, width, height)
//        return result
//    }
//}


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