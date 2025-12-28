package com.eastshine.screentranslator.ocr

import android.graphics.Bitmap
import com.eastshine.screentranslator.ocr.model.TextElement

class DummyOCRProcessor : OCRProcessor {
    override suspend fun process(bitmap: Bitmap): List<TextElement> {
        return emptyList()
    }

    override fun release() {
    }
}
