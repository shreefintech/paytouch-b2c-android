package com.shreefintech.paytouchconsumer.glass

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

// ─────────────────────────────────────────────────────────────────
// MARK: - LiquidButton
// Android equivalent of LiquidButton (LiquidComponents.swift)
// ─────────────────────────────────────────────────────────────────

/**
 * LiquidButton
 *
 * Mirrors iOS LiquidButton — a pill-shaped liquid-glass button.
 *
 * ── XML ──────────────────────────────────────────────────────────
 *  <com.shreefintech.dashboard.glass.LiquidButton
 *      android:layout_width="wrap_content"
 *      android:layout_height="wrap_content"
 *      app:button_label="Follow"
 *      app:button_icon="@drawable/ic_person_add" />
 *
 * ── Kotlin ───────────────────────────────────────────────────────
 *  val btn = LiquidButton(context).apply {
 *      label = "Follow"
 *      setOnClickListener { /* action */ }
 *  }
 *
 *   val root = window.decorView as ViewGroup
 *
 *   LiquidGlassEffect.attach( targetView = binding.flSignIn, rootView = root, cornerRadius = 40f, // 22dp × density distortion = 0.3f, blur = 18f, tintColor = Color.TRANSPARENT )
 *
 */
class LiquidButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var label: String = "Button"
        set(v) { field = v; invalidate() }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.WHITE
        textSize  = 38f
        typeface  = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val glassDrawable = LiquidGlassDrawable(
        context = context,
        cornerRadius = 999f,   // pill — mirrors liquidGlassPill()
    )
    private var scaleAnim: ValueAnimator? = null

    init {
        background   = glassDrawable
        clipToOutline = true
        setPadding(56, 32, 56, 32)
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f + (textPaint.textSize / 3f)
        canvas.drawText(label, cx, cy, textPaint)
    }

    // Press scale — mirrors LiquidPressStyle spring(response:0.25, dampingFraction:0.7)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> animateScale(0.95f)
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> animateScale(1.0f)
        }
        return super.onTouchEvent(event)
    }

    private fun animateScale(to: Float) {
        scaleAnim?.cancel()
        scaleAnim = ValueAnimator.ofFloat(scaleX, to).apply {
            duration     = 200
            interpolator = OvershootInterpolator(2f)
            addUpdateListener {
                val v = it.animatedValue as Float
                scaleX = v; scaleY = v
            }
            start()
        }
    }


}


// ─────────────────────────────────────────────────────────────────
// MARK: - LiquidToggle
// Android equivalent of LiquidToggle (LiquidComponents.swift)
// ─────────────────────────────────────────────────────────────────

/**
 * LiquidToggle
 *
 * Mirrors iOS LiquidToggle — glass track with a white thumb.
 *
 * ── XML ──────────────────────────────────────────────────────────
 *  <com.shreefintech.dashboard.glass.LiquidToggle
 *      android:id="@+id/toggle"
 *      android:layout_width="104dp"
 *      android:layout_height="60dp" />
 *
 * ── Kotlin ───────────────────────────────────────────────────────
 *  toggle.isOn = true
 *  toggle.onToggled = { isOn -> /* handle */ }
 *
 */
class LiquidToggle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var isOn: Boolean = false
        set(v) { field = v; animateThumb() }

    var onToggled: ((Boolean) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val trackBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 1f
        color       = Color.argb(102, 255, 255, 255)
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color      = Color.WHITE
        style      = Paint.Style.FILL
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    private var thumbX: Float = 0f
    private var thumbAnim: ValueAnimator? = null

    init {
        setOnClickListener {
            isOn = !isOn
            onToggled?.invoke(isOn)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        thumbX = thumbOffX(isOn)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f

        // Track fill — matches iOS Capsule fill logic
        val trackAlpha = if (isOn) 77 else 20   // 0.30 / 0.08 opacity
        trackPaint.color = Color.argb(trackAlpha, 255, 255, 255)
        canvas.drawRoundRect(0f, 0f, w, h, r, r, trackPaint)
        canvas.drawRoundRect(0f, 0f, w, h, r, r, trackBorderPaint)

        // Thumb — white circle with shadow
        val thumbR  = h * 0.37f     // 22/30 of track height (mirrors iOS)
        val thumbCy = h / 2f
        canvas.drawCircle(thumbX, thumbCy, thumbR, thumbPaint)
    }

    private fun thumbOffX(on: Boolean): Float {
        val h    = height.toFloat().coerceAtLeast(1f)
        val w    = width.toFloat()
        val tR   = h * 0.37f
        return if (on) w - tR - h * 0.1f else tR + h * 0.1f
    }

    // Spring animation — mirrors iOS .spring(response:0.35, dampingFraction:0.7)
    private fun animateThumb() {
        thumbAnim?.cancel()
        val target = thumbOffX(isOn)
        thumbAnim = ValueAnimator.ofFloat(thumbX, target).apply {
            duration     = 300
            interpolator = OvershootInterpolator(1.5f)
            addUpdateListener { thumbX = it.animatedValue as Float; invalidate() }
            start()
        }
    }
}
