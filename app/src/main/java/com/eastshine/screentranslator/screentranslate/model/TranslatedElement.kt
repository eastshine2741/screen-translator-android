package com.eastshine.screentranslator.screentranslate.model

import android.graphics.Point
import android.graphics.Rect

data class TranslatedElement(
    val originalText: String,
    val translatedText: String,
    val boundingBox: Rect,
    val cornerPoints: Array<Point>?,
    val confidence: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TranslatedElement

        if (originalText != other.originalText) return false
        if (translatedText != other.translatedText) return false
        if (boundingBox != other.boundingBox) return false
        if (cornerPoints != null) {
            if (other.cornerPoints == null) return false
            if (!cornerPoints.contentEquals(other.cornerPoints)) return false
        } else if (other.cornerPoints != null) return false

        return confidence == other.confidence
    }

    override fun hashCode(): Int {
        var result = originalText.hashCode()
        result = 31 * result + translatedText.hashCode()
        result = 31 * result + boundingBox.hashCode()
        result = 31 * result + (cornerPoints?.contentHashCode() ?: 0)
        result = 31 * result + confidence.hashCode()
        return result
    }
}