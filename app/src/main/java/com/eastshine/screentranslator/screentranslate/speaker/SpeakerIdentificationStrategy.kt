package com.eastshine.screentranslator.screentranslate.speaker

import com.eastshine.screentranslator.screentranslate.Screen
import com.eastshine.screentranslator.screentranslate.model.TextElementWithSpeaker

/**
 * Strategy for identifying speakers from text elements
 */
interface SpeakerIdentificationStrategy {
    /**
     * Identifies speakers for each text element
     * @param screen Captured screen
     * @return List of text elements with identified speakers
     */
    fun identifySpeakers(screen: Screen): List<TextElementWithSpeaker>
}
