package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemPrepaidPlanBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidPlanItem

class PrepaidPlanAdp(
    private val mContext: Context,
    private val mArrayList: ArrayList<PrepaidPlanItem>
) : RecyclerView.Adapter<PrepaidPlanAdp.ViewHolder>() {

    var onClickItem: ((PrepaidPlanItem) -> Unit)? = null

    class ViewHolder(val binding: ItemPrepaidPlanBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var pendingDescriptionRunnable: Runnable? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPrepaidPlanBinding.inflate(
            LayoutInflater.from(mContext), parent, false
        )
        LiquidGlassEffect.attach(
            targetView   = binding.flCard,
            rootView     = binding.root as ViewGroup,
            cornerRadius = binding.root.resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = binding.root.resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor  = ContextCompat.getColor(mContext, R.color.glass_stroke_primary),
            strokeWidth  = 1,
            solidStroke  = true,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mArrayList[position]

        holder.binding.apply {
            tvPlanAmount.text = mContext.getString(R.string.fmtCurrencyAmount).format((item.amount ?: 0).toDouble())
            tvPlanValidity.text = item.validity ?: "--"
            tvPlanFooter.text = mContext.getString(
                R.string.fmtPlanTalktimeData,
                formatTalktime(item.talktime),
                if (item.data.isNullOrEmpty()) "--" else item.data
            )

            tvPlanDescription.maxLines = 3
            tvPlanDescription.text = item.description ?: "--"
            tvPlanDescription.setOnClickListener(null)
            tvPlanDescription.isClickable = false

            holder.pendingDescriptionRunnable?.let { tvPlanDescription.removeCallbacks(it) }
            val descriptionRunnable = Runnable {
                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@Runnable
                val w = tvPlanDescription.width - tvPlanDescription.paddingLeft - tvPlanDescription.paddingRight
                if (w <= 0) return@Runnable
                val fullText = mArrayList[pos].description ?: "--"
                val paint = tvPlanDescription.paint
                val fullLayout = StaticLayout.Builder.obtain(fullText, 0, fullText.length, paint, w).build()
                if (fullLayout.lineCount <= 3) return@Runnable

                val suffix = mContext.getString(R.string.labelViewMore)
                val suffixWidth = paint.measureText(suffix)
                val line3Start = fullLayout.getLineStart(2)
                val line3End = fullLayout.getLineVisibleEnd(2)
                val line3Text = fullText.substring(line3Start, line3End)
                val keepCount = paint.breakText(line3Text, true, w - suffixWidth, null)
                val truncateAt = line3Start + keepCount

                val display = fullText.substring(0, truncateAt) + suffix
                val spannable = SpannableString(display)
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(mContext, R.color.primary)),
                    truncateAt,
                    display.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                tvPlanDescription.maxLines = Int.MAX_VALUE
                tvPlanDescription.text = spannable
                tvPlanDescription.isClickable = true
                tvPlanDescription.setOnClickListener {
                    tvPlanDescription.text = fullText
                    tvPlanDescription.setOnClickListener(null)
                    tvPlanDescription.isClickable = false
                }
            }
            holder.pendingDescriptionRunnable = descriptionRunnable
            tvPlanDescription.post(descriptionRunnable)
        }

        holder.binding.root.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onClickItem?.invoke(mArrayList[pos])
            }
        }
    }

    fun updateList(items: List<PrepaidPlanItem>) {
        mArrayList.clear()
        mArrayList.addAll(items)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mArrayList.size

    private fun formatTalktime(talktime: Double?): String {
        if (talktime == null || talktime < 0) return "-"
        return mContext.getString(R.string.fmtCurrencyAmount).format(talktime)
    }
}
