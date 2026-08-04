package com.shreefintech.paytouchconsumer.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.ItemRecentTransactionBinding
import com.shreefintech.paytouchconsumer.transactions.model.RecentTransactionItem

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

            val statusText = item.status.replaceFirstChar { it.uppercaseChar() }
            val textColor = when (item.status.lowercase()) {
                "success" -> ContextCompat.getColor(context, R.color.toast_text_success)
                "failed"  -> ContextCompat.getColor(context, R.color.form_wizard_reject)
                else      -> ContextCompat.getColor(context, R.color.orange)
            }
            tvStatus.text = context.getString(R.string.labelStatusBullet, statusText)
            tvStatus.setTextColor(textColor)

            tvDetailAmount.text = context.getString(R.string.labelDetailAmount, item.amount)
            val accountLabelRes = if (item.isMobileCategory) R.string.labelDetailMobileNo else R.string.labelDetailConsumerNo
            tvDetailAccountNumber.text = context.getString(accountLabelRes, item.accountNumber)
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

    fun updateList(items: List<RecentTransactionItem>) {
        mArrayList.clear()
        mArrayList.addAll(items)
        notifyDataSetChanged()
    }

    fun appendList(items: List<RecentTransactionItem>) {
        val insertStart = mArrayList.size
        mArrayList.addAll(items)
        notifyItemRangeInserted(insertStart, items.size)
    }

    override fun getItemCount(): Int = mArrayList.size
}
