package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.google.android.material.card.MaterialCardView
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
        val white = ContextCompat.getColor(context, R.color.white)
        val black = ContextCompat.getColor(context, R.color.black)
        allTabs.forEach { tab ->
            innerCard(tab)?.setCardBackgroundColor(white)
            innerText(tab)?.setTextColor(black)
        }
    }

    fun selectPayBill() = selectTab(llPayBill)

    fun animateAndNavigate(tab: LinearLayout, navigate: () -> Unit) {
        resetAll()
        selectTab(tab)
        tab.postDelayed(navigate, 150)
    }

    private fun selectTab(tab: LinearLayout) {
        innerCard(tab)?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.primary_light))
        innerText(tab)?.setTextColor(ContextCompat.getColor(context, R.color.primary))
    }

    private fun innerCard(tab: LinearLayout): MaterialCardView? =
        tab.children.filterIsInstance<MaterialCardView>().firstOrNull()

    private fun innerText(tab: LinearLayout): AppCompatTextView? =
        innerCard(tab)
            ?.children?.filterIsInstance<LinearLayout>()?.firstOrNull()
            ?.children?.filterIsInstance<AppCompatTextView>()?.firstOrNull()
}
