package com.shreefintech.paytouchconsumer.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.shreefintech.paytouchconsumer.R

/**
 * Dims a full-screen camera preview everywhere except a centred circle and outlines it with a
 * thin stroke. Circle diameter = [DIAMETER_RATIO] × view width — layouts that place content
 * relative to the circle must use the same ratio (see activity_selfie_capture.xml `spCircle`).
 */
class FaceCircleOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        const val DIAMETER_RATIO = 0.75f
    }

    private var cx = 0f
    private var cy = 0f
    private var radius = 0f
    private val scrimPath = Path().apply { fillType = Path.FillType.EVEN_ODD }

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.selfie_overlay_scrim)
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density   // 1dp
        color = ContextCompat.getColor(context, R.color.white)
    }

    fun setStrokeColor(@ColorInt color: Int) {
        if (strokePaint.color == color) return
        strokePaint.color = color
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cx = w / 2f
        cy = h / 2f
        radius = w * DIAMETER_RATIO / 2f

        scrimPath.reset()
        scrimPath.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
        scrimPath.addCircle(cx, cy, radius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(scrimPath, scrimPaint)
        canvas.drawCircle(cx, cy, radius, strokePaint)
    }
}
