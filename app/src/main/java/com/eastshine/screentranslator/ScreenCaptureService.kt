package com.eastshine.screentranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.eastshine.screentranslator.capture.CaptureManager
import com.eastshine.screentranslator.debug.DebugConfig
import com.eastshine.screentranslator.debug.LogcatReader
import com.eastshine.screentranslator.ocr.OCRProcessor
import com.eastshine.screentranslator.screentranslate.Screen
import com.eastshine.screentranslator.screentranslate.ScreenTranslator
import com.eastshine.screentranslator.screentranslate.model.TranslatedElement
import com.eastshine.screentranslator.translation.TranslationTrigger
import com.eastshine.screentranslator.ui.FloatingTranslateButton
import com.eastshine.screentranslator.ui.LogcatOverlayView
import com.eastshine.screentranslator.ui.TranslationOverlayView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class ScreenCaptureService : Service() {
    @Inject
    lateinit var ocrProcessor: OCRProcessor

    @Inject
    lateinit var screenTranslator: ScreenTranslator

    private lateinit var overlayView: TranslationOverlayView
    private lateinit var floatingButton: FloatingTranslateButton
    private lateinit var windowManager: WindowManager
    private lateinit var captureManager: CaptureManager

    private var logcatOverlayView: LogcatOverlayView? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val translationTriggers =
        MutableSharedFlow<TranslationTrigger>(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ScreenCaptureChannel"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupOverlayView()
        setupFloatingButton()

        if (DebugConfig.isDebugEnabled) {
            setupLogcatOverlay()
        }

        // Create CaptureManager (no overlay dependency)
        captureManager = CaptureManager(this)

        // Start translation pipeline
        startTranslationPipeline()

        if (DebugConfig.isDebugEnabled) {
            startLogcatStreaming()
        }
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
                // Get overlay dimensions
                overlayView.post {
                    val width = overlayView.width
                    val height = overlayView.height

                    captureManager.startCapture(
                        resultCode = resultCode,
                        data = data,
                        width = width,
                        height = height,
                    )

                    // Trigger initial translation after 500ms delay
                    overlayView.postDelayed({
                        emitTrigger(TranslationTrigger.ServiceStart)
                    }, 500)
                }
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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            )

        windowManager.addView(overlayView, params)
    }

    private fun setupFloatingButton() {
        floatingButton =
            FloatingTranslateButton(this) {
                emitTrigger(TranslationTrigger.FloatingButtonTap)
            }

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 100
            }

        windowManager.addView(floatingButton, params)
        floatingButton.setWindowParams(params, windowManager)

        floatingButton.post {
            val displayMetrics = resources.displayMetrics
            params.x = displayMetrics.widthPixels - floatingButton.width - (16 * displayMetrics.density).toInt()
            params.y = displayMetrics.heightPixels - floatingButton.height - (80 * displayMetrics.density).toInt()
            windowManager.updateViewLayout(floatingButton, params)
        }
    }

    private fun setupLogcatOverlay() {
        logcatOverlayView = LogcatOverlayView(this)

        // Convert 200dp to pixels
        val displayMetrics = resources.displayMetrics
        val widthPx = (300 * displayMetrics.density).toInt()
        val heightPx = (200 * displayMetrics.density).toInt()

        val params =
            WindowManager.LayoutParams(
                widthPx,
                heightPx,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

        windowManager.addView(logcatOverlayView, params)
        logcatOverlayView?.setWindowParams(params, windowManager)
    }

    private fun startLogcatStreaming() {
        scope.launch {
            LogcatReader.streamLogs().collect { line ->
                logcatOverlayView?.appendLog(line)
            }
        }
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

    /**
     * Starts the translation Flow pipeline.
     * Handles debouncing, cancellation, and orchestrates OCR + translation.
     */
    private fun startTranslationPipeline() {
        scope.launch {
            translationTriggers
                .debounce(300.milliseconds)
                .onEach { trigger ->
                    Log.d(TAG, "Processing trigger: $trigger")
                }
                .flatMapLatest { processTranslation() }
                .flowOn(Dispatchers.Default)
                .catch { e ->
                    Log.e(TAG, "Translation pipeline failed", e)
                }
                .collect { (translatedElements, bitmap) ->
                    updateOverlay(translatedElements, bitmap)
                }
        }
    }

    /**
     * Processes a single translation request.
     * Captures screen, runs OCR, translates, and returns result.
     */
    private fun processTranslation(): Flow<Pair<List<TranslatedElement>, Bitmap>> =
        flow {
            // Capture current screen on-demand
            val bitmap =
                captureManager.captureCurrentScreen() ?: run {
                    Log.w(TAG, "No image available from ImageReader")
                    return@flow
                }

            try {
                Log.d(TAG, "Bitmap captured: ${bitmap.width}x${bitmap.height}")

                // Step 1: OCR processing
                val textElements = ocrProcessor.process(bitmap)
                if (textElements.isEmpty()) {
                    Log.d(TAG, "No text detected in image")
                    return@flow
                }

                Log.d(TAG, "OCR detected ${textElements.size} text elements")

                // Step 2: Translation
                val translatedElements =
                    screenTranslator.translate(
                        Screen(
                            textElements = textElements,
                            width = bitmap.width,
                            height = bitmap.height,
                        ),
                    )

                Log.d(TAG, "Translation completed: ${translatedElements.size} elements")

                emit(translatedElements to bitmap)
            } finally {
                // Note: Bitmap could be recycled here for memory optimization
                // bitmap.recycle() - only if not used elsewhere
            }
        }

    /**
     * Updates overlay with translation results on Main thread.
     */
    private suspend fun updateOverlay(
        translatedElements: List<TranslatedElement>,
        bitmap: Bitmap,
    ) {
        withContext(Dispatchers.Main) {
            overlayView.updateTranslations(
                translatedElements,
                bitmap.width,
                bitmap.height,
            )
        }
    }

    /**
     * Emits a translation trigger event.
     */
    private fun emitTrigger(trigger: TranslationTrigger) {
        scope.launch {
            translationTriggers.emit(trigger)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changed: orientation=${newConfig.orientation}")

        // Resize VirtualDisplay and trigger translation
        if (captureManager.isCapturing()) {
            // Wait for layout to complete before getting overlay dimensions
            overlayView.viewTreeObserver.addOnGlobalLayoutListener(
                object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        overlayView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                        val width = overlayView.width
                        val height = overlayView.height

                        captureManager.resizeDisplay(width, height)
                        emitTrigger(TranslationTrigger.ConfigurationChange)
                    }
                },
            )
        }

        // Reposition floating button to bottom-right
        floatingButton.post {
            val params = floatingButton.layoutParams as WindowManager.LayoutParams
            val displayMetrics = resources.displayMetrics
            params.x = displayMetrics.widthPixels - floatingButton.width - (16 * displayMetrics.density).toInt()
            params.y = displayMetrics.heightPixels - floatingButton.height - (80 * displayMetrics.density).toInt()
            windowManager.updateViewLayout(floatingButton, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        scope.cancel() // Cancels all flows automatically
        ocrProcessor.release()
        captureManager.stopCapture()
        windowManager.removeView(overlayView)
        windowManager.removeView(floatingButton)

        if (DebugConfig.isDebugEnabled) {
            logcatOverlayView?.let { windowManager.removeView(it) }
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
