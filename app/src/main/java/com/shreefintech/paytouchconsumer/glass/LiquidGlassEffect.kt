package com.shreefintech.paytouchconsumer.glass

import android.graphics.Color
import android.view.View
import android.view.ViewGroup

class LiquidGlassEffect private constructor(
    private val targetView: View,
    private val rootView:   ViewGroup,
    private val drawable:   LiquidGlassDrawable,
) {
    var captureOnce: Boolean = false

    init {
        targetView.background = drawable
    }

    fun detach() {
        targetView.background = null
    }

    init {
        targetView.background = drawable
        targetView.clipToOutline = true
        targetView.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, drawable.cornerRadius)
            }
        }
    }

    fun updateTint(color: Int) {
        drawable.tintColor = color
        targetView.invalidate()
    }

    companion object {
        fun attach(
            targetView:   View,
            rootView:     ViewGroup,
            cornerRadius: Int     = 22,
            distortion:   Float   = 0.3f,
            blur:         Int     = 12,
            tintColor:    Int     = Color.TRANSPARENT,
            strokeWidth:  Int     = 1,
            strokeColor:  Int     = Color.argb(120, 255, 255, 255),
            solidStroke:  Boolean = true,
            captureOnce:  Boolean = true,
        ): LiquidGlassEffect {
            val drawable = LiquidGlassDrawable(
                context      = targetView.context,
                cornerRadius = cornerRadius.toFloat(),
                tintColor    = tintColor,
            ).also {
                it.strokeWidth = strokeWidth.toFloat()
                it.strokeColor = strokeColor
                it.solidStroke = solidStroke
            }
            return LiquidGlassEffect(targetView, rootView, drawable)
                .also { it.captureOnce = captureOnce }
        }
    }
}