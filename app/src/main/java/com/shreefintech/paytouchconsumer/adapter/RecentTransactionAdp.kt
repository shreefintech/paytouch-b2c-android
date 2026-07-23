package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemRecentTransactionBinding
import com.shreefintech.paytouchconsumer.electricity.model.RecentTransactionItem

class RecentTransactionAdp(
    private val mContext: Context,
    private val mArrayList: ArrayList<RecentTransactionItem>
) : RecyclerView.Adapter<RecentTransactionAdp.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRecentTransactionBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentTransactionBinding.inflate(
            LayoutInflater.from(mContext), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mArrayList[position]
        bindItem(holder.binding, item)

        holder.binding.llHeader.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            mArrayList[pos].isExpanded = !mArrayList[pos].isExpanded
            notifyItemChanged(pos)
        }
    }

    private fun bindItem(binding: ItemRecentTransactionBinding, item: RecentTransactionItem) {
        with(binding) {
            val context = root.context
            ivCategoryIcon.setImageResource(item.categoryIconRes)
            tvCategoryName.text = item.categoryName
            tvCollapsedDate.text = item.date
            tvDetailDate.text = context.getString(R.string.labelDetailDate, item.date)
            tvDetailAmount.text = context.getString(R.string.labelDetailAmount, item.amount)
            tvDetailAccountNumber.text = context.getString(R.string.labelDetailAccountNumber, item.accountNumber)
            tvDetailReference.text = context.getString(R.string.labelDetailReference, item.reference)


            if (item.isExpanded) {

                llExpandedContent.visibility = View.VISIBLE
                ivChevron.rotation = 180f
            } else {
                llExpandedContent.visibility = View.GONE
                ivChevron.rotation = 0f
            }
        }
    }

    override fun getItemCount(): Int = mArrayList.size
}
