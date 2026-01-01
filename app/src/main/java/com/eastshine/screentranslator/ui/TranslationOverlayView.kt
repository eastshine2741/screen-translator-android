package com.eastshine.screentranslator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import android.view.View
import com.eastshine.screentranslator.screentranslate.model.TranslatedElement

/**
 * 번역 결과를 표시하는 오버레이 뷰.
 * FLAG_NOT_TOUCHABLE로 설정되어 모든 터치를 하위 앱으로 전달함.
 */
class TranslationOverlayView(
    context: Context,
) : View(context) {
    private val translatedElements = mutableListOf<TranslatedElement>()

    private var sourceWidth: Int = 0
    private var sourceHeight: Int = 0

    private val backgroundPaint =
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val textPaint =
        Paint().apply {
            color = Color.WHITE
            textSize = 40f
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

    private val borderPaint =
        Paint().apply {
            style = Paint.Style.STROKE
            color = Color.argb(150, 0, 255, 0)
            strokeWidth = 2f
            isAntiAlias = true
        }

    fun updateTranslations(
        elements: List<TranslatedElement>,
        sourceWidth: Int,
        sourceHeight: Int,
    ) {
        this.translatedElements.clear()
        this.translatedElements.addAll(elements)
        this.sourceWidth = sourceWidth
        this.sourceHeight = sourceHeight

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (sourceWidth == 0 || sourceHeight == 0) return

        val scaleX = width.toFloat() / sourceWidth
        val scaleY = height.toFloat() / sourceHeight

        Log.d(
            "TranslationOverlayView",
            "onDraw() scaleX=$scaleX, scaleY=$scaleY, width=$width, sourceWidth=$sourceWidth, height=$height, sourceHeight=$sourceHeight",
        )
        for (element in translatedElements) {
            drawTranslatedElement(canvas, element, scaleX, scaleY)
        }
    }

    private fun drawTranslatedElement(
        canvas: Canvas,
        element: TranslatedElement,
        scaleX: Float,
        scaleY: Float,
    ) {
        val rect = transformRect(element.boundingBox, scaleX, scaleY)

        // 배경 그리기
        backgroundPaint.color = Color.argb(220, 0, 0, 0)
        canvas.drawRect(rect, backgroundPaint)

        // 테두리 그리기
        canvas.drawRect(rect, borderPaint)

        // 텍스트 그리기
        val text = element.translatedText
        val textBounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, textBounds)

        // 텍스트가 박스에 맞게 조정
        val maxWidth = rect.width() - 16f
        var adjustedTextSize = textPaint.textSize

        textPaint.textSize = adjustedTextSize
        while (textPaint.measureText(text) > maxWidth && adjustedTextSize > 20f) {
            adjustedTextSize -= 2f
            textPaint.textSize = adjustedTextSize
        }

        val textX = rect.left + 8f
        val textY = rect.top + (rect.height() + textBounds.height()) / 2f

        canvas.drawText(text, textX, textY, textPaint)

        // 텍스트 크기 원상복구
        textPaint.textSize = 40f
    }

    private fun transformRect(
        rect: Rect,
        scaleX: Float,
        scaleY: Float,
    ): RectF {
        return RectF(
            rect.left * scaleX,
            rect.top * scaleY,
            rect.right * scaleX,
            rect.bottom * scaleY,
        )
    }

    fun clear() {
        translatedElements.clear()
        invalidate()
    }
}
