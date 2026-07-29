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
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemDropdownIconBinding
import kotlin.math.max

object CustomDropdown {

    fun showDropdown(
        activity: Activity,
        anchorView: View,
        arrowView: View?,
        textView: TextView?,
        items: List<String>,
        iconList: List<Int> = emptyList(),
        minWidth: Int? = null,
        onItemSelected: (String, Int) -> Unit
    ) {

        rotateArrow(arrowView, 0f, 180f)

        val listView = ListView(activity).apply {
            adapter = IconDropdownAdapter(activity, items, iconList)
            divider = "#1A000000".toColorInt().toDrawable()
            dividerHeight = 1
            clipToOutline = true
            background = ContextCompat.getDrawable(activity, R.drawable.bg_popup_rounded)
            // Hide scroll bars
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }



        val screenHeight = activity.resources.displayMetrics.heightPixels

        val anchorLocation = IntArray(2)
        anchorView.getLocationOnScreen(anchorLocation)

        val anchorTop = anchorLocation[1]
        val anchorBottom = anchorTop + anchorView.height

        val spaceBelow = screenHeight - anchorBottom - 16
        val spaceAbove = anchorTop - 16

        val contentHeight = measureListViewHeight(listView, items.size)

        val showBelow = spaceBelow >= spaceAbove
        val availableSpace = if (showBelow) spaceBelow else spaceAbove
        val popupHeight = minOf(contentHeight, availableSpace)

        val popupWidth =
            if (minWidth != null) max(minWidth, anchorView.width) else anchorView.width

        val popup = PopupWindow(
            listView,
            popupWidth,
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
            popup.showAsDropDown(
                anchorView,
                0,
                -(anchorView.height + popupHeight + 8),
                Gravity.START
            )
        }
    }

    /**
     * Single adapter used for both:
     * - Dropdown with icons
     * - Dropdown without icons
     */
    private class IconDropdownAdapter(
        context: Context,
        private val items: List<String>,
        private val iconRes: List<Int> = emptyList()
    ) : ArrayAdapter<String>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            val binding: ItemDropdownIconBinding

            if (convertView == null) {
                binding = ItemDropdownIconBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                binding.root.tag = binding
            } else {
                binding = convertView.tag as ItemDropdownIconBinding
            }

            binding.tvDropdownItem.text = items[position]

            if (position < iconRes.size && iconRes[position] != 0) {
                binding.ivDropdownIcon.visibility = View.VISIBLE
                binding.ivDropdownIcon.setImageResource(iconRes[position])
            } else {
                binding.ivDropdownIcon.visibility = View.GONE
            }

            return binding.root
        }
    }

    private fun measureListViewHeight(listView: ListView, itemCount: Int): Int {
        val adapter = listView.adapter ?: return 0
        if (itemCount == 0) return 0
        val sampleItem = adapter.getView(0, null, listView)
        sampleItem.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        return sampleItem.measuredHeight * itemCount + listView.dividerHeight * (itemCount - 1)
    }

    private fun rotateArrow(
        arrowView: View?,
        from: Float,
        to: Float
    ) {
        arrowView ?: return

        ObjectAnimator.ofFloat(
            arrowView,
            "rotation",
            from,
            to
        ).apply {
            duration = 200
        }.start()
    }
}