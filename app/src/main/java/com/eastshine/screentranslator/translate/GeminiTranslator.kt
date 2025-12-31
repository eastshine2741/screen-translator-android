package com.eastshine.screentranslator.translate

import android.util.Log
import com.eastshine.screentranslator.translate.model.TranslationPrompt
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini-based LLM translator with model caching
 */
class GeminiTranslator(
    private val targetLanguage: String = "Korean",
) : LLMTranslator {
    // Cache generative models by translation prompt to reuse same system instructions
    private val modelCache = mutableMapOf<TranslationPrompt, com.google.firebase.ai.GenerativeModel>()

    override suspend fun translate(
        text: String,
        translationPrompt: TranslationPrompt,
    ): String =
        withContext(Dispatchers.IO) {
            try {
                if (text.isBlank()) {
                    return@withContext text
                }

                // Get or create cached model
                val model =
                    modelCache.getOrPut(translationPrompt) {
                        createGenerativeModel(translationPrompt)
                    }

                // Create user prompt
                val userPrompt = "Translate to $targetLanguage: $text" // FIXME: pass text only

                // Call Gemini API
                val response = model.generateContent(userPrompt)

                // Extract response text
                response.text?.trim() ?: text
            } catch (e: Exception) {
                Log.e("GeminiTranslator", "Translation failed: ${e.message}", e)
                text // Return original on failure
            }
        }

    private fun createGenerativeModel(translationPrompt: TranslationPrompt): GenerativeModel {
        return Firebase
            .ai(
                backend = GenerativeBackend.googleAI(),
            ).generativeModel(
                modelName = "gemini-2.5-flash-lite",
                systemInstruction =
                    content {
                        text(translationPrompt.systemInstruction)
                    },
            )
    }

    /**
     * Clears model cache to free resources
     */
    fun clearCache() {
        modelCache.clear()
    }
}
