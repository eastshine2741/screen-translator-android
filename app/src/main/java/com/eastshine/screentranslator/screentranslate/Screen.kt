package com.eastshine.screentranslator.screentranslate

import com.eastshine.screentranslator.ocr.model.TextElement

data class Screen(
    val textElements: List<TextElement>,
    val width: Int,
    val height: Int,
)
