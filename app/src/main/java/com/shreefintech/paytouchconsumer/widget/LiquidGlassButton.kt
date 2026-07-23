package com.shreefintech.paytouchconsumer.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect

class LiquidGlassButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private var glassEffect: LiquidGlassEffect? = null

    private var normalTint   = Color.TRANSPARENT
    var pressedTint  = Color.TRANSPARENT
    private var cornerRadius = 25f

    private var outlineTextView: OutlineTextView? = null
    private var imageViews: List<ImageView>       = emptyList()
    private var textViews: List<TextView>         = emptyList()

    // ← store original colors to restore on release
    private var originalTextColors: MutableMap<TextView, Int>        = mutableMapOf()
    private var originalImageTints: MutableMap<ImageView, Int?>       = mutableMapOf()
    private var originalStrokeColor: Int                              = Color.TRANSPARENT
    private var originalOutlineTextColor: Int                         = Color.TRANSPARENT

    init {
        context.obtainStyledAttributes(attrs, R.styleable.LiquidGlassButton).apply {
            normalTint   = getColor(R.styleable.LiquidGlassButton_normalTint, Color.TRANSPARENT)
            pressedTint  = getColor(R.styleable.LiquidGlassButton_pressedTint, Color.TRANSPARENT)
            cornerRadius = getDimension(R.styleable.LiquidGlassButton_glassCornerRadius, 50f)
            recycle()
        }
        isClickable = true
        isFocusable = true
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        outlineTextView = findOutlineTextViewRecursive(this)
        imageViews      = findImageViewsRecursive(this)
        textViews       = findTextViewsRecursive(this)

        // ← snapshot original colors right after inflation
        outlineTextView?.let {
            originalOutlineTextColor = it.currentTextColor
            originalStrokeColor      = it.strokeColor
        }
        textViews.forEach { tv ->
            originalTextColors[tv] = tv.currentTextColor
        }
        imageViews.forEach { iv ->
            // imageTintList is the XML app:tint value
            originalImageTints[iv] = iv.imageTintList?.defaultColor
        }
    }

    // ─── Recursive finders ───────────────────────────────────────────────────

    private fun findOutlineTextViewRecursive(group: ViewGroup): OutlineTextView? {
        for (child in group.children) {
            if (child is OutlineTextView) return child
            if (child is ViewGroup) {
                val found = findOutlineTextViewRecursive(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findImageViewsRecursive(group: ViewGroup): List<ImageView> {
        val result = mutableListOf<ImageView>()
        for (child in group.children) {
            if (child is ImageView)      result.add(child)
            else if (child is ViewGroup) result.addAll(findImageViewsRecursive(child))
        }
        return result
    }

    private fun findTextViewsRecursive(group: ViewGroup): List<TextView> {
        val result = mutableListOf<TextView>()
        for (child in group.children) {
            if (child is OutlineTextView)  { /* skip */ }
            else if (child is TextView)    result.add(child)
            else if (child is ViewGroup)   result.addAll(findTextViewsRecursive(child))
        }
        return result
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    fun attach(rootView: ViewGroup) {
        glassEffect = LiquidGlassEffect.attach(
            targetView   = this,
            rootView     = rootView,
            cornerRadius = cornerRadius.toInt(),
            tintColor    = normalTint,
        )
    }

    // ─── Touch ───────────────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                setTint(pressedTint)
                setChildrenPressed(true)
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                setTint(normalTint)
                setChildrenPressed(false)
            }
        }
        return super.onTouchEvent(event)
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun setTint(color: Int) {
        glassEffect?.updateTint(color)
        invalidate()
    }

    private fun setChildrenPressed(pressed: Boolean) {
        // OutlineTextView
        outlineTextView?.apply {
            setTextColor(if (pressed) Color.WHITE else originalOutlineTextColor)
            strokeColor = if (pressed) Color.WHITE else originalStrokeColor
            invalidate()
        }

        // plain TextViews — restore exact original color from snapshot
        textViews.forEach { tv ->
            tv.setTextColor(
                if (pressed) Color.WHITE
                else originalTextColors[tv] ?: Color.TRANSPARENT
            )
        }

        // ImageViews — restore exact original tint from snapshot
        imageViews.forEach { iv ->
            if (pressed) {
                iv.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            } else {
                iv.clearColorFilter()
                // restore XML app:tint if it was set
                originalImageTints[iv]?.let { originalColor ->
                    iv.imageTintList = ColorStateList.valueOf(originalColor)
                }
            }
        }
    }
}