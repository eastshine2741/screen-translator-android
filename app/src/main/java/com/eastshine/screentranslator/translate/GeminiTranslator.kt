package com.eastshine.screentranslator.translate

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiTranslator(
    private val targetLanguage: String = "Korean",
) : LLMTranslator {
    // TODO: fine-tuning https://firebase.google.com/docs/ai-logic/model-parameters?api=dev
    private val generativeModel =
        Firebase.ai(
            backend = GenerativeBackend.googleAI(),
        ).generativeModel(
            modelName = "gemini-2.5-flash-lite",
            systemInstruction =
                content {
                    text(
                        """
                        You are a professional translator.
                        Translate text to $targetLanguage naturally and concisely.
                        Only respond with the translated text, no explanations.
                        If the text is already in $targetLanguage, return it as is.
                        Preserve the original tone and meaning.
                        """.trimIndent(),
                    )
                },
        )

    override suspend fun translate(text: String): String =
        withContext(Dispatchers.IO) {
            try {
                if (text.isBlank()) {
                    return@withContext text
                }

                // 프롬프트 생성
                val prompt = "Translate to $targetLanguage: $text"

                // Gemini API 호출
                val response = generativeModel.generateContent(prompt)

                // 응답 텍스트 추출
                response.text?.trim() ?: text
            } catch (e: Exception) {
                Log.e("FirebaseGeminiTranslator", "Translation failed: ${e.message}", e)
                text // 실패 시 원본 반환
            }
        }
}
