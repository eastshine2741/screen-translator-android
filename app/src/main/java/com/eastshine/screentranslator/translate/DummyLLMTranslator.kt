package com.eastshine.screentranslator.translate

import com.eastshine.screentranslator.translate.model.TranslationPrompt

class DummyLLMTranslator : LLMTranslator {
    override suspend fun translate(
        text: String,
        translationPrompt: TranslationPrompt,
    ): String {
        return text
    }
}
