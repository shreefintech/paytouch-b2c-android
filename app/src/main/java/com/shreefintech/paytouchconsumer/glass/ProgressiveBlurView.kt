package com.shreefintech.paytouchconsumer.glass

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * ProgressiveBlurView
 *
 * Android equivalent of ProgressiveBlur.swift.
 *
 * Applies a directional blur that increases in intensity toward one edge,
 * identical to the iOS "Progressive blur" modifier that uses stacked
 * ultraThinMaterial layers masked by a LinearGradient.
 *
 * Technique: captures a software-rendered bitmap of sibling views,
 * blurs it at N increasing strengths, and composites them through
 * alpha-gradient masks — matching the iOS `steps` + ease-in curve.
 *
 * ── XML Usage ────────────────────────────────────────────────────
 *
 *  <com.shreefintech.dashboard.glass.ProgressiveBlurView
 *      android:id="@+id/progressiveBlur"
 *      android:layout_width="match_parent"
 *      android:layout_height="80dp"
 *      android:layout_gravity="bottom"
 *      app:blur_direction="up"
 *      app:max_blur_radius="20"
 *      app:blur_steps="8" />
 *
 * ── Kotlin Usage ─────────────────────────────────────────────────
 *
 *  progressiveBlurView.direction    = ProgressiveBlurView.Direction.UP
 *  progressiveBlurView.maxBlurRadius = 20f
 *  progressiveBlurView.steps         = 8
 *
 */
class ProgressiveBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    enum class Direction {
        UP, DOWN, LEFT, RIGHT;

        /** Gradient start alpha (opaque side) */
        fun gradientColors(fraction: Float): IntArray {
            val alpha = (fraction * 255).toInt().coerceIn(0, 255)
            return when (this) {
                UP    -> intArrayOf(Color.argb(alpha, 0, 0, 0), Color.TRANSPARENT)
                DOWN  -> intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0))
                LEFT  -> intArrayOf(Color.argb(alpha, 0, 0, 0), Color.TRANSPARENT)
                RIGHT -> intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0))
            }
        }
    }

    var direction:     Direction = Direction.DOWN
    var maxBlurRadius: Float     = 20f   // kept for API compat — drives opacity now
    var steps:         Int       = 8     // kept for API compat

    private val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * No-op — backdrop capture removed to prevent activity lag.
     * Progressive blur is now faked with stacked gradient overlays.
     */
    @Suppress("UNUSED_PARAMETER")
    fun setBackdrop(source: Bitmap) { /* no-op */ }

    // ── Draw ─────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Stack `steps` gradient layers, each slightly more opaque,
        // using an ease-in curve — same visual intent as the original.
        for (i in 1..steps) {
            val fraction  = i.toFloat() / steps.toFloat()
            val eased     = fraction * fraction              // ease-in curve
            val alpha     = ((eased * (maxBlurRadius / 25f)) * 120f)
                                .toInt().coerceIn(0, 255)   // scale to 0-120 max

            val colors = when (direction) {
                Direction.UP    -> intArrayOf(Color.argb(alpha, 255,255,255), Color.TRANSPARENT)
                Direction.DOWN  -> intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 255,255,255))
                Direction.LEFT  -> intArrayOf(Color.argb(alpha, 255,255,255), Color.TRANSPARENT)
                Direction.RIGHT -> intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 255,255,255))
            }

            gradPaint.shader = when (direction) {
                Direction.UP    -> LinearGradient(0f, h, 0f, 0f, colors, null, Shader.TileMode.CLAMP)
                Direction.DOWN  -> LinearGradient(0f, 0f, 0f, h, colors, null, Shader.TileMode.CLAMP)
                Direction.LEFT  -> LinearGradient(w, 0f, 0f, 0f, colors, null, Shader.TileMode.CLAMP)
                Direction.RIGHT -> LinearGradient(0f, 0f, w, 0f, colors, null, Shader.TileMode.CLAMP)
            }
            gradPaint.alpha = 255 / steps
            canvas.drawRect(0f, 0f, w, h, gradPaint)
        }
    }
}
