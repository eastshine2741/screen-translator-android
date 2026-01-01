package com.eastshine.screentranslator.translation

/**
 * Represents events that trigger screen translation.
 */
sealed interface TranslationTrigger {
    /**
     * Initial translation when service starts
     */
    data object ServiceStart : TranslationTrigger

    /**
     * Translation triggered by configuration change (e.g., rotation)
     */
    data object ConfigurationChange : TranslationTrigger

    /**
     * Translation triggered by user tapping the overlay
     */
    data object UserTap : TranslationTrigger
}
