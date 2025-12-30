package com.eastshine.screentranslator.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.eastshine.screentranslator.ocr.model.TextElement
import com.eastshine.screentranslator.util.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MLKitOCRProcessor(
    private val context: Context,
) : OCRProcessor {
    private val recognizer =
        TextRecognition.getClient(
            JapaneseTextRecognizerOptions.Builder().build(),
        )

    override suspend fun process(bitmap: Bitmap): List<TextElement> =
        withContext(Dispatchers.Default) {
            val image = InputImage.fromBitmap(bitmap, 0)

            try {
                val result = recognizer.process(image).await()
                extractTextElements(result).also {
                    Log.d("OCRProcessor", "Total ${it.size} elements extracted:")
                    it.forEach(::logTextElement)
                }
            } catch (e: Exception) {
                Log.e("OCRProcessor", "OCR failed", e)
                emptyList()
            }
        }

    private fun extractTextElements(result: Text): List<TextElement> {
        // FIXME: 잘 모르지만 일단 confidence를 sum으로 채움
        return result.textBlocks.mapNotNull { block ->
            val boundingBox = block.boundingBox ?: return@mapNotNull null
            TextElement(
                text = block.text,
                boundingBox = boundingBox,
                cornerPoints = block.cornerPoints,
                confidence = block.lines.sumOf { it.confidence.toDouble() },
            )
        }
    }

    private fun logTextElement(textElement: TextElement) {
        Log.d(
            "OCRProcessor",
            "text = ${textElement.text}\nboundingBox = ${textElement.boundingBox}\n" +
                "cornerPoints = ${textElement.cornerPoints}\n" +
                "confidence = ${textElement.confidence}",
        )
    }

    override fun release() {
        recognizer.close()
    }
}
