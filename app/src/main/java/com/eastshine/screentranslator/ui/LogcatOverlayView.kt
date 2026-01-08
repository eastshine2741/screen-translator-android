package com.eastshine.screentranslator.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Displays logcat output with scrollable TextView
 */
class LogcatOverlayView(
    context: Context,
) : LinearLayout(context) {
    private val headerView: TextView
    private val scrollView: ScrollView
    private val textView: TextView

    private var windowParams: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null

    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    private val dragThreshold = (10f * resources.displayMetrics.density).toInt()
    private val maxLines = 500 // Keep last 500 lines

    init {
        orientation = VERTICAL
        // Semi-transparent black background
        setBackgroundColor(Color.argb(100, 0, 0, 0))

        // Create header for dragging
        headerView =
            TextView(context).apply {
                text = "LOGCAT (Drag here)"
                textSize = 12f
                setTextColor(Color.YELLOW)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
                setBackgroundColor(Color.argb(150, 0, 0, 0))
            }

        // Create TextView for log content
        textView =
            TextView(context).apply {
                textSize = 8f
                setTextColor(Color.GREEN)
                typeface = Typeface.MONOSPACE
                setPadding(16, 16, 16, 16)
            }

        // Create ScrollView
        scrollView =
            ScrollView(context).apply {
                addView(textView)
            }

        // Add header and scrollview
        addView(
            headerView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(
            scrollView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f, // weight = 1 to fill remaining space
            ),
        )

        // Setup drag on header
        headerView.setOnTouchListener { _, event ->
            handleDrag(event)
        }
    }

    private fun handleDrag(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = windowParams?.x?.toFloat() ?: 0f
                initialY = windowParams?.y?.toFloat() ?: 0f
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
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

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return false
    }

    fun setWindowParams(
        params: WindowManager.LayoutParams,
        wm: WindowManager,
    ) {
        this.windowParams = params
        this.windowManager = wm
    }

    /**
     * Appends a new log line
     */
    fun appendLog(line: String) {
        post {
            val currentText = textView.text.toString()
            val lines = currentText.split("\n").toMutableList()

            // Add new line
            lines.add(line)

            // Keep only last maxLines
            if (lines.size > maxLines) {
                lines.removeAt(0)
            }

            textView.text = lines.joinToString("\n")

            // Auto-scroll to bottom
            scrollView.post {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    /**
     * Clears all logs
     */
    fun clear() {
        post {
            textView.text = ""
        }
    }
}
