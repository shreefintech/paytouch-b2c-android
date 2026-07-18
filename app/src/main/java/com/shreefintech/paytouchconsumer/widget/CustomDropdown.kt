package com.shreefintech.paytouchconsumer.widget


import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.shreefintech.paytouchconsumer.R
import kotlin.math.max

object CustomDropdown {

    fun showDropdown(
        activity: Activity,
        anchorView: View,
        arrowView: View?,
        textView: TextView?,
        items: List<String>,
        isIcon: Boolean = false,
        iconList: List<Int> = emptyList(),
        minWidth: Int? = null,
        onItemSelected: (String, Int) -> Unit
    ) {

        rotateArrow(arrowView, 0f, 180f)

        val listView = ListView(activity).apply {
            adapter = if (isIcon && iconList.size == items.size) {
                IconDropdownAdapter(activity, items, iconList)
            } else {
                ArrayAdapter(
                    activity,
                    android.R.layout.simple_list_item_1,
                    items
                )
            }
            divider = "#1A000000".toColorInt().toDrawable()
            dividerHeight = 1
            clipToOutline = true
            background = ContextCompat.getDrawable(activity, R.drawable.popup_rounded_bg)
        }

        val screenHeight = activity.resources.displayMetrics.heightPixels
        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)
        val anchorBottom = anchorLocation[1] + anchorView.height
        val anchorTop = anchorLocation[1]

        val spaceBelow = screenHeight - anchorBottom - 16
        val spaceAbove = anchorTop - 16

        val contentHeight = measureListViewHeight(listView, items.size)

        val showBelow = spaceBelow >= spaceAbove
        val availableSpace = if (showBelow) spaceBelow else spaceAbove
        val popupHeight = minOf(contentHeight, availableSpace)

        val width = if(minWidth != null) max(minWidth, anchorView.width) else anchorView.width
        val popup = PopupWindow(
            listView,
            width,
            popupHeight,
            true
        ).apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            elevation = 8f
            isOutsideTouchable = true
            setOnDismissListener {
                rotateArrow(arrowView, 180f, 0f)
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            textView?.text = items[position]
            onItemSelected(items[position], position)
            popup.dismiss()
        }

        if (showBelow) {
            popup.showAsDropDown(anchorView, 0, 8, Gravity.START)
        } else {
            popup.showAsDropDown(anchorView, 0, -(anchorView.height + popupHeight + 8), Gravity.START)
        }
    }

    // ─── Icon Adapter ──────────────────────────────────────────────────────────

    private class IconDropdownAdapter(
        context: Context,
        private val items: List<String>,
        private val iconRes: List<Int>
    ) : ArrayAdapter<String>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_dropdown_icon, parent, false)

            view.findViewById<TextView>(R.id.tvDropdownItem).text = items[position]
            view.findViewById<ImageView>(R.id.ivDropdownIcon)
                .setImageResource(iconRes[position])

            return view
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private fun measureListViewHeight(listView: ListView, itemCount: Int): Int {
        val adapter = listView.adapter ?: return 0
        var totalHeight = 0
        for (i in 0 until itemCount) {
            val item = adapter.getView(i, null, listView)
            item.measure(
                View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            totalHeight += item.measuredHeight
        }
        totalHeight += listView.dividerHeight * (itemCount - 1)
        return totalHeight
    }

    private fun rotateArrow(arrowView: View?, from: Float, to: Float) {
        arrowView ?: return
        ObjectAnimator.ofFloat(arrowView, "rotation", from, to).apply {
            duration = 200
        }.start()
    }
}