package com.eastshine.screentranslator.character.matcher

import com.eastshine.screentranslator.character.domain.model.CharacterPrompt

/**
 * Matches speakers to character prompts using alias matching
 * Performs case-insensitive comparison and supports partial matching
 */
class AliasMatchingStrategy : CharacterPromptMatchingStrategy {
    override fun findMatch(
        speaker: String,
        characterPrompts: List<CharacterPrompt>,
    ): CharacterPrompt? {
        val normalizedSpeaker = speaker.trim().lowercase()

        return characterPrompts.firstOrNull { characterPrompt ->
            characterPrompt.aliases.any { alias ->
                val normalizedAlias = alias.trim().lowercase()
                normalizedSpeaker == normalizedAlias
            }
        }
    }
}
