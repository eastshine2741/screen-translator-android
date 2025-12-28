package com.eastshine.screentranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.eastshine.screentranslator.ocr.OCRProcessor
import com.eastshine.screentranslator.screentranslate.ScreenTranslator
import com.eastshine.screentranslator.ui.TranslationOverlayView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ScreenCaptureService : Service() {
    @Inject
    lateinit var ocrProcessor: OCRProcessor

    @Inject
    lateinit var screenTranslator: ScreenTranslator

    private lateinit var overlayView: TranslationOverlayView
    private lateinit var windowManager: WindowManager

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

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var lastProcessTime = 0L
    private val processingInterval = 1000L

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ScreenCaptureChannel"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupOverlayView()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        intent?.let {
            val resultCode = it.getIntExtra(EXTRA_RESULT_CODE, 0)
            val data = it.getParcelableExtra<Intent>(EXTRA_DATA)
            if (resultCode != 0 && data != null) {
                startCapture(resultCode, data)
            }
        }

        return START_STICKY
    }

    private fun setupOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = TranslationOverlayView(this)

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            )

        windowManager.addView(overlayView, params)
    }

    private fun startCapture(
        resultCode: Int,
        data: Intent,
    ) {
        val manager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = manager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(mediaProjectionCallback, handler)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader =
            ImageReader.newInstance(
                width, height,
                PixelFormat.RGBA_8888,
                2,
            )

        virtualDisplay =
            mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null, null,
            )

        imageReader?.setOnImageAvailableListener({ reader ->
            captureScreen(reader)
        }, handler)
    }

    private fun captureScreen(reader: ImageReader) {
        val image = reader.acquireLatestImage() ?: return

        try {
            val bitmap = imageToBitmap(image)
            processScreen(bitmap)
        } finally {
            image.close()
        }
    }

    private fun processScreen(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastProcessTime < processingInterval) {
            return
        }

        lastProcessTime = currentTime

        scope.launch {
            try {
                // 1. OCR 실행 (Hilt로 주입받은 객체)
                val textElements = ocrProcessor.process(bitmap)

                if (textElements.isEmpty()) {
                    return@launch
                }

                // 2. 번역 실행 (Hilt로 주입받은 객체)
                val translatedElements = screenTranslator.translate(textElements)

                // 3. Overlay 업데이트
                withContext(Dispatchers.Main) {
                    overlayView.updateTranslations(
                        translatedElements,
                        bitmap.width,
                        bitmap.height,
                    )
                }
            } catch (e: Exception) {
                Log.e("ScreenCaptureService", "Processing failed", e)
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap =
            Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888,
            )
        bitmap.copyPixelsFromBuffer(buffer)

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            image.width,
            image.height,
        )
    }

    private fun stopCapture() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()

        virtualDisplay = null
        imageReader = null
        mediaProjection = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "화면 번역 서비스",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "실시간 화면 번역이 실행 중입니다"
                }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("화면 번역 활성화")
            .setContentText("화면의 텍스트를 실시간으로 번역하고 있습니다")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()

        scope.cancel()
        ocrProcessor.release()
        screenTranslator.release()
        windowManager.removeView(overlayView)
        stopCapture()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
