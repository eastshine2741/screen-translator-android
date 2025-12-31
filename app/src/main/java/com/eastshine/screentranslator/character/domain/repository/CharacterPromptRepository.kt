package com.eastshine.screentranslator.character.domain.repository

import com.eastshine.screentranslator.character.domain.model.CharacterPrompt

/**
 * Repository for managing character prompts
 */
interface CharacterPromptRepository {
    /**
     * Saves a character prompt (not implemented initially)
     */
    suspend fun save(characterPrompt: CharacterPrompt)

    /**
     * Retrieves all stored character prompts
     */
    suspend fun findAll(): List<CharacterPrompt>
}
