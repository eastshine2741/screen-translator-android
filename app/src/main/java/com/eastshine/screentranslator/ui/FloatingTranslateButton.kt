package com.eastshine.screentranslator.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager

class FloatingTranslateButton(
    context: Context,
    private val onTranslateClick: () -> Unit,
) : View(context) {
    private var windowParams: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null

    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val dragThreshold = (10f * resources.displayMetrics.density).toInt()

    private val backgroundPaint =
        Paint().apply {
            style = Paint.Style.FILL
            color = Color.argb(136, 33, 150, 243)
            isAntiAlias = true
        }

    private val iconPaint =
        Paint().apply {
            color = Color.WHITE
            textSize = 24f * resources.displayMetrics.density
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val size = (56 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = width / 2f

        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        val icon = "翻"
        val textBounds = Rect()
        iconPaint.getTextBounds(icon, 0, icon.length, textBounds)

        canvas.drawText(
            icon,
            centerX,
            centerY + textBounds.height() / 2f,
            iconPaint,
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = windowParams?.x?.toFloat() ?: 0f
                initialY = windowParams?.y?.toFloat() ?: 0f
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false

                backgroundPaint.alpha = 200
                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                if (Math.abs(deltaX) > dragThreshold || Math.abs(deltaY) > dragThreshold) {
                    isDragging = true

                    windowParams?.let { params ->
                        params.x = (initialX + deltaX).toInt()
                        params.y = (initialY + deltaY).toInt()
                        windowManager?.updateViewLayout(this, params)
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                backgroundPaint.alpha = 136
                invalidate()

                if (!isDragging) {
                    onTranslateClick()
                    Log.d(TAG, "Floating button tapped - triggering translation")
                }

                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                backgroundPaint.alpha = 136
                invalidate()
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setWindowParams(
        params: WindowManager.LayoutParams,
        wm: WindowManager,
    ) {
        this.windowParams = params
        this.windowManager = wm
    }

    companion object {
        private const val TAG = "FloatingTranslateButton"
    }
}
