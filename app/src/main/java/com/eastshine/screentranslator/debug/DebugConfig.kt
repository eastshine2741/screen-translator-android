package com.eastshine.screentranslator.debug

/**
 * Global debug configuration
 * Change isDebugEnabled to enable/disable all debug features
 */
object DebugConfig {
    /**
     * Master debug switch
     * Set to true to enable debug overlays
     */
    const val isDebugEnabled = true // 이 값만 수정하면 전체 디버그 on/off

    /**
     * Logcat tag filter
     * Only logs with these tags will be displayed
     * Add or remove tags as needed
     */
    val logTags =
        listOf(
            "ScreenCaptureService",
            "ScreenTranslator",
            "OCRProcessor",
            "CaptureManager",
            "TranslationOverlayView",
        )
}
