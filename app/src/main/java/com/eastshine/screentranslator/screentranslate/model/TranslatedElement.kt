package com.eastshine.screentranslator.screentranslate.model

import android.graphics.Point
import android.graphics.Rect

data class TranslatedElement(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect,
    val cornerPoints: List<Point>?,
    val confidence: Double,
)
