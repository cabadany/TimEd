package com.example.timed_mobile

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class FaceBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val faceBounds = RectF()
    private var isFaceDetected = false
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 8.0f // Adjust thickness as needed
        color = ContextCompat.getColor(context, R.color.face_box_detected) // Green color
    }
    private val clearPaint = Paint().apply { // Used to clear previous drawings
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    // Call this method to update the overlay
    fun updateFaceBox(bounds: RectF?, detected: Boolean) {
        isFaceDetected = detected
        if (detected && bounds != null) {
            faceBounds.set(bounds)
        } else {
            faceBounds.setEmpty() // Clear bounds if not detected
        }
        // Request redraw
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Clear the previous drawing before drawing the new one
        // canvas.drawPaint(clearPaint) // Optional: uncomment if you see artifacts

        if (isFaceDetected && !faceBounds.isEmpty) {
            // Draw the green box
            canvas.drawRect(faceBounds, boxPaint)
        }
        // No need to draw anything if no face is detected
    }
}