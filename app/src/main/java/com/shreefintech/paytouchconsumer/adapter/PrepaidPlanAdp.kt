package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemPrepaidPlanBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.retrofit.model.prepaid.PrepaidPlanItem

class PrepaidPlanAdp(
    private val mContext: Context,
    private val mArrayList: List<PrepaidPlanItem>
) : RecyclerView.Adapter<PrepaidPlanAdp.ViewHolder>() {

    var onClickItem: ((PrepaidPlanItem) -> Unit)? = null

    inner class ViewHolder(val binding: ItemPrepaidPlanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPrepaidPlanBinding.inflate(
            LayoutInflater.from(mContext), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mArrayList[position]

        LiquidGlassEffect.attach(
            targetView   = holder.binding.flCard,
            rootView     = holder.binding.root as ViewGroup,
            cornerRadius = holder.binding.root.resources.getDimensionPixelSize(R.dimen.glass_frem_radius),
            distortion   = 0f,
            blur         = holder.binding.root.resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
            strokeColor  = Color.argb(180, 213, 38, 98),
            strokeWidth  = 1,
            solidStroke  = true,
        )

        holder.binding.apply {
            tvPlanAmount.text = "₹${item.amount ?: 0}"
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

    override fun getItemCount(): Int = mArrayList.size

    private fun formatTalktime(talktime: Double?): String {
        if (talktime == null || talktime < 0) return "-"
        return "₹%.2f".format(talktime)
    }
}
