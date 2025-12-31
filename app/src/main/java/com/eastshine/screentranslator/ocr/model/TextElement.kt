package com.eastshine.screentranslator.ocr.model

import android.graphics.Point
import android.graphics.Rect

data class TextElement(
    val text: String,
    val boundingBox: Rect,
    val cornerPoints: List<Point>?,
    val confidence: Double,
)
