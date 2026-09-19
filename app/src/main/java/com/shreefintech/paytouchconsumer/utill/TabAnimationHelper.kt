package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.shreefintech.paytouchconsumer.R

class TabAnimationHelper(
    private val context: Context,
    private val llPayBill: LinearLayout,
    private val llReport: LinearLayout,
    private val llStatus: LinearLayout,
    private val llSmsReceipt: LinearLayout
) {
    private val allTabs get() = listOf(llPayBill, llReport, llStatus, llSmsReceipt)

    fun resetAll() {
        val selectableBg = TypedValue().also {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, it, true)
        }.resourceId
        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        val blackColor = ContextCompat.getColor(context, R.color.black)
        allTabs.forEach { tab ->
            tab.setBackgroundResource(selectableBg)
            (tab.getChildAt(0) as? AppCompatImageView)?.imageTintList = ColorStateList.valueOf(primaryColor)
            (tab.getChildAt(1) as? AppCompatTextView)?.setTextColor(blackColor)
        }
    }

    fun selectPayBill() = selectTab(llPayBill)

    fun animateAndNavigate(tab: LinearLayout, navigate: () -> Unit) {
        resetAll()
        selectTab(tab)
        tab.postDelayed(navigate, 150)
    }

    private fun selectTab(tab: LinearLayout) {
        tab.background = ContextCompat.getDrawable(context, R.drawable.bg_toggle_selected)?.mutate()
        (tab.getChildAt(0) as? AppCompatImageView)?.imageTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
        (tab.getChildAt(1) as? AppCompatTextView)?.setTextColor(
            ContextCompat.getColor(context, R.color.white))
    }
}
