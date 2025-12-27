package com.eastshine.screentranslator.translate

class DummyLLMTranslator : LLMTranslator {
    override suspend fun translate(text: String): String {
        return text
    }
}