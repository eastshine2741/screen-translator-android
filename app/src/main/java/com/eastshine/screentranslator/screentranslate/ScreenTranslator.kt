package com.eastshine.screentranslator.screentranslate

import android.util.Log
import com.eastshine.screentranslator.character.domain.usecase.GetCharacterPromptUseCase
import com.eastshine.screentranslator.screentranslate.model.TranslatedElement
import com.eastshine.screentranslator.screentranslate.speaker.SpeakerIdentificationStrategy
import com.eastshine.screentranslator.translate.LLMTranslator
import com.eastshine.screentranslator.translate.model.TranslationPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Orchestrates screen translation with character-aware prompts and speaker identification
 */
class ScreenTranslator(
    private val speakerIdentificationStrategy: SpeakerIdentificationStrategy,
    private val getCharacterPromptUseCase: GetCharacterPromptUseCase,
    private val llmTranslator: LLMTranslator,
    private val targetLanguage: String = "Korean",
) {
    suspend fun translate(screen: Screen): List<TranslatedElement> =
        withContext(Dispatchers.Default) {
            if (screen.textElements.isEmpty()) {
                return@withContext emptyList()
            }

            // Step 1: Identify speakers for each text element
            val elementsWithSpeakers =
                speakerIdentificationStrategy.identifySpeakers(screen)

            // Step 2: Process each element in parallel
            elementsWithSpeakers
                .map { elementWithSpeaker ->
                    async {
                        try {
                            // Step 3: Find character prompt for speaker
                            val characterPrompt =
                                getCharacterPromptUseCase.execute(
                                    elementWithSpeaker.speaker,
                                )

                            Log.d(
                                "ScreenTranslator",
                                "Element: ${elementWithSpeaker.textElement.text}, " +
                                    "Speaker: ${elementWithSpeaker.speaker}, " +
                                    "Character: ${characterPrompt.characterName}",
                            )

                            // Step 4: Build translation prompt
                            val translationPrompt =
                                TranslationPrompt.build(
                                    characterPrompt,
                                    targetLanguage,
                                )

                            // Step 5: Translate with character-specific prompt
                            val translatedText =
                                llmTranslator.translate(
                                    elementWithSpeaker.textElement.text,
                                    translationPrompt,
                                )

                            TranslatedElement(
                                originalText = elementWithSpeaker.textElement.text,
                                translatedText = translatedText,
                                boundingBox = elementWithSpeaker.textElement.boundingBox,
                                cornerPoints = elementWithSpeaker.textElement.cornerPoints,
                                confidence = elementWithSpeaker.textElement.confidence,
                                speaker = elementWithSpeaker.speaker,
                                characterName = characterPrompt.characterName,
                            )
                        } catch (e: Exception) {
                            Log.e(
                                "ScreenTranslator",
                                "Translation failed for: ${elementWithSpeaker.textElement.text}",
                                e,
                            )

                            // On failure, return original
                            val element = elementWithSpeaker.textElement
                            TranslatedElement(
                                originalText = element.text,
                                translatedText = element.text,
                                boundingBox = element.boundingBox,
                                cornerPoints = element.cornerPoints,
                                confidence = element.confidence,
                                speaker = elementWithSpeaker.speaker,
                                characterName = "ERROR",
                            )
                        }
                    }
                }.awaitAll()
        }
}
