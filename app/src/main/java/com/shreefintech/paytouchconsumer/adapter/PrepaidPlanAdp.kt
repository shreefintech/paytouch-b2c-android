package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.graphics.Color
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

    inner class ViewHolder(val binding: ItemPrepaidPlanBinding) :
        RecyclerView.ViewHolder(binding.root)

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
            tvPlanDescription.text = item.description ?: "--"
            tvPlanFooter.text = mContext.getString(
                R.string.fmtPlanTalktimeData,
                formatTalktime(item.talktime),
                 if(item.data.isNullOrEmpty()) "--" else item.data
            )
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
