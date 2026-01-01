package com.eastshine.screentranslator.util

import android.graphics.Bitmap
import android.media.Image
import androidx.core.graphics.createBitmap

object BitmapUtils {
    /**
     * Converts an Image to a Bitmap, handling row padding.
     *
     * @param image The Image from ImageReader
     * @return Bitmap with correct dimensions (padding removed)
     */
    fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap =
            createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            image.width,
            image.height,
        )
    }
}
