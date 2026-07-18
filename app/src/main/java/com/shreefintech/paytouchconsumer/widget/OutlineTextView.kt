package com.shreefintech.paytouchconsumer.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.shreefintech.paytouchconsumer.R

class OutlineTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AppCompatTextView(context, attrs) {

    var strokeColor: Int   = Color.WHITE
    var strokeWidth: Float = 0f

    init {
        context.obtainStyledAttributes(attrs, R.styleable.OutlineTextView).apply {
            strokeColor = getColor(R.styleable.OutlineTextView_stroke_color, Color.WHITE)
            strokeWidth = getDimension(R.styleable.OutlineTextView_stroke_width, 0f)
            recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (strokeWidth > 0f) {
            val textColor = currentTextColor
            setTextColor(strokeColor)
            paint.style       = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.strokeJoin  = Paint.Join.ROUND
            paint.strokeCap   = Paint.Cap.ROUND
            super.onDraw(canvas)

            setTextColor(textColor)
            paint.style       = Paint.Style.FILL
            paint.strokeWidth = 0f
        }
        super.onDraw(canvas)
    }
}