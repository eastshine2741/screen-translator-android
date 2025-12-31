package com.eastshine.screentranslator.character.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eastshine.screentranslator.character.domain.model.CharacterPrompt
import com.eastshine.screentranslator.character.domain.repository.CharacterPromptRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * DataStore implementation of CharacterPromptRepository
 * Stores all character prompts as JSON array in a single preference key
 */
class DataStoreCharacterPromptRepository(
    private val context: Context,
) : CharacterPromptRepository {
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "character_prompts")
        private val CHARACTER_PROMPTS_KEY = stringPreferencesKey("character_prompts_json")

        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
    }

    override suspend fun save(characterPrompt: CharacterPrompt) {
        // Not implemented initially
        TODO("Save functionality will be implemented in future iteration")
    }

    override suspend fun findAll(): List<CharacterPrompt> {
        return context.dataStore.data
            .map { preferences ->
                val jsonString = preferences[CHARACTER_PROMPTS_KEY]
                if (jsonString.isNullOrBlank()) {
                    emptyList()
                } else {
                    try {
                        json.decodeFromString<List<CharacterPrompt>>(jsonString)
                    } catch (e: Exception) {
                        Log.e(
                            "DataStoreCharacterPromptRepository",
                            "Failed to decode character prompts",
                            e,
                        )
                        emptyList()
                    }
                }
            }.first()
    }
}
