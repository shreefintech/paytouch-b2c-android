package com.shreefintech.paytouchconsumer.rewards

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private class Piece(
        val xFrac: Float, val w: Float, val h: Float, val color: Int,
        val delay: Long, val dur: Long, val spin: Float, val round: Boolean
    )

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pieces: List<Piece> = emptyList()
    private var elapsed = 0L
    private var animator: ValueAnimator? = null

    fun burst(colors: IntArray, count: Int = 26, seed: Int = 7) {
        stop()
        val r = Random(seed)
        pieces = List(count) { i ->
            Piece(
                xFrac = r.nextInt(2, 97) / 100f,
                w = listOf(6f, 8f, 10f).random(r) * dp,
                h = listOf(10f, 14f, 6f).random(r) * dp,
                color = colors[r.nextInt(colors.size)],
                delay = r.nextLong(0, 1400),
                dur = r.nextLong(2600, 4200),
                spin = 760f,
                round = i % 5 == 0
            )
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 5600L
            interpolator = LinearInterpolator()
            addUpdateListener { elapsed = it.currentPlayTime; invalidate() }
            start()
        }
    }

    fun stop() {
        animator?.cancel(); animator = null; pieces = emptyList(); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fallTo = height + 40 * dp
        for (p in pieces) {
            val t = ((elapsed - p.delay).toFloat() / p.dur)
            if (t <= 0f || t >= 1f) continue
            val eased = 1f - (1f - t) * (1f - t) * 0.4f - 0.6f * (1f - t)
            val y = -40 * dp + (fallTo + 40 * dp) * eased.coerceIn(0f, 1f)
            val x = width * p.xFrac
            paint.color = p.color
            paint.alpha = when {
                t < 0.08f -> (255 * t / 0.08f).toInt()
                t > 0.85f -> (255 * (1f - t) / 0.15f).toInt()
                else -> 255
            }
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(p.spin * t)
            if (p.round) canvas.drawCircle(0f, 0f, p.w / 2f, paint)
            else canvas.drawRoundRect(-p.w / 2, -p.h / 2, p.w / 2, p.h / 2, 2 * dp, 2 * dp, paint)
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() { animator?.cancel(); super.onDetachedFromWindow() }
}
