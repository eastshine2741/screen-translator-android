package com.eastshine.screentranslator.screentranslate.speaker

import android.util.Log
import com.eastshine.screentranslator.screentranslate.Screen
import com.eastshine.screentranslator.screentranslate.model.TextElementWithSpeaker

/**
 * Identifies speakers using geometric heuristics based on screen position
 *
 * Heuristic:
 * - Text in bottom 20-30% of screen is considered dialogue body
 * - Text directly above dialogue is considered speaker name
 */
class GeometricHeuristicStrategy : SpeakerIdentificationStrategy {
    companion object {
        private const val DIALOGUE_ZONE_START_PERCENT = 0.70f // Bottom 30%
        private const val DIALOGUE_ZONE_END_PERCENT = 1f // Bottom 0%
        private const val SPEAKER_SEARCH_DISTANCE = 100 // pixels to search above dialogue
    }

    override fun identifySpeakers(screen: Screen): List<TextElementWithSpeaker> {
        val textElements = screen.textElements
        val screenHeight = screen.height

        if (textElements.isEmpty()) {
            return emptyList()
        }

        // Define dialogue zone (bottom 20-30% of screen)
        val dialogueZoneStart = (screenHeight * DIALOGUE_ZONE_START_PERCENT).toInt()
        val dialogueZoneEnd = (screenHeight * DIALOGUE_ZONE_END_PERCENT).toInt()

        Log.d(
            "GeometricHeuristicStrategy",
            "Screen height: $screenHeight, Dialogue zone: $dialogueZoneStart - $dialogueZoneEnd",
        )

        val textElementsInDialogueZone =
            textElements.filter {
                val centerY = it.boundingBox.centerY()
                centerY >= dialogueZoneStart && centerY <= dialogueZoneEnd
            }

        val speakerTextElement = textElementsInDialogueZone.minByOrNull { it.boundingBox.centerY() }

        return textElements.map { element ->
            TextElementWithSpeaker(
                textElement = element,
                speaker = speakerTextElement?.text,
            )
        }
    }
}
