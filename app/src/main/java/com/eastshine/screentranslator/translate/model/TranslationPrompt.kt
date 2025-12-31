package com.eastshine.screentranslator.translate.model

import com.eastshine.screentranslator.character.domain.model.CharacterPrompt

/**
 * Represents a complete translation prompt with placeholders resolved
 */
data class TranslationPrompt(
    val systemInstruction: String,
) {
    companion object {
        /**
         * Builds a translation prompt by replacing placeholders in character prompt
         * Currently supports: {language}
         */
        fun build(
            characterPrompt: CharacterPrompt,
            targetLanguage: String,
        ): TranslationPrompt {
            val resolvedPrompt =
                characterPrompt.prompt
                    .replace("{language}", targetLanguage)

            return TranslationPrompt(systemInstruction = resolvedPrompt)
        }
    }
}
