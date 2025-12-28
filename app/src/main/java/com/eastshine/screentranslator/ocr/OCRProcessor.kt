package com.eastshine.screentranslator.ocr

import android.graphics.Bitmap
import com.eastshine.screentranslator.ocr.model.TextElement

interface OCRProcessor {
    suspend fun process(bitmap: Bitmap): List<TextElement>

    fun release()
}
