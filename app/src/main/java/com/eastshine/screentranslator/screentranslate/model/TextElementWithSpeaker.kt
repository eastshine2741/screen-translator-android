package com.eastshine.screentranslator.screentranslate.model

import com.eastshine.screentranslator.ocr.model.TextElement

/**
 * Represents a text element with identified speaker information
 */
data class TextElementWithSpeaker(
    val textElement: TextElement,
    val speaker: String?,
)
