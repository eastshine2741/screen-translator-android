package com.eastshine.screentranslator.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.eastshine.screentranslator.util.BitmapUtils

/**
 * Manages screen capture using MediaProjection, VirtualDisplay, and ImageReader.
 * Provides on-demand bitmap capture without continuous frame processing.
 */
class CaptureManager(
    private val context: Context,
) {
    private var mediaProjection: MediaProjection? = null
    private val mediaProjectionCallback =
        object : MediaProjection.Callback() {
            override fun onStop() {
                stopCapture()
            }
        }

    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Captures the current screen on-demand.
     * Returns null if no image is available.
     *
     * This uses acquireLatestImage() which automatically discards old frames
     * and returns only the most recent one, making it memory efficient.
     */
    fun captureCurrentScreen(): Bitmap? {
        val image =
            imageReader?.acquireLatestImage() ?: run {
                Log.w(TAG, "No image available from ImageReader")
                return null
            }

        return try {
            BitmapUtils.imageToBitmap(image)
        } finally {
            image.close() // Must close to avoid queue exhaustion
        }
    }

    /**
     * Checks if screen capture is currently active
     */
    fun isCapturing(): Boolean = virtualDisplay != null

    /**
     * Starts screen capture with MediaProjection
     *
     * @param resultCode Result code from MediaProjection permission
     * @param data Intent data from MediaProjection permission
     * @param width Width of the capture area
     * @param height Height of the capture area
     */
    fun startCapture(
        resultCode: Int,
        data: Intent,
        width: Int,
        height: Int,
    ) {
        // Create MediaProjection
        val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(mediaProjectionCallback, handler)

        // Create VirtualDisplay and ImageReader
        createVirtualDisplayAndImageReader(width, height)
    }

    /**
     * Creates VirtualDisplay and ImageReader with specified dimensions
     */
    private fun createVirtualDisplayAndImageReader(
        width: Int,
        height: Int,
    ) {
        val density = context.resources.displayMetrics.densityDpi

        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid dimensions: width=$width, height=$height")
            return
        }

        Log.d(
            TAG,
            "Creating VirtualDisplay and ImageReader with dimensions: width=$width, height=$height, density=$density",
        )

        // Create ImageReader with maxImages=2 (minimum for acquireLatestImage)
        imageReader =
            ImageReader.newInstance(
                width, height,
                PixelFormat.RGBA_8888,
                2, // maxImages must be at least 2 for acquireLatestImage
            )

        virtualDisplay =
            mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null, null,
            )

        Log.d(TAG, "VirtualDisplay and ImageReader created successfully")
    }

    /**
     * Resizes VirtualDisplay when screen orientation changes
     *
     * @param width New width
     * @param height New height
     */
    fun resizeDisplay(
        width: Int,
        height: Int,
    ) {
        val density = context.resources.displayMetrics.densityDpi

        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid dimensions for resize: width=$width, height=$height")
            return
        }

        if (virtualDisplay == null) {
            Log.e(TAG, "VirtualDisplay is null, cannot resize")
            return
        }

        Log.d(TAG, "Resizing VirtualDisplay to: width=$width, height=$height")

        // Close old ImageReader
        imageReader?.close()

        // Create new ImageReader with new dimensions
        imageReader =
            ImageReader.newInstance(
                width, height,
                PixelFormat.RGBA_8888,
                2,
            )

        // Resize VirtualDisplay and set new surface
        virtualDisplay?.resize(width, height, density)
        virtualDisplay?.surface = imageReader?.surface

        Log.d(TAG, "VirtualDisplay resized successfully")
    }

    /**
     * Stops screen capture and releases all resources
     */
    fun stopCapture() {
        Log.d(TAG, "Stopping capture")

        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()

        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    companion object {
        private const val TAG = "CaptureManager"
    }
}
