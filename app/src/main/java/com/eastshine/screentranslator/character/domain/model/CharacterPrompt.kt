package com.eastshine.screentranslator.character.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a character-specific translation prompt configuration
 */
@Serializable
data class CharacterPrompt(
    val characterName: String,
    val prompt: String,
    val aliases: Set<String>,
) {
    companion object {
        /**
         * Default prompt used when no character match is found
         */
        fun default(): CharacterPrompt {
            return CharacterPrompt(
                characterName = "Default",
                prompt =
                    """
                    You are a professional translator.
                    Translate text to {language} naturally and concisely.
                    Only respond with the translated text, no explanations.
                    If the text is already in {language}, return it as is.
                    Preserve the original tone and meaning.
                    """.trimIndent(),
                aliases = emptySet(),
            )
        }
    }
}
