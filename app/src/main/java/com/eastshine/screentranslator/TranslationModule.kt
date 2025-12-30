package com.eastshine.screentranslator

import android.content.Context
import com.eastshine.screentranslator.ocr.MLKitOCRProcessor
import com.eastshine.screentranslator.ocr.OCRProcessor
import com.eastshine.screentranslator.screentranslate.ScreenTranslator
import com.eastshine.screentranslator.translate.DummyLLMTranslator
import com.eastshine.screentranslator.translate.LLMTranslator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext

@Module
@InstallIn(ServiceComponent::class)
object TranslationModule {
    @Provides
    fun provideOCRProcessor(
        @ApplicationContext context: Context,
    ): OCRProcessor {
        return MLKitOCRProcessor(context)
    }

    @Provides
    fun provideLLMTranslator(): LLMTranslator {
        return DummyLLMTranslator()
    }

    @Provides
    fun provideScreenTranslator(llmTranslator: LLMTranslator): ScreenTranslator {
        return ScreenTranslator(llmTranslator)
    }
}
