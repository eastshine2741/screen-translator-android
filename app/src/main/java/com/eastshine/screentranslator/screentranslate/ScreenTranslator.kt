package com.eastshine.screentranslator.screentranslate

import android.util.Log
import com.eastshine.screentranslator.ocr.model.TextElement
import com.eastshine.screentranslator.screentranslate.model.TranslatedElement
import com.eastshine.screentranslator.translate.LLMTranslator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

class ScreenTranslator(
    private val llmTranslator: LLMTranslator,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun translate(textElements: List<TextElement>): List<TranslatedElement> =
        withContext(Dispatchers.Default) {
            if (textElements.isEmpty()) {
                return@withContext emptyList()
            }

            // 병렬 처리로 성능 향상
            textElements.map { element ->
                async {
                    try {
                        val translatedText = llmTranslator.translate(element.text)

                        TranslatedElement(
                            originalText = element.text,
                            translatedText = translatedText,
                            boundingBox = element.boundingBox,
                            cornerPoints = element.cornerPoints,
                            confidence = element.confidence,
                        )
                    } catch (e: Exception) {
                        Log.e("ScreenTranslator", "Translation failed for: ${element.text}", e)

                        // 번역 실패 시 원본 반환
                        TranslatedElement(
                            originalText = element.text,
                            translatedText = element.text,
                            boundingBox = element.boundingBox,
                            cornerPoints = element.cornerPoints,
                            confidence = element.confidence,
                        )
                    }
                }
            }.awaitAll()
        }

    fun release() {
        scope.cancel()
    }
}
