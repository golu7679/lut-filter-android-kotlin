package com.blank.kotlinproject

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsic3DLUT
import android.renderscript.Type
import android.util.LruCache
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ColorCubeFilter(private val context: android.content.Context) {
    private val cache = LruCache<String, ByteBuffer>(5) // Cache for processed LUT data

    fun applyFilter(sourceBitmap: Bitmap, lutBitmap: Bitmap, dimension: Int, amount: Float = 1.0f): Bitmap {
        // Create output bitmap
        val outputBitmap = Bitmap.createBitmap(
            sourceBitmap.width,
            sourceBitmap.height,
            Bitmap.Config.ARGB_8888
        )

        // Get RenderScript instance
        val rs = RenderScript.create(context)

        try {
            // Create allocations for input and output
            val inputAllocation = Allocation.createFromBitmap(rs, sourceBitmap)
            val outputAllocation = Allocation.createFromBitmap(rs, outputBitmap)

            // Create and set up the 3D LUT script
            val script = ScriptIntrinsic3DLUT.create(rs, Element.U8_4(rs))

            // Get or create cube data
            val cubeData = getCubeData(lutBitmap, dimension, rs)
            script.setLUT(cubeData)

            // Apply the filter
            script.forEach(inputAllocation, outputAllocation)

            // Copy the result to the output bitmap
            outputAllocation.copyTo(outputBitmap)

            // If amount is less than 1, blend with original
            if (amount < 1.0f) {
                return blendBitmaps(sourceBitmap, outputBitmap, amount)
            }

            return outputBitmap
        } finally {
            rs.destroy()
        }
    }

    private fun getCubeData(lutBitmap: Bitmap, dimension: Int, rs: RenderScript): Allocation {
        // Generate a unique key for this LUT
        val cacheKey = lutBitmap.hashCode().toString() + dimension

        // Check if we have this LUT in cache
        var cubeBuffer = cache.get(cacheKey)

        if (cubeBuffer == null) {
            // Create a new buffer
            cubeBuffer = createCubeDataFromHaldLut(lutBitmap, dimension)
            cache.put(cacheKey, cubeBuffer)
        }

        // Create 3D LUT allocation
        val elemType = Element.U8_4(rs)
        val typeBuilder = Type.Builder(rs, elemType)
        typeBuilder.setX(dimension)
        typeBuilder.setY(dimension)
        typeBuilder.setZ(dimension)

        val allocation = Allocation.createTyped(rs, typeBuilder.create())
        allocation.copyFromUnchecked(cubeBuffer.array())

        return allocation
    }

    private fun createCubeDataFromHaldLut(lutBitmap: Bitmap, dimension: Int): ByteBuffer {
        val lutPixels = IntArray(lutBitmap.width * lutBitmap.height)
        lutBitmap.getPixels(lutPixels, 0, lutBitmap.width, 0, 0, lutBitmap.width, lutBitmap.height)

        // Create a buffer for cube data (RGBA for each point in the cube)
        val cubeBuffer = ByteBuffer.allocateDirect(dimension * dimension * dimension * 4)
        cubeBuffer.order(ByteOrder.nativeOrder())

        // For a 64x64x64 cube, we need to extract values from HALD image
        for (r in 0 until dimension) {
            for (g in 0 until dimension) {
                for (b in 0 until dimension) {
                    // Calculate position in HALD image
                    val x = b + (r % 8) * dimension + (g % 8) * dimension * 8
                    val y = (g / 8) + (r / 8) * 8

                    // Get color from LUT
                    val index = y * lutBitmap.width + x
                    if (index < lutPixels.size) {
                        val lutColor = lutPixels[index]

                        // Add to cube data
                        cubeBuffer.put((lutColor shr 16 and 0xFF).toByte())
                        cubeBuffer.put((lutColor shr 8 and 0xFF).toByte())
                        cubeBuffer.put((lutColor and 0xFF).toByte())
                        cubeBuffer.put(0xFF.toByte()) // Alpha
                    } else {
                        // Default color if out of bounds
                        cubeBuffer.put((r * 255 / (dimension - 1)).toByte())
                        cubeBuffer.put((g * 255 / (dimension - 1)).toByte())
                        cubeBuffer.put((b * 255 / (dimension - 1)).toByte())
                        cubeBuffer.put(0xFF.toByte())
                    }
                }
            }
        }

        cubeBuffer.rewind()
        return cubeBuffer
    }

    private fun blendBitmaps(source: Bitmap, filtered: Bitmap, amount: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)

        // Draw source bitmap
        canvas.drawBitmap(source, 0f, 0f, null)

        // Draw filtered bitmap with alpha
        val paint = android.graphics.Paint().apply {
            alpha = (amount * 255).toInt()
        }
        canvas.drawBitmap(filtered, 0f, 0f, paint)

        return result
    }
}