package com.eastshine.screentranslator.character.matcher

import com.eastshine.screentranslator.character.domain.model.CharacterPrompt

/**
 * Strategy for matching speaker names to character prompts
 */
interface CharacterPromptMatchingStrategy {
    /**
     * Finds matching character prompt for a speaker
     * @param speaker The identified speaker name
     * @param characterPrompts Available character prompts
     * @return Matching CharacterPrompt or null if no match found
     */
    fun findMatch(
        speaker: String,
        characterPrompts: List<CharacterPrompt>,
    ): CharacterPrompt?
}
