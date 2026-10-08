package com.shreefintech.paytouchconsumer.rewards
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.shreefintech.paytouchconsumer.R

/** Gold I → Platinum countdown ring: light pink track + pink arc, 10dp stroke. */
class ProgressRingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val stroke = 10f * resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = stroke
        color = ContextCompat.getColor(context, R.color.primary_light)
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.primary)
    }
    private val oval = RectF()
    private var progress = 0f
    private var animator: ValueAnimator? = null

    fun setProgress(target: Float, animate: Boolean) {
        animator?.cancel()
        val to = target.coerceIn(0f, 1f)
        if (!animate) { progress = to; invalidate(); return }
        animator = ValueAnimator.ofFloat(0f, to).apply {
            duration = 1400L
            startDelay = 300L
            interpolator = DecelerateInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val r = minOf(w, h) / 2f - stroke / 2f
        oval.set(w / 2f - r, h / 2f - r, w / 2f + r, h / 2f + r)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawOval(oval, track)
        if (progress > 0f) canvas.drawArc(oval, -90f, 360f * progress, false, arc)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}
