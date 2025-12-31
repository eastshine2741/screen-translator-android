package com.eastshine.screentranslator

import android.content.Context
import com.eastshine.screentranslator.character.data.repository.DataStoreCharacterPromptRepository
import com.eastshine.screentranslator.character.domain.repository.CharacterPromptRepository
import com.eastshine.screentranslator.character.domain.usecase.GetCharacterPromptUseCase
import com.eastshine.screentranslator.character.matcher.AliasMatchingStrategy
import com.eastshine.screentranslator.character.matcher.CharacterPromptMatchingStrategy
import com.eastshine.screentranslator.ocr.MLKitOCRProcessor
import com.eastshine.screentranslator.ocr.OCRProcessor
import com.eastshine.screentranslator.screentranslate.ScreenTranslator
import com.eastshine.screentranslator.screentranslate.speaker.GeometricHeuristicStrategy
import com.eastshine.screentranslator.screentranslate.speaker.SpeakerIdentificationStrategy
import com.eastshine.screentranslator.translate.GeminiTranslator
import com.eastshine.screentranslator.translate.LLMTranslator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Named

@Module
@InstallIn(ServiceComponent::class)
object TranslationModule {
    @Provides
    @ServiceScoped
    fun provideOCRProcessor(
        @ApplicationContext context: Context,
    ): OCRProcessor {
        return MLKitOCRProcessor(context)
    }

    // Speaker Identification
    @Provides
    @ServiceScoped
    fun provideSpeakerIdentificationStrategy(): SpeakerIdentificationStrategy {
        return GeometricHeuristicStrategy()
    }

    // Character Prompt Repository
    @Provides
    @ServiceScoped
    fun provideCharacterPromptRepository(
        @ApplicationContext context: Context,
    ): CharacterPromptRepository {
        return DataStoreCharacterPromptRepository(context)
    }

    // Character Prompt Matcher
    @Provides
    @ServiceScoped
    fun provideCharacterPromptMatcher(): CharacterPromptMatchingStrategy {
        return AliasMatchingStrategy()
    }

    // Target Language Configuration
    @Provides
    @Named("targetLanguage")
    fun provideTargetLanguage(): String {
        return "Korean" // TODO: Make this configurable via settings
    }

    // Use Case
    @Provides
    @ServiceScoped
    fun provideGetCharacterPromptUseCase(
        repository: CharacterPromptRepository,
        matcher: CharacterPromptMatchingStrategy,
    ): GetCharacterPromptUseCase {
        return GetCharacterPromptUseCase(
            repository = repository,
            matcher = matcher,
        )
    }

    // LLM Translator
    @Provides
    @ServiceScoped
    fun provideLLMTranslator(
        @Named("targetLanguage") targetLanguage: String,
    ): LLMTranslator {
        return GeminiTranslator(targetLanguage)
    }

    // Screen Translator
    @Provides
    @ServiceScoped
    fun provideScreenTranslator(
        speakerIdentificationStrategy: SpeakerIdentificationStrategy,
        getCharacterPromptUseCase: GetCharacterPromptUseCase,
        llmTranslator: LLMTranslator,
        @Named("targetLanguage") targetLanguage: String,
    ): ScreenTranslator {
        return ScreenTranslator(
            speakerIdentificationStrategy = speakerIdentificationStrategy,
            getCharacterPromptUseCase = getCharacterPromptUseCase,
            llmTranslator = llmTranslator,
            targetLanguage = targetLanguage,
        )
    }
}
