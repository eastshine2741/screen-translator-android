package com.eastshine.screentranslator.ocr.model

import android.graphics.Point
import android.graphics.Rect

data class TextElement(
    val text: String,
    val boundingBox: Rect,
    val cornerPoints: Array<Point>?,
    val confidence: Double,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextElement

        if (text != other.text) return false
        if (boundingBox != other.boundingBox) return false
        if (cornerPoints != null) {
            if (other.cornerPoints == null) return false
            if (!cornerPoints.contentEquals(other.cornerPoints)) return false
        } else if (other.cornerPoints != null) return false

        return confidence == other.confidence
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + boundingBox.hashCode()
        result = 31 * result + (cornerPoints?.contentHashCode() ?: 0)
        result = 31 * result + confidence.hashCode()
        return result
    }
}