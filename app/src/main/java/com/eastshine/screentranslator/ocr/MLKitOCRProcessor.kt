package com.eastshine.screentranslator.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.eastshine.screentranslator.ocr.model.TextElement
import com.eastshine.screentranslator.util.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MLKitOCRProcessor(
    private val context: Context,
) : OCRProcessor{
    private val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    override suspend fun process(bitmap: Bitmap): List<TextElement> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)

        try {
            val result = recognizer.process(image).await()
            extractTextElements(result)
        } catch (e: Exception) {
            Log.e("OCRProcessor", "OCR failed", e)
            emptyList()
        }
    }

    private fun extractTextElements(result: Text): List<TextElement> {
        val elements = mutableListOf<TextElement>()

        for (block in result.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val boundingBox = element.boundingBox ?: continue

                    elements.add(
                        TextElement(
                            text = element.text,
                            boundingBox = boundingBox,
                            cornerPoints = element.cornerPoints,
                            confidence = element.confidence
                        )
                    )
                }
            }
        }

        return elements
    }

    override fun release() {
        recognizer.close()
    }
}