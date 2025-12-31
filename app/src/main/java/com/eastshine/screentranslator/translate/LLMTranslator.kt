package com.eastshine.screentranslator.translate

import com.eastshine.screentranslator.translate.model.TranslationPrompt

/**
 * Interface for LLM-based translation
 */
interface LLMTranslator {
    /**
     * Translates text using character-specific prompt
     * @param text Text to translate
     * @param translationPrompt Prompt containing character-specific instructions
     * @return Translated text
     */
    suspend fun translate(
        text: String,
        translationPrompt: TranslationPrompt,
    ): String
}
