package com.shreefintech.paytouchconsumer.rewards

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.tan

class ShieldShineView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val shieldPath = Path().apply {
        moveTo(84f, 6f); lineTo(156f, 32f); lineTo(156f, 96f)
        cubicTo(156f, 140f, 124f, 172f, 84f, 186f)
        cubicTo(44f, 172f, 12f, 140f, 12f, 96f)
        lineTo(12f, 32f); close()
    }
    private val stripWidth = 64f
    private val stripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, stripWidth, 0f,
            intArrayOf(Color.TRANSPARENT, Color.argb(242, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
    }
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val skewX = tan(Math.toRadians(-20.0)).toFloat()
    private val ease = AccelerateDecelerateInterpolator()
    private var stripX = -90f
    private var animator: ValueAnimator? = null

    fun start(startDelayMs: Long = 800L) {
        stop()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3200L
            startDelay = startDelayMs
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val p = it.animatedValue as Float
                stripX = if (p < 0.55f) -90f + 320f * ease.getInterpolation(p / 0.55f) else 230f
                invalidate()
            }
            start()
        }
    }

    fun stop() {
        animator?.cancel(); animator = null; stripX = -90f; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.scale(width / 168f, height / 192f)
        canvas.save()
        canvas.translate(stripX, 0f)
        canvas.skew(skewX, 0f)
        canvas.drawRect(0f, -20f, stripWidth, 220f, stripPaint)
        canvas.restore()
        canvas.drawPath(shieldPath, maskPaint)
        canvas.restoreToCount(layer)
    }

    override fun onDetachedFromWindow() { animator?.cancel(); super.onDetachedFromWindow() }
}
