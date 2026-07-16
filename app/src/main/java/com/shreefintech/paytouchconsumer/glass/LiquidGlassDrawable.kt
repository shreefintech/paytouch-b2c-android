package com.shreefintech.paytouchconsumer.glass

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable

class LiquidGlassDrawable(
    @Suppress("UNUSED_PARAMETER") context: Context,
    cornerRadius: Float = 22f,
    var tintColor: Int = Color.argb(40, 255, 255, 255),
) : Drawable() {

    var strokeWidth: Float = 1f
        set(value) { field = value; invalidateSelf() }

    var strokeColor: Int = Color.argb(120, 255, 255, 255)
        set(value) { field = value; invalidateSelf() }

    var solidStroke: Boolean = true
        set(value) { field = value; invalidateSelf() }

    var cornerRadius: Float = cornerRadius
        set(value) { field = value; invalidateSelf() }

    // Unused — kept for API compat
    var distortionStrength: Float = 0.3f
    var blurRadius: Float = 12f
    fun setBackdrop(source: Bitmap) {}

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val r    = cornerRadius
        val rect = RectF(bounds)

        // White tint fill — glass base
        fillPaint.color = Color.argb(45, 255, 255, 255)
        canvas.drawRoundRect(rect, r, r, fillPaint)

        // Optional color tint
        if (Color.alpha(tintColor) > 0) {
            fillPaint.color = tintColor
            canvas.drawRoundRect(rect, r, r, fillPaint)
        }

        // Specular top highlight
        specularPaint.shader = LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom * 0.5f,
            intArrayOf(Color.argb(60, 255, 255, 255), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, r, r, specularPaint)

        // Border
        val half = strokeWidth / 2f
        val borderRect = RectF(rect.left + half, rect.top + half, rect.right - half, rect.bottom - half)
        borderPaint.strokeWidth = strokeWidth
        borderPaint.color = strokeColor
        canvas.drawRoundRect(borderRect, r, r, borderPaint)
    }

    override fun setAlpha(alpha: Int) { fillPaint.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { fillPaint.colorFilter = cf }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}