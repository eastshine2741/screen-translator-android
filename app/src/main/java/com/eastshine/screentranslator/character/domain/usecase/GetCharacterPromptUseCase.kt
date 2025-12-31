package com.eastshine.screentranslator.character.domain.usecase

import android.util.Log
import com.eastshine.screentranslator.character.domain.model.CharacterPrompt
import com.eastshine.screentranslator.character.domain.repository.CharacterPromptRepository
import com.eastshine.screentranslator.character.matcher.CharacterPromptMatchingStrategy

/**
 * Use case for retrieving character-specific prompts
 * Loads prompts into memory and uses matcher to find appropriate prompt
 */
class GetCharacterPromptUseCase(
    private val repository: CharacterPromptRepository,
    private val matcher: CharacterPromptMatchingStrategy,
) {
    // Cache loaded prompts to avoid repeated repository calls
    private var cachedPrompts: List<CharacterPrompt>? = null

    /**
     * Gets character prompt for a speaker
     * @param speaker The identified speaker name (null for default prompt)
     * @return CharacterPrompt matching the speaker or default prompt
     */
    suspend fun execute(speaker: String?): CharacterPrompt {
        // Load prompts if not cached
        if (cachedPrompts == null) {
            cachedPrompts =
                try {
                    repository.findAll()
                } catch (e: Exception) {
                    Log.e("GetCharacterPromptUseCase", "Failed to load prompts", e)
                    emptyList()
                }
        }

        // If no speaker, return default
        if (speaker.isNullOrBlank()) {
            return CharacterPrompt.default()
        }

        // Try to find match
        val matchedPrompt =
            cachedPrompts?.let { prompts ->
                matcher.findMatch(speaker, prompts)
            }

        // Return matched or default
        return matchedPrompt ?: CharacterPrompt.default()
    }

    /**
     * Invalidates cache to force reload on next call
     */
    fun invalidateCache() {
        cachedPrompts = null
    }
}
