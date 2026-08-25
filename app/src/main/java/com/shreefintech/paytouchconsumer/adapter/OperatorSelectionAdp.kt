package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemOperatorSelectionBinding
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.operator.model.OperatorSelectionItem

class OperatorSelectionAdp(
    private val context: Context,
    private val fullList: List<OperatorSelectionItem>
) : RecyclerView.Adapter<OperatorSelectionAdp.ViewHolder>() {

    var selectedId: String? = null
    var onSelectItem: ((OperatorSelectionItem) -> Unit)? = null

    private val displayList = fullList.toMutableList()

    fun filter(query: String) {
        displayList.clear()
        if (query.isBlank()) {
            displayList.addAll(fullList)
        } else {
            val lower = query.lowercase()
            fullList.filterTo(displayList) { it.name.lowercase().contains(lower) }
        }
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemOperatorSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OperatorSelectionItem) {

            LiquidGlassEffect.attach(
                targetView = binding.frameBg,
                rootView = binding.root as ViewGroup,
                cornerRadius = context.resources.getDimensionPixelSize(R.dimen.normal_card_radius),
                distortion = 0f,
                strokeColor = if (item.id == selectedId) ContextCompat.getColor(context, R.color.primary) else ContextCompat.getColor(context, R.color.white),
                strokeWidth = if (item.id == selectedId) 1 else 0,
                blur = context.resources.getDimensionPixelSize(R.dimen.glass_frem_blur),
                tintColor = ContextCompat.getColor(context, R.color.normal_card_bg_glass)
            )

            binding.tvName.text = item.name
            binding.ivSelected.visibility = if (item.id == selectedId) View.VISIBLE else View.GONE
            binding.llRoot.setOnClickListener { onSelectItem?.invoke(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOperatorSelectionBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(displayList[position])

    override fun getItemCount() = displayList.size
}
