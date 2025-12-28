package com.eastshine.screentranslator.translate

interface LLMTranslator {
    suspend fun translate(text: String): String
}
