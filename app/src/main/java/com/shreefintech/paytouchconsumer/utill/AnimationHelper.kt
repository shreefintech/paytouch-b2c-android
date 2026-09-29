package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator

object AnimationHelper {

    private const val ENTRANCE_TRANSLATE_DP = 24f
    private const val ROW_TRANSLATE_DP = 20f
    private const val ENTRANCE_DURATION_MS = 420L
    private const val ENTRANCE_DELAY_MS = 200L
    private const val STAGGER_STEP_MS = 150L
    private const val ROW_STAGGER_MS = 60L
    private const val ROW_DURATION_MS = 350L
    private const val REDUCE_MOTION_DURATION_MS = 300L

    fun animateEntrance(view: View, order: Int, stepMs: Long = STAGGER_STEP_MS) {
        if (isReduceMotion(view.context)) {
            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .setDuration(REDUCE_MOTION_DURATION_MS)
                .setStartDelay(ENTRANCE_DELAY_MS + order * stepMs)
                .setInterpolator(DecelerateInterpolator())
                .start()
            return
        }
        val dp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, ENTRANCE_TRANSLATE_DP, view.context.resources.displayMetrics
        )
        view.alpha = 0f
        view.translationY = dp
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ENTRANCE_DURATION_MS)
            .setStartDelay(ENTRANCE_DELAY_MS + order * stepMs)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    fun animateChildren(parent: ViewGroup, stepMs: Long = STAGGER_STEP_MS) {
        for (i in 0 until parent.childCount) {
            animateEntrance(parent.getChildAt(i), i, stepMs)
        }
    }

    fun animateListRowEntrance(view: View, index: Int) {
        if (index >= 8) return
        if (isReduceMotion(view.context)) {
            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .setDuration(250L)
                .setStartDelay(index * ROW_STAGGER_MS)
                .setInterpolator(DecelerateInterpolator())
                .start()
            return
        }
        val dp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, ROW_TRANSLATE_DP, view.context.resources.displayMetrics
        )
        view.alpha = 0f
        view.translationY = dp
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ROW_DURATION_MS)
            .setStartDelay(index * ROW_STAGGER_MS)
            .setInterpolator(DecelerateInterpolator(1.2f))
            .start()
    }

    fun isReduceMotion(context: Context): Boolean {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return scale == 0f
    }
}
