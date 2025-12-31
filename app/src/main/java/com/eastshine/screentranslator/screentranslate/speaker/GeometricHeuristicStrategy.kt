package com.eastshine.screentranslator.screentranslate.speaker

import android.util.Log
import com.eastshine.screentranslator.ocr.model.TextElement
import com.eastshine.screentranslator.screentranslate.Screen
import com.eastshine.screentranslator.screentranslate.model.TextElementWithSpeaker
import kotlin.math.abs

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
        private const val DIALOGUE_ZONE_END_PERCENT = 0.80f // Bottom 20%
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

        // Separate elements into dialogue candidates and potential speakers
        val potentialSpeakers =
            textElements.filter { element ->
                val centerY = element.boundingBox.centerY()
                centerY < dialogueZoneStart
            }

        return textElements.map { element ->
            val speaker =
                if (isDialogueCandidate(element, dialogueZoneStart, dialogueZoneEnd)) {
                    findSpeakerAbove(element, potentialSpeakers)
                } else {
                    null
                }

            TextElementWithSpeaker(
                textElement = element,
                speaker = speaker,
            )
        }
    }

    private fun isDialogueCandidate(
        element: TextElement,
        dialogueZoneStart: Int,
        dialogueZoneEnd: Int,
    ): Boolean {
        val centerY = element.boundingBox.centerY()
        return centerY >= dialogueZoneStart && centerY <= dialogueZoneEnd
    }

    private fun findSpeakerAbove(
        dialogueElement: TextElement,
        potentialSpeakers: List<TextElement>,
    ): String? {
        val dialogueTop = dialogueElement.boundingBox.top
        val dialogueCenterX = dialogueElement.boundingBox.centerX()

        // Find speakers within search distance above dialogue
        val nearbyAbove =
            potentialSpeakers.filter { speaker ->
                val speakerBottom = speaker.boundingBox.bottom
                val verticalDistance = dialogueTop - speakerBottom

                // Must be above and within search distance
                verticalDistance in 0..SPEAKER_SEARCH_DISTANCE
            }

        if (nearbyAbove.isEmpty()) {
            return null
        }

        // Find closest speaker horizontally aligned
        return nearbyAbove
            .minByOrNull { speaker ->
                val horizontalDistance =
                    abs(speaker.boundingBox.centerX() - dialogueCenterX)
                horizontalDistance
            }?.text
    }
}
